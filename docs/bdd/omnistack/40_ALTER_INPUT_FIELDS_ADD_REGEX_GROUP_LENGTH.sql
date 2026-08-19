-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   FIELD_LENGTH (script 34) valida una unica magnitud por campo
--   (max caracteres / max digitos / decimales). No alcanza para campos
--   compuestos por varios numeros concatenados con espacio, como
--   "combinacion" de Tradicionales (ej: "11 21 23 23" = 4 numeros de
--   2 digitos cada uno) — el POS necesita saber cuantas cajas de input
--   dibujar y de que ancho cada una, ademas de un patron de validacion
--   explicito para el valor final.
--
--   Se agregan 2 columnas:
--     REGEX        VARCHAR2(500) — patron de validacion completo del
--                   valor final que escribe el cajero. Si es NULL, se
--                   sigue validando solo por FIELD_TYPE/FIELD_LENGTH
--                   como hasta ahora (retrocompatible).
--     GROUP_LENGTH  NUMBER(5)    — cantidad de caracteres por grupo,
--                   SOLO para campos agrupados (ej: combinacion). Si
--                   GROUP_LENGTH es NULL, FIELD_LENGTH conserva su
--                   significado original (script 34: max caracteres/
--                   digitos/decimales segun FIELD_TYPE).
--
--   REINTERPRETACION DE FIELD_LENGTH cuando GROUP_LENGTH NO es NULL:
--     FIELD_LENGTH pasa a significar "cantidad de grupos" (no cantidad
--     de caracteres). Ejemplo: FIELD_LENGTH=4, GROUP_LENGTH=2 -> el POS
--     arma 4 cajas de 2 digitos cada una, cajero completa algo como
--     "11 21 23 23" (grupos separados por un espacio).
--
--   Ambas columnas nullable, retrocompatibles con las filas existentes
--   (ningun campo actual usa agrupamiento; todos siguen validando por
--   FIELD_LENGTH simple hasta que se les cargue REGEX/GROUP_LENGTH).
-- ============================================================

ALTER TABLE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS ADD (
  REGEX        VARCHAR2(500),
  GROUP_LENGTH NUMBER(5)
);

COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_INPUT_FIELDS.REGEX IS
  'Patron de validacion completo del valor que escribe el cajero. Null = sin regex, se valida solo por FIELD_TYPE/FIELD_LENGTH.';

COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_INPUT_FIELDS.GROUP_LENGTH IS
  'Caracteres por grupo para campos compuestos por varios numeros concatenados con espacio (ej: combinacion). Si no es null, FIELD_LENGTH pasa a significar "cantidad de grupos" en vez de "cantidad de caracteres". Null = FIELD_LENGTH conserva su significado original (script 34).';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'TUKUNAFUNC'
   AND TABLE_NAME = 'IN_OMNI_INPUT_FIELDS'
   AND COLUMN_NAME IN ('REGEX', 'GROUP_LENGTH');
