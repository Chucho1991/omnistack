-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   Estandarizar en todas las tablas IN_OMNI_* un set de columnas de
--   extension reservadas para uso futuro, sin necesidad de alterar
--   tablas ya en produccion cuando falte un campo nuevo. El patron ya
--   existia de forma parcial e inconsistente en 3 tablas (ej. CP_VAR1
--   de IN_OMNI_REGISTRO_TRX, que ya se usa en codigo para el codigo
--   homologado — ver HomologatedCodeService/OracleRegistroTrxAdapter).
--   Este script completa esas 3 tablas al set completo y lo agrega
--   desde cero a las 6 que no lo tenian.
--
--   Set objetivo (igual en las 9 tablas):
--     CP_VAR1, CP_VAR2, CP_VAR3       VARCHAR2(1500)
--     CP_NUMBER1, CP_NUMBER2, CP_NUMBER3  NUMBER
--     CP_DATE1, CP_DATE2, CP_DATE3    DATE
--
--   Ninguna columna existente se modifica ni se renombra — solo se
--   agregan las que faltan por tabla. Todas nullable, cero impacto en
--   datos/queries existentes.
-- ============================================================

-- ---- Tablas sin ninguna columna CP_* previa: se agrega el set completo ----

ALTER TABLE TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS ADD (
  CP_VAR1    VARCHAR2(1500),
  CP_VAR2    VARCHAR2(1500),
  CP_VAR3    VARCHAR2(1500),
  CP_NUMBER1 NUMBER,
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE1   DATE,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

ALTER TABLE TUKUNAFUNC.IN_OMNI_PROVEEDOR_HEADERS ADD (
  CP_VAR1    VARCHAR2(1500),
  CP_VAR2    VARCHAR2(1500),
  CP_VAR3    VARCHAR2(1500),
  CP_NUMBER1 NUMBER,
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE1   DATE,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

ALTER TABLE TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS ADD (
  CP_VAR1    VARCHAR2(1500),
  CP_VAR2    VARCHAR2(1500),
  CP_VAR3    VARCHAR2(1500),
  CP_NUMBER1 NUMBER,
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE1   DATE,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

ALTER TABLE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG ADD (
  CP_VAR1    VARCHAR2(1500),
  CP_VAR2    VARCHAR2(1500),
  CP_VAR3    VARCHAR2(1500),
  CP_NUMBER1 NUMBER,
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE1   DATE,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

ALTER TABLE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS ADD (
  CP_VAR1    VARCHAR2(1500),
  CP_VAR2    VARCHAR2(1500),
  CP_VAR3    VARCHAR2(1500),
  CP_NUMBER1 NUMBER,
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE1   DATE,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

ALTER TABLE TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO ADD (
  CP_VAR1    VARCHAR2(1500),
  CP_VAR2    VARCHAR2(1500),
  CP_VAR3    VARCHAR2(1500),
  CP_NUMBER1 NUMBER,
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE1   DATE,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

-- ---- Tablas con set parcial: solo se agrega lo que falta ----

-- IN_OMNI_LOGS_APP ya tiene CP_VAR1-3, CP_NUMBER1-2, CP_DATE1-2
ALTER TABLE TUKUNAFUNC.IN_OMNI_LOGS_APP ADD (
  CP_NUMBER3 NUMBER,
  CP_DATE3   DATE
);

-- IN_OMNI_LOGS_WS_EXT ya tiene CP_VAR1-2, CP_DATE1
ALTER TABLE TUKUNAFUNC.IN_OMNI_LOGS_WS_EXT ADD (
  CP_VAR3    VARCHAR2(1500),
  CP_NUMBER1 NUMBER,
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

-- IN_OMNI_REGISTRO_TRX ya tiene CP_VAR1-3 (CP_VAR1 en uso: codigo homologado), CP_NUMBER1, CP_DATE1
ALTER TABLE TUKUNAFUNC.IN_OMNI_REGISTRO_TRX ADD (
  CP_NUMBER2 NUMBER,
  CP_NUMBER3 NUMBER,
  CP_DATE2   DATE,
  CP_DATE3   DATE
);

COMMIT;

-- ============================================================
-- VERIFICACION: cada tabla debe listar las 9 columnas CP_*
-- ============================================================
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TUKUNAFUNC'
   AND TABLE_NAME LIKE 'IN_OMNI_%'
   AND COLUMN_NAME LIKE 'CP_%'
 ORDER BY TABLE_NAME, COLUMN_NAME;
-- Esperado: 9 tablas x 9 columnas = 81 filas
