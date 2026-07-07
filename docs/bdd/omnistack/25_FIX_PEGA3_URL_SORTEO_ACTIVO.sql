-- ============================================================
-- Ejecutar como: usuario con acceso a TUKUNAFUNC (prod datasource)
--
-- El endpoint del sorteo activo de Pega quedó mal en los scripts 02/03:
-- 'ObtieneSorteosActivo' no existe en el proveedor (responde HTTP 404 sin body).
-- El nombre real según el spec técnico (LOTERIA NACIONAL.docx) y Postman v8
-- es 'ObtieneSorteosActivoxJuego' (request: deviceId + token + productoVender).
-- ============================================================

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
   SET URL = 'https://www8.loteria.com.ec/APIVentasLoteria/api/Ventas/ObtieneSorteosActivoxJuego'
 WHERE PROVEEDOR_KEY = 'pega3'
   AND WS_KEY = 'PRECHECK_SORTEO.CASHIN';

COMMIT;

-- Verificar:
-- SELECT WS_KEY, URL
--   FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
--  WHERE PROVEEDOR_KEY = 'pega3'
--    AND WS_KEY = 'PRECHECK_SORTEO.CASHIN';
