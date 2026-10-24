#!/bin/sh
# Création des rôles PostgreSQL au premier démarrage du conteneur.
# - $DB_MIGRATION_USER : propriétaire de la base, utilisé par Flyway (DDL).
# - $DB_APP_USER       : rôle applicatif limité (DML), utilisé par Hikari.
#
# Les scripts de /docker-entrypoint-initdb.d ne sont exécutés qu'à la création
# du volume de données. Pour appliquer un changement de mot de passe sur une
# base déjà initialisée, exécuter ALTER ROLE manuellement (voir README).

set -eu

# 1. Rôles et propriété de la base (connexion à la base d'administration).
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname postgres \
  -v migration_user="$DB_MIGRATION_USER" \
  -v migration_password="$DB_MIGRATION_PASSWORD" \
  -v app_user="$DB_APP_USER" \
  -v app_password="$DB_APP_PASSWORD" \
  -v db_name="$POSTGRES_DB" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'migration_user', :'migration_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'migration_user')
\gexec

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

SELECT format('ALTER ROLE %I PASSWORD %L', :'migration_user', :'migration_password')
\gexec

SELECT format('ALTER ROLE %I PASSWORD %L', :'app_user', :'app_password')
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', :'db_name', :'migration_user')
\gexec

GRANT CONNECT ON DATABASE :"db_name" TO :"migration_user", :"app_user";
SQL

# 2. Privilèges sur le schéma applicatif (connexion à la base kyc).
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v migration_user="$DB_MIGRATION_USER" \
  -v app_user="$DB_APP_USER" <<'SQL'
GRANT USAGE, CREATE ON SCHEMA public TO :"migration_user";
GRANT USAGE ON SCHEMA public TO :"app_user";

ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";
SQL

echo "Rôles PostgreSQL initialisés : $DB_MIGRATION_USER (migration), $DB_APP_USER (application)."
