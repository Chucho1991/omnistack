/* ============================================================
   ESQUEMA  : TUKUNAFUNC
   PROYECTO : OmniStack
   OBJETIVO : Script consolidado de TODAS las secuencias del proyecto.
              Ejecutar de una sola pasada para crear/recrear secuencias.

   SECUENCIAS (9 en total — esquema TUKUNAFUNC)
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

   SECUENCIAS (4 en total — esquema GPF_OMNISTACK, local-setup)
   ---------------------------------------------------------------
   10. SEQ_AD_SERVICIO_PARAMETROS      (03_GPF_OMNISTACK_CAMBIOS)
   11. SEQ_AD_CANAL_SERVICIO           (03_GPF_OMNISTACK_CAMBIOS)
   12. SEQ_AD_COM_FORMPAG_SERVICIO     (03_GPF_OMNISTACK_CAMBIOS)
   13. SEQ_AD_ITEM_SERVICIO            (03_GPF_OMNISTACK_CAMBIOS)

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

---------------------------------------------------------------
-- ESQUEMA GPF_OMNISTACK — secuencias para tablas AD_*
-- (Solo aplica a local-setup Docker; en QA/PROD pueden ya existir
--  bajo otro mecanismo o con START WITH ajustado al MAX actual.)
---------------------------------------------------------------

-- 10) SEQ_AD_SERVICIO_PARAMETROS
--     Usada por: AD_SERVICIO_PARAMETROS.ID_CONFIG
---------------------------------------------------------------
-- CREATE SEQUENCE GPF_OMNISTACK.SEQ_AD_SERVICIO_PARAMETROS
--   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 11) SEQ_AD_CANAL_SERVICIO
--     Usada por: AD_CANAL_SERVICIO.COD_CAN_SERV
---------------------------------------------------------------
-- CREATE SEQUENCE GPF_OMNISTACK.SEQ_AD_CANAL_SERVICIO
--   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 12) SEQ_AD_COM_FORMPAG_SERVICIO
--     Usada por: AD_COM_FORMPAG_SERVICIO.COD_COM_FORMPAG_SERV
---------------------------------------------------------------
-- CREATE SEQUENCE GPF_OMNISTACK.SEQ_AD_COM_FORMPAG_SERVICIO
--   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

---------------------------------------------------------------
-- 13) SEQ_AD_ITEM_SERVICIO
--     Usada por: AD_ITEM_SERVICIO.COD_ITEM_SERV
---------------------------------------------------------------
-- CREATE SEQUENCE GPF_OMNISTACK.SEQ_AD_ITEM_SERVICIO
--   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- NOTA: Las secuencias 10-13 se crean en local-setup/03_GPF_OMNISTACK_CAMBIOS.sql
--       (ejecutar como GPF_OMNISTACK). No se incluyen aqui sin comentar porque
--       este script se ejecuta como TUKUNAFUNC.

COMMIT;
