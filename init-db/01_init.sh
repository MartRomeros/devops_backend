#!/bin/bash
# Crea las bases de datos para ventas y despachos si no existen.
# Este script es ejecutado automáticamente por el contenedor de PostgreSQL
# al iniciar por primera vez.
set -e

DB_NAME_VENTAS="${DB_NAME_VENTAS:-ventas_db}"
DB_NAME_DESPACHOS="${DB_NAME_DESPACHOS:-despachos_db}"

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v db_ventas="$DB_NAME_VENTAS" \
  -v db_despachos="$DB_NAME_DESPACHOS" <<-'EOSQL'
    SELECT format('CREATE DATABASE %I', :'db_ventas')
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db_ventas')\gexec

    SELECT format('CREATE DATABASE %I', :'db_despachos')
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db_despachos')\gexec
EOSQL

echo "Bases de datos '$DB_NAME_VENTAS' y '$DB_NAME_DESPACHOS' listas."
