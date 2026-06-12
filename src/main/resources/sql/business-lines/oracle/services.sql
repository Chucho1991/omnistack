SELECT
    TO_CHAR(c.CLASS)        AS category_code,
    TO_CHAR(sc.SUBCLASS_ID) AS subcategory_code,
    TO_CHAR(sp.TERCERO)     AS service_provider_code,
    sp.CODIGO_ITEM_RMS      AS rms_item_code,
    im.ITEM_DESC            AS description,
    CASE WHEN cs.ACTIVO             = 'S' THEN 1 ELSE 0 END AS is_active,
    TO_CHAR(NULL)           AS jde_code,
    uv.UDA_VALUE_DESC       AS movement_type,
    CASE WHEN sp.FLG_PAGO_MIXTO     = 'S' THEN 1 ELSE 0 END AS is_mixed_payment,
    sp.FLG_ITEM             AS flg_item,
    CASE WHEN sp.FLG_DEVOLUCION     = 'S' THEN 1 ELSE 0 END AS is_refund,
    TO_CHAR(sp.MONTO_MIN)   AS min_amount,
    TO_CHAR(sp.MONTO_MAX)   AS max_amount,
    sp.TIMEOUT_WS_MAX       AS timeout_ws_max,
    sp.RETRIES_WS_MAX       AS retries_ws_max,
    sp.NUM_TICKETS          AS num_tickets,
    CASE WHEN sp.REQUIERE_CONSENTIMIENTO = 'S' THEN 1 ELSE 0 END AS requires_consent,
    sp.TEXTO_CONSENTIMIENTO AS consent_text
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
JOIN rms.UDA_ITEM_LOV uil
    ON TRIM(uil.ITEM) = TRIM(sp.CODIGO_ITEM_RMS)
   AND uil.UDA_ID     = 3330
JOIN rms.UDA_VALUES uv
    ON uv.UDA_ID    = uil.UDA_ID
   AND uv.UDA_VALUE = uil.UDA_VALUE
ORDER BY c.CLASS, sc.SUBCLASS_ID, sp.TERCERO, sp.CODIGO_ITEM_RMS
