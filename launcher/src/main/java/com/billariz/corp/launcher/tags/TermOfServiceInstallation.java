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

package com.billariz.corp.launcher.tags;

import static com.billariz.corp.launcher.utils.FilterUtils.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.FinancialInformation;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.ServiceElementStartOption;
import com.billariz.corp.database.model.ServiceElementType;
import com.billariz.corp.database.model.Service;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.ServiceStartOption;
import com.billariz.corp.database.model.ServiceType;
import com.billariz.corp.database.model.TermOfService;
import com.billariz.corp.database.model.Third;
import com.billariz.corp.database.model.TermOfServiceStartOption;
import com.billariz.corp.database.model.TermOfServiceType;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.SeStatus;
import com.billariz.corp.database.model.enumeration.ServiceStatus;
import com.billariz.corp.database.model.enumeration.TosInstallationUseCase;
import com.billariz.corp.database.model.enumeration.TosStatus;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.PointOfServiceConfigurationRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceElementStartOptionRepository;
import com.billariz.corp.database.repository.ServiceElementRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.database.repository.ServiceStartOptionRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.database.repository.ThirdRepository;
import com.billariz.corp.database.repository.TosStartOptionRepository;
import com.billariz.corp.database.repository.TosTypeRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.EventUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TermOfServiceInstallation implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";
    private static final String ACTIVITY_TOS = "ACTIVITY_TOS";

    private static final String                         ACTIVITY_SERVICE             = "ACTIVITY_SERVICE";

    private static final String                         REF_DATE_TYPE_SUBSCRIPTION = "SUBSCRIPTION";

    private static final String                         PRICE_TYPE_SANDARD = "STANDARD";

    private final ContractRepository                    contractRepository;

    private final EventRepository                       eventRepository;

    private final JournalRepository                     journalRepository;

    private final RelationRepository                    relationRepository;

    private final PointOfServiceConfigurationRepository pointOfServiceConfigurationRepository;

    private final ServiceRepository                     serviceRepository;

    private final ServiceElementRepository              serviceElementRepository;

    private final ServiceStartOptionRepository          serviceStartOptionRepository;

    private final ServiceElementStartOptionRepository               seStartOptionRepository;

    private final TermOfServiceRepository              termOfServicesRepository;

    private final ThirdRepository                       thirdRepository;

    private final TosStartOptionRepository              tosStartOptionRepository;

    private final TosTypeRepository                     tosTypeRepository;

    private final ParameterRepository              parameterRepository;

    private final EventUtils                            eventUtils;

    private final TermOfServiceTermination termOfServiceTermination;

    private List<ServiceStartOption>                    serviceStartOptions;

    List<SeStatus> seListStatus;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

        LocalDate now = LocalDate.now();
        seListStatus = parameterRepository
                                .findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", 
                                            BaseValidator.SERVICE_ELEMENT_STATUS, now)
                                .stream()
                                .map(Parameter::getValue)
                                .map(SeStatus::valueOf)
                                .toList();

        serviceStartOptions = serviceStartOptionRepository.findAll();
        events.forEach(e -> process(e, executionMode));
    }

    private void process(Event event, EventExecutionMode executionMode)
    {
        // Prepare
        List<messageCode> messages = new ArrayList<>();
        event.setCascaded(true);
        event.setStatus(EventStatus.IN_PROGRESS);
        if (event.getRank() == 1)
                    event.getActivity().setStatus(ActivityStatus.IN_PROGRESS);
        event.setExecutionDate(LocalDateTime.now());
        event.setExecutionMode(executionMode);
        var journal = eventUtils.addJournal(event);

        // Execute
        try
        {
            handle(event, journal, messages);
            event.setStatus(EventStatus.COMPLETED);
        }
        catch (LauncherException e)
        {
            log.error("handle: " + e.getMessage(), e);
            event.setStatus(EventStatus.IN_FAILURE);
            journal.setMethod(JournalAction.ERROR.getValue());
            messages.add(new messageCode(e.getMessage(), e.getArgs()));
            journal.setComment(e.getMessage());
            event.getActivity().setStatus(ActivityStatus.BLOCKED);
        }
        // Finish
        log.info("Event[{}] processed, strating next process", event);
        eventUtils.triggerOnUpdateEventStatus(event);
        journal.setNewStatus(Objects.toString(event.getStatus()));
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
    }

    @Transactional
    private void handle(Event event, Journal journal, List<messageCode> messages) throws LauncherException
    {
        var useCase = TosInstallationUseCase.valueOf(event.getAction());
        messages.add(new messageCode("EVENT_USE_CASE", new Object[]{useCase}));
        boolean isDefault = false;

        var relationContract = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT, 
                event.getActivityId()).orElseThrow(
                () -> new LauncherFatalException("MISSING_CONTRACT", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relationContract, event);
        var contract = contractRepository.findById(relationContract.getSecondObjectId())
                        .orElseThrow(() -> new LauncherFatalException("MISSING_CONTRACT_IN_DB",  new Object[]{relationContract.getSecondObjectId()}));
        
        //USE CASE TOS_ONE_SELF
        if (TosInstallationUseCase.TOS_ONE_SELF.equals(useCase))
        {
            var relationTos = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_TOS,
                                event.getActivityId()).orElseThrow(
                                () -> new LauncherFatalException("MISSING_POS",new Object[]{event.getId()}));
            var tos = termOfServicesRepository.findById(relationTos.getSecondObjectId()).get();
            handleTos(tos, tos.getTosType(), tos.getService(), contract, event, messages);
            setServiceElements(tos, tos.getService(), tos.getPriceType());
            updateTOSandServiceStatus(tos, tos.getService());
        }
        //AUTRES USE CASES 
        else {
            var paymentMode = thirdRepository.findByContractInPerimeter(BaseValidator.ACTOR_PAYER, contract.getId())
                                            .map(Third::getFinancialInformation).map(FinancialInformation::getPaymentMode)
                                            .orElseThrow(() -> new LauncherFatalException("MISSSING_PAYER", new Object[]{contract.getId()}));
            messages.add(new messageCode("PAYMENT_MODE", new Object[]{paymentMode}));
            var posConfiguration = pointOfServiceConfigurationRepository.findFirstByContractIdAndEndDateIsNullOrderByStartDateDesc(contract.getId())
                                            .orElseThrow(() -> new LauncherFatalException("MISSING_POS_CONF", new Object[]{contract.getId()}));
            messages.add(new messageCode("POS_CONF", new Object[]{posConfiguration.getId()}));
            var pos = posConfiguration.getPointOfService();
            var customer = contract.getContractPerimeter().getPerimeter().getCustomer();

            //Création des services par defaut
            if (TosInstallationUseCase.DEFAULT_SERVICES.equals(useCase))
            {
                isDefault = true;
                createDefaultService(contract, customer, pos, posConfiguration, paymentMode, event, messages);
                messages.add(new messageCode("DEFAULT_SERVICES", new Object[]{contract.getId()}));
            }
            //Lancement de l'installation des services
            settingContractListServices(isDefault, event, contract, customer, pos, posConfiguration, paymentMode, useCase, messages);
        }

        journal.setMessageCodes(messages);
    }

    private void settingContractListServices(boolean isDefault, Event event, Contract contract, Customer customer, 
                                    PointOfService pos, PointOfServiceConfiguration posConfiguration, String paymentMode,
                                    TosInstallationUseCase useCase, List<messageCode> messages) throws LauncherException
    {
        List<Service> serviceToProcess = new ArrayList<>();
        if(!TosInstallationUseCase.SERVICE.equals(useCase)){
            var contractServiceList = serviceRepository.findAllByContractIdAndStatusIn(contract.getId(),
                    Arrays.asList(ServiceStatus.INITIALIZED, ServiceStatus.IN_FAILURE));

            serviceToProcess = contractServiceList.stream().filter(s -> s.getServiceType().isDefaultService() == isDefault).collect(Collectors.toList());
            
            if (serviceToProcess.isEmpty())
                throw new LauncherFatalException(
                        "MISSING_SERVICES", new Object[]{contract.getId(), event.getId()});
        }
        else {
            var relationService = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_SERVICE,
                     event.getActivityId()).orElseThrow(
                     () -> new LauncherFatalException("MISSING_SERVICES_EVENT", new Object[]{event.getId()}));
            serviceToProcess.add(serviceRepository.findById(relationService.getSecondObjectId()).get());
        }
        messages.add(new messageCode("PROCESSING_SERVICES", new Object[]{serviceToProcess.size()}));
        for (Service service : serviceToProcess)
            handleService(service, contract, customer, pos, posConfiguration, paymentMode, event, messages);
    }

    private void createDefaultService(Contract contract, Customer customer, PointOfService pos, PointOfServiceConfiguration posConfiguration, 
                        String paymentMode, Event event, List<messageCode> messages) throws LauncherException
    {
        var defaultServiceTypeList = serviceStartOptions.stream().filter(sso -> sso.getServiceType().isDefaultService()).filter(
                serviceStartOptionStream -> filter(serviceStartOptionStream, paymentMode, pos, posConfiguration, customer, contract, null)).map(
                        ServiceStartOption::getServiceType).collect(Collectors.toSet());

        if (defaultServiceTypeList.isEmpty())
        {
            log.debug("contract {} doesn't have default services", contract.getId());
            throw new LauncherFatalException("MISSING_DEFAULT_SERVICES", new Object[]{contract.getId(), event.getId()});
        }
        messages.add(new messageCode("PROCESSING_DEFAULT_SERVICES", new Object[]{contract.getId(), event.getId()}));
        defaultServiceTypeList.forEach(s -> insertService(s, contract));
    }

    private void insertService(ServiceType serviceType, Contract contract)
    {
        var service = new Service();
        service.setContractId(contract.getId());
        service.setStatus(ServiceStatus.INITIALIZED);
        service.setStartDate(contract.getSubscriptionDate());
        service.setTou("*");
        service.setTouGroup("*");
        service.setServiceType(serviceType);
        service.setServiceTypeId(serviceType.getId());

        serviceRepository.save(service);
    }

    private void handleService(Service service, Contract contract, Customer customer, PointOfService pos,
        PointOfServiceConfiguration posConfiguration, String paymentMode, Event event, List<messageCode> messages) throws LauncherException {
        
        log.debug("Processing Service with Event Action: {}", event.getAction());
        log.debug("Service Details: {}", service);
        log.debug("Contract Details: {}", contract);
        log.debug("Customer Information - Category: '{}' | Type: '{}'", customer.getCategory(), customer.getType());
        log.debug("Point of Service Configuration: {}", posConfiguration);
        log.debug("Payment Mode: {}", paymentMode);

        // Construction de la liste des TosType en filtrant les options valides
        var tosTypeList = serviceStartOptions.stream()
                .filter(sso -> determineListService(event.getAction(), sso, service))
                .filter(sso -> filter(sso, paymentMode, pos, posConfiguration, customer, contract, service))
                .map(ServiceStartOption::getTosType)
                .collect(Collectors.toSet());

        log.debug("Filtered TosType List: {}", tosTypeList);
        messages.add(new messageCode("FILTRED_TOS_LIST", new Object[]{tosTypeList}));

        // Gestion des cas où la liste est vide
        if (tosTypeList.isEmpty()) {
            updateServiceStatus(service, ServiceStatus.IN_FAILURE);
            throw new LauncherFatalException("MISSING_SERVICE_START_OPTION", new Object[]{service.getId()});
        }

        // Traitement des TosType
        handlingTos(tosTypeList, service, contract, event, messages);
    }

    private boolean determineListService(String eventAction, ServiceStartOption serviceStartOption, Service service)
    {

        return Objects.equals(serviceStartOption.getService(), service.getServiceTypeId()) || "*".equals(serviceStartOption.getService());
    }

    private void updateServiceStatus(Service service, ServiceStatus status)
    {
        if (service != null)
        {
            service.setStatus(status);
            serviceRepository.save(service);
        }
    }

    private void handlingTos(Set<String> tosTypeList, Service service, Contract contract, Event event, List<messageCode> messages) throws LauncherException
    {
        for (String tosType : tosTypeList)
        {
            createTermOfServices(service, tosType, contract, event, messages);
        }
    }

    private void createTermOfServices(Service service, String tosType, Contract contract, Event event, List<messageCode> messages) throws LauncherException {
        // Récupération du TosType avec gestion directe de l'absence
        var tosTypeObj = tosTypeRepository.findById(tosType)
                .orElseThrow(() -> {
                    updateServiceStatus(service, ServiceStatus.IN_FAILURE);
                    return new LauncherFatalException("MISSING_TOS_TYPE", new Object[]{tosType});
                });
        log.debug("TosType From DB [{}]", tosTypeObj);
    
        // Récupération des options de démarrage
        var tosStartOptionInfo = tosStartOptionRepository.filterByTosTypeAndDate(tosType, LocalDate.now())
                .orElseThrow(() -> {
                    updateServiceStatus(service, ServiceStatus.IN_FAILURE);
                    return new LauncherFatalException("MISSING_TOS_START_OPTION", new Object[]{tosType});
                });
    
        log.debug("tosStartOptionInfo from DB => {}", tosStartOptionInfo);
        handleTos(null, tosTypeObj, service, contract, event, messages);
        insertTermOfServices(contract, tosTypeObj, tosStartOptionInfo, service);
    }

    private void handleTos(TermOfService tos, TermOfServiceType tosType, Service service, Contract contract, Event event, List<messageCode> messages) throws LauncherException {
        if (!tosType.isExclusive()) {
            messages.add(new messageCode("NO_EXCLUSIVE_TOS", new Object[]{tosType.getId()}));
            return;
        }
    
        // Récupération de la liste des TOS existants
        messages.add(new messageCode("EXCLUSIVE_TOS", new Object[]{tosType.getId()}));
        var termOfServicesList = termOfServicesRepository.findAllByTosTypeIdAndContractIdAndStatusNotIn(
                tosType.getId(),
                contract.getId(),
                List.of(TosStatus.CANCELLED, TosStatus.CLOSED, TosStatus.PENDING_STOP, TosStatus.STOPPED)
        );
        if(tos!=null)
            termOfServicesList.remove(tos);
    
        if (termOfServicesList.isEmpty()) {
            log.debug("No TOS found for TosType [{}] and Contract [{}]. Inserting new TOS.", tosType.getId(), contract.getId());
        } else {
            log.debug("Existing TOS found. Size: {}", termOfServicesList.size());
            messages.add(new messageCode("TOS_TYPE_PROCESSING", new Object[]{termOfServicesList.size()}));
            handleExistingTos(tos, termOfServicesList, service, event, messages);
        }
    }
    
    private void handleExistingTos(TermOfService tos, List<TermOfService> existingTosList, Service service, Event event, List<messageCode> messages) throws LauncherException {
        
        for (TermOfService termOfService : existingTosList) {
            var endDate = tos!=null ? tos.getStartDate() : service.getStartDate();
            termOfServiceTermination.terminateTos(termOfService, seListStatus, endDate, event);
            messages.add(new messageCode("TOS_CLOSED_BY_EVENT", new Object[]{termOfService.getId(), event.getId()}));
            
            if(tos == null) {
            termOfServiceTermination.terminateService(termOfService.getService(), service.getStartDate(), event);
            messages.add(new messageCode("SERVICE_CLOSED_BY_EVENT", new Object[]{termOfService.getService().getId(), event.getId()}));
            }
        }
    }

    private void insertTermOfServices(Contract contract, TermOfServiceType tosType, TermOfServiceStartOption tosStartOption, Service service) throws LauncherException
    {
        var termOfServices = new TermOfService();

        termOfServices.setTosTypeId(tosType.getId());
        termOfServices.setServiceId(service.getId());
        termOfServices.setContractId(service.getContractId());
        termOfServices.setStartDate(service.getStartDate());
        termOfServices.setDirection(contract.getDirection() == null ? service.getDirection() : contract.getDirection());
        if (tosStartOption.getInitialDuration() != null)
            termOfServices.setEndDate(service.getStartDate().plusMonths(tosStartOption.getInitialDuration()));
        termOfServices.setPriceType(tosStartOption.getPriceType());
        if (REF_DATE_TYPE_SUBSCRIPTION.equals(tosStartOption.getRefDateTypeForFixedPrice()))
            termOfServices.setRefDateForFixedPrice(contract.getSubscriptionDate());
        termOfServices.setInitialDuration(tosStartOption.getInitialDuration());
        termOfServices.setMinimumDuration(tosStartOption.getMinimumDuration());
        termOfServices.setMarket(tosStartOption.getMarket());
        termOfServices.setTosDefault(tosType.isDefaultTos());
        termOfServices.setMaster(tosType.isMaster());
        termOfServices.setExclusive(tosType.isExclusive());
        termOfServices.setTouGroup(tosType.getTouGroup());
        termOfServices.setStatus(TosStatus.PENDING_START);
        termOfServices.setCascaded(true);
        termOfServices = termOfServicesRepository.save(termOfServices);
        setServiceElements(termOfServices, service, tosStartOption.getPriceType());
        updateTOSandServiceStatus(termOfServices, service);
    }

    private void updateTOSandServiceStatus(TermOfService termOfServices, Service service)
    {
        termOfServicesRepository.save(termOfServices);
        if (service != null)
            updateServiceStatus(service, service.getStatus());
    }

    private void setServiceElements(TermOfService termOfServices, Service service, String priceType) throws LauncherException
    {
        var seStartOptions = seStartOptionRepository.filterByTosTypeAndDate(termOfServices.getTosTypeId(), LocalDate.now()).stream().filter(
                seStartOption -> filter(seStartOption.getTouGroup(), termOfServices.getTouGroup())).toList();

        log.debug("SeStartOption List => {}", seStartOptions);
        if (seStartOptions.isEmpty())
        {
            termOfServices.setStatus(TosStatus.IN_FAILURE);
            termOfServicesRepository.save(termOfServices);
            updateServiceStatus(service, ServiceStatus.IN_FAILURE);
            throw new LauncherFatalException("MISSING_SE_START_OPTION", new Object[]{termOfServices.getId()});
        }
        handleSeStartOption(seStartOptions, service, priceType, termOfServices);
    }

    private void handleSeStartOption(List<ServiceElementStartOption> seStartOptions, Service service, String priceType, TermOfService termOfServices) throws LauncherException
    {
        for (ServiceElementStartOption seStartOption : seStartOptions)
        {
            var seType = seStartOption.getSeType();
            log.debug("SeType Info from DB => {}", seType);

            if (seType == null)
            {
                termOfServices.setStatus(TosStatus.IN_FAILURE);
                termOfServicesRepository.save(termOfServices);
                updateServiceStatus(service, ServiceStatus.IN_FAILURE);
                throw new LauncherFatalException("MISSING_SE_TYPE",new Object[]{termOfServices.getId()});
            }
            createServiceElement(seType, seStartOption, termOfServices, service, priceType);
            termOfServices.setStatus(TosStatus.PENDING_START);
            termOfServicesRepository.save(termOfServices);
            updateServiceStatus(service, ServiceStatus.INSTALLED);
        }
    }

    private void createServiceElement(ServiceElementType seType, ServiceElementStartOption seStartOption, TermOfService termOfServices, Service service, String priceType) throws LauncherException
    {
        var serviceElement = new ServiceElement();

        serviceElement.setTosId(termOfServices.getId());
        serviceElement.setSeType(seType);
        serviceElement.setSeTypeId(seType.getId());
        serviceElement.setMaster(seType.isSeMaster());
        serviceElement.setMetered(seType.isMetered());
        serviceElement.setVatRate(seStartOption.getVatRate());
        if (PRICE_TYPE_SANDARD.equals(priceType))
        {
            serviceElement.setOperand(seStartOption.getOperand());
            serviceElement.setOperandType(seStartOption.getOperandType());
            serviceElement.setFactor(seStartOption.getFactor());
            serviceElement.setFactorType(seStartOption.getFactorType());
            serviceElement.setRateType(seStartOption.getRateType());
            serviceElement.setThreshold(seStartOption.getThreshold());
            serviceElement.setThresholdBase(seStartOption.getThresholdBase());
            serviceElement.setThresholdType(seStartOption.getThresholdType());
        }
        else if (service != null)
        {
            serviceElement.setOperand(service.getOperand());
            serviceElement.setOperandType(service.getOperandType());
            serviceElement.setFactor(service.getFactor());
            serviceElement.setFactorType(service.getFactorType());
            serviceElement.setRateType(service.getRateType());
            serviceElement.setThreshold(service.getThreshold());
            serviceElement.setThresholdBase(service.getThresholdBase());
            serviceElement.setThresholdType(service.getThresholdType());
        }
        serviceElement.setBillingScheme(seStartOption.getBillingScheme());
        serviceElement.setAccountingScheme(seStartOption.getAccountingScheme());
        serviceElement.setEstimateAuthorized(seStartOption.isEstimateAuthorized());
        serviceElement.setSqType(seStartOption.getSqType());
        serviceElement.setTouGroup(seStartOption.getTouGroup());
        serviceElement.setTou(seStartOption.getTou());
        serviceElement.setStartDate(termOfServices.getStartDate());
        serviceElement.setEndDate(termOfServices.getEndDate());
        serviceElement.setStatus(seStartOption.getDefaultSeStatus());
        serviceElement.setMinDayForEstimate(seStartOption.getMinDayForEstimate());
        serviceElement.setSeListBaseForSq(seStartOption.getSeListBaseForSq());
        serviceElement.setCategory(seStartOption.getCategory());
        serviceElement.setSubCategory(seStartOption.getSubCategory());
        serviceElementRepository.save(serviceElement);
    }
}
