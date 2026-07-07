/* ============================================================
   ESQUEMA  : TUKUNAFUNC
   PROYECTO : OmniStack
   OBJETIVO : Crear tabla de bitacora para control de cupos
              maximos diarios de retiro (CASH_OUT) por local.

   TABLA CREADA
   ---------------------------------------------------------------
   IN_OMNI_CASHOUT_CUPO_DIARIO   Bitacora de reservas y consumos
                                  de cupo CASH_OUT por farmacia/dia.

   LOGICA DE NEGOCIO
   ---------------------------------------------------------------
   - MONTO_MAX de AD_SERVICIO_PARAMETROS es el cupo maximo diario
     por local (farmacia) y al mismo tiempo el maximo por transaccion.
   - Cada PRECHECK exitoso para CASH_OUT registra una RESERVA
     (ESTADO = 'RESERVADO') que descuenta cupo disponible.
   - Cada EXECUTE exitoso confirma la reserva (ESTADO = 'CONFIRMADO').
   - Si la reserva no se confirma en N minutos (configurable),
     un proceso scheduled la expira (ESTADO = 'EXPIRADO') y
     restituye el cupo al saldo disponible del local.
   - Un REVERSE exitoso en el mismo dia restituye el cupo
     (ESTADO = 'REVERTIDO').
   - Un REVERSE en fecha posterior a la original NO restituye cupo
     porque corresponde a un saldo de otro dia.

   NOTAS
   ---------------------------------------------------------------
   - No se usan triggers. Los IDs se generan via
     SEQ_IN_OMNI_CASHOUT_CUPO.NEXTVAL desde Java.
   - El cupo disponible se calcula como:
     MONTO_MAX - SUM(MONTO) WHERE ESTADO IN ('RESERVADO','CONFIRMADO')
     AND FECHA_OPERACION = TRUNC(SYSDATE)
   ============================================================ */

---------------------------------------------------------------
-- 1) TABLA: IN_OMNI_CASHOUT_CUPO_DIARIO
---------------------------------------------------------------
CREATE TABLE TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO (
  ID_CUPO               NUMBER(18)      NOT NULL,
  UUID                  VARCHAR2(200)   NOT NULL,
  CADENA                NUMBER(10)      NOT NULL,
  FARMACIA              NUMBER(10)      NOT NULL,
  POS                   VARCHAR2(20),
  RMS_ITEM_CODE         VARCHAR2(100)   NOT NULL,
  SERVICE_PROVIDER_CODE VARCHAR2(20)    NOT NULL,
  MONTO                 NUMBER(18,2)    NOT NULL,
  ESTADO                VARCHAR2(20)    NOT NULL,
  FECHA_OPERACION       DATE            NOT NULL,
  FECHA_RESERVA         TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
  FECHA_CONFIRMACION    TIMESTAMP,
  FECHA_EXPIRACION      TIMESTAMP,
  FECHA_REVERSION       TIMESTAMP,
  USR_CREACION          VARCHAR2(100)   DEFAULT USER NOT NULL,
  FEC_CREACION          DATE            DEFAULT SYSDATE NOT NULL,
  USR_MODIFICACION      VARCHAR2(100),
  FEC_MODIFICACION      DATE,

  CONSTRAINT PK_IN_OMNI_CASHOUT_CUPO
    PRIMARY KEY (ID_CUPO),
  CONSTRAINT CK_IN_OMNI_CASHOUT_CUPO_EST
    CHECK (ESTADO IN ('RESERVADO','CONFIRMADO','EXPIRADO','REVERTIDO'))
);

---------------------------------------------------------------
-- 2) SECUENCIA
---------------------------------------------------------------
CREATE SEQUENCE TUKUNAFUNC.SEQ_IN_OMNI_CASHOUT_CUPO
  START WITH 1 INCREMENT BY 1 CACHE 20 NOCYCLE;

---------------------------------------------------------------
-- 3) INDICES
---------------------------------------------------------------
-- Indice principal para calcular cupo disponible del local en el dia
CREATE INDEX TUKUNAFUNC.IX_CASHOUT_CUPO_FARM_FECHA
  ON TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO (CADENA, FARMACIA, RMS_ITEM_CODE, FECHA_OPERACION, ESTADO);

-- Indice para busqueda por UUID (vincular precheck con execute)
CREATE UNIQUE INDEX TUKUNAFUNC.UX_CASHOUT_CUPO_UUID
  ON TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO (UUID);

-- Indice para el scheduler de expiracion
CREATE INDEX TUKUNAFUNC.IX_CASHOUT_CUPO_EXPIRACION
  ON TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO (ESTADO, FECHA_RESERVA);

---------------------------------------------------------------
-- 4) COMENTARIOS
---------------------------------------------------------------
COMMENT ON TABLE  TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO IS 'Bitacora de control de cupos maximos diarios de CASH_OUT por farmacia/local.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.ID_CUPO               IS 'PK generada con SEQ_IN_OMNI_CASHOUT_CUPO.NEXTVAL.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.UUID                  IS 'UUID de la transaccion OmniStack (vincula precheck con execute).';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.CADENA                IS 'Codigo de cadena del local.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.FARMACIA              IS 'Codigo de farmacia/local (identifica el punto de venta).';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.RMS_ITEM_CODE         IS 'Codigo del item RMS del servicio CASH_OUT.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.SERVICE_PROVIDER_CODE IS 'Codigo del proveedor de servicio.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.MONTO                 IS 'Monto de la transaccion reservado/confirmado.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.ESTADO                IS 'Estado: RESERVADO (precheck), CONFIRMADO (execute), EXPIRADO (timeout), REVERTIDO (reverse).';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.FECHA_OPERACION       IS 'Fecha del dia de operacion (TRUNC de la fecha de reserva). Agrupa cupo diario.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.FECHA_RESERVA         IS 'Timestamp de creacion de la reserva (precheck).';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.FECHA_CONFIRMACION    IS 'Timestamp de confirmacion (execute).';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.FECHA_EXPIRACION      IS 'Timestamp de expiracion automatica por timeout.';
COMMENT ON COLUMN TUKUNAFUNC.IN_OMNI_CASHOUT_CUPO_DIARIO.FECHA_REVERSION       IS 'Timestamp de restitucion por reverso.';

COMMIT;
