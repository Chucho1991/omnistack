SELECT
    TO_CHAR(sp.TERCERO)     AS service_provider_code,
    sp.CODIGO_ITEM_RMS      AS rms_item_code,
    pf.COD_COM_FORMPAG_SERV AS service_payment_method_id,
    CASE WHEN pf.COD_FORMA_PAGO = 1 THEN 'EFECTIVO'
         WHEN pf.COD_FORMA_PAGO = 2 THEN 'TARJETA_CREDITO'
         ELSE TO_CHAR(pf.COD_FORMA_PAGO) END AS payment_method_code,
    CASE WHEN pf.ACTIVO = 'S' THEN 1 ELSE 0 END AS is_active
FROM gpf_omnistack.AD_COM_FORMPAG_SERVICIO pf
JOIN gpf_omnistack.AD_SERVICIO_PARAMETROS sp
    ON sp.ID_CONFIG    = pf.ID_CONFIG
JOIN gpf_omnistack.AD_CANAL_SERVICIO cs
    ON cs.ID_CONFIG    = sp.ID_CONFIG
   AND cs.CODIGO_CANAL = :canal_codigo
   AND cs.ACTIVO       = 'S'
ORDER BY sp.TERCERO, sp.CODIGO_ITEM_RMS, pf.COD_COM_FORMPAG_SERV
