CREATE TABLE IF NOT EXISTS BL_RATE_TYPE
(
   rateType varchar(255) PRIMARY KEY,
   market varchar(55),
   category varchar(55),
   subCategory varchar(55),
   defaultLabel varchar(255),
   description varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_COMPANY
(
   companyid bigserial PRIMARY KEY,
   companyname varchar(255),
   legalformcode varchar(255),
   identificationid varchar(255),
   vatid varchar(255),
   vatable boolean,
   nacecode varchar(255)
);

CREATE TABLE IF NOT EXISTS BL_RATE
(
   rateId bigserial PRIMARY KEY,
   rateType varchar(255) REFERENCES BL_RATE_TYPE(rateType),
   priceType varchar(255),
   market varchar(255),
   startDate date,
   endDate date,
   customerType varchar(255),
   customerCategory varchar(255),
   channel varchar(255),
   installmentFrequency varchar(255),
   serviceCategory varchar(255),
   serviceSubCategory varchar(255),
   posCategory varchar(255),
   touGroup varchar(45),
   gridRate varchar(55),
   dgoCode varchar(55),
   tgoCode varchar(55),
   tou varchar(55),
   unit varchar(55),
   threshold numeric(15,4),
   thresholdType varchar(55),
   thresholdBase varchar(55),
   price numeric(15,4)
);

CREATE TABLE IF NOT EXISTS CC_ACTIVITY_TYPE
(
   activityType varchar(255) PRIMARY KEY,
   defaultLabel varchar(255),
   category varchar(255),
   subCategory varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_ACTIVITY
(
   activityId bigserial PRIMARY KEY,
   activityType varchar(255) REFERENCES CC_ACTIVITY_TYPE(activityType),
   category varchar(255),
   subCategory varchar(255),
   status varchar(255),
   startDate timestamp with time zone,
   endDate timestamp with time zone,
   createdBy varchar(255)
);


CREATE TABLE IF NOT EXISTS CC_ACTIVITY_MANAGEMENT_RULES
(
   id varchar(255) PRIMARY KEY,
   activityInitialStatus varchar(255),
   activityFinalStatus varchar(255),
   eventFinalStatus varchar(255),
   processFinalStatus varchar(255)
);

CREATE TABLE IF NOT EXISTS RF_POSTAL_CODE
(
   id bigserial PRIMARY KEY,
   postalCode varchar(255) NOT NULL,
   cityName varchar(255) NOT NULL,
   dispatchCityName varchar(255),
   otherName varchar(255),
   gpsLocation varchar(255),
   areaCode varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS CC_ADDRESS
(
   addressId bigserial PRIMARY KEY,
   street varchar(255),
   number varchar(255),
   box varchar(255),
   postalCode varchar(255) NOT NULL,
   city varchar(255),
   countryCode varchar(255)
);

CREATE TABLE IF NOT EXISTS RF_READING_CYCLE
(
   readingCycle varchar(255) PRIMARY KEY,
   periodicity varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_POS
(
   posId bigserial PRIMARY KEY,
   reference varchar(255),
   addressId bigint REFERENCES CC_ADDRESS(addressId),
   market varchar(10),
   tgoCode varchar(255),
   dgoCode varchar(255),
   deliveryState varchar(55),
   deliveryStatus varchar(55),
   temporaryConnection boolean,
   temporaryConnectionType varchar(10),
   direction varchar(25),
   readingCycleId varchar(255) REFERENCES RF_READING_CYCLE(readingCycle)
);

CREATE TABLE IF NOT EXISTS TR_LANGUAGE_MAPPING (
    id SERIAL PRIMARY KEY,
    locale VARCHAR(10) NOT NULL, -- Ex: 'fr', 'en', 'es'
    labelColumn VARCHAR(50) NOT NULL, -- Ex: 'standardLabel', 'otherLabel', 'defaultLabel'
    active boolean
);

CREATE TABLE IF NOT EXISTS RF_RP_RC
(
   id bigserial PRIMARY KEY,
   readingPeriod varchar(255),
   market varchar(10),
   readingFrequency varchar(255),
   readingCycleId varchar(255) REFERENCES RF_READING_CYCLE(readingCycle)
);

CREATE TABLE IF NOT EXISTS BL_BILLING_CYCLE(
   billingCycle varchar(255) PRIMARY KEY,
   periodicity varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS BL_RC_BC
(
   id bigserial PRIMARY KEY,
   readingCycleId varchar(255) REFERENCES RF_READING_CYCLE(readingCycle),
   market varchar(10),
   billingFrequency varchar(255),
   billingCycleId varchar(255) REFERENCES BL_BILLING_CYCLE(billingCycle)
);

CREATE TABLE IF NOT EXISTS BL_BILLING_WINDOW
(
   Id bigserial PRIMARY KEY,
   billingCycleId varchar(255) REFERENCES BL_BILLING_CYCLE(billingCycle),
   billingFrequency varchar(255),
   startDate varchar(255)
);


CREATE TABLE IF NOT EXISTS BL_BILLING_RUN (
   Id bigserial PRIMARY KEY,
   billType varchar(55),
   billingCycleId varchar(255) REFERENCES BL_BILLING_CYCLE(billingCycle),
   billingWindowId bigint REFERENCES BL_BILLING_WINDOW(id),
   runDate date,
   startDate date,
   endDate date,
   status varchar(255)
);


CREATE TABLE IF NOT EXISTS CC_CONTACT
(
   contactId bigserial PRIMARY KEY,
   phone1 varchar(255),
   phone2 varchar(255),
   phone3 varchar(255),
   email varchar(255),
   fax varchar(255),
   deliveryModeCode varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_CONTRACT
(
   id bigserial PRIMARY KEY,
   reference varchar(255) UNIQUE,
   status varchar(255),
   market varchar(255),
   contractualStartDate date,
   contractualEndDate date,
   installPeriodicity varchar(255),
   billingMode varchar(55),
   billingFrequency varchar(255),
   billingCycleId varchar(255) REFERENCES BL_BILLING_CYCLE(billingCycle),
   billAfterDate date,
   channel varchar(55),
   seller varchar(55),
   serviceCategory varchar(55),
   serviceSubCategory varchar(55),
   direction varchar(55),
   effectiveEndDate date,
   subscriptionDate date
);

CREATE TABLE IF NOT EXISTS CC_CONTRACT_POS
(
   id bigserial PRIMARY KEY,
   contractId bigint REFERENCES CC_CONTRACT(id),
   posId bigint REFERENCES CC_POS(posId),
   startDate date,
   endDate date
);


CREATE TABLE IF NOT EXISTS CC_INDIVIDUAL
(
   individualId bigserial PRIMARY KEY,
   titleCode varchar(255),
   lastname varchar(255),
   firstName varchar(255),
   birthDate date,
   addressIdOfBirthPlace bigint REFERENCES CC_ADDRESS(addressId),
   birthplace varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_CUSTOMER
(
   id bigserial PRIMARY KEY,
   reference varchar(255) UNIQUE,
   category varchar(255),
   languageCode varchar(255),
   type varchar(55),
   companyId bigint REFERENCES CC_COMPANY(companyId),
   individualId bigint REFERENCES CC_INDIVIDUAL(individualId),
   contactId bigint REFERENCES CC_CONTACT(contactId),
   addressId bigint REFERENCES CC_ADDRESS(addressId),
   status varchar(255),
   creationDate timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS TR_LAUNCHER_TAG_TYPE
(
   launcherTagType varchar(255) PRIMARY KEY,
   rank int,
   category varchar(255),
   subCategory varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   executionMode varchar(255),
   synchronous boolean,
   packetSize int,
   active boolean
);

CREATE TABLE IF NOT EXISTS TR_ORGANISM
(
   id bigserial PRIMARY KEY,
   companyId bigint REFERENCES CC_COMPANY(companyId),
   isMaster boolean,
   masterOrganismId bigint,
   category varchar(255),
   subCategory varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   description varchar(255),
   picture varchar(255)
);

CREATE TABLE IF NOT EXISTS TR_GROUP
(
   id bigserial PRIMARY KEY,
   organismId bigint REFERENCES TR_ORGANISM(id),
   "group" varchar(255),
   category varchar(255),
   subCategory varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   description varchar(255),
   "authorization" text
);

CREATE TABLE IF NOT EXISTS TR_USER
(
   id bigserial PRIMARY KEY,
   userName varchar(255) UNIQUE NOT NULL,
   individualId bigint REFERENCES CC_INDIVIDUAL(individualId),
   contactId bigint REFERENCES CC_CONTACT(contactId),
   groupId bigint REFERENCES TR_GROUP(id),
   organismId bigint REFERENCES TR_ORGANISM(id),
   master boolean,
   readOnly boolean,
   picture varchar(255),
   active boolean,
   userRole varchar(255),
   status varchar(255),
   defaultLanguage varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_EVENT_TEMPLATE
(
   eventType varchar(255) PRIMARY KEY,
   defaultLabel varchar(255),
   defaultExecutionMode varchar(255),
   category varchar(255),
   subCategory varchar(255),
   launcherTagType varchar(255) REFERENCES TR_LAUNCHER_TAG_TYPE(launcherTagType),
   defaultStatus varchar(255),
   action varchar(255),
   defaultHolder bigint REFERENCES TR_USER(id),
   groupId bigint REFERENCES TR_GROUP(id),
   organismId bigint REFERENCES TR_ORGANISM(id),
   triggerDateMode varchar(255),
   periodSystem varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   recurrencePeriod int,
   recurrencePeriodType varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_EVENT
(
   eventId bigserial PRIMARY KEY,
   activityId bigint REFERENCES CC_ACTIVITY(activityId),
   eventType varchar(255) REFERENCES CC_EVENT_TEMPLATE(eventType),
   action varchar(255),
   launcherTag varchar(255) REFERENCES TR_LAUNCHER_TAG_TYPE(launcherTagType),
   rank bigint,
   triggerDate date,
   creationDate timestamp with time zone,
   status varchar(255),
   executionDate timestamp with time zone,
   executionMode varchar(255),
   userId bigint REFERENCES TR_USER(id),
   groupId bigint REFERENCES TR_GROUP(id),
   organismId bigint REFERENCES TR_ORGANISM(id),
   externalEventRef varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_ACTIVITY_TEMPLATE
(
   id bigserial PRIMARY KEY,
   activityType varchar(255) REFERENCES CC_ACTIVITY_TYPE(activityType),
   rank int DEFAULT 0,
   defaultStatus varchar(255),
   startDatePeriod int DEFAULT 0,
   eventType varchar(255) REFERENCES CC_EVENT_TEMPLATE(eventType)
);

CREATE TABLE IF NOT EXISTS CC_EVENT_MANAGEMENT_RULES
(
   id varchar(255) PRIMARY KEY,
   eventInitialStatus varchar(255),
   eventFinalStatus varchar(255),
   executionMode varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_FINANCIAL_INFORMATION
(
   financialInformationId bigserial PRIMARY KEY,
   paymentModeCode varchar(255),
   domicilationId varchar(255),
   domicilationStatus varchar(255),
   iban varchar(255),
   bicCode varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_METER_READ
(
   id bigserial PRIMARY KEY,
   originalRef varchar(255),
   posRef varchar(255),
   startDate date,
   endDate date,
   readingDate date,
   receptionDate date,
   type varchar(255),
   quality varchar(255),
   context varchar(255),
   source varchar(255),
   status varchar(255),
   totalQuantity numeric(15,4),
   unit varchar(255),
   climaticCoef varchar(255),
   calorificCoef varchar(255),
   touGroup varchar(255),
   market varchar(255),
   direction varchar(255),
   cancelledBy bigint
);

CREATE TABLE IF NOT EXISTS TR_SEQUENCE_MANAGER (
    id bigserial PRIMARY KEY,
    sequencename VARCHAR(255) NOT NULL UNIQUE,
    startvalue bigint NOT NULL,
    lastvalue bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS CC_METER_READ_DETAIL
(
   id bigserial PRIMARY KEY,
   meterReadId bigint REFERENCES CC_METER_READ(id),
   startDate date,
   endDate date,
   startValue varchar(255),
   endValue varchar(255),
   quantity numeric(15,4),
   unit varchar(255),
   tou varchar(255),
   measureType varchar(255),
   source varchar(255),
   quality varchar(255),
   gridCode varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_INDEX
(
   indexId bigserial PRIMARY KEY,
   meterReadId bigint REFERENCES CC_METER_READ(id),
   startDate date,
   endDate date,
   startIndex varchar(255),
   endIdex varchar(255),
   unit varchar(255),
   tou varchar(255),
   source varchar(255),
   quality varchar(255),
   gridCode varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_METER
(
   meterId bigserial PRIMARY KEY,
   posId bigint REFERENCES CC_POS(posId),
   startDate date,
   endDate date,
   meterNumber varchar(255),
   meterType varchar(55),
   smartMeterStatus varchar(25),
   convertCoef varchar(255),
   digitNumber varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_PERIMETER_TYPE
(
   perimeterType varchar(64) PRIMARY KEY,
   billable boolean,
   standardLabel varchar(255),
   defaultLabel varchar(255),
   otherLabel varchar(255),
   printShopTemplate varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_PERIMETER
(
   perimeterId bigserial PRIMARY KEY,
   reference varchar(255),
   customerId bigint REFERENCES CC_CUSTOMER(id),
   startDate date,
   endDate date,
   analyticCode varchar(255),
   perimeterType varchar(64) REFERENCES CC_PERIMETER_TYPE(perimeterType),
   billingCycleId varchar(255) REFERENCES BL_BILLING_CYCLE(billingCycle),
   billAfterDate date,
   billingFrequency varchar(255),
   market varchar(255),
   status varchar(45)
);

CREATE TABLE IF NOT EXISTS CC_CONTRACT_PERIMETER
(
   id bigserial PRIMARY KEY,
   contractId bigint REFERENCES CC_CONTRACT(id),
   perimeterId bigint REFERENCES CC_PERIMETER(perimeterId),
   startDate date,
   endDate date
);

CREATE TABLE IF NOT EXISTS CC_POS_CAPACITY
(
   posCapacityId bigserial PRIMARY KEY,
   posId bigint REFERENCES CC_POS(posId),
   startDate date,
   endDate date,
   tou varchar(255),
   "value" numeric(15,4),
   capacityType varchar(25),
   unit varchar(255),
   source varchar(255),
   status varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_POS_CONFIGURATION
(
   posConfigurationId bigserial PRIMARY KEY,
   posId bigint REFERENCES CC_POS(posId),
   startDate date,
   endDate date,
   gridRate varchar(255),
   profile varchar(55),
   posCategory varchar(55),
   netArea varchar(25),
   readingFrequency varchar(25),
   source varchar(25),
   status varchar(25),
   touGroup varchar(25),
   businessGridCode varchar(255),
   marketGridCode varchar(255),
   readingperiode varchar(25)
);


CREATE TABLE IF NOT EXISTS CC_POS_ESTIMATE
(
   posEstimateId bigserial PRIMARY KEY,
   posId bigint REFERENCES CC_POS(posId),
   startDate date,
   endDate date,
   tou varchar(255),
   "value" numeric(15,4),
   estimateType varchar(25),
   unit varchar(255),
   source varchar(255),
   status varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_TOS_TYPE
(
   tosType varchar(255) PRIMARY KEY,
   category varchar(255),
   subCategory varchar(255),
   defaultLabel varchar(255),
   description varchar(255),
   market varchar(10),
   "default" boolean,
   master boolean,
   startDate date,
   endDate date,
   exclusive boolean,
   touGroup varchar(55),
   standardLabel varchar(255),
   otherLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_TOS_START_OPTION
(
   id bigserial PRIMARY KEY,
   tosType varchar(255) REFERENCES CC_TOS_TYPE(tosType),
   startDate date,
   endDate date,
   market varchar(55),
   initialDuration int,
   minimumDuration int,
   renewalTosType varchar(255),
   renewalDuration varchar(255),
   priceMode varchar(55),
   priceType varchar(255),
   refDateTypeForFixedPrice varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_SERVICE_TYPE
(
   serviceType varchar(255) PRIMARY KEY,
   startDate date,
   endDate date,
   category varchar(255),
   subCategory varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   description varchar(255),
   isDefaultService boolean
);

CREATE TABLE IF NOT EXISTS CC_SERVICE
(
   serviceId bigserial PRIMARY KEY,
   contractId bigint REFERENCES CC_CONTRACT(id),
   serviceTypeId varchar(255) REFERENCES CC_SERVICE_TYPE(serviceType),
   startDate date,
   endDate date,
   touGroup varchar(255),
   tou varchar(255),
   direction varchar(55),
   amount numeric(15,4),
   threshold varchar(255),
   thresholdType varchar(255),
   thresholdBase varchar(255),
   operand varchar(255),
   operandType varchar(255),
   factor varchar(255),
   factorType varchar(255),
   rateType varchar(255),
   status varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_SERVICE_START_OPTION
(
   id bigserial PRIMARY KEY,
   seller varchar(255),
   channel varchar(255),
   startDate date,
   endDate date,
   serviceCategory varchar(55),
   serviceSubCategory varchar(55),
   market varchar(255),
   customerType varchar(255),
   customerCategory varchar(255),
   billingMode varchar(255),
   paymentMode varchar(255),
   posCategory varchar(255),
   direction varchar(255),
   dgoCode varchar(255),
   tgoCode varchar(255),
   consumptionThreshold varchar(255),
   touGroup varchar(255),
   service varchar(255) NOT NULL REFERENCES CC_SERVICE_TYPE(serviceType),
   tosType varchar(255) NOT NULL REFERENCES CC_TOS_TYPE(tosType)
);

CREATE TABLE IF NOT EXISTS CC_TERM_OF_SERVICES
(
   tosId bigserial PRIMARY KEY,
   tosTypeId varchar(55) REFERENCES CC_TOS_TYPE(tosType),
   startDate date,
   endDate date,
   contractId bigint REFERENCES CC_CONTRACT(id),
   serviceId bigint  REFERENCES CC_SERVICE(serviceId),
   market varchar(10),
   direction varchar(55),
   touGroup varchar(55),
   estimateAuthorized boolean,
   priceType varchar(255),
   refDateForFixedPrice date,
   initialDuration int,
   minimumDuration int,
   "default" boolean,
   master boolean,
   exclusive boolean,
   status varchar(255)
);

CREATE TABLE IF NOT EXISTS CC_THIRD
(
   thirdId bigserial PRIMARY KEY,
   type varchar(255),
   addressId bigint REFERENCES CC_ADDRESS(addressId),
   financialInformationId bigint REFERENCES CC_FINANCIAL_INFORMATION(financialInformationId),
   individualId bigint REFERENCES CC_INDIVIDUAL(individualId),
   companyId bigint REFERENCES CC_COMPANY(companyId),
   contactId bigint REFERENCES CC_CONTACT(contactId)
);

CREATE TABLE IF NOT EXISTS PA_ACCOUNTING_DOCUMENT_PREPAYMENT
(
   installmentId varchar(255) PRIMARY KEY,
   invoiceDate date,
   invoiceNature varchar(255),
   endDate date,
   startDate date,
   runDate date,
   runId varchar(255),
   paymentMode varchar(255),
   contractId bigint REFERENCES CC_CONTRACT(id),
   totalAmount numeric(15,4),
   vatNR varchar(45),
   vatRR varchar(45),
   totalVat numeric(15,4),
   totalWithoutVat numeric(15,4),
   status varchar(255),
   processId bigint,
   amountToPay numeric(15,4)
);

CREATE TABLE IF NOT EXISTS TR_EVENT_MANAGER
(
   id bigserial PRIMARY KEY,
   createdat timestamp with time zone,
   rank int,
   launcherTagType varchar(255) REFERENCES TR_LAUNCHER_TAG_TYPE(launcherTagType),
   defaultLabel varchar(255),
   executionMode varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_ACCOUNTING_DOCUMENT_INVOICE
(
   billId varchar(255) PRIMARY KEY,
   cancelledBillId varchar(255),
   billDate date,
   dueDate date,
   billNature varchar(255),
   endDate date,
   startDate date,
   runDate date,
   runId varchar(255),
   paymentMode varchar(255),
   contractId bigint REFERENCES CC_CONTRACT(id),
   totalAmount numeric(15,4),
   vatNR varchar(45),
   vatRR varchar(45),
   totalVat numeric(15,4),
   totalWithoutVat numeric(15,4),
   status varchar(255),
   processId bigint,
   amountPaid numeric(15,4),
   amountToPay numeric(15,4)
);

CREATE TABLE IF NOT EXISTS PA_ADHOC_PAYMENT
(
   adhocPaymentId varchar(255) PRIMARY KEY,
   creationDate date,
   dueDate date,
   startDate date,
   endDate date,
   paymentStatus varchar(255),
   paymentNature varchar(255),
   paymentMode varchar(255),
   amount numeric(15,4),
   externalReference varchar(255),
   intermediate varchar(255),
   initialTransactionId varchar(255),
   initialTransactionStatus varchar(255),
   contractId bigint,
   pieceReference varchar(255),
   processId bigint,
   amountToPay numeric(15,4),
   amountPaid numeric(15,4),
   sepaMandate varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_PROCESS
(
   processId bigserial PRIMARY KEY,
   invoiceId varchar(255),
   nature varchar(255),
   accountancyTerms varchar(255),
   amount numeric(15,4),
   intermediate varchar(255),
   paymentMethod varchar(255),
   status varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_PROCESSING_LAUNCHER_RULES
(
   processingToLaunch varchar(255) NOT NULL,
   processingToCheck varchar(255) NOT NULL,
   statusProcessingToCheck varchar(255),
   PRIMARY KEY(processingToLaunch,processingToCheck)
);

CREATE TABLE IF NOT EXISTS TR_RELATION
(
   id bigserial PRIMARY KEY,
   relationType varchar(255),
   firstObjectId bigint,
   secondObjectId bigint,
   secondObjectType varchar(255),
   createdAt timestamp with time zone
);

CREATE TABLE IF NOT EXISTS PA_TRANSACTION_VALIDATION_RULES
(
   id bigserial PRIMARY KEY,
   action varchar(255),
   root varchar(255),
   mode varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   pieceReferenceStatus varchar(255),
   pieceReferenceType varchar(255),
   intermediate varchar(255),
   paymentMode varchar(255),
   processNature varchar(255),
   accountancyTerms varchar(255),
   initialStatus varchar(255),
   threshold varchar(255),
   delayDays int,
   checkIntermediate boolean,
   intermediateLinks varchar(255),
   intermediateAction varchar(255),
   finalStatus varchar(255),
   processFinalStatus varchar(255),
   updatePiece boolean,
   activityToCreate varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_TRANSACTION
(
   transactionId varchar(255) PRIMARY KEY,
   processId bigint REFERENCES PA_PROCESS(processId),
   intermediate varchar(255),
   transfertStatus varchar(255),
   transfertDate date,
   transactionFailureMotive varchar(255),
   transfertFailureMotive varchar(255),
   transfertPattern varchar(255),
   transactionStatus varchar(255),
   transactionDate varchar(255),
   dueDate date,
   amount numeric(15,4),
   paymentMode varchar(255),
   transactionLabel varchar(255),
   financialInformationId bigint REFERENCES CC_FINANCIAL_INFORMATION(financialInformationId),
   pieceReference varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_ALLOCATION
(
   allocationId bigserial PRIMARY KEY,
   transactionId varchar(255) REFERENCES PA_TRANSACTION(transactionId),
   nature varchar(255),
   initialTransactionAmount numeric(15,4),
   amountToAllocate numeric(15,4),
   startDate date,
   endDate date,
   invoiceId varchar(255),
   status varchar(255),
   contractId bigint,
   linkedTo varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_ALLOCATION_RULES
(
   processNature varchar(255) PRIMARY KEY,
   toAllocate varchar(255),
   allocationMode varchar(255),
   directAllocation varchar(255),
   allocationTarget varchar(255),
   transactionLabel varchar(255),
   allocationOrder varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_BANK_ACCOUNT
(
   bankAccountId varchar(255) PRIMARY KEY,
   bankCode varchar(255),
   bankName varchar(255),
   bankAccountNumber varchar(255),
   startBalance varchar(255),
   status varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_BANK_FINANCIAL_INFORMATION_TRACKING
(
   idImport varchar(255) PRIMARY KEY,
   bankCode varchar(255),
   bankCptNumber varchar(255),
   fromDate date,
   startBalance varchar(255),
   toTate date,
   endBalance varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_BANK_FINANCIAL_TRANSACTION
(
   idImport varchar(255),
   idBkTransaction varchar(255) PRIMARY KEY,
   idTransaction varchar(255),
   referenceDate date,
   transactionDate date,
   transaction_type varchar(255),
   devise varchar(255),
   transactionSens varchar(255),
   pieceNumber varchar(255),
   transactionAmount numeric(15,4),
   remainsAmount numeric(15,4),
   transactionLabel varchar(255),
   transactionLabelAdd varchar(255),
   transactionStatus varchar(255),
   KEYment varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_BANK_REFERENCE
(
   codeOperation varchar(255) PRIMARY KEY,
   category varchar(255),
   label varchar(255),
   default_label varchar(255),
   sens varchar(255),
   defaultStatus varchar(255),
   paymentMode varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_BANK_TRANSACTION_ALLOC_PROPOSAL
(
   proposalId bigserial PRIMARY KEY,
   bankTransactionId varchar(255),
   intermediate varchar(255) DEFAULT 'bank',
   paymentModeCode varchar(255),
   proposalDate date,
   proposalStatus varchar(255) DEFAULT 'toValidate',
   proposalType varchar(255),
   transactionDate date,
   initialTransactionAmount numeric(15,4),
   amountToAllocate numeric(15,4),
   counterpartId varchar(255),
   counterpartType varchar(255),
   counterpartProcessId varchar(255),
   validationMode varchar(255),
   KEYment varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_COLLECTION_CLASS
(
   collectionClassCode varchar(255) PRIMARY KEY,
   collectionClassLabel varchar(255),
   collectionClassDescription varchar(255),
   processNature varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_COLLECTION_CLASS_CONTROL
(
   id bigserial PRIMARY KEY,
   collectionClassCode varchar(255) REFERENCES PA_COLLECTION_CLASS(collectionClassCode),
   customerCategory varchar(255),
   customerType varchar(255),
   social varchar(255),
   paymentMode varchar(255),
   intermediate varchar(255),
   debtNature varchar(255),
   collectionMode varchar(255),
   debtAgeThreshold int DEFAULT 0,
   debtAmountThreshold numeric(15,4) DEFAULT 0.00,
   gracePeriod int DEFAULT 0,
   scoreThreshold varchar(255),
   collectionActivityType varchar(255),
   active boolean
);

CREATE TABLE IF NOT EXISTS PA_PAYMENT_ISSUES
(
   paymentIssueId bigserial PRIMARY KEY,
   paymentType varchar(255),
   subscriberReference varchar(255),
   initialTransactionId varchar(255),
   payerKEYmpanyName varchar(255),
   payerFullName varchar(255),
   payerEmail varchar(255),
   payerPhone varchar(255),
   initialTransactionDate date,
   initialTransactionAmount numeric(15,4),
   bic varchar(255),
   iban varchar(255),
   bbanAccount varchar(255),
   bbanBank varchar(255),
   bbanBranch varchar(255),
   bbanKEY varchar(255),
   initialTransactionLabel varchar(255),
   initialInvoiceId varchar(255),
   issueType varchar(255),
   issueDate date,
   issueCode varchar(255),
   issueDescription varchar(255),
   mandateId varchar(255),
   processingStatus varchar(255),
   failureReason varchar(255),
   intermediate varchar(255)
);

CREATE TABLE IF NOT EXISTS RF_CLIMATIC_REF
(
   id bigserial PRIMARY KEY,
   weatherChannelCode varchar(255),
   market varchar(255),
   profile varchar(255),
   zi numeric(15,4)
);

CREATE TABLE IF NOT EXISTS RF_MARKET_GEO_REF
(
   id bigserial PRIMARY KEY,
   areaCode varchar(255) NOT NULL,
   market varchar(255) NOT NULL,
   startDate date NOT NULL,
   endDate date,
   dgoRank varchar(255) NOT NULL,
   dispatchRate varchar(255),
   dgoRank_1 varchar(255) NOT NULL,
   dgoRank_2 varchar(255),
   netAreaCode varchar(255),
   netAreaLabel varchar(255),
   weatherChannelCode varchar(255),
   climaticZone varchar(255),
   proximityRateCoef numeric(15,4),
   tgoCode varchar(255),
   nbrOfPos varchar(255),
   energyNature varchar(255),
   regionalRateLevel numeric(15,4),
   balanceZone varchar(255),
   coefA varchar(255)
);

CREATE TABLE IF NOT EXISTS RF_PROFILE
(
   id bigserial PRIMARY KEY,
   date date NOT NULL,
   market varchar(255) NOT NULL,
   profileType varchar(255),
   profile varchar(255) NOT NULL,
   subProfile varchar(255),
   step int,
   coef bigserial
);

CREATE TABLE IF NOT EXISTS RF_TOU
(
   id bigserial PRIMARY KEY,
   touGroup varchar(255),
   tou varchar(255),
   rank varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   periodStart date,
   periodEnd date,
   nbrOfHours int
);

CREATE TABLE IF NOT EXISTS RF_TOU_GROUP
(
   id bigserial PRIMARY KEY,
   market varchar(255),
   customerType varchar(255),
   posCategory varchar(255),
   gridRate varchar(255),
   capacity varchar(255),
   gridCodeType varchar(255),
   touGroup varchar(255),
   gridCode varchar(255),
   profile varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS TR_CHARACTERISTIC_TYPE
(
   id varchar(255) PRIMARY KEY,
   characteristicCategory varchar(255),
   characteristicLabel varchar(255),
   characteristicValueType varchar(255),
   characteristicValueList varchar(255)
);

CREATE TABLE IF NOT EXISTS TR_CHARACTERISTIC
(
   id bigserial PRIMARY KEY,
   characteristicType varchar(255) REFERENCES TR_CHARACTERISTIC_TYPE(id),
   objectType varchar(255),
   objectId varchar(255),
   characteristicValue varchar(255),
   startDate date,
   endDate date
);

CREATE TABLE IF NOT EXISTS TR_OBJECT_PROCESS_RULES
(
   id bigserial PRIMARY KEY,
   objectType varchar(255) NOT NULL,
   initialStatus varchar(255),
   newStatus varchar(255) NOT NULL,
   seller varchar(255) NOT NULL,
   channel varchar(255) NOT NULL,
   direction varchar(255) NOT NULL,
   serviceCategory varchar(255) NOT NULL,
   serviceSubCategory varchar(255) NOT NULL,
   customerCategory varchar(255) NOT NULL,
   market varchar(255) NOT NULL,
   finalStatus varchar(255) NOT NULL,
   activityType varchar(255) REFERENCES CC_ACTIVITY_TYPE(activityType)
);


CREATE TABLE IF NOT EXISTS TR_DOCUMENT
(
   id bigserial PRIMARY KEY,
   objectType varchar(255),
   objectId bigint,
   name varchar(255),
   description varchar(255),
   type varchar(255),
   path varchar(255)
);

CREATE TABLE IF NOT EXISTS TR_JOURNAL
(
   id bigserial PRIMARY KEY,
   objectType varchar(255),
   objectId bigint,
   userName varchar(255),
   creationDate timestamp with time zone,
   method varchar(255),
   ipAdress varchar(255),
   userAgent varchar(255),
   forwardedFor varchar(255),
   apiPath varchar(255),
   comment text,
   newStatus varchar(255),
   messageCodes JSONB
);

CREATE TABLE IF NOT EXISTS TR_MONITORING
(
   id bigserial PRIMARY KEY,
   objectType varchar(255),
   objectId bigint,
   fileName varchar(255),
   action varchar(255),
   status varchar(255),
   date timestamp with time zone,
   message text
);

CREATE TABLE IF NOT EXISTS RF_PARAMETER
(
   id bigserial PRIMARY KEY,
   parameterType varchar(55),
   parameter varchar(55),
   category varchar(55),
   subCategory varchar(55),
   startDate date,
   "value" varchar(55),
   standardLabel varchar(255),
   defaultLabel varchar(255),
   otherLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS PA_PIECE_VALIDATION_RULES
(
   id bigserial PRIMARY KEY,
   action varchar(100),
   root varchar(255),
   mode varchar(255),
   defaultLabel varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   pieceType varchar(100),
   pieceNature varchar(255),
   intermediate varchar(100),
   paymentMode varchar(100),
   processNature varchar(255),
   accountancyTerms varchar(255),
   initialStatus varchar(100),
   threshold numeric(15,4),
   delayDays int,
   contractStatus varchar(100),
   checkTransaction boolean,
   finalStatus varchar(100),
   activityToCreate varchar(255)
);

CREATE TABLE IF NOT EXISTS BL_SE_TYPE
(
   seType varchar(255) PRIMARY KEY,
   seMaster boolean,
   masterSeTypeId varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   defaultLabel varchar(255),
   description varchar(255),
   seTypeCategory varchar(255),
   market varchar(255),
   direction varchar(255),
   metered boolean
);

CREATE TABLE IF NOT EXISTS BL_SQ_TYPE
(
   sqType varchar(55) PRIMARY KEY,
   measureType varchar(55),
   unit varchar(55),
   touGroup varchar(55),
   label varchar(255),
   description varchar(55)
);

CREATE TABLE IF NOT EXISTS BL_SE
(
   seId bigserial PRIMARY KEY,
   tosId bigint REFERENCES CC_TERM_OF_SERVICES(tosId),
   seTypeId varchar(255) REFERENCES BL_SE_TYPE(seType),
   seMaster boolean,
   category varchar(255),
   subCategory varchar(255),
   vatRate varchar(255),
   rateType varchar(255) REFERENCES BL_RATE_TYPE(rateType),
   operand varchar(255),
   operandType varchar(255),
   factor varchar(255),
   factorType varchar(255),
   metered boolean,
   billingScheme varchar(255),
   accountingScheme varchar(255),
   estimateAuthorized boolean,
   touGroup varchar(255),
   tou varchar(255),
   startDate date,
   endDate date,
   status varchar(255),
   minDayForEstimate int,
   seListBaseForSq varchar(255),
   threshold varchar(255),
   thresholdType varchar(255),
   thresholdBase varchar(255),
   sqType varchar(255) REFERENCES BL_SQ_TYPE(sqType)
);


CREATE TABLE IF NOT EXISTS BL_SE_START_OPTION
(
   id bigserial PRIMARY KEY,
   tosType varchar(255) REFERENCES CC_TOS_TYPE(tosType),
   seType varchar(255) REFERENCES BL_SE_TYPE(seType),
   startDate date,
   endDate date,
   vatRate varchar(255),
   premiseCondition varchar(255),
   premiseValue varchar(255),
   rateType varchar(255) REFERENCES BL_RATE_TYPE(rateType),
   operand varchar(255),
   operandType varchar(255),
   factor varchar(255),
   factorType varchar(255),
   billingScheme varchar(255),
   accountingScheme varchar(255),
   estimateAuthorized boolean,
   sqType varchar(255) REFERENCES BL_SQ_TYPE(sqType),
   touGroup varchar(255),
   tou varchar(255),
   defaultSeStatus varchar(255),
   minDayForEstimate int,
   analyticCode varchar(255),
   additionalCode varchar(255),
   threshold varchar(255),
   thresholdType varchar(255),
   thresholdBase varchar(255),
   seListBaseForSq varchar(255),
   category varchar(255),
   subCategory varchar(255)
);


CREATE TABLE IF NOT EXISTS RF_COEF_A (
  id bigserial PRIMARY KEY,
  startDate date,
  endDate date,
  tgoCode varchar(255),
  dgoCode varchar(255),
  energyNature varchar(255),
  coef_A numeric(15,4)
);

CREATE TABLE IF NOT EXISTS RF_GEO_FACTORS (
  id bigserial PRIMARY KEY,
  areaCode varchar(255) NOT NULL,
  startDate date,
  endDate date,
  municipalIncreaseCoef numeric(15,4),
  departmentalIncreaseCoef numeric(15,4),
  rateLevel numeric(15,4)
);

CREATE TABLE IF NOT EXISTS CC_ACTOR
(
   actorId bigserial PRIMARY KEY,
   thirdId bigint REFERENCES CC_THIRD(thirdId),
   role varchar(255),
   startDate date,
   endDate date,
   perimeterId bigint REFERENCES CC_PERIMETER(perimeterId)
);

CREATE TABLE IF NOT EXISTS BL_BILL
(
   billId bigserial PRIMARY KEY,
   billRef varchar(255),
   billingRunId bigint REFERENCES BL_BILLING_RUN(id),
   status varchar(45),
   type varchar(45),
   nature varchar(45),
   cancelledbillid bigint,
   cancelledbybillid bigint,
   perimeterId bigint REFERENCES CC_PERIMETER(perimeterId),
   contractId bigint NULL REFERENCES CC_CONTRACT(id),
   customerId bigint REFERENCES CC_CUSTOMER(id),
   totalAmount numeric(15,4),
   totalVat numeric(15,4),
   totalWithoutVat numeric(15,4),
   billDate date,
   startDate date,
   endDate date,
   accountingDate date,
   groupBillId bigint,
   "group" boolean
);

CREATE TABLE IF NOT EXISTS BL_BILL_DETAIL
(
   billDetailId bigserial PRIMARY KEY,
   billId bigint REFERENCES BL_BILL(billId),
   billLineCategory varchar(45),
   billLineSubCategory varchar(45),
   startDate date,
   endDate date,
   quantity numeric(15,4),
   quantityUnit varchar(45),
   price numeric(15,4),
   priceUnit varchar(45),
   vatRate varchar(45),
   totalWithoutVat numeric(15,4),
   tou varchar(45),
   lineType varchar(45)
);

CREATE TABLE IF NOT EXISTS BL_VAT_DETAIL
(
   vatDetailId bigserial PRIMARY KEY,
   billId bigint REFERENCES BL_BILL(billId),
   vatRate varchar(45),
   totalWithoutVat numeric(15,4),
   totalVat numeric(15,4)
);

CREATE TABLE IF NOT EXISTS BL_BILLABLE_CHARGE_TYPE
(
   billableChargeType varchar(255) PRIMARY KEY,
   category varchar(255),
   subCategory varchar(255),
   aquisitionStrategy varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   defaultLabel varchar(255)
);

CREATE TABLE IF NOT EXISTS BL_ARTICLE_TYPE
(
   articleType varchar(255) PRIMARY KEY,
   category varchar(255),
   subCategory varchar(255),
   analyticCode varchar(255),
   additionalCode varchar(255),
   billingScheme varchar(255),
   accountingScheme varchar(255),
   standardLabel varchar(255),
   otherLabel varchar(255),
   defaultLabel varchar(255),
   market varchar(255),
   counterpart varchar(255)
);

CREATE TABLE IF NOT EXISTS BL_BILLABLE_CHARGE
(
   billableChargeId bigserial PRIMARY KEY,
   billableChargeTypeId varchar(255) REFERENCES BL_BILLABLE_CHARGE_TYPE(billableChargeType),
   externalId varchar(255),
   externalInvoiceRef varchar(255),
   posRef varchar(255),
   source varchar(255),
   market varchar(255),
   direction varchar(255),
   startDate date,
   endDate date,
   externalInvoiceDate date,
   receptionDate date,
   amount numeric(15,4),
   type varchar(255),
   context varchar(255),
   status varchar(255),
   cancelledBy bigint
);

CREATE TABLE IF NOT EXISTS BL_ARTICLE
(
   articleId bigserial PRIMARY KEY,
   billableChargeId bigint REFERENCES BL_BILLABLE_CHARGE(billableChargeId),
   externalArticleId varchar(255),
   articleTypeId varchar(255) REFERENCES BL_ARTICLE_TYPE(articleType),
   startDate date,
   endDate date,
   effectiveDate date,
   tou varchar(45),
   unitPrice varchar(255),
   unitOfUnitPrice varchar(255),
   quantity varchar(255),
   unitOfQuantity varchar(255),
   amount numeric(15,4),
   vatRate varchar(255),
   status varchar(255),
   billId bigint REFERENCES BL_BILL(billId)
);

CREATE TABLE IF NOT EXISTS BL_BILL_SEGMENT
(
   billSegmentId bigserial PRIMARY KEY,
   seId bigint REFERENCES BL_SE(seId),
   seType varchar(45),
   startDate date,
   endDate date,
   quantity numeric(15,4),
   quantityUnit varchar(45),
   quantityThreshold varchar(45),
   quantityThresholdBase varchar(45),
   price numeric(15,4),
   priceUnit varchar(45),
   schema varchar(45),
   priceThreshold varchar(45),
   priceThresholdBase varchar(45),
   amount numeric(15,4),
   tou varchar(45),
   touGroup varchar(45),
   vatRate varchar(45),
   nature varchar(45),
   status varchar(45),
   billId bigint REFERENCES BL_BILL(billId),
   meterReadId bigint REFERENCES CC_METER_READ(id),
   articleId bigint REFERENCES BL_ARTICLE(articleId),
   cancelledBy bigint
);

CREATE TABLE IF NOT EXISTS TR_ROLE (
   id BIGSERIAL PRIMARY KEY,
   name VARCHAR(50) NOT NULL, -- Nom du rôle (ex. 'ADMIN', 'USER', 'MANAGER')
   standardLabel varchar(255),
   otherLabel varchar(255),
   defaultLabel varchar(255),
   UNIQUE (name)
);

-- Table des permissions
CREATE TABLE IF NOT EXISTS TR_PERMISSION (
    id BIGSERIAL PRIMARY KEY,
    category varchar(255),
    action VARCHAR(50) NOT NULL, -- Action autorisée (ex. 'read', 'create', 'update', 'delete')
    entity VARCHAR(50) NOT NULL, -- Ressource ciblée (ex. 'Invoice', 'User', 'Report')
    --condition VARCHAR(50),
    UNIQUE (action, entity) -- Empêcher la duplication des permissions
);

-- Table de relation utilisateur-rôle
CREATE TABLE IF NOT EXISTS TR_USER_ROLE (
    id BIGSERIAL PRIMARY KEY,
    userId BIGINT NOT NULL REFERENCES TR_USER(id),
    roleId BIGINT NOT NULL REFERENCES TR_ROLE(id),
    UNIQUE (userId, roleId) -- Un utilisateur ne peut avoir un même rôle qu'une seule fois
);

-- Table de relation rôle-permission
CREATE TABLE IF NOT EXISTS TR_ROLE_PERMISSION (
    id BIGSERIAL PRIMARY KEY,
    roleId BIGINT NOT NULL REFERENCES TR_ROLE(id),
    permissionId BIGINT NOT NULL REFERENCES TR_PERMISSION(id),
    restriction VARCHAR(50),
    UNIQUE (roleId, permissionId) -- Un rôle ne peut avoir une même permission qu'une seule fois
);

-- Table des permissions spécifiques à un utilisateur
CREATE TABLE IF NOT EXISTS TR_USER_PERMISSION (
    id BIGSERIAL PRIMARY KEY,
    userId BIGINT NOT NULL REFERENCES TR_USER(id),
    permissionId BIGINT NOT NULL REFERENCES TR_PERMISSION(id),
    restriction VARCHAR(50),
    expirationDate TIMESTAMP, -- Permet de définir une durée limitée à la permission (facultatif)
    UNIQUE (userId, permissionId) -- Un utilisateur ne peut avoir une permission spécifique qu'une seule fois
);
