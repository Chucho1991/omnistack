-- ============================================================================
-- Agrega a AD_SERVICIO_PARAMETROS las reglas de presentacion de business-lines.
-- Ejecutar conectado como propietario de GPF_OMNISTACK.
--
-- El script es reejecutable:
--   * crea solamente columnas y constraints inexistentes;
--   * inicializa solamente registros sin SERVICE_TYPE;
--   * no vuelve a sobrescribir items ya clasificados como P o B.
-- ============================================================================
SET SERVEROUTPUT ON
SET SQLBLANKLINES ON

DECLARE
    PROCEDURE add_column_if_missing(p_column_name VARCHAR2, p_definition VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM USER_TAB_COLUMNS
         WHERE TABLE_NAME = 'AD_SERVICIO_PARAMETROS'
           AND COLUMN_NAME = UPPER(p_column_name);

        IF v_count = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE AD_SERVICIO_PARAMETROS ADD ('
                || p_column_name || ' ' || p_definition || ')';
            DBMS_OUTPUT.PUT_LINE('Columna creada: ' || p_column_name);
        END IF;
    END;

    PROCEDURE add_constraint_if_missing(p_constraint_name VARCHAR2, p_condition VARCHAR2) IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM USER_CONSTRAINTS
         WHERE TABLE_NAME = 'AD_SERVICIO_PARAMETROS'
           AND CONSTRAINT_NAME = UPPER(p_constraint_name);

        IF v_count = 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE AD_SERVICIO_PARAMETROS ADD CONSTRAINT '
                || p_constraint_name || ' CHECK (' || p_condition || ')';
            DBMS_OUTPUT.PUT_LINE('Constraint creado: ' || p_constraint_name);
        END IF;
    END;
BEGIN
    add_column_if_missing('FLG_ONLY', 'CHAR(1)');
    add_column_if_missing('ALLOW_OTHER_BILLABLE_SERVICES', 'CHAR(1)');
    add_column_if_missing('ALLOW_SAME_SERVICE', 'CHAR(1)');
    add_column_if_missing('FLG_UNIQUE', 'CHAR(1)');
    add_column_if_missing('SERVICE_TYPE', 'CHAR(1)');
    add_column_if_missing('REC_TELEPEAJE_ACTIVE', 'CHAR(1)');
    add_column_if_missing('PRINT_CONFIRMATION_VOUCHER', 'CHAR(1)');

    add_constraint_if_missing('AD_SERV_PARAM_ONLY_CK', 'FLG_ONLY IN (''S'',''N'')');
    add_constraint_if_missing('AD_SERV_PARAM_OTH_BILL_CK', 'ALLOW_OTHER_BILLABLE_SERVICES IN (''S'',''N'')');
    add_constraint_if_missing('AD_SERV_PARAM_SAME_SERV_CK', 'ALLOW_SAME_SERVICE IN (''S'',''N'')');
    add_constraint_if_missing('AD_SERV_PARAM_UNIQUE_CK', 'FLG_UNIQUE IN (''S'',''N'')');
    add_constraint_if_missing('AD_SERV_PARAM_SERV_TYPE_CK', 'SERVICE_TYPE IN (''P'',''R'',''B'')');
    add_constraint_if_missing('AD_SERV_PARAM_REC_TELE_CK', 'REC_TELEPEAJE_ACTIVE IN (''S'',''N'')');
    add_constraint_if_missing('AD_SERV_PARAM_PRINT_CONF_CK', 'PRINT_CONFIRMATION_VOUCHER IN (''S'',''N'')');
END;
/

-- Perfil general R: recargas, peaje, telefonia y facturas.
UPDATE AD_SERVICIO_PARAMETROS
   SET FLG_ONLY = NVL(FLG_ONLY, 'N'),
       ALLOW_OTHER_BILLABLE_SERVICES = NVL(ALLOW_OTHER_BILLABLE_SERVICES, 'S'),
       ALLOW_SAME_SERVICE = NVL(ALLOW_SAME_SERVICE, 'S'),
       FLG_UNIQUE = NVL(FLG_UNIQUE, 'N'),
       SERVICE_TYPE = NVL(SERVICE_TYPE, 'R'),
       REC_TELEPEAJE_ACTIVE = NVL(REC_TELEPEAJE_ACTIVE, 'S'),
       PRINT_CONFIRMATION_VOUCHER = NVL(PRINT_CONFIRMATION_VOUCHER, 'S')
 WHERE FLG_ONLY IS NULL
    OR ALLOW_OTHER_BILLABLE_SERVICES IS NULL
    OR ALLOW_SAME_SERVICE IS NULL
    OR FLG_UNIQUE IS NULL
    OR SERVICE_TYPE IS NULL
    OR REC_TELEPEAJE_ACTIVE IS NULL
    OR PRINT_CONFIRMATION_VOUCHER IS NULL;

-- Perfil P: BET593 y ECUABET, CASH_IN y CASH_OUT.
UPDATE AD_SERVICIO_PARAMETROS
   SET FLG_ITEM = 'RECA',
       FLG_ONLY = 'S',
       ALLOW_OTHER_BILLABLE_SERVICES = 'S',
       ALLOW_SAME_SERVICE = 'N',
       FLG_UNIQUE = 'N',
       SERVICE_TYPE = 'P',
       REC_TELEPEAJE_ACTIVE = 'S',
       PRINT_CONFIRMATION_VOUCHER = 'N',
       USUARIO_MODIFICACION = USER,
       FECHA_MODIFICACION = SYSDATE
 WHERE CODIGO_ITEM_RMS IN ('100713841', '100708846', '100708850', '100708848')
   AND SERVICE_TYPE = 'R';

-- Perfil B: LOTERIA, LOTTO, POZO y PEGA (venta y premios).
UPDATE AD_SERVICIO_PARAMETROS
   SET FLG_ITEM = 'RECA',
       FLG_ONLY = 'N',
       ALLOW_OTHER_BILLABLE_SERVICES = 'N',
       ALLOW_SAME_SERVICE = 'S',
       FLG_UNIQUE = 'N',
       SERVICE_TYPE = 'B',
       REC_TELEPEAJE_ACTIVE = 'S',
       PRINT_CONFIRMATION_VOUCHER = 'N',
       USUARIO_MODIFICACION = USER,
       FECHA_MODIFICACION = SYSDATE
 WHERE CODIGO_ITEM_RMS IN (
       '100713842', '100708854', -- LOTERIA
       '100713844', '100708855', -- LOTTO
       '100713846', '100708856', -- POZO
       '100713848', '100713850', '100708852',
       '100708858', '100708860', '100708862'  -- PEGA
   )
   AND SERVICE_TYPE = 'R';

ALTER TABLE AD_SERVICIO_PARAMETROS MODIFY (
    FLG_ONLY DEFAULT 'N' NOT NULL,
    ALLOW_OTHER_BILLABLE_SERVICES DEFAULT 'S' NOT NULL,
    ALLOW_SAME_SERVICE DEFAULT 'S' NOT NULL,
    FLG_UNIQUE DEFAULT 'N' NOT NULL,
    SERVICE_TYPE DEFAULT 'R' NOT NULL,
    REC_TELEPEAJE_ACTIVE DEFAULT 'S' NOT NULL,
    PRINT_CONFIRMATION_VOUCHER DEFAULT 'S' NOT NULL
);

COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.FLG_ONLY IS 'S si el item debe mostrarse como unica opcion del flujo';
COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.ALLOW_OTHER_BILLABLE_SERVICES IS 'S si permite agregar otros servicios facturables';
COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.ALLOW_SAME_SERVICE IS 'S si permite repetir el mismo servicio';
COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.FLG_UNIQUE IS 'S si el item debe ser unico dentro de la transaccion';
COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.SERVICE_TYPE IS 'Tipo de servicio para POS: P pronostico, R recarga/factura, B boleto';
COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.REC_TELEPEAJE_ACTIVE IS 'S si habilita el flujo de recarga o telepeaje';
COMMENT ON COLUMN AD_SERVICIO_PARAMETROS.PRINT_CONFIRMATION_VOUCHER IS 'S si imprime comprobante de confirmacion';

COMMIT;

-- Verificacion de la parametrizacion aplicada.
SELECT CODIGO_ITEM_RMS,
       FLG_ITEM,
       FLG_ONLY,
       ALLOW_OTHER_BILLABLE_SERVICES,
       ALLOW_SAME_SERVICE,
       FLG_UNIQUE,
       SERVICE_TYPE,
       REC_TELEPEAJE_ACTIVE,
       PRINT_CONFIRMATION_VOUCHER
  FROM AD_SERVICIO_PARAMETROS
 ORDER BY CODIGO_ITEM_RMS;
