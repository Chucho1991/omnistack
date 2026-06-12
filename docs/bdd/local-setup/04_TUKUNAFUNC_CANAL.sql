-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- docker exec -it oracle-oracle-1 sqlplus TUKUNAFUNC/tvkvn4tst@XEPDB1
-- Verifica e inserta AD_CANAL con el registro POS
-- Estructura real: CODIGO VARCHAR2, DESCRIPCION VARCHAR2, ACTIVO NUMBER
-- ============================================================
SET SQLBLANKLINES ON

-- Insertar POS solo si no existe
INSERT INTO AD_CANAL (CODIGO, DESCRIPCION, ACTIVO)
SELECT 'POS', 'PUNTO DE VENTA / CAJA', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM AD_CANAL WHERE CODIGO = 'POS');

COMMIT;
