-- DROP VIEW IF EXISTS PUBLIC.vw_billing_run_overview;
-- DROP VIEW IF EXISTS PUBLIC.statusstats;

CREATE OR REPLACE VIEW PUBLIC.vw_billing_run_overview AS
WITH statusstats AS (
    SELECT e_1.status,
           COUNT(*) AS statuscount,
           AVG(CASE WHEN CAST(e_1.status AS VARCHAR) = 'completed' THEN 1.0 ELSE 0 END) AS statuspercent
      FROM PUBLIC.cc_event e_1
      JOIN PUBLIC.cc_event_template et_1 ON CAST(e_1.eventtype AS VARCHAR) = CAST(et_1.eventtype AS VARCHAR)
      JOIN PUBLIC.tr_relation re_1 ON re_1.firstobjectid = e_1.activityid AND CAST(re_1.relationtype AS VARCHAR) = 'ACTIVITY_BILLING_RUN'
     GROUP BY e_1.status
)
SELECT ROW_NUMBER() OVER () AS unique_id,
       re.secondobjectid AS billingrunid,
       et.subcategory AS step,
       ss.status,
       COALESCE(COUNT(CASE WHEN CAST(e.status AS VARCHAR) = CAST(ss.status AS VARCHAR) THEN 1.0 ELSE NULL END), 0) AS statuscount,
       COALESCE(AVG(CASE WHEN CAST(e.status AS VARCHAR) = CAST(ss.status AS VARCHAR) THEN 1.0 ELSE 0 END), 0) AS statuspercent,
       COUNT(*) AS total
  FROM PUBLIC.cc_event e
  JOIN PUBLIC.cc_event_template et ON CAST(e.eventtype AS VARCHAR) = CAST(et.eventtype AS VARCHAR)
  JOIN PUBLIC.tr_relation re ON re.firstobjectid = e.activityid AND CAST(re.relationtype AS VARCHAR) = 'ACTIVITY_BILLING_RUN'
  CROSS JOIN statusstats ss
GROUP BY re.secondobjectid, et.subcategory, ss.status
ORDER BY re.secondobjectid, ss.status;


-- DROP VIEW IF EXISTS public.vw_bill_overview;

CREATE OR REPLACE VIEW public.vw_bill_overview
 AS
 SELECT row_number() OVER () AS unique_id,
    bl_bill.billingrunid,
    bl_bill.status,
    count(*) AS totalcount,
    sum(bl_bill.totalwithoutvat) AS totalamount,
    sum(bl_bill.totalamount) AS totalamountvated
   FROM bl_bill
  GROUP BY bl_bill.billingrunid, bl_bill.status;
