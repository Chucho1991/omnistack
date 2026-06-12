SELECT
    TO_CHAR(c.CLASS)        AS category_code,
    TO_CHAR(sc.SUBCLASS_ID) AS subcategory_code,
    TO_CHAR(sp.TERCERO)     AS service_provider_code,
    sp.CODIGO_ITEM_RMS      AS rms_item_code,
    pf.COD_COM_FORMPAG_SERV AS service_payment_method_id,
    TO_CHAR(pf.COD_FORMA_PAGO) AS payment_method_code,
    CASE WHEN pf.ACTIVO = 'S' THEN 1 ELSE 0 END AS is_active
FROM AD_COM_FORMPAG_SERVICIO pf
JOIN AD_SERVICIO_PARAMETROS sp
    ON sp.ID_CONFIG = pf.ID_CONFIG
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
ORDER BY c.CLASS, sc.SUBCLASS_ID, sp.TERCERO, sp.CODIGO_ITEM_RMS, pf.COD_COM_FORMPAG_SERV
