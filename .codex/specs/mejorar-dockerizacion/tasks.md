# Tasks: Mejorar dockerizacion

## Task 1: Unificar variables de entorno
- Objetivo: reemplazar la configuracion de dos bases por una sola base centralizada.
- Archivos esperados:
  - `.env.example`
  - `docker-compose.yml`
- Cambios:
  - Reemplazar `DB_NAME_VENTAS` y `DB_NAME_DESPACHOS` por `DB_NAME`.
  - Mantener `POSTGRES_USER`, `POSTGRES_PASSWORD` y `DOCKERHUB_USERNAME`.
- Tests requeridos:
  - Revisar que `docker compose config` resuelva variables sin errores.
- Criterio de finalizacion:
  - Ambos backends usan el mismo `DB_NAME`.

## Task 2: Corregir y simplificar PostgreSQL en Docker Compose
- Objetivo: dejar un solo contenedor PostgreSQL con volumen persistente.
- Archivos esperados:
  - `docker-compose.yml`
- Cambios:
  - Crear un unico servicio `postgres`.
  - Eliminar cualquier referencia a `postgres-ventas` o `postgres-despachos`.
  - Definir `postgres_data`.
  - Montar `script.sql` en `/docker-entrypoint-initdb.d/`.
  - Mantener healthcheck.
- Tests requeridos:
  - `docker compose config`.
- Criterio de finalizacion:
  - El compose contiene un solo servicio PostgreSQL y un volumen `postgres_data`.

## Task 3: Conectar ambos backends a la base centralizada
- Objetivo: ambos microservicios deben apuntar al mismo PostgreSQL y a la misma DB.
- Archivos esperados:
  - `docker-compose.yml`
- Cambios:
  - Configurar `DB_ENDPOINT=postgres`.
  - Configurar `DB_PORT=5432`.
  - Configurar `DB_NAME=${DB_NAME}` en ambos backends.
  - Corregir `backend-despachos` para que no dependa de `postgres-despachos`.
  - Evitar dependencias entre `backend-ventas` y `backend-despachos`.
- Tests requeridos:
  - `docker compose config`.
  - Revisar que ningun backend tenga `depends_on` hacia el otro backend.
- Criterio de finalizacion:
  - Ambos backends dependen como maximo de PostgreSQL healthy y comparten red `backend-net`.

## Task 4: Crear esquema explicito en script.sql
- Objetivo: crear tablas compatibles con las entidades JPA actuales.
- Archivos esperados:
  - `script.sql`
- Cambios:
  - Crear tabla `productos`.
  - Crear tabla `venta`.
  - Crear tabla `despachos`.
  - Crear secuencias/defaults si son necesarias para compatibilidad con `GenerationType.AUTO`.
  - Respetar columnas quoted y snake_case esperadas.
- Tests requeridos:
  - Levantar PostgreSQL desde volumen limpio.
  - Consultar `\dt` en la base centralizada.
- Criterio de finalizacion:
  - Las tablas existen y Hibernate no crea tablas duplicadas.

## Task 5: Insertar datos iniciales coherentes
- Objetivo: poblar la base con datos utiles para pruebas manuales.
- Archivos esperados:
  - `script.sql`
- Cambios:
  - Insertar productos.
  - Insertar ventas con IDs conocidos.
  - Insertar despachos asociados a ventas existentes.
  - Mantener direccion y valor coherentes entre venta y despacho.
  - Incluir ventas con y sin despacho, despachos pendientes, entregados y con mas de un intento.
- Tests requeridos:
  - Consultas SQL para validar conteos y relaciones.
- Criterio de finalizacion:
  - Cada `despachos."idCompra"` apunta a una venta existente.

## Task 6: Validar arranque completo
- Objetivo: comprobar que la infraestructura funciona desde cero.
- Archivos esperados:
  - Sin cambios adicionales salvo ajustes menores derivados de la validacion.
- Comandos sugeridos:
  - `docker compose down -v`
  - `docker compose up -d`
  - `docker compose ps`
  - `docker compose logs backend-ventas backend-despachos`
- Tests requeridos:
  - Probar endpoint de ventas.
  - Probar endpoint de despachos.
- Criterio de finalizacion:
  - PostgreSQL y ambos backends quedan levantados sin errores de conexion.

## Task 7: Ejecutar tests de backend
- Objetivo: asegurar que los cambios de infraestructura no rompen los proyectos Spring Boot.
- Archivos esperados:
  - Sin cambios salvo correcciones necesarias detectadas por tests.
- Comandos sugeridos:
  - `./mvnw test` en `back-Ventas_SpringBoot/Springboot-API-REST`.
  - `./mvnw test` en `back-Despachos_SpringBoot/Springboot-API-REST-DESPACHO`.
- Tests requeridos:
  - Tests existentes de ambos backends.
- Criterio de finalizacion:
  - Los tests pasan o queda documentada una falla preexistente.

## Task 8: Actualizar documentacion operacional
- Objetivo: alinear la documentacion con una sola base de datos centralizada.
- Archivos esperados:
  - `README.md`
- Cambios:
  - Reemplazar referencias a `ventas_db` y `despachos_db` como bases separadas.
  - Documentar `DB_NAME`.
  - Documentar `postgres_data`.
  - Actualizar comandos de conexion `psql`.
- Tests requeridos:
  - Revision manual de comandos documentados.
- Criterio de finalizacion:
  - README no contradice el nuevo compose.
