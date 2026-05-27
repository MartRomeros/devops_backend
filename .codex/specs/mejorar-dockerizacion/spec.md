# Spec: Mejorar dockerizacion

## Objetivo
Unificar la infraestructura Docker del backend para que ambos microservicios Spring Boot usen un solo contenedor PostgreSQL, una sola base de datos centralizada y una unica red Docker compartida, con datos iniciales coherentes cargados desde `script.sql`.

## Alcance
- Actualizar `docker-compose.yml` para definir un unico servicio PostgreSQL.
- Configurar ambos backends para conectarse al mismo host PostgreSQL y a la misma base de datos.
- Corregir la referencia invalida actual a `postgres-despachos`.
- Mantener los microservicios independientes entre si: ningun backend debe depender del otro backend en Docker Compose.
- Definir un volumen Docker nombrado y persistente para PostgreSQL.
- Actualizar `.env.example` para usar una unica variable de nombre de base de datos.
- Crear explicitamente en `script.sql` las tablas requeridas por los modelos JPA actuales.
- Insertar datos iniciales coherentes para ventas, productos y despachos.
- Asegurar que todos los contenedores participen en la misma red Docker.

## Fuera de alcance
- Cambiar la logica de negocio de los controladores o servicios Spring Boot.
- Crear nuevos microservicios.
- Agregar nuevas dependencias.
- Modificar el pipeline CI/CD salvo que sea estrictamente necesario para mantener compatibilidad con el nuevo compose.
- Cambiar el frontend.
- Separar schemas por microservicio dentro de PostgreSQL.
- Implementar migraciones con Flyway o Liquibase.

## Reglas tecnicas
- Debe existir un solo contenedor PostgreSQL.
- Debe existir una sola base de datos compartida por ambos microservicios, por ejemplo `devops_db`.
- Debe existir un solo volumen Docker nombrado para PostgreSQL, por ejemplo `postgres_data`.
- El script de inicializacion debe montarse en `/docker-entrypoint-initdb.d/` para que PostgreSQL lo ejecute al crear el volumen por primera vez.
- `script.sql` debe ser compatible con PostgreSQL.
- Las tablas deben respetar los nombres esperados por las entidades JPA actuales:
  - `productos`
  - `venta`
  - `despachos`
- El SQL debe considerar los nombres de columnas generados o definidos por las entidades actuales, incluyendo columnas con comillas cuando corresponda.
- Los datos de `despachos` deben ser coherentes con ventas existentes mediante `idCompra`.
- Como los modelos no declaran una relacion JPA entre `Despacho` y `Venta`, la coherencia debe resolverse en el seed SQL usando IDs existentes y, si se decide agregar FK en SQL, debe ser compatible con el comportamiento actual de las APIs.
- Los backends pueden depender de que PostgreSQL este healthy, pero no deben depender uno del otro.
- No se deben instalar dependencias nuevas sin aprobacion previa.

## Modelo de datos esperado
La base de datos centralizada debe contener, como minimo:

### Tabla `productos`
- `"idProducto"` como clave primaria.
- `"nombreProducto"`.
- `"descripcionProducto"`.
- `"precioProducto"`.
- `"stockProducto"`.

### Tabla `venta`
- `id_venta` como clave primaria esperada por la estrategia de nombres de Hibernate para `idVenta`.
- `direccion_compra`.
- `valor_compra`.
- `fecha_compra`.
- `despacho_generado`.

### Tabla `despachos`
- `"idDespacho"` como clave primaria.
- `"fechaDespacho"`.
- `"patenteCamion"`.
- `intento`.
- `"idCompra"`.
- `"direccionCompra"`.
- `"valorCompra"`.
- `entregado`.

## Datos iniciales
- Deben existir productos suficientes para probar el backend de ventas.
- Deben existir ventas con direcciones, fechas, valores y estado de despacho.
- Deben existir despachos asociados a ventas existentes.
- Los valores duplicados entre ventas y despachos, como direccion y valor, deben coincidir cuando representen la misma compra.
- Deben incluirse casos utiles para pruebas manuales:
  - venta sin despacho generado.
  - venta con despacho generado.
  - despacho pendiente.
  - despacho entregado.
  - despacho con mas de un intento.

## Criterios de aceptacion
- Dado un `docker-compose.yml` limpio, cuando se ejecuta `docker compose config`, entonces la configuracion es valida.
- Dado un entorno sin volumen previo, cuando se ejecuta `docker compose up -d`, entonces se crea un solo contenedor PostgreSQL.
- Dado el compose levantado, cuando se listan los servicios, entonces ambos backends estan conectados a la misma red Docker que PostgreSQL.
- Dado el compose levantado, cuando se inspeccionan las variables de ambos backends, entonces apuntan al mismo `DB_ENDPOINT`, `DB_PORT` y `DB_NAME`.
- Dado el compose levantado, cuando PostgreSQL inicializa por primera vez, entonces ejecuta `script.sql`.
- Dado PostgreSQL inicializado, cuando se consultan las tablas, entonces existen `productos`, `venta` y `despachos`.
- Dado PostgreSQL inicializado, cuando se consultan los datos de seed, entonces existen ventas, productos y despachos coherentes.
- Dado un despacho con `"idCompra"`, cuando se busca la venta correspondiente, entonces existe una fila en `venta` con ese ID.
- Dado el compose actualizado, `backend-despachos` no referencia `postgres-despachos`.
- Dado el compose actualizado, ningun backend declara dependencia Docker sobre el otro backend.
- Dado `.env.example`, cuando se copia a `.env`, entonces contiene las variables necesarias para levantar una sola base de datos.
- El build de ambos backends debe pasar.
- Los tests existentes de ambos backends deben pasar o documentarse si fallan por una causa preexistente.