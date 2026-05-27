#!/bin/bash
# Crea las bases de datos para ventas y despachos si no existen.
# Este script es ejecutado automáticamente por el contenedor de PostgreSQL
# al iniciar por primera vez.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE ventas_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ventas_db')\gexec

    SELECT 'CREATE DATABASE despachos_db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'despachos_db')\gexec
EOSQL

echo "Bases de datos 'ventas_db' y 'despachos_db' listas."
