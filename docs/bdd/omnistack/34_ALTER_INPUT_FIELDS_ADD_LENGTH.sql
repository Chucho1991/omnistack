-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   IN_OMNI_INPUT_FIELDS.FIELD_TYPE (STRING/DOUBLE/INTEGER/BOOLEAN)
--   no tiene ninguna restriccion de longitud/precision asociada, asi
--   que el POS no puede validar formato/rango de entrada del cajero
--   (regla de negocio RN-00 del analisis GEOPos: "GEOPos solo valida
--   formato/rango de entrada del cajero" — pero hoy no se le entrega
--   el dato para hacerlo).
--
--   Se agrega una sola columna, no dos: FIELD_TYPE ya es unico por
--   fila, asi que su significado no colisiona:
--     STRING  -> cantidad maxima de caracteres
--     INTEGER -> cantidad maxima de digitos
--     DOUBLE  -> cantidad de decimales
--     BOOLEAN -> no aplica (null)
--
--   Nullable, retrocompatible con las filas ya existentes.
-- ============================================================

ALTER TABLE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS ADD (
  FIELD_LENGTH NUMBER(5)
);

COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_INPUT_FIELDS.FIELD_LENGTH IS
  'Restriccion de tamano segun FIELD_TYPE: STRING=max caracteres, INTEGER=max digitos, DOUBLE=cantidad de decimales. Null=sin restriccion.';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT COLUMN_NAME, DATA_TYPE, DATA_PRECISION, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TUKUNAFUNC'
   AND TABLE_NAME = 'IN_OMNI_INPUT_FIELDS'
   AND COLUMN_NAME = 'FIELD_LENGTH';
