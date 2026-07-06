-- ============================================================
-- Ejecutar como: usuario con acceso a TUKUNAFUNC (prod datasource)
--
-- CLARO confirmó que el endpoint correcto es /sprXslt/sprXslt (sin SOAP wrapper).
-- El endpoint anterior (SprXsltWSService) era un gateway JMS asíncrono fire-and-forget.
-- El body ahora es umsprot directo (no envuelto en SOAP Envelope).
-- ============================================================

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
   SET WS_URL = 'http://192.168.37.40:50004/sprXslt/sprXslt'
 WHERE PROVEEDOR_KEY = 'claro'
   AND WS_KEY IN ('PRECHECK.CASHIN', 'EXECUTE.CASHIN');

COMMIT;

-- Verificar:
-- SELECT WS_KEY, WS_URL
--   FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
--  WHERE PROVEEDOR_KEY = 'claro';
