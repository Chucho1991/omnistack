/* ============================================================
   ESQUEMA  : TUKUNAFUNC
   PROYECTO : OmniStack
   OBJETIVO : Script consolidado de TODAS las secuencias del proyecto.
              Ejecutar de una sola pasada para crear/recrear secuencias.

   SECUENCIAS (8 en total)
   ---------------------------------------------------------------
   1. SEQ_IN_OMNI_PROVEEDOR_WS        (01_DDL_ESTRUCTURA)
   2. SEQ_IN_OMNI_PROVEEDOR_HEADERS   (01_DDL_ESTRUCTURA)
   3. SEQ_IN_OMNI_PROVEEDOR_WS_DEFS   (01_DDL_ESTRUCTURA)
   4. SEQ_IN_OMNI_PROVEEDOR_CONFIG    (01_DDL_ESTRUCTURA)
   5. SEQ_IN_OMNI_LOGS_APP            (01_DDL_ESTRUCTURA)
   6. SEQ_IN_OMNI_LOGS_WS_EXT         (01_DDL_ESTRUCTURA)
   7. SEQ_IN_OMNI_REGISTRO_TRX        (01_DDL_ESTRUCTURA)
   8. SEQ_IN_OMNI_INPUT_FIELDS        (06_DDL_INPUT_FIELDS)
   9. SEQ_IN_OMNI_CASHOUT_CUPO        (26_DDL_CASHOUT_CUPO_DIARIO)

   NOTA: Si las secuencias ya existen, descomentar el bloque DROP
         al inicio de este script.
   ============================================================ */

---------------------------------------------------------------
-- (OPCIONAL) DROP de secuencias existentes - descomentar si necesario
---------------------------------------------------------------
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_WS;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_HEADERS;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_WS_DEFS;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_CONFIG;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_LOGS_APP;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_LOGS_WS_EXT;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_REGISTRO_TRX;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_INPUT_FIELDS;
-- DROP SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_CASHOUT_CUPO;

---------------------------------------------------------------
-- 1) SEQ_IN_OMNI_PROVEEDOR_WS
--    Usada por: IN_OMNI_PROVEEDOR_WS.ID_WS
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_WS
  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 2) SEQ_IN_OMNI_PROVEEDOR_HEADERS
--    Usada por: IN_OMNI_PROVEEDOR_HEADERS.ID_HEADER
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_HEADERS
  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 3) SEQ_IN_OMNI_PROVEEDOR_WS_DEFS
--    Usada por: IN_OMNI_PROVEEDOR_WS_DEFS.ID_DEFAULT
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_WS_DEFS
  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 4) SEQ_IN_OMNI_PROVEEDOR_CONFIG
--    Usada por: IN_OMNI_PROVEEDOR_CONFIG.ID_CONFIG
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_PROVEEDOR_CONFIG
  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 5) SEQ_IN_OMNI_LOGS_APP
--    Usada por: IN_OMNI_LOGS_APP.CODIGO
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_LOGS_APP
  START WITH 1 INCREMENT BY 1 CACHE 500 NOCYCLE;

---------------------------------------------------------------
-- 6) SEQ_IN_OMNI_LOGS_WS_EXT
--    Usada por: IN_OMNI_LOGS_WS_EXT.CODIGO
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_LOGS_WS_EXT
  START WITH 1 INCREMENT BY 1 CACHE 500 NOCYCLE;

---------------------------------------------------------------
-- 7) SEQ_IN_OMNI_REGISTRO_TRX
--    Usada por: IN_OMNI_REGISTRO_TRX.CODIGO
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_REGISTRO_TRX
  START WITH 1 INCREMENT BY 1 CACHE 20 NOCYCLE;

---------------------------------------------------------------
-- 8) SEQ_IN_OMNI_INPUT_FIELDS
--    Usada por: IN_OMNI_INPUT_FIELDS.ID_FIELD
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_INPUT_FIELDS
  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 9) SEQ_IN_OMNI_CASHOUT_CUPO
--    Usada por: IN_OMNI_CASHOUT_CUPO_DIARIO.ID_CUPO
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_CASHOUT_CUPO
  START WITH 1 INCREMENT BY 1 CACHE 20 NOCYCLE;

COMMIT;
