-- ============================================================
-- Ejecutar como: usuario con permisos ALTER sobre gpf_omnistack
-- (En local: ejecutar como el owner de AD_SERVICIO_PARAMETROS)
--
-- Proposito:
--   Agrega la columna ID_HOMOLOGADO a AD_SERVICIO_PARAMETROS.
--   Cuando ID_HOMOLOGADO = 'S', OmniStack genera un codigo de
--   autorizacion homologado (alfanumerico, max 10 chars) para
--   devolver al POS en lugar del codigo original del proveedor.
--
-- Regla:
--   ID_HOMOLOGADO = 'S' → se activa la homologacion de autorizacion.
--   ID_HOMOLOGADO = 'N' o NULL → comportamiento actual sin cambios.
--
-- Datos existentes: cero impacto — ID_HOMOLOGADO queda NULL (sin homologacion).
-- ============================================================

-- 1. Agregar columna
ALTER TABLE AD_SERVICIO_PARAMETROS
  ADD ID_HOMOLOGADO CHAR(1) DEFAULT 'N';

COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.ID_HOMOLOGADO
  IS 'S=activar homologacion de codigo de autorizacion. N o NULL=usar authorization del proveedor tal cual.';

-- 2. Constraint para validar valores permitidos
ALTER TABLE AD_SERVICIO_PARAMETROS
  ADD CONSTRAINT CK_AD_SERVPARAM_HOMOLOGADO
  CHECK (ID_HOMOLOGADO IS NULL OR ID_HOMOLOGADO IN ('S','N'));

COMMIT;

-- Verificar:
-- SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE, DATA_DEFAULT
--   FROM USER_TAB_COLUMNS
--  WHERE TABLE_NAME = 'AD_SERVICIO_PARAMETROS'
--    AND COLUMN_NAME = 'ID_HOMOLOGADO';
