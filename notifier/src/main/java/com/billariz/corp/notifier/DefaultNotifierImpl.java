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

package com.billariz.corp.notifier;

import java.util.Map;
import javax.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import com.billariz.corp.database.model.User;
import com.billariz.corp.notifier.exception.NotifierException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class DefaultNotifierImpl implements UserNotifier
{
    private final ITemplateEngine thymeleafTemplate;

    @Autowired(required = false)
    private JavaMailSender        mailSender;

    @Value("${spring.mail.properties.default.from:#{null}}}")
    private String                emailFrom;

    @Value("${spring.mail.properties.default.bcc:#{null}}")
    private String                emailBcc;

    @Override
    public void newUser(User user, String password) throws NotifierException
    {
        var userEmail = user.getContact().getEmail();

        try
        {
            Map<String, Object> kv = Map.of("login", user.getContact().getEmail(), "user_civilite", "", "user_first_name", user.getIndividual().getFirstName(),
                    "user_last_name", user.getIndividual().getLastName(), "password", password);

            sendMail(userEmail, "user_create_title.txt", "user_create_body.html", kv);
        }
        catch (Exception e)
        {
            throw new NotifierException("Error while sending mail", e);
        }
    }

    @Override
    public void resetUserPassword(User user, String password) throws NotifierException
    {
        var userEmail = user.getContact().getEmail();

        try
        {
            Map<String, Object> kv = Map.of("login", user.getContact().getEmail(), "user_civilite", "", "user_first_name", user.getIndividual().getFirstName(),
                    "user_last_name", user.getIndividual().getLastName(), "password", password);

            sendMail(userEmail, "reset_password_title.txt", "reset_password_body.html", kv);
        }
        catch (Exception e)
        {
            throw new NotifierException("Error while sending mail", e);
        }
    }

    private void sendMail(String userEmail, String titleTemplate, String bodyTemplate, Map<String, Object> kv) throws MessagingException
    {
        var body = resolve(thymeleafTemplate, bodyTemplate, kv);
        var subjet = resolve(thymeleafTemplate, titleTemplate, kv);
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message);

        log.info("Send email to: {}", userEmail);
        helper.setFrom(emailFrom);
        if (emailBcc != null)
            helper.setBcc(emailBcc);
        helper.setTo(userEmail);
        helper.setSubject(subjet);
        helper.setText(body, true);
        mailSender.send(message);
        log.info("Send email: title={}\nmessage={}", subjet, body);
    }

    private String resolve(ITemplateEngine templateEngine, String templateName, Map<String, Object> variables)
    {
        var context = new Context(null, variables);

        return templateEngine.process(templateName, context);
    }
}
