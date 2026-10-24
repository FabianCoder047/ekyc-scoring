-- Immutabilité applicative : audit_log et decision_kyc sont append-only.
CREATE OR REPLACE FUNCTION refuser_modification()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  RAISE EXCEPTION 'TABLE_APPEND_ONLY: %', TG_TABLE_NAME
    USING ERRCODE = 'P0001';
END;
$$;

CREATE TRIGGER trg_audit_log_append_only
  BEFORE UPDATE OR DELETE ON audit_log
  FOR EACH ROW EXECUTE FUNCTION refuser_modification();

CREATE TRIGGER trg_audit_log_truncate
  BEFORE TRUNCATE ON audit_log
  FOR EACH STATEMENT EXECUTE FUNCTION refuser_modification();

CREATE TRIGGER trg_decision_kyc_append_only
  BEFORE UPDATE OR DELETE ON decision_kyc
  FOR EACH ROW EXECUTE FUNCTION refuser_modification();

CREATE TRIGGER trg_decision_kyc_truncate
  BEFORE TRUNCATE ON decision_kyc
  FOR EACH STATEMENT EXECUTE FUNCTION refuser_modification();

-- Rôle applicatif dédié : lus et écritures métier, mais audit_log et decision_kyc
-- en lecture/insertion uniquement. Le rôle est créé par docker/postgres/init.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kyc_app') THEN
    GRANT USAGE ON SCHEMA public TO kyc_app;
    REVOKE ALL ON ALL TABLES IN SCHEMA public FROM kyc_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO kyc_app;
    -- Le GRANT global ci-dessus a accordé UPDATE/DELETE : on les retire
    -- explicitement pour que le rôle applicatif reste en lecture/insertion.
    REVOKE UPDATE, DELETE ON audit_log, decision_kyc FROM kyc_app;
    GRANT SELECT, INSERT ON audit_log TO kyc_app;
    GRANT SELECT, INSERT ON decision_kyc TO kyc_app;
    REVOKE ALL ON flyway_schema_history FROM kyc_app;
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO kyc_app;
  END IF;
END;
$$;
