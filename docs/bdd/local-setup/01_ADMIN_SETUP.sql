-- ============================================================
-- Ejecutar como: SYSDBA
-- docker exec -it oracle-oracle-1 sqlplus sys/sys@XEPDB1 as sysdba
-- ============================================================
SET SQLBLANKLINES ON

-- Crear schema RMS local (simula las tablas Oracle Retail)
CREATE USER rms IDENTIFIED BY rms123
    QUOTA UNLIMITED ON USERS
    DEFAULT TABLESPACE USERS;

GRANT CREATE SESSION TO rms;
GRANT CREATE TABLE TO rms;
GRANT CREATE SEQUENCE TO rms;
GRANT CREATE SYNONYM TO rms;

COMMIT;
