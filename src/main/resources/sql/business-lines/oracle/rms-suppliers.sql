SELECT DISTINCT
    TRIM(isup.ITEM)          AS rms_item_code,
    TO_CHAR(isup.SUPPLIER)   AS supplier_code,
    s.SUP_NAME               AS provider_name,
    s.SUP_NAME_SECONDARY     AS ruc_provider
FROM ITEM_SUPPLIER isup
JOIN SUPS s
    ON s.SUPPLIER = isup.SUPPLIER
WHERE TRIM(isup.ITEM) IN (:rms_item_codes)
