SET REFERENTIAL_INTEGRITY FALSE;
BEGIN TRANSACTION;

delete from CC_ACTIVITY;
delete from CC_ACTOR;
delete from CC_ADDRESS;
delete from CC_COMPANY;
delete from CC_CONTACT;
delete from CC_CONTRACT;
delete from CC_CONTRACT_PERIMETER;
delete from CC_CUSTOMER;
delete from CC_EVENT;
delete from CC_FINANCIAL_INFORMATION;
delete from CC_INDIVIDUAL;
delete from CC_PERIMETER;
delete from CC_SERVICE;
delete from CC_THIRD;
delete from PA_RELATION;
delete from TR_CHARACTERISTIC;

COMMIT;
SET REFERENTIAL_INTEGRITY TRUE;