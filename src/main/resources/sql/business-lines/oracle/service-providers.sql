SELECT DISTINCT
    TO_CHAR(c.CLASS)        AS category_code,
    TO_CHAR(sc.SUBCLASS_ID) AS subcategory_code,
    TO_CHAR(sp.TERCERO)     AS service_provider_code,
    s.SUP_NAME_SECONDARY    AS ruc_provider,
    s.SUP_NAME              AS provider_name,
    1                       AS is_active
FROM AD_SERVICIO_PARAMETROS sp
JOIN AD_CANAL_SERVICIO cs
    ON cs.ID_CONFIG    = sp.ID_CONFIG
   AND cs.CODIGO_CANAL = :canal_codigo
   AND cs.ACTIVO       = 'S'
JOIN ITEM_MASTER im
    ON TRIM(im.ITEM) = TRIM(sp.CODIGO_ITEM_RMS)
JOIN CLASS c
    ON c.CLASS = im.CLASS
JOIN SUBCLASS sc
    ON sc.CLASS       = im.CLASS
   AND sc.SUBCLASS_ID = im.SUBCLASS
JOIN rms.ITEM_SUPPLIER isup
    ON TRIM(isup.ITEM) = TRIM(sp.CODIGO_ITEM_RMS)
   AND isup.SUPPLIER   = sp.TERCERO
JOIN rms.SUPS s
    ON s.SUPPLIER = sp.TERCERO
ORDER BY c.CLASS, sc.SUBCLASS_ID, sp.TERCERO
