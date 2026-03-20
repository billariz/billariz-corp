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

package com.billariz.corp.launcher.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.BillableCharge;
import com.billariz.corp.database.model.Article;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.ServiceElementType;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeSource;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.BillingValuationBase;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.model.enumeration.SeStatus;
import com.billariz.corp.database.model.enumeration.ServiceElementTypeCategory;
import com.billariz.corp.database.model.enumeration.ServiceStatus;
import com.billariz.corp.database.model.enumeration.TosStatus;
import com.billariz.corp.database.model.light.EligibleServiceElement;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.PointOfServiceCapacityRepository;
import com.billariz.corp.database.repository.PointOfServiceConfigurationRepository;
import com.billariz.corp.database.repository.ServiceElementRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.BillingUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingService
{

    private final BillSegmentRepository                 billSegmentRepository;

    private final ContractRepository                    contractRepository;

    private final JournalUtils                 journalUtils;

    private final MeterReadRepository                   meterReadRepository;

    private final PointOfServiceCapacityRepository      pointOfServiceCapacityRepository;

    private final PointOfServiceConfigurationRepository pointOfServiceConfigurationRepository;

    private final PriceService                          priceService;

    private final QuantityService                       quantityService;

    private final ServiceElementRepository              serviceElementRepository;

    private final TermOfServiceRepository              termOfServiceRepository;

    private final ServiceRepository                     serviceRepository;

    private final BillingUtils       billingUtils;

    private final BillableChargeRepository billableChargeRepository;

    @Transactional(readOnly = true)
    public Optional<ContractAndCustomerInfo> fetchContractAndCustomerInfo(Long contractId) throws LauncherException
    {
        var contractOpt = contractRepository.findById(contractId);

        if (contractOpt.isPresent())
        {
            var contract = contractOpt.get();
            log.debug("Contract -> {}", contract);
            var customer = contract.getContractPerimeter().getPerimeter().getCustomer();
            log.debug("Customer -> {}", customer);

            var posConfigurations = pointOfServiceConfigurationRepository.findAllByContractIdAndStatusIn(contract.getId(),
                    List.of(PointOfServiceDataStatus.VALIDATED, PointOfServiceDataStatus.CLOSED));
            var capacities = pointOfServiceCapacityRepository.findAllByContractIdAndStatusIn(contract.getId(), 
                    List.of(PointOfServiceDataStatus.VALIDATED, PointOfServiceDataStatus.CLOSED ));
            log.debug("PosAndPosCategory: {}", posConfigurations);
            log.debug("PosCapacity: {}", capacities);

            if (posConfigurations.isEmpty() || capacities.isEmpty()) {
                throw new LauncherFatalException(
                        "MISSING_VALIDE_CONF_OR_CAPA",new Object[]{contract.getId()}
                    );
            }
            PointOfService pos = posConfigurations.get(0).getPointOfService();
            log.debug("PointOfService -> {}", pos);
            return Optional.of(new ContractAndCustomerInfo(contract, customer.getType(), customer.getCategory(), posConfigurations, capacities, pos));
        }
        else
            throw new LauncherFatalException("MISSING_CONTRACT_IN_DB", new Object[]{contractId});

    }

    @Transactional
    public void handleValuation(ContractAndCustomerInfo ctfAndCuInfo, List<EligibleServiceElement> eligibleServiceElements, 
                            BillingValuationBase valuationBase, LocalDate cutOffDate) throws LauncherException
    {
        List<BillSegment> bss = new ArrayList<>();
        handleEligibleSe(bss, eligibleServiceElements, ctfAndCuInfo, valuationBase, cutOffDate);
        billSegmentRepository.saveAll(bss);
        bss.stream().forEach(bs -> startServices(bs));

        log.debug("contractId {} - List of bill segments valuated: {}", ctfAndCuInfo.getContract().getId(), bss);
    }

    private void handleEligibleSe(List<BillSegment> bss, 
                              List<EligibleServiceElement> eligibleServiceElements, 
                              ContractAndCustomerInfo ctfAndCuInfo, 
                              BillingValuationBase valuationBase, LocalDate cutOffDate) throws LauncherException {

        List<QuantityObject> quantities = new ArrayList<>();

        // Grouper les SEs Master par seType
        Map<ServiceElementType, List<EligibleServiceElement>> seMastersPerSeType = eligibleServiceElements.stream()
                .collect(Collectors.groupingBy(EligibleServiceElement::getType));
        
        var processed=false;
        for (Map.Entry<ServiceElementType, List<EligibleServiceElement>> entry : seMastersPerSeType.entrySet()) {
            ServiceElementType seType = entry.getKey();
            List<EligibleServiceElement> seList = entry.getValue();

            // Traiter selon les cas du valuationBase
            if (billingUtils.shouldProcessForBillingCharge(seType, valuationBase)) {
                processBillingCharge(bss, seType, seList, ctfAndCuInfo, quantities, cutOffDate);
                processed=true;
            }

            if (billingUtils.shouldProcessForConsumption(seType, valuationBase)) {
                processConsumption(bss, seType, seList, ctfAndCuInfo, quantities, cutOffDate);
                processed=true;
            }

            if (billingUtils.shouldProcessForSubscription(seType, valuationBase)) {
                processSubscription(bss, seType, seList, ctfAndCuInfo, quantities, cutOffDate);
                processed=true;
            }
        }
        if(!processed)
                throw new LauncherFatalException("MISSING_SE_MASTERS", new Object[]{valuationBase});
    }

    // Méthodes de traitement pour chaque cas
    private void processBillingCharge(List<BillSegment> bss, ServiceElementType seType, 
                                    List<EligibleServiceElement> seList, 
                                    ContractAndCustomerInfo ctfAndCuInfo, 
                                    List<QuantityObject> quantities, LocalDate cutOffDate) throws LauncherException{
        creatingServiceElementsListForBilling(bss, seType, seList, ctfAndCuInfo, quantities, cutOffDate);
    }

    private void processConsumption(List<BillSegment> bss, ServiceElementType seType, 
                                    List<EligibleServiceElement> seList, 
                                    ContractAndCustomerInfo ctfAndCuInfo, 
                                    List<QuantityObject> quantities, LocalDate cutOffDate) throws LauncherException {
        creatingServiceElementsListForBilling(bss, seType, seList, ctfAndCuInfo, quantities, cutOffDate);

        var meterReadIdList = quantities.stream()
                .filter(quantityObject -> quantityObject.getMeterReadId() !=null)
                .filter(quantityObject -> MeterReadStatus.AVAILABLE.equals(quantityObject.getMeterReadStatus()))
                .map(QuantityObject::getMeterReadId)
                .collect(Collectors.toSet());

        if (meterReadIdList.isEmpty()) {
            throw new LauncherFatalException("MISSING_METER_READS", new Object[]{quantities.toString()});
        }

        meterReadRepository.updateStatus(meterReadIdList, MeterReadStatus.VALUATED);
        log.debug("Update MeterRead Status TO [VALUATED] : {}", meterReadIdList);

        handleCancelledMeterRead(meterReadIdList);
    }

    private void processSubscription(List<BillSegment> bss, ServiceElementType seType, 
                                    List<EligibleServiceElement> seList, 
                                    ContractAndCustomerInfo ctfAndCuInfo, 
                                    List<QuantityObject> quantities, LocalDate cutOffDate) throws LauncherException {
        creatingServiceElementsListForBilling(bss, seType, seList, ctfAndCuInfo, quantities, cutOffDate);
    }

    private void handleCancelledMeterRead(Set<Long> meterReadIdList) throws LauncherException {
        
        for(Long mrId: meterReadIdList){
            //1 Récupérer la liste des Mr a annuler
            var mrs = meterReadRepository.findAllByCancelledBy(mrId).stream()
                                                    .filter(mr -> mr.getStatus().equals(MeterReadStatus.TO_CANCEL))
                                                    .map(MeterRead::getId)
                                                    .toList();
            //2 récuperer la liste des BS associés
            List<BillSegment> bsmr = billSegmentRepository.findAllByMeterReadIdIn(mrs);
            //3 supprimer les BS non billed/computed
            bsmr.stream().filter(e -> e.getStatus().equals(BillSegmentStatus.CALCULATED))
                                            .forEach(ee -> billSegmentRepository.deleteById(ee.getId()));
            //4 reverse les BS billed/computed
            var bssToCancel = bsmr.stream().filter(e -> e.getStatus().equals(BillSegmentStatus.COMPUTED) 
                                                        || e.getStatus().equals(BillSegmentStatus.BILLED)).toList();
            for(BillSegment bsToCancel : bssToCancel ){
                var mbs = reverseBillSegment(bsToCancel);
                mbs.setBillId(null);
                mbs.setStatus(BillSegmentStatus.CALCULATED);
                billSegmentRepository.save(mbs);
                bsToCancel.setCancelledBy(mbs.getId());
            }
            //5 mettre a jour le statut des Mr
            meterReadRepository.updateStatus(mrs, MeterReadStatus.CANCELLED);
        }
    }
    
    private void creatingServiceElementsListForBilling(List<BillSegment> bss, 
                                                        ServiceElementType seType, 
                                                        List<EligibleServiceElement> eligibleServiceElements, 
                                                        ContractAndCustomerInfo ctfAndCuInfo, 
                                                        List<QuantityObject> quantities, 
                                                        LocalDate cutOffDate) 
                                                        throws LauncherException {

            if (eligibleServiceElements.isEmpty())
                throw new LauncherFatalException("MISSING_ELIGIBLE_SES",null);

            for (EligibleServiceElement eligibleServiceElement : eligibleServiceElements) {
                List<ServiceElement> serviceElements = new ArrayList<>();

                // Ajout du SE Maitre
                serviceElements.add(
                    serviceElementRepository.findById(eligibleServiceElement.getId())
                    .orElseThrow(() -> new LauncherFatalException("MISSING_SE", new Object[]{eligibleServiceElement.getId()}))
                );

                // Ajout des SE non maitre si condition satisfaite
                if (eligibleServiceElements.get(0).getId().equals(eligibleServiceElement.getId())) {
                    serviceElements.addAll(
                        serviceElementRepository.findWithServiceElementMaster(
                            seType.getId(), eligibleServiceElements.get(0).getContractId()
                        )
                    );
                }
                // Traitement des éléments de service
                handleServiceElements(bss, serviceElements, ctfAndCuInfo, quantities, cutOffDate);
            }
    }

    private void handleServiceElements(List<BillSegment> bss, List<ServiceElement> serviceElements, ContractAndCustomerInfo ctfAndCuInfo, 
                            List<QuantityObject> quantities, LocalDate cutOffDate) throws LauncherException
    {
        serviceElements.sort(new BillingSEComparator());
        log.debug("handleServiceElements: serviceElements={}", serviceElements);

        for (ServiceElement serviceElement : serviceElements)
        {
            // Traiter les SEs BC
            if(serviceElement.getSeType().getSeTypeCategory().equals(ServiceElementTypeCategory.BILLABLE_CHARGE))
                handleBillableChargeSe(bss, serviceElement, ctfAndCuInfo, cutOffDate);
            else // Limiter a seulement les SE non BC et PP
                quantities.addAll(billingSE(bss, serviceElements.get(0), serviceElement, ctfAndCuInfo, cutOffDate));
        }
    }

    private void handleBillableChargeSe(List<BillSegment> bss, ServiceElement serviceElement, 
                            ContractAndCustomerInfo ctfAndCuInfo, LocalDate cutOffDate ) throws LauncherException
    {
        try {
            log.debug("handle Billable Charge ServiceElement: BC serviceElements={}", serviceElement);
            //Récupérer une liste de BC éligibles
            var bcList = billableChargeRepository.findWithStatusAndContractAndSourceAndCategory(
                        BillableChargeStatus.AVAILABLE, 
                        ctfAndCuInfo.getContract().getId(),
                        new ArrayList<>(Arrays.asList(BillableChargeSource.MARKET,BillableChargeSource.USER, BillableChargeSource.SYSTEM)),
                        serviceElement.getCategory(),
                        cutOffDate
                        );
            if(bcList.isEmpty() && serviceElement.isMaster())
                throw new LauncherFatalException("MISSING_BILLABLE_CHARGE_TO_VALUATE", new Object[]{ctfAndCuInfo.getContract().getId(), serviceElement.getId()});

            //Traiter les BC
            bcList.stream()
                .flatMap(bc -> bc.getBillableChargeDetails().stream())
                .forEach(a -> creatingBcBillSegment(bss, serviceElement, a));

            //Mettre à jour le statut des BC traités
            var bcTreated = bcList.stream().map(
            BillableCharge::getId).collect(Collectors.toSet());
            billableChargeRepository.updateStatus(bcTreated, BillableChargeStatus.VALUATED);
        
            // Traiter les BS lié aux BC annulée par la BC des bs calculé précédemment
            handleCancelledBillableCharge(bcList);

        } catch (Exception e) {
            throw new LauncherFatalException("EXCEPTION_ON_SE_PROCESSING", new Object[]{serviceElement.getId(), e.getMessage()});
        }
    }

    private void handleCancelledBillableCharge(List<BillableCharge> bcList) throws LauncherException {
        
        for(BillableCharge bc: bcList){
            //1 Récupérer la liste des BC a annuler
            var bcs = billableChargeRepository.findAllByCancelledBy(bc.getId()).stream()
                                                    .filter(e -> e.getStatus().equals(BillableChargeStatus.TO_CANCEL))
                                                    .map(BillableCharge::getId)
                                                    .toList();
            //2 récuperer la liste des BS associés
            List<BillSegment> bsmr = billSegmentRepository.findAllByBillableChargeIdIn(bcs);
            //3 supprimer les BS non billed/computed
            bsmr.stream().filter(e -> e.getStatus().equals(BillSegmentStatus.CALCULATED))
                                            .forEach(ee -> billSegmentRepository.deleteById(ee.getId()));
            //4 reverse les BS billed/computed
            var bssToCancel = bsmr.stream().filter(e -> e.getStatus().equals(BillSegmentStatus.COMPUTED) 
                                                        || e.getStatus().equals(BillSegmentStatus.BILLED)).toList();
            for(BillSegment bsToCancel : bssToCancel ){
                var mbs = reverseBillSegment(bsToCancel);
                mbs.setBillId(null);
                mbs.setStatus(BillSegmentStatus.CALCULATED);
                billSegmentRepository.save(mbs);
                bsToCancel.setCancelledBy(mbs.getId());
            }
            //5 mettre a jour le statut des BC
            billableChargeRepository.updateStatus(bcs, BillableChargeStatus.CANCELLED);
        }
    }

    private List<QuantityObject> billingSE(List<BillSegment> bss, ServiceElement seMaster, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo, LocalDate cutOffDate) throws LauncherException
    {
        log.debug("Se -> {}", serviceElement);
        log.debug("SeMaster -> {}", seMaster);
        var quantities = handleQuantity(bss, seMaster, serviceElement, ctfAndCuInfo, cutOffDate);
        if (quantities.isEmpty() && serviceElement.isMaster())
                throw new LauncherFatalException("MISSING_QUANTITIES", new Object[]{serviceElement.getId()});
            
        for (QuantityObject quantity : quantities)
            {
                    log.debug("ctfAndCuInfo -> {}", ctfAndCuInfo);
                    log.debug("Quantity => {}", quantity);

                    var indexes = handlePrice(quantity, serviceElement, ctfAndCuInfo);
                    handleBillSegment(bss, quantity, indexes, serviceElement);
            }
        return quantities;
    }

    private List<QuantityObject> handleQuantity(List<BillSegment> bss, ServiceElement seMaster, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo, LocalDate cutOffDate) throws LauncherException
    {
        log.debug("Start Valuating Quantities");
        var quantityObjects = quantityService.getQuantity(bss, seMaster, serviceElement, ctfAndCuInfo, cutOffDate);
        log.debug("End Of Valuating Quantities =>", quantityObjects.toString());
        return quantityObjects;
    }

    private List<PriceObject> handlePrice(QuantityObject quantity, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo) throws LauncherException
    {
        log.debug("Start Valuating Prices");
        var priceObjects = priceService.getPrice(quantity, serviceElement, ctfAndCuInfo);
        log.debug("Prices: {}", priceObjects);
        log.debug("End Valuating Prices");
        return priceObjects;
    }

    private void handleBillSegment(List<BillSegment> bss, QuantityObject quantity, List<PriceObject> priceObjects, ServiceElement serviceElement) throws LauncherException
    {
        log.debug("Start Of BillSegment Insertion Process");

        setQuantityPricePerPeriod(bss, serviceElement, quantity, priceObjects);
       
        log.debug("End Of BillSegment Insertion Process");
    }

    private void setQuantityPricePerPeriod(List<BillSegment> bss, ServiceElement serviceElement, QuantityObject quantity, List<PriceObject> eligiblePriceObjects) throws LauncherException
    {
        if (eligiblePriceObjects.isEmpty())
            throw new LauncherFatalException("MISSING_PRICES", new Object[]{quantity.toString(), serviceElement.getId()});

        log.debug("Eligible Prices: {}", eligiblePriceObjects);
        creatingBillSegment(bss, eligiblePriceObjects, serviceElement, quantity);
        quantity.setMeterReadStatus(MeterReadStatus.AVAILABLE);
    }

    private void creatingBillSegment(List<BillSegment> bss, List<PriceObject> priceObjects, ServiceElement serviceElement, QuantityObject quantity) throws LauncherException
    {
        for (PriceObject priceObject : priceObjects)
        {
            log.debug("Price -> {}", priceObject);
            var startDate = billingUtils.maxDate(quantity.getStartDate(), priceObject.getStartDate());
            var endDate = priceObject.getEndDate() != null ? billingUtils.minDate(quantity.getEndDate(), priceObject.getEndDate()) : quantity.getEndDate();
            var overlappingPeriod = BigDecimal.valueOf(ChronoUnit.DAYS.between(startDate, endDate));

            if (overlappingPeriod.compareTo(BigDecimal.ZERO) > 0)
                insertBillSegment(bss, serviceElement, quantity, priceObject, overlappingPeriod, startDate, endDate);
        }
    }

    private void creatingBcBillSegment(List<BillSegment> bss, ServiceElement serviceElement, Article article)
    {
        var billSegment = new BillSegment();
        billSegment.setSeId(serviceElement.getId());
        billSegment.setSe(serviceElement);
        billSegment.setSeType(serviceElement.getSeTypeId());
        billSegment.setVatRate(article.getVatRate());
        billSegment.setTou(article.getTou());
        billSegment.setArticleId(article.getId());
        billSegment.setPrice(article.getUnitPrice());
        billSegment.setPriceUnit(article.getUnitOfUnitPrice());
        billSegment.setQuantityUnit(article.getUnitOfQuantity());
        billSegment.setQuantity(article.getQuantity());
        billSegment.setStartDate(article.getStartDate());
        billSegment.setEndDate(article.getEndDate());
        billSegment.setAmount(article.getAmount());
        billSegment.setStatus(BillSegmentStatus.CALCULATED);
        billSegment.setSchema(BaseValidator.BS_SCHEMA_NORMAL);
        //billSegment.setNature(MeterReadQuality.REAL_CORRECTED);
        log.debug("BillSegment to Insert -> {}", billSegment);
        bss.add(billSegment);
        if (article.getArticleType().getBillingScheme() == null) {
            String serviceBillingScheme = serviceElement.getBillingScheme();
            if (serviceBillingScheme != null && BaseValidator.BILLING_SCHEME_REVERSE.equals(serviceBillingScheme)) {
                bss.add(reverseBillSegment(billSegment));
            }
        } else if (BaseValidator.BILLING_SCHEME_REVERSE.equals(article.getArticleType().getBillingScheme())) {
            bss.add(reverseBillSegment(billSegment));
        }
    }

    private void insertBillSegment(List<BillSegment> bss, ServiceElement serviceElement, QuantityObject quantity, PriceObject priceObject, BigDecimal overlappingPeriod, LocalDate startDate, LocalDate endDate) throws LauncherException
    {
        var billSegment = new BillSegment();

        billSegment.setSeId(serviceElement.getId());
        billSegment.setSe(serviceElement);
        billSegment.setTouGroup(serviceElement.getTouGroup());
        billSegment.setVatRate(serviceElement.getVatRate());
        billSegment.setSeType(serviceElement.getSeTypeId());
        billSegment.setTou(serviceElement.getTou());
        if (serviceElement.isMetered())
            billSegment.setMeterReadId(quantity.getMeterReadId());
        try
        {
            var totalPeriod = BigDecimal.valueOf(ChronoUnit.DAYS.between(quantity.getStartDate(), quantity.getEndDate()));
            var billSegmentQuantity = determineBillSegQuantity(overlappingPeriod, totalPeriod, quantity.getQuantity());

            log.debug("BillSegment Quantity -> {} | From {} | To {} | OverlappingPeriod {} | TotalPeriod -> {}", billSegmentQuantity, startDate, endDate,
                    overlappingPeriod, totalPeriod);

            billSegment.setPrice(priceObject.getPrice());
            billSegment.setStartDate(startDate);
            billSegment.setEndDate(endDate);
            billSegment.setPriceUnit(priceObject.getUnit());
            billSegment.setNature(quantity.getQuality());
            billSegment.setQuantityUnit(billingUtils.setQuantityAndPriceUnit(quantity.getUnit(), priceObject));
            billSegment.setQuantity(billSegmentQuantity);
                if(priceObject.getUnit().equalsIgnoreCase(BaseValidator.UNIT_PERCENTAGE))
                        billSegment.setAmount(billingUtils.calculatePercentage(billSegmentQuantity, priceObject.getPrice()));
                else    billSegment.setAmount(billingUtils.calculateAmount(billSegmentQuantity, priceObject.getPrice()));
            billSegment.setQuantityThreshold(serviceElement.getThreshold());
            billSegment.setQuantityThresholdBase(serviceElement.getThresholdBase());
            billSegment.setPriceThreshold(priceObject.getThreshold() == null ? null : priceObject.getThreshold().toString());
            billSegment.setPriceThresholdBase(priceObject.getThresholdBase());
            billSegment.setStatus(BillSegmentStatus.CALCULATED);
            billSegment.setSchema(BaseValidator.BS_SCHEMA_NORMAL);
            log.debug("BillSegment to Insert -> {}", billSegment);
        }
        catch (LauncherException e)
        {
            log.error("BillSegment insertion failed. Check in Journal for BillSegment -> " + billSegment, e);
            journalUtils.addJournal(ObjectType.SERVICE_ELEMENT, serviceElement.getId(),
                                    "BILL_SEGMENT_EXCEPTION", 
                                    new Object[]{e.toString()},
                                    null,
                                    JournalAction.ERROR
                                    );
            throw e;
        }
        finally
        { 
            bss.add(billSegment);
            if (serviceElement.getBillingScheme() !=null && serviceElement.getBillingScheme().equals(BaseValidator.BILLING_SCHEME_REVERSE))
                bss.add(reverseBillSegment(billSegment));
        }
    }

    public void startServices (BillSegment bs) //throws LauncherException
    {
        if (bs.getSe().getStatus().equals(SeStatus.PENDING_START) && bs.getStartDate().isEqual(bs.getSe().getStartDate()))
            startTosAndServices(bs.getSe());
    }

    public void stopServices (BillSegment bs) //throws LauncherException
    {
        if (bs.getSe().getStatus().equals(SeStatus.PENDING_STOP) && bs.getEndDate().isEqual(bs.getSe().getEndDate()))
            stopTosAndServices(bs.getSe());
    }

    public void startTosAndServices(ServiceElement serviceElement) //throws LauncherException
    {
        serviceElement.setStatus(SeStatus.ACTIVE);
        var tos = termOfServiceRepository.findById(serviceElement.getTosId()).get();
        if (tos.getStatus().equals(TosStatus.PENDING_START))
            tos.setStatus(TosStatus.ACTIVE);
        var service = serviceRepository.findById(tos.getServiceId()).get();
        if (service.getStatus().equals(ServiceStatus.INSTALLED))
            service.setStatus(ServiceStatus.ACTIVE);
    }

    public void stopTosAndServices(ServiceElement serviceElement) //throws LauncherException
    {
            serviceElement.setStatus(SeStatus.STOPPED);
            var tos = termOfServiceRepository.findById(serviceElement.getTosId()).get();
            if (tos.getStatus().equals(TosStatus.PENDING_STOP))
                tos.setStatus(TosStatus.STOPPED);
            var service = serviceRepository.findById(tos.getServiceId()).get();
            if (service.getStatus().equals(ServiceStatus.PENDING_STOP))
                service.setStatus(ServiceStatus.TERMINATED);
    }

    private BillSegment reverseBillSegment(BillSegment billSegment) //throws LauncherException
    {
        BillSegment reverseBillSegment = new BillSegment();
        reverseBillSegment.setAmount(billSegment.getAmount().multiply(new BigDecimal(-1)));
        reverseBillSegment.setBillId(billSegment.getBillId());
        reverseBillSegment.setEndDate(billSegment.getEndDate());
        reverseBillSegment.setMeterReadId(billSegment.getMeterReadId());
        reverseBillSegment.setNature(billSegment.getNature());
        reverseBillSegment.setPrice(billSegment.getPrice().multiply(new BigDecimal(-1)));
        reverseBillSegment.setPriceThreshold(billSegment.getPriceThreshold());
        reverseBillSegment.setPriceThresholdBase(billSegment.getPriceThresholdBase());
        reverseBillSegment.setPriceUnit(billSegment.getPriceUnit());
        reverseBillSegment.setQuantity(billSegment.getQuantity());
        reverseBillSegment.setQuantityThreshold(billSegment.getQuantityThreshold());
        reverseBillSegment.setQuantityThresholdBase(billSegment.getQuantityThresholdBase());
        reverseBillSegment.setQuantityUnit(billSegment.getQuantityUnit());
        reverseBillSegment.setSe(billSegment.getSe());
        reverseBillSegment.setSeId(billSegment.getSeId());
        reverseBillSegment.setSeType(billSegment.getSeType());
        reverseBillSegment.setStartDate(billSegment.getStartDate());
        reverseBillSegment.setStatus(billSegment.getStatus());
        reverseBillSegment.setTou(billSegment.getTou());
        reverseBillSegment.setTouGroup(billSegment.getTouGroup());
        reverseBillSegment.setVatRate(billSegment.getVatRate());
        reverseBillSegment.setSchema(BaseValidator.BS_SCHEMA_REVERSE);
        return reverseBillSegment;
    }

    private BigDecimal determineBillSegQuantity(BigDecimal overlappingPeriod, BigDecimal totalPeriod, BigDecimal quantity) throws LauncherException
    {
        return quantity.multiply(overlappingPeriod).divide(totalPeriod, billingUtils.getMathContext());
    }
}