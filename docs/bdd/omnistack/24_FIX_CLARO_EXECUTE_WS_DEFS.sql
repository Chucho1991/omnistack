-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- PROBLEMA: ClaroXmlAdapter.buildExecuteXml (processRechargeRetail) envia
-- CHANNELID, MEDIADETAILID, SUBSCRIBERTYPE y SUBSCRIPTIONTYPE, pero
-- IN_OMNI_PROVEEDOR_WS_DEFS solo tiene esas claves cargadas para
-- WS_KEY = 'PRECHECK.CASHIN' (ver script 02_DML_PROVEEDORES.sql lineas
-- 331-369) — nunca se insertaron para 'EXECUTE.CASHIN'. Ademas el proveedor
-- exige el nombre real "PV_CHANNELID", no "CHANNELID" (confirmado con el
-- error real: "El siguiente parametro es requerido para la transaccion
-- solicitada: PV_CHANNELID").
--
-- Este script agrega en EXECUTE.CASHIN los mismos valores que ya existen
-- en PRECHECK.CASHIN para channel_id, media_detail_id, subscriber_type y
-- subscription_type. consumer_id y company_id ya existian para
-- EXECUTE.CASHIN (script 02, lineas 383-390) y no se tocan aqui.
-- ============================================================

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'TEXTO', 'channel_id', 'CAJA'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
WHERE w.PROVEEDOR_KEY = 'claro' AND w.WS_KEY = 'EXECUTE.CASHIN';

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'TEXTO', 'media_detail_id', 'RETA_FYBECA'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
WHERE w.PROVEEDOR_KEY = 'claro' AND w.WS_KEY = 'EXECUTE.CASHIN';

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'TEXTO', 'subscriber_type', '2'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
WHERE w.PROVEEDOR_KEY = 'claro' AND w.WS_KEY = 'EXECUTE.CASHIN';

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'TEXTO', 'subscription_type', 'EVENTUAL'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
WHERE w.PROVEEDOR_KEY = 'claro' AND w.WS_KEY = 'EXECUTE.CASHIN';

COMMIT;

-- Verificar (esperado: 4 filas nuevas + las 5 que ya existian para EXECUTE.CASHIN):
-- SELECT d.DEFAULT_CLAVE, d.DEFAULT_VALOR_TEXT, w.WS_KEY
--   FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d
--   JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w ON w.ID_WS = d.ID_WS
--  WHERE w.PROVEEDOR_KEY = 'claro' AND w.WS_KEY = 'EXECUTE.CASHIN' AND d.TIPO_DEF = 'TEXTO'
--  ORDER BY d.DEFAULT_CLAVE;
