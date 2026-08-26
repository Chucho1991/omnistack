SELECT
    TRIM(c.RMS_ITEM_CODE) AS rms_item_code,
    c.FLAG_ITEM AS flg_item,
    CASE WHEN c.FLG_ONLY = 'S' THEN 1 ELSE 0 END AS is_only,
    CASE WHEN c.ALLOW_OTHER_BILLABLE_SERVICES = 'S' THEN 1 ELSE 0 END AS allow_other_billable_services,
    CASE WHEN c.ALLOW_SAME_SERVICE = 'S' THEN 1 ELSE 0 END AS allow_same_service,
    CASE WHEN c.FLG_UNIQUE = 'S' THEN 1 ELSE 0 END AS is_unique,
    c.SERVICE_TYPE AS service_type,
    CASE WHEN c.REC_TELEPEAJE_ACTIVE = 'S' THEN 1 ELSE 0 END AS rec_telepeaje_active,
    CASE WHEN c.PRINT_CONFIRMATION_VOUCHER = 'S' THEN 1 ELSE 0 END AS print_confirmation_voucher
FROM IN_OMNI_BUSINESS_LINE_ITEM c
WHERE c.ENABLED = 'S'
  AND TRIM(c.RMS_ITEM_CODE) IN (:rms_item_codes)
ORDER BY c.RMS_ITEM_CODE
