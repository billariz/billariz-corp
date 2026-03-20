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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPerimeter;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ContractPerimeterRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.launcher.queue.LauncherQueue;

@SpringBootTest(classes = { ContractCaptorImpl.class })
@ContextConfiguration
public class ContractCaptorImplTest
{
    @Autowired
    private ContractCaptorImpl           eventHandler;

    @MockBean
    private EntityManagerFactory         entityManagerFactory;

    @MockBean
    private EntityManager                entityManager;

    @MockBean
    private ContractPerimeterRepository  contractPerimeterRepository;

    @MockBean
    private ObjectProcessRuleRepository objectProcessRulesRepository;

    @MockBean
    private ContractRepository           contractRepository;

    @MockBean
    private LauncherQueue                launcherQueue;

    @BeforeEach
    public void setup()
    {
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
    }

    /*
     * @Test void testCreateNewContractWithExitingPerimeter() { var contract =
     * new Contract().setMarket("EL").setId(1L).setStatus("initialized");
     * when(objectProcessRulesRepository.
     * findAllByNewStatusAndInitialStatusAndObjectType("initialized", "",
     * ObjectType.CONTRACT)).thenReturn( makeRules("initialized"));
     * when(contractRepository.save(any())).thenReturn(contract);
     * when(contractPerimeterRepository.findByContractId(contract.getId())).
     * thenReturn(Optional.of(makeContractPerimeter()));
     * eventHandler.onPrePersistOrUpdate(contract);
     * eventHandler.onPostPersistOrUpdate(contract);
     * verify(launcherQueue).createActivityEvent("actInstalled",
     * contract.getId()); }
     * @Test void testCreateNewContractWithNewPerimeter() { var contract = new
     * Contract().setMarket("EL").setId(1L).setStatus("").setContractPerimeter(
     * makeContractPerimeter()); when(objectProcessRulesRepository.
     * findAllByNewStatusAndInitialStatusAndObjectType("waiting", "",
     * ObjectType.CONTRACT)).thenReturn(makeRules(""));
     * when(contractRepository.save(any())).thenReturn(contract);
     * eventHandler.onPrePersistOrUpdate(contract);
     * eventHandler.onPostPersistOrUpdate(contract);
     * verify(launcherQueue).createActivityEvent("actInstalled",
     * contract.getId()); }
     */

    @Test
    void testCreateExistingContract()
    {
        var oldContract = new Contract().setId(2L).setMarket("EL").setStatus("");
        var newContract = new Contract().setId(2L).setMarket("EL").setStatus("initialized");
        when(objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType("INITIALIZED", "", ObjectType.CONTRACT)).thenReturn(
                makeRules("initialized"));
        when(contractRepository.findById(oldContract.getId())).thenReturn(Optional.of(oldContract));
        when(contractRepository.save(any())).thenReturn(newContract);
        when(contractPerimeterRepository.findByContractId(newContract.getId())).thenReturn(Optional.of(makeContractPerimeter()));

        eventHandler.onPrePersistOrUpdate(newContract);
        eventHandler.onPostPersistOrUpdate(newContract);

        // verify(launcherQueue).createActivityEvent("actInstalled",
        // newContract.getId());
    }

    @Test
    void testUpdateExistingContract()
    {
        var oldContract = new Contract().setId(3L).setMarket("EL").setStatus("");
        var newContract = new Contract().setId(3L).setMarket("EL").setStatus("initialized");
        when(objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType("INITIALIZED", "", ObjectType.CONTRACT)).thenReturn(
                makeRules("initialized"));
        when(contractRepository.findById(oldContract.getId())).thenReturn(Optional.of(oldContract));
        when(contractRepository.save(any())).thenReturn(oldContract);
        when(contractPerimeterRepository.findByContractId(newContract.getId())).thenReturn(Optional.of(makeContractPerimeter()));

        eventHandler.onPrePersistOrUpdate(newContract);
        eventHandler.onPostPersistOrUpdate(newContract);

        // verify(launcherQueue).createActivityEvent("actInstalled",
        // newContract.getId());
    }

    private ContractPerimeter makeContractPerimeter()
    {
        return new ContractPerimeter().setPerimeter(new Perimeter().setCustomer(new Customer().setCategory("CUSTOMERB2B")));
    }

    private List<ObjectProcessRule> makeRules(String initialStatus)
    {
        var r1 = makeRule("", "initialized", "toInstall", "actToInstall");
        var r2 = makeRule("initialized", "toInstall", "installed", "actInstalled");

        if (r1.getInitialStatus().equals(initialStatus))
            return Collections.singletonList(r1);
        if (r2.getInitialStatus().equals(initialStatus))
            return Collections.singletonList(r2);
        return Arrays.asList(r1, r2);
    }

    private ObjectProcessRule makeRule(String initialStatus, String externalEventStatus, String finalStatus, String activityType)
    {
        return new ObjectProcessRule().setInitialStatus(initialStatus).setNewStatus(externalEventStatus).setFinalStatus(finalStatus).setMarket(
                "*").setCustomerCategory("*").setChannel("*").setSeller("*").setDirection("*").setServiceCategory("*").setServiceSubCategory(
                        "*").setActivityType(activityType);
    }
}
