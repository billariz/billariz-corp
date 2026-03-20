/**
 * Copyright (C) 2025 Uppli SAS — Billariz
 *
 * This file is part of Billariz, licensed under the GNU Affero General
 * Public License v3.0 (AGPL-3.0). You may use, modify and distribute
 * this software under the terms of the AGPL-3.0.
 *
 * For commercial use without AGPL obligations, contact:
 * contact@billariz.com | contact@uppli.fr
 * https://billariz.com
 */

package com.billariz.corp.app.event;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.User;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MonitoringObject;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.database.repository.UserRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.JournalUtils;
import com.billariz.corp.notifier.UserNotifier;
import com.billariz.corp.notifier.exception.NotifierException;
import com.billariz.corp.notifier.user.UserCaptor;
import com.billariz.corp.notifier.utils.PasswordGenerator;
import com.billariz.corp.provider.AuthentificationProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCaptorImpl implements UserCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final LauncherQueue                       launcherQueue;

    private final JournalUtils                 journalUtils;

    private final UserRepository                      userRepository;

    private final PasswordGenerator                   passwordGenerator;

    private final AuthentificationProvider            authProvider;

    private final UserNotifier                        userNotifier;

    private final Locale locale = LocaleContextHolder.getLocale();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPreRemove(User user)
    {
        var oldStatus = getOldValue(user);
        if(!oldStatus.equals("INACTIVE")) 
                throw new IllegalArgumentException(
                    journalUtils.getMessage(
                        "USER_STATUS_NOT_COMPLAINT",new Object[]{
                        user.getUserName()},
                        locale)
                );
       
        var newStatus = "DELETED";
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.USER);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(billProcessRules -> checkConditions(billProcessRules, user)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                user.setStatus(finalStatus.get());
                user.setCascaded(true);
            }
            ruleActive.set(rule);
        }
        else
        {
            throw new IllegalArgumentException(
                journalUtils.getMessage(
                    "NO_RULES_TO_DELETE_USER",new Object[]{
                    user.getUserName()},
                    locale)
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersistOrUpdate(User user)
    {
        if(isUserExist(user) && user.getId() == null)
                throw new IllegalArgumentException(
                    journalUtils.getMessage(
                        "USER_NAME_ALREADY_EXIST",new Object[]{
                        user.getUserName()},
                        locale)
                );
        var oldStatus = getOldValue(user);
        var newStatus = Objects.toString(user.getStatus(), "");
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.USER);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(billProcessRules -> checkConditions(billProcessRules, user)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                user.setStatus(finalStatus.get());
            }
            ruleActive.set(rule);
        }
        else
        {
            ruleActive.set(Optional.empty());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(User user)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var rule = ruleActive.get();
        if (rule == null || !rule.isPresent()) {
            return;
        }

        if (!rule.isEmpty() && rule.get().getActivityType() != null)
        {
                var activity = getAction(rule.get().getActivityType());
                if (activity.equals(rule.get().getActivityType()))
                {
                    launcherQueue.createActivityEvent(rule.get().getActivityType(),
                                user.getId(),  ObjectType.USER,  "systemAgent");
                    journalUtils.addJournal(ObjectType.USER, user.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"USER",rule.toString()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
                }
                else
                {
                    launchAction(user, activity);
                    journalUtils.addJournal(ObjectType.USER, user.getId(),
                            "LAUNCH_ACTIVITY_ON_OBJECT", 
                            new Object[]{activity, "USER", user.getId()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
                }
        }
        else
            updateUser(user);
        
        ruleActive.remove();
    }

    private String getAction(String actType)
    {
        int indexDeuxPoints = actType.indexOf(":");
        if (indexDeuxPoints != -1)
        {
            return actType.substring(indexDeuxPoints + 1);
        }
        else
        {
            return actType;
        }
    }

    private boolean isUserExist(User user){
        
        return !userRepository.findByUserName(user.getUserName()).isEmpty();
    }

    private void launchAction(User user, String action)
    {
        switch (action)
        {
        case "CREATE_USER":
            createUser(user);
            break;
        case "DELETE_USER":
            deleteUser(user);
            break;
        case "DISABLE_USER":
            disableUser(user);
            break;
        case "ENABLE_USER":
            enableUser(user);
            break;
        default:
            break;
        }
    }

    private void createUser(User user)
    {
        var userPassword = passwordGenerator.generate();

        try
        {
            authProvider.createUser(user, userPassword);
            userNotifier.newUser(user, userPassword);
        }
        catch (ProviderException e)
        {
            throw new InternalError(journalUtils.getMessage(
                "UNABLE_CREATE_USER",new Object[]{
                user.getUserName(), e},
                locale));
        }
        catch (NotifierException e)
        {
            throw new InternalError(journalUtils.getMessage(
                "UNABLE_NOTIFY_USER",new Object[]{
                user.getUserName(), e},
                locale));
        }
        log.info("User {} have been created", user);
    }

    private void deleteUser(User user)
    {
        try
        {
            authProvider.deleteUser(user);
        }
        catch (ProviderException e)
        {
            throw new InternalError(journalUtils.getMessage(
                "UNABLE_DELETE_USER",new Object[]{
                user.getUserName(), e},
                locale));
        }
        log.info("User {} have been deleted", user);
    }

    private void disableUser(User user)
    {
        try
        {
            authProvider.disableUser(user);
        }
        catch (ProviderException e)
        {
            throw new InternalError(journalUtils.getMessage(
                "UNABLE_DISABLE_USER",new Object[]{
                user.getUserName(), e},
                locale));
        }
        log.info("User {} have been disabled", user);
    }

    private void enableUser(User user)
    {
        try
        {
            authProvider.enableUser(user);
        }
        catch (ProviderException e)
        {
            throw new InternalError(journalUtils.getMessage(
                "UNABLE_ENABLE_USER",new Object[]{
                user.getUserName(), e},
                locale));
        }
        log.info("User {} have been enabled", user);
    }

    private void updateUser(User user)
    {
        try
        {
            authProvider.updateUser(user);
        }
        catch (ProviderException e)
        {
            throw new InternalError(journalUtils.getMessage(
                "UNABLE_UPDATE_USER",new Object[]{
                user.getUserName(), e},
                locale));
        }
        log.info("User {} have been updated", user);
    }

    private boolean checkConditions(ObjectProcessRule streamRule, User eventDatabase)
    {
        return true;
    }

    private String getOldValue(User user)
    {
        String oldValue = null;
        var id = user.getId();

        if (id != null)
        {
            oldValue = userRepository.findById(id).map(User::getStatus).orElse(null);
        }
        log.info("[User]getOldValue: id={} oldStatus={} newStatus={}", user.getUserName(), oldValue, user.getStatus());
        return oldValue;
    }
}
