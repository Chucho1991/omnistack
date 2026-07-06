-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- Corrige la URL del endpoint CLARO:
--   Antes: /sprXslt/SprXslt         (path sin WSService → retorna 404)
--   Ahora: /sprXslt/SprXsltWSService (endpoint SOAP real)
--
-- El servicio de CLARO es un WebService SOAP con operación OnMensaje.
-- El path correcto fue confirmado con el curl de integración.
-- ============================================================

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
SET URL = 'http://192.168.37.40:50004/sprXslt/SprXsltWSService'
WHERE PROVEEDOR_KEY = 'claro'
  AND WS_KEY IN ('PRECHECK.CASHIN', 'EXECUTE.CASHIN');

COMMIT;

-- Verificar:
-- SELECT WS_KEY, URL FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
--  WHERE PROVEEDOR_KEY = 'claro';
