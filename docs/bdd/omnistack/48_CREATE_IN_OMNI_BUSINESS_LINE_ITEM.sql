-- ============================================================================
-- ESQUEMA  : TRX3 (QA) / esquema transaccional OMNISTACK equivalente
-- OBJETIVO : Configuracion de presentacion de /business-lines por item RMS.
--
-- Ejecutar conectado como TRX3. El script es reejecutable: crea los objetos
-- solamente si no existen e inserta solamente codigos RMS aun no configurados.
-- ============================================================================
SET SERVEROUTPUT ON
SET SQLBLANKLINES ON

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
      FROM USER_TABLES
     WHERE TABLE_NAME = 'IN_OMNI_BUSINESS_LINE_ITEM';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE IN_OMNI_BUSINESS_LINE_ITEM (
                ID_CONFIG                      NUMBER(12)    NOT NULL,
                RMS_ITEM_CODE                  VARCHAR2(50)  NOT NULL,
                FLAG_ITEM                      VARCHAR2(10)  NOT NULL,
                FLG_ONLY                       CHAR(1)       DEFAULT 'N' NOT NULL,
                ALLOW_OTHER_BILLABLE_SERVICES  CHAR(1)       DEFAULT 'S' NOT NULL,
                ALLOW_SAME_SERVICE             CHAR(1)       DEFAULT 'S' NOT NULL,
                FLG_UNIQUE                     CHAR(1)       DEFAULT 'N' NOT NULL,
                SERVICE_TYPE                   CHAR(1)       DEFAULT 'R' NOT NULL,
                REC_TELEPEAJE_ACTIVE           CHAR(1)       DEFAULT 'S' NOT NULL,
                PRINT_CONFIRMATION_VOUCHER     CHAR(1)       DEFAULT 'S' NOT NULL,
                ENABLED                        CHAR(1)       DEFAULT 'S' NOT NULL,
                USR_CREACION                   VARCHAR2(100) DEFAULT USER NOT NULL,
                FEC_CREACION                   DATE          DEFAULT SYSDATE NOT NULL,
                USR_MODIFICACION               VARCHAR2(100),
                FEC_MODIFICACION               DATE,
                CONSTRAINT PK_IN_OMNI_BUS_LINE_ITEM PRIMARY KEY (ID_CONFIG),
                CONSTRAINT UX_IN_OMNI_BUS_LINE_ITEM UNIQUE (RMS_ITEM_CODE),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_FLAG CHECK (FLAG_ITEM IN ('RECA','FACT')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_ONLY CHECK (FLG_ONLY IN ('S','N')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_OTHER CHECK (ALLOW_OTHER_BILLABLE_SERVICES IN ('S','N')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_SAME CHECK (ALLOW_SAME_SERVICE IN ('S','N')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_UNIQUE CHECK (FLG_UNIQUE IN ('S','N')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_TYPE CHECK (SERVICE_TYPE IN ('P','R','B')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_TELE CHECK (REC_TELEPEAJE_ACTIVE IN ('S','N')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_PRINT CHECK (PRINT_CONFIRMATION_VOUCHER IN ('S','N')),
                CONSTRAINT CK_IN_OMNI_BUS_LINE_ENABLED CHECK (ENABLED IN ('S','N'))
            )
        ]';
        DBMS_OUTPUT.PUT_LINE('Tabla creada: IN_OMNI_BUSINESS_LINE_ITEM');
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME = 'SEQ_IN_OMNI_BUS_LINE_ITEM';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE
            'CREATE SEQUENCE SEQ_IN_OMNI_BUS_LINE_ITEM START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
        DBMS_OUTPUT.PUT_LINE('Secuencia creada: SEQ_IN_OMNI_BUS_LINE_ITEM');
    END IF;
END;
/

COMMENT ON TABLE IN_OMNI_BUSINESS_LINE_ITEM IS 'Configuracion POS de business-lines asociada a un codigo de item RMS';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.RMS_ITEM_CODE IS 'Codigo de item del catalogo RMS';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.FLAG_ITEM IS 'Bandera de item expuesta al POS como flg_item: RECA o FACT';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.FLG_ONLY IS 'S si el item debe mostrarse como unica opcion del flujo';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.ALLOW_OTHER_BILLABLE_SERVICES IS 'S si permite agregar otros servicios facturables';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.ALLOW_SAME_SERVICE IS 'S si permite repetir el mismo servicio';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.FLG_UNIQUE IS 'S si el item debe ser unico dentro de la transaccion';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.SERVICE_TYPE IS 'P pronostico, R recarga/factura, B boleto';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.REC_TELEPEAJE_ACTIVE IS 'S si habilita el flujo de recarga o telepeaje';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.PRINT_CONFIRMATION_VOUCHER IS 'S si imprime comprobante de confirmacion';
COMMENT ON COLUMN IN_OMNI_BUSINESS_LINE_ITEM.ENABLED IS 'S si la configuracion se encuentra activa';

DECLARE
    PROCEDURE insert_if_missing(
            p_rms_item_code VARCHAR2,
            p_flag_item VARCHAR2,
            p_only CHAR,
            p_allow_other CHAR,
            p_allow_same CHAR,
            p_unique CHAR,
            p_service_type CHAR,
            p_rec_telepeaje CHAR,
            p_print_voucher CHAR) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count
          FROM IN_OMNI_BUSINESS_LINE_ITEM
         WHERE RMS_ITEM_CODE = p_rms_item_code;

        IF v_count = 0 THEN
            INSERT INTO IN_OMNI_BUSINESS_LINE_ITEM (
                ID_CONFIG, RMS_ITEM_CODE, FLAG_ITEM, FLG_ONLY,
                ALLOW_OTHER_BILLABLE_SERVICES, ALLOW_SAME_SERVICE,
                FLG_UNIQUE, SERVICE_TYPE, REC_TELEPEAJE_ACTIVE,
                PRINT_CONFIRMATION_VOUCHER, ENABLED,
                USR_CREACION, FEC_CREACION)
            VALUES (
                SEQ_IN_OMNI_BUS_LINE_ITEM.NEXTVAL, p_rms_item_code, p_flag_item, p_only,
                p_allow_other, p_allow_same, p_unique, p_service_type,
                p_rec_telepeaje, p_print_voucher, 'S', USER, SYSDATE);
        END IF;
    END;
BEGIN
    -- Perfil P: BET593 y ECUABET.
    insert_if_missing('100713841', 'RECA', 'S', 'S', 'N', 'N', 'P', 'S', 'N');
    insert_if_missing('100708846', 'RECA', 'S', 'S', 'N', 'N', 'P', 'S', 'N');
    insert_if_missing('100708850', 'RECA', 'S', 'S', 'N', 'N', 'P', 'S', 'N');
    insert_if_missing('100708848', 'RECA', 'S', 'S', 'N', 'N', 'P', 'S', 'N');

    -- Perfil B: LOTERIA, LOTTO y POZO (venta y premios).
    insert_if_missing('100713842', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100708854', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100713844', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100708855', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100713846', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100708856', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');

    -- Perfil B: PEGA2, PEGA3 y PEGA4 (venta y premios).
    insert_if_missing('100713848', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100713850', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100708852', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100708858', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100708860', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
    insert_if_missing('100708862', 'RECA', 'N', 'N', 'S', 'N', 'B', 'S', 'N');
END;
/

COMMIT;

SELECT RMS_ITEM_CODE,
       FLAG_ITEM,
       FLG_ONLY,
       ALLOW_OTHER_BILLABLE_SERVICES,
       ALLOW_SAME_SERVICE,
       FLG_UNIQUE,
       SERVICE_TYPE,
       REC_TELEPEAJE_ACTIVE,
       PRINT_CONFIRMATION_VOUCHER,
       ENABLED
  FROM IN_OMNI_BUSINESS_LINE_ITEM
 ORDER BY RMS_ITEM_CODE;
