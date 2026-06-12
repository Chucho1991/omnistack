-- ============================================================
-- Ejecutar como: SYSDBA
-- docker exec -it oracle-oracle-1 sqlplus sys/sys@XEPDB1 as sysdba
-- Otorga acceso de RMS a GPF_OMNISTACK y crea sinónimos
-- ============================================================
SET SQLBLANKLINES ON

-- Grants RMS → GPF_OMNISTACK (para que GPF_OMNISTACK pueda leer tablas RMS)
GRANT SELECT ON rms.CLASS          TO GPF_OMNISTACK;
GRANT SELECT ON rms.SUBCLASS       TO GPF_OMNISTACK;
GRANT SELECT ON rms.ITEM_MASTER    TO GPF_OMNISTACK;
GRANT SELECT ON rms.SUPS           TO GPF_OMNISTACK;
GRANT SELECT ON rms.ITEM_SUPPLIER  TO GPF_OMNISTACK;
GRANT SELECT ON rms.UDA            TO GPF_OMNISTACK;
GRANT SELECT ON rms.UDA_ITEM_LOV   TO GPF_OMNISTACK;
GRANT SELECT ON rms.UDA_VALUES     TO GPF_OMNISTACK;

-- Sinonimos en GPF_OMNISTACK para tablas sin prefijo de schema en las queries
CREATE OR REPLACE SYNONYM GPF_OMNISTACK.CLASS       FOR rms.CLASS;
CREATE OR REPLACE SYNONYM GPF_OMNISTACK.SUBCLASS    FOR rms.SUBCLASS;
CREATE OR REPLACE SYNONYM GPF_OMNISTACK.ITEM_MASTER FOR rms.ITEM_MASTER;

-- Grants GPF_OMNISTACK.AD_* → rms (para que el datasource rms acceda al catalogo AD_* via gpf_omnistack.)
GRANT SELECT ON GPF_OMNISTACK.AD_SERVICIO_PARAMETROS  TO rms;
GRANT SELECT ON GPF_OMNISTACK.AD_CANAL_SERVICIO        TO rms;
GRANT SELECT ON GPF_OMNISTACK.AD_COM_FORMPAG_SERVICIO  TO rms;
GRANT SELECT ON GPF_OMNISTACK.AD_ITEM_SERVICIO         TO rms;

COMMIT;
