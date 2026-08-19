-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   GROUP_LENGTH (script 40) define cuantos caracteres tiene cada
--   grupo cuando un input_field esta compuesto por varios numeros
--   concatenados con espacio. GROUP_LENGTH=0 no tiene ningun
--   significado valido (un grupo no puede tener cero caracteres) y
--   dejarlo posible obliga a cada consumidor (POS, este microservicio,
--   futuras integraciones) a decidir por su cuenta como tratarlo.
--
--   Se agrega una CHECK constraint para que ese estado invalido no
--   pueda existir en la base: GROUP_LENGTH debe ser NULL (campo simple,
--   sin agrupamiento) o un entero positivo (>=1, agrupamiento activo).
--   GROUP_LENGTH=1 es valido y distinto de NULL: significa "N grupos
--   de 1 caracter separados por espacio" (ej. "1 2 3 4"), no lo mismo
--   que un campo simple de N caracteres sin espacios ("1234").
--
--   Ademas, el agrupamiento (varios numeros separados por espacio) solo
--   tiene sentido sobre un campo de texto (FIELD_TYPE='STRING') — un
--   INTEGER o DOUBLE es un unico valor numerico, no admite agrupamiento.
--   La constraint tambien impide cargar GROUP_LENGTH en filas que no
--   sean STRING.
-- ============================================================

ALTER TABLE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
  ADD CONSTRAINT CK_INPUT_FIELDS_GROUP_LENGTH
  CHECK (GROUP_LENGTH IS NULL OR (GROUP_LENGTH > 0 AND FIELD_TYPE = 'STRING'));

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT CONSTRAINT_NAME, SEARCH_CONDITION, STATUS
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'TUKUNAFUNC'
   AND TABLE_NAME = 'IN_OMNI_INPUT_FIELDS'
   AND CONSTRAINT_NAME = 'CK_INPUT_FIELDS_GROUP_LENGTH';
