-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- Agrega la columna VALOR_ORIGEN a IN_OMNI_PROVEEDOR_CONFIG.
--
-- Propósito:
--   Soporta homologación de valores entre el POS y el proveedor.
--   VALOR_ORIGEN = valor que llega del request del POS (ej: chain="1")
--   CONFIG_VALOR = valor que se envía al proveedor externo (ej: COMPANYID="2")
--
-- Regla:
--   VALOR_ORIGEN IS NULL  → registro de valor fijo, sin homologación.
--                           CONFIG_VALOR se usa directamente.
--   VALOR_ORIGEN NOT NULL → registro de mapeo. Cuando el POS envía
--                           ese valor, OmniStack traduce a CONFIG_VALOR.
--
-- Datos existentes: cero impacto — VALOR_ORIGEN queda NULL en todos.
-- ============================================================

-- 1. Agregar columna
ALTER TABLE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
  ADD VALOR_ORIGEN VARCHAR2(200);

COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG.VALOR_ORIGEN
  IS 'Valor que llega del POS para homologacion. NULL = valor fijo sin mapeo. NOT NULL = clave de traduccion hacia CONFIG_VALOR.';

-- 2. Eliminar el unique constraint anterior (solo cubre PROVEEDOR_KEY + CONFIG_KEY)
--    ya no es valido porque con homologacion puede haber varias filas
--    con el mismo CONFIG_KEY (una por cada valor de origen)
ALTER TABLE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
  DROP CONSTRAINT UX_IN_OMNI_PROVEEDOR_CONFIG_01;

-- 3. Nuevo indice unico que cubre ambos casos:
--    - Fijo (VALOR_ORIGEN IS NULL)   → NVL devuelve 'FIXED', no puede duplicarse
--    - Mapeo (VALOR_ORIGEN NOT NULL) → NVL devuelve el valor real, no puede duplicarse
CREATE UNIQUE INDEX TUKUNAFUNC.UX_IN_OMNI_PROVEEDOR_CONFIG_01
  ON TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
     (PROVEEDOR_KEY, CONFIG_KEY, NVL(VALOR_ORIGEN, 'FIXED'));

COMMIT;

-- Verificar estructura resultante:
-- SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
--   FROM USER_TAB_COLUMNS
--  WHERE TABLE_NAME = 'IN_OMNI_PROVEEDOR_CONFIG'
--  ORDER BY COLUMN_ID;
--
-- Verificar indice:
-- SELECT INDEX_NAME, UNIQUENESS, STATUS
--   FROM USER_INDEXES
--  WHERE TABLE_NAME = 'IN_OMNI_PROVEEDOR_CONFIG';
