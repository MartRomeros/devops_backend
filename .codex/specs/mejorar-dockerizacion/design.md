# Design: Mejorar dockerizacion

## Resumen
La solucion propuesta centraliza PostgreSQL en un unico servicio Docker y una unica base de datos compartida por ambos microservicios. Los backends siguen siendo independientes entre si: ambos consumen la base de datos, pero no se declaran dependencias entre ellos.

`script.sql` pasa a ser la fuente explicita del esquema inicial y de los datos de prueba. La forma mas segura de mantener coherencia entre ventas y despachos, sin cambiar las entidades Java, es insertar ventas con IDs conocidos y despachos que usen esos IDs en `"idCompra"`. Se puede agregar una foreign key desde `despachos."idCompra"` hacia `venta.id_venta` si las operaciones actuales no requieren crear despachos para compras inexistentes.

## Archivos que probablemente se modificaran
- `docker-compose.yml`
- `.env.example`
- `script.sql`
- `README.md`, si se desea actualizar la documentacion operacional

## Arquitectura propuesta
- Servicio `postgres`:
  - Imagen `postgres:16-alpine`.
  - Contenedor unico, por ejemplo `postgres_db`.
  - Variables `POSTGRES_USER`, `POSTGRES_PASSWORD` y `POSTGRES_DB`.
  - Volumen `postgres_data:/var/lib/postgresql/data`.
  - Montaje de `./script.sql:/docker-entrypoint-initdb.d/01-script.sql:ro`.
  - Healthcheck con `pg_isready`.
  - Red `backend-net`.
- Servicio `backend-ventas`:
  - Mantiene build e imagen actuales.
  - Usa `DB_ENDPOINT=postgres`.
  - Usa `DB_NAME=${DB_NAME}`.
  - Depende solo de PostgreSQL healthy.
  - Red `backend-net`.
- Servicio `backend-despachos`:
  - Mantiene build e imagen actuales.
  - Usa `DB_ENDPOINT=postgres`.
  - Usa `DB_NAME=${DB_NAME}`.
  - Depende solo de PostgreSQL healthy.
  - Red `backend-net`.

## Flujo de datos
1. Docker Compose levanta PostgreSQL.
2. PostgreSQL crea la base de datos definida por `POSTGRES_DB`.
3. Si el volumen `postgres_data` no existe, PostgreSQL ejecuta `script.sql`.
4. `script.sql` crea tablas y secuencias compatibles con las entidades actuales.
5. `script.sql` inserta productos, ventas y despachos coherentes.
6. Los backends esperan a que PostgreSQL este healthy.
7. Ambos backends se conectan a la misma base de datos mediante variables de entorno.

## Diseno de base de datos
El script debe crear explicitamente estas tablas:

### `productos`
Debe respetar los nombres definidos con `@Column` en `Producto.java`, que usan columnas con comillas y camelCase.

Campos:
- `"idProducto"` `BIGINT PRIMARY KEY`
- `"nombreProducto"` `VARCHAR`
- `"descripcionProducto"` `VARCHAR`
- `"precioProducto"` `INTEGER`
- `"stockProducto"` `INTEGER`

### `venta`
La entidad `Venta` no declara `@Table` ni `@Column`, por lo que Hibernate normalmente usa la tabla `venta` y columnas snake_case.

Campos:
- `id_venta` `BIGINT PRIMARY KEY`
- `direccion_compra` `VARCHAR`
- `valor_compra` `INTEGER`
- `fecha_compra` `DATE`
- `despacho_generado` `BOOLEAN`

### `despachos`
Debe respetar los nombres definidos con `@Table` y `@Column` en `Despacho.java`.

Campos:
- `"idDespacho"` `BIGINT PRIMARY KEY`
- `"fechaDespacho"` `DATE`
- `"patenteCamion"` `VARCHAR`
- `intento` `INTEGER`
- `"idCompra"` `BIGINT`
- `"direccionCompra"` `VARCHAR`
- `"valorCompra"` `BIGINT`
- `entregado` `BOOLEAN`

## Coherencia entre ventas y despachos
La mejor forma sin cambiar codigo Java es:
- Insertar ventas con IDs fijos.
- Insertar despachos cuyo `"idCompra"` apunte a esos IDs.
- Mantener `"direccionCompra"` y `"valorCompra"` iguales a `direccion_compra` y `valor_compra` de la venta asociada.
- Ajustar `despacho_generado=true` en ventas que tengan al menos un despacho.

Opcionalmente, agregar una foreign key:
- `despachos."idCompra" REFERENCES venta(id_venta)`

Esta FK es recomendable porque ahora ambas tablas viven en la misma base de datos. Si durante la implementacion se detecta que la API de despachos necesita aceptar compras aun no existentes, la FK debe omitirse y la coherencia debe quedar solo en datos de seed.

## Variables de entorno
`.env.example` debe reemplazar las variables separadas:
- `DB_NAME_VENTAS`
- `DB_NAME_DESPACHOS`

por una variable unica:
- `DB_NAME=devops_db`

Se conservan:
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `DOCKERHUB_USERNAME`

## Riesgos
- `script.sql` solo se ejecuta automaticamente cuando el volumen PostgreSQL se crea por primera vez. Para probar cambios en el script hay que recrear el volumen con `docker compose down -v`.
- Las columnas quoted de `Producto` y `Despacho` son sensibles a mayusculas/minusculas en PostgreSQL.
- `ddl-auto=update` puede intentar ajustar el esquema si el SQL no coincide exactamente con lo esperado por Hibernate.
- El README actual describe dos bases de datos, por lo que quedara desactualizado si no se modifica.
- Una unica base compartida reduce aislamiento entre microservicios, aunque simplifica el despliegue academico.

## Estrategia de testing
- Ejecutar `docker compose config`.
- Levantar desde cero con `docker compose down -v` y luego `docker compose up -d`.
- Verificar que solo exista un servicio PostgreSQL.
- Verificar health de PostgreSQL y estado de ambos backends.
- Consultar tablas con `psql`:
  - `\dt`
  - `SELECT * FROM productos;`
  - `SELECT * FROM venta;`
  - `SELECT * FROM despachos;`
- Validar coherencia:
  - cada `despachos."idCompra"` existe en `venta.id_venta`.
  - direccion y valor coinciden entre despacho y venta asociada.
- Ejecutar tests de ventas con Maven wrapper.
- Ejecutar tests de despachos con Maven wrapper.
