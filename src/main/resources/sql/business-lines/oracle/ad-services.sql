SELECT
    TO_CHAR(sp.TERCERO)     AS service_provider_code,
    TRIM(sp.CODIGO_ITEM_RMS) AS rms_item_code,
    CASE WHEN cs.ACTIVO = 'S' THEN 1 ELSE 0 END AS is_active,
    CASE WHEN sp.FLG_PAGO_MIXTO = 'S' THEN 1 ELSE 0 END AS is_mixed_payment,
    sp.FLG_ITEM             AS flg_item,
    CASE WHEN sp.FLG_DEVOLUCION = 'S' THEN 1 ELSE 0 END AS is_refund,
    sp.MONTO_MIN            AS min_amount,
    sp.MONTO_MAX            AS max_amount,
    sp.TIMEOUT_WS_MAX       AS timeout_ws_max,
    sp.RETRIES_WS_MAX       AS retries_ws_max,
    sp.NUM_TICKETS          AS num_tickets,
    CASE WHEN sp.REQUIERE_CONSENTIMIENTO = 'S' THEN 1 ELSE 0 END AS requires_consent,
    sp.TEXTO_CONSENTIMIENTO AS consent_text,
    CASE WHEN sp.ID_HOMOLOGADO = 'S' THEN 1 ELSE 0 END AS homologated_auth
FROM gpf_omnistack.AD_SERVICIO_PARAMETROS sp
JOIN gpf_omnistack.AD_CANAL_SERVICIO cs
    ON cs.ID_CONFIG    = sp.ID_CONFIG
   AND cs.CODIGO_CANAL = :canal_codigo
   AND cs.ACTIVO       = 'S'
ORDER BY sp.TERCERO, sp.CODIGO_ITEM_RMS
