# Tasks: Mejorar CI/CD para despliegue en dos EC2 privadas

> **Para agentes ejecutores:** trabajar una tarea a la vez. No implementar fuera de los archivos indicados sin actualizar primero el design. No borrar volumenes de Docker. No usar `git reset --hard` en la EC2 privada.

## Task 1: Actualizar secrets y nombres del workflow

**Objetivo:** reemplazar el deploy directo a `EC2_*` por nombres de secrets separados para EC2 publica y privada.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`
- Modificar: `README.md`

**Tests requeridos:**
- Revisar que `.github/workflows/deploy.yml` no use `secrets.EC2_HOST`, `secrets.EC2_USER` ni `secrets.EC2_SSH_KEY`.
- Revisar que el README documente la lista completa de secrets finales.

**Criterio de finalizacion:**
- El workflow usa `PUBLIC_EC2_HOST`, `PUBLIC_EC2_USER`, `PUBLIC_EC2_SSH_KEY`, `PRIVATE_EC2_VENTAS_HOST`, `PRIVATE_EC2_DESPACHOS_HOST`, `PRIVATE_EC2_USER` y `PRIVATE_EC2_SSH_KEY`.
- `REPO_URL` queda documentado como URL publica del repositorio.

## Task 2: Mantener build y push con `latest` y SHA

**Objetivo:** asegurar que ambas imagenes se publiquen en Docker Hub con tags `latest` y `${{ github.sha }}` antes de desplegar.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`

**Tests requeridos:**
- Confirmar visualmente que `build-and-push` publica:
  - `${{ env.IMAGE_VENTAS }}:latest`
  - `${{ env.IMAGE_VENTAS }}:${{ github.sha }}`
  - `${{ env.IMAGE_DESPACHOS }}:latest`
  - `${{ env.IMAGE_DESPACHOS }}:${{ github.sha }}`
- Confirmar que `deploy` tiene `needs: build-and-push`.

**Criterio de finalizacion:**
- Si falla build/push, deploy no se ejecuta.
- El deploy operativo sigue usando las imagenes `:latest` definidas en `docker-compose.yml`.

## Task 3: Implementar salto SSH EC2 publica -> dos EC2 privadas

**Objetivo:** hacer que GitHub Actions entre a la EC2 publica y desde ahi ejecute el despliegue en las dos EC2 privadas.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`

**Tests requeridos:**
- Revisar que el job `deploy` ya no copie `docker-compose.yml` directo a una EC2 unica.
- Revisar que la clave `PRIVATE_EC2_SSH_KEY` se escriba en la EC2 publica con permisos `600`.
- Revisar que exista limpieza de la clave temporal con `trap` o bloque equivalente.

**Criterio de finalizacion:**
- El workflow se conecta a `PUBLIC_EC2_HOST`.
- Desde la EC2 publica se conecta a `PRIVATE_EC2_VENTAS_HOST` y `PRIVATE_EC2_DESPACHOS_HOST`.
- La clave privada temporal de la EC2 privada se elimina al finalizar, incluso ante errores.

## Task 4: Preparar dependencias idempotentes en EC2 privada

**Objetivo:** validar o instalar `git`, Docker Engine y Docker Compose en Ubuntu cuando falten.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`
- Modificar: `README.md`

**Tests requeridos:**
- Revisar que el script remoto detecte `git`.
- Revisar que el script remoto detecte `docker`.
- Revisar que el script remoto detecte `docker compose`.
- Revisar que la instalacion use comandos compatibles con Ubuntu y requiera `sudo` cuando corresponda.

**Criterio de finalizacion:**
- En una EC2 privada limpia, el deploy puede instalar las herramientas faltantes.
- En una EC2 privada ya preparada, el deploy no reinstala innecesariamente.

## Task 5: Detectar uso de `sudo` para Docker

**Objetivo:** ejecutar comandos Docker con o sin `sudo` segun permisos del usuario remoto.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`

**Tests requeridos:**
- Revisar que el script pruebe `docker ps` sin `sudo`.
- Revisar que si `docker ps` falla, el script pruebe `sudo docker ps`.
- Revisar que los comandos `docker compose`, `docker image prune` y validaciones usen el prefijo detectado.

**Criterio de finalizacion:**
- Si Docker funciona sin `sudo`, el deploy usa `docker`.
- Si Docker requiere `sudo`, el deploy usa `sudo docker`.
- Si Docker no funciona de ninguna forma, el deploy falla con mensaje claro.

## Task 6: Clonar o actualizar el repositorio publico en EC2 privada

**Objetivo:** mantener el codigo actualizado en `/home/<PRIVATE_EC2_USER>/devops_backend` sin pisar cambios locales.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`

**Tests requeridos:**
- Revisar que si no existe `.git`, se ejecute `git clone "$REPO_URL"`.
- Revisar que si existe `.git`, se ejecute `git fetch`, `git checkout main` y `git pull --ff-only origin main`.
- Revisar que no se use `git reset --hard`.

**Criterio de finalizacion:**
- Primera ejecucion clona el repo publico.
- Ejecuciones posteriores actualizan `main` sin borrar cambios manuales.
- Si hay divergencia local, el deploy falla en vez de sobrescribir.

## Task 7: Generar `.env` productivo en EC2 privada

**Objetivo:** escribir las variables que consume `docker-compose.yml` usando GitHub Secrets.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`
- Modificar: `.env.example`
- Modificar: `README.md`

**Tests requeridos:**
- Revisar que `.env` incluya:
  - `POSTGRES_USER`
  - `POSTGRES_PASSWORD`
  - `DB_NAME_VENTAS`
  - `DB_NAME_DESPACHOS`
  - `DOCKERHUB_USERNAME`
- Revisar que no se impriman valores secretos en logs.

**Criterio de finalizacion:**
- Docker Compose recibe todas las variables requeridas.
- `.env.example` y README coinciden con el workflow.

## Task 8: Parametrizar inicializacion de PostgreSQL

**Objetivo:** permitir que `init-db/01_init.sh` cree las bases indicadas por `DB_NAME_VENTAS` y `DB_NAME_DESPACHOS`.

**Archivos esperados:**
- Modificar: `init-db/01_init.sh`
- Modificar: `docker-compose.yml`
- Modificar: `.env.example`
- Modificar: `README.md`

**Tests requeridos:**
- Ejecutar: `bash -n init-db/01_init.sh`
- Ejecutar: `docker compose config`
- Revisar que `postgres.environment` reciba `DB_NAME_VENTAS` y `DB_NAME_DESPACHOS` si el script los necesita.

**Criterio de finalizacion:**
- Con defaults, el script crea `ventas_db` y `despachos_db`.
- Con secrets personalizados, el script crea los nombres indicados.
- El cambio no elimina ni recrea `postgres_data`.

## Task 9: Ordenar despliegue Docker Compose en cada EC2 privada

**Objetivo:** levantar primero PostgreSQL, descargar imagenes nuevas y recrear solo backends.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`

**Tests requeridos:**
- Revisar que el orden por cada EC2 privada sea:
  - `docker compose up -d postgres`
  - `docker compose pull <backend-asignado>`
  - `docker compose up -d --no-deps --force-recreate <backend-asignado>`
- Revisar que no aparezca `docker compose down -v`.
- Revisar que no se elimine `postgres_data`.

**Criterio de finalizacion:**
- PostgreSQL existe antes de recrear backends.
- Cada backend usa su imagen `latest` recien descargada en su EC2 privada.
- La base de datos persiste entre despliegues.

## Task 10: Agregar verificacion final simple del deploy

**Objetivo:** verificar que Docker Compose haya recibido la orden de levantar PostgreSQL y el backend asignado sin esperar endpoints HTTP.

**Archivos esperados:**
- Modificar: `.github/workflows/deploy.yml`

**Tests requeridos:**
- Revisar que el script ejecute `docker compose ps`.
- Revisar que el script no haga `curl` a los backends durante el deploy.
- Revisar que la EC2 de ventas ejecute `docker compose ps postgres backend-ventas`.
- Revisar que la EC2 de despachos ejecute `docker compose ps postgres backend-despachos`.

**Criterio de finalizacion:**
- El pipeline levanta PostgreSQL y el backend asignado en cada EC2 privada.
- El pipeline no espera endpoints HTTP, para evitar fallos por arranques lentos.

## Task 11: Actualizar documentacion de operacion

**Objetivo:** dejar documentado el flujo bastion, los secrets, puertos y verificaciones manuales.

**Archivos esperados:**
- Modificar: `README.md`

**Tests requeridos:**
- Revisar que README indique push a `main`.
- Revisar que README liste la coleccion completa de secrets requeridos.
- Revisar que README explique que PostgreSQL puede publicar `5432` en el host, pero el Security Group no debe exponerlo externamente.
- Revisar que README explique que el deploy corre en EC2 privada y la EC2 publica solo es bastion.

**Criterio de finalizacion:**
- Un operador puede configurar GitHub Secrets y entender el flujo sin leer el workflow completo.

## Task 12: Validacion local antes de PR

**Objetivo:** comprobar sintaxis y coherencia de los cambios antes de ejecutar el pipeline real.

**Archivos esperados:**
- Verificar: `.github/workflows/deploy.yml`
- Verificar: `docker-compose.yml`
- Verificar: `init-db/01_init.sh`
- Verificar: `.env.example`
- Verificar: `README.md`

**Tests requeridos:**
- Ejecutar: `docker compose config`
- Ejecutar: `bash -n init-db/01_init.sh`
- Ejecutar si esta disponible: `actionlint .github/workflows/deploy.yml`
- Ejecutar si no esta disponible `actionlint`: revisar manualmente indentacion YAML, expresiones `${{ secrets.* }}` y heredocs del workflow.

**Criterio de finalizacion:**
- `docker compose config` termina con exit code `0`.
- `bash -n init-db/01_init.sh` termina con exit code `0`.
- No quedan referencias a secrets antiguos `EC2_HOST`, `EC2_USER` o `EC2_SSH_KEY`.
- No quedan marcas pendientes ni valores de ejemplo dentro del workflow.

## Task 13: Validacion real en GitHub Actions

**Objetivo:** confirmar que el pipeline funciona de extremo a extremo en AWS Academy.

**Archivos esperados:**
- No modificar archivos salvo ajustes derivados de fallos encontrados.

**Tests requeridos:**
- Hacer push a `main`.
- Confirmar que `build-and-push` publica las cuatro etiquetas esperadas en Docker Hub.
- Confirmar que `deploy` conecta GitHub Actions -> EC2 publica -> ambas EC2 privadas.
- Confirmar que en la EC2 privada de ventas `docker compose ps` muestra `postgres_db` y `backend_ventas`.
- Confirmar que en la EC2 privada de despachos `docker compose ps` muestra `postgres_db` y `backend_despachos`.
- Confirmar endpoints:
  - `http://localhost:8082`
  - `http://localhost:8081`
- Ejecutar un segundo deploy y confirmar que `postgres_data` persiste.

**Criterio de finalizacion:**
- El workflow completo termina exitosamente.
- Los contenedores productivos corren separados en las dos EC2 privadas.
- Docker Hub contiene tags `latest` y `<commit-sha>` para ambos backends.
