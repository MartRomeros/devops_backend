# Spec: Mejorar CI/CD para despliegue en EC2 privada

## Objetivo

Actualizar el pipeline de GitHub Actions para que, al hacer push a la rama `main`, construya y publique las imagenes Docker de los backends en Docker Hub y despliegue cada backend en una EC2 privada distinta usando una EC2 publica como bastion.

El despliegue debe dejar corriendo PostgreSQL y `backend-ventas` en una EC2 privada, y PostgreSQL y `backend-despachos` en otra EC2 privada.

## Contexto actual

- El repositorio contiene dos servicios Spring Boot:
  - `backend-ventas`, expuesto por Docker Compose en `8082:8080`.
  - `backend-despachos`, expuesto por Docker Compose en `8081:8081`.
- La base de datos es PostgreSQL `16-alpine`, con volumen persistente `postgres_data`.
- El archivo `docker-compose.yml` usa imagenes `${DOCKERHUB_USERNAME}/backend-ventas:latest` y `${DOCKERHUB_USERNAME}/backend-despachos:latest`.
- El pipeline actual se ejecuta desde la rama `deploy`; esta mejora debe cambiar el disparador a `main`.
- La topologia esperada en AWS es:
  - EC2 publica Ubuntu: bastion con acceso SSH desde GitHub Actions.
  - EC2 privada Ubuntu de ventas: host final donde deben correr Docker, Docker Compose, PostgreSQL y `backend-ventas`.
  - EC2 privada Ubuntu de despachos: host final donde deben correr Docker, Docker Compose, PostgreSQL y `backend-despachos`.
- Los repositorios son publicos, por lo que la EC2 privada puede clonar y actualizar el codigo sin credenciales adicionales de Git.

## Alcance

- Cambiar el trigger del workflow para ejecutarse con push a `main`.
- Construir y pushear a Docker Hub las imagenes:
  - `backend-ventas:latest`
  - `backend-ventas:<commit-sha>`
  - `backend-despachos:latest`
  - `backend-despachos:<commit-sha>`
- Conectarse por SSH desde GitHub Actions a la EC2 publica.
- Desde la EC2 publica, conectarse por SSH a cada EC2 privada.
- En cada EC2 privada, preparar o actualizar el directorio del proyecto.
- Validar si existen `git`, `docker` y Docker Compose; instalarlos si faltan.
- Detectar si los comandos Docker requieren `sudo` y usarlo cuando corresponda.
- Clonar el repositorio si no existe en la EC2 privada.
- Actualizar el codigo si el repositorio ya existe en la EC2 privada.
- Generar el archivo `.env` en la EC2 privada usando GitHub Secrets.
- Levantar PostgreSQL y ejecutar la inicializacion de base de datos cuando corresponda.
- Descargar las imagenes actualizadas desde Docker Hub.
- Recrear los contenedores de backend usando Docker Compose.
- Preservar el volumen de PostgreSQL entre despliegues.
- Documentar todos los secrets requeridos.
- Copiar o disponer la clave `.pem` en la EC2 publica para que esta pueda conectarse a la EC2 privada.

## Fuera de alcance

- Crear infraestructura AWS con Terraform, CloudFormation o CDK.
- Crear o modificar Security Groups desde el pipeline.
- Configurar dominios, TLS, balanceadores de carga o certificados.
- Desplegar frontend.
- Migrar la base de datos a RDS.
- Cambiar puertos actuales de los servicios.
- Cambiar la tecnologia de registry fuera de Docker Hub.

## Flujo esperado del pipeline

1. Un desarrollador hace push a `main`.
2. GitHub Actions ejecuta checkout del repositorio.
3. GitHub Actions autentica contra Docker Hub.
4. GitHub Actions construye y publica las dos imagenes Docker.
5. GitHub Actions se conecta por SSH a la EC2 publica.
6. El workflow deja disponible en la EC2 publica la clave privada necesaria para conectar a la EC2 privada.
7. Desde la EC2 publica se abre una conexion SSH hacia cada EC2 privada.
8. En cada EC2 privada:
   - Se detecta si los comandos administrativos requieren `sudo`.
   - Se valida que existan `git`, `docker` y Docker Compose; si faltan, se instalan usando los paquetes compatibles con Ubuntu.
   - Se valida que Docker quede activo.
   - Se clona el repositorio publico si no existe.
   - Se actualiza el repositorio publico si ya existe.
   - Se escribe `.env` con los valores productivos.
   - Se ejecuta `docker compose up -d postgres` para asegurar que la base este disponible.
   - Se ejecuta `docker compose pull <backend-asignado>`.
   - Se ejecuta `docker compose up -d --no-deps --force-recreate <backend-asignado>`.
   - Se limpian imagenes antiguas sin eliminar volumenes.
9. El pipeline valida que los contenedores queden corriendo.

## Reglas de negocio y operacion

- La rama productiva para este pipeline es `main`.
- Las EC2 privadas son los unicos hosts donde deben correr los contenedores productivos.
- La EC2 publica actua solo como bastion.
- Ambas EC2 son Ubuntu.
- La clave privada para conectarse a la EC2 privada no debe quedar expuesta en logs.
- Si se copia la clave `.pem` a la EC2 publica, debe guardarse con permisos `600`.
- Si se copia la clave `.pem` a la EC2 publica, debe eliminarse al finalizar el deploy, aunque el deploy falle.
- El despliegue no debe ejecutar `docker compose down -v`.
- El despliegue no debe borrar el volumen `postgres_data`.
- El workflow debe publicar imagenes con tag `latest` y con tag `<commit-sha>`.
- El despliegue operativo debe usar `latest`.
- PostgreSQL puede publicar el puerto `5432` dentro del host, pero el Security Group no debe permitir acceso externo directo a ese puerto.
- Los backends deben quedar accesibles en los puertos actuales:
  - Ventas: `8082`.
  - Despachos: `8081`.
- La base de datos debe quedar accesible para los backends por la red interna de Docker usando el hostname `postgres`.
- Los nombres de base de datos usados por `.env` deben existir en PostgreSQL.

## Secrets requeridos

### Docker Hub

| Secret | Descripcion |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub usado para nombrar y publicar imagenes. |
| `DOCKERHUB_TOKEN` | Token de acceso de Docker Hub con permiso para pushear imagenes. |

### EC2 publica

| Secret | Descripcion |
|---|---|
| `PUBLIC_EC2_HOST` | IP publica o DNS de la EC2 publica bastion. |
| `PUBLIC_EC2_USER` | Usuario SSH de la EC2 publica, por ejemplo `ubuntu` o `ec2-user`. |
| `PUBLIC_EC2_SSH_KEY` | Clave privada PEM completa para conectarse desde GitHub Actions a la EC2 publica. |

### EC2 privada

| Secret | Descripcion |
|---|---|
| `PRIVATE_EC2_VENTAS_HOST` | IP privada o DNS privado de la EC2 privada de ventas accesible desde la EC2 publica. |
| `PRIVATE_EC2_DESPACHOS_HOST` | IP privada o DNS privado de la EC2 privada de despachos accesible desde la EC2 publica. |
| `PRIVATE_EC2_USER` | Usuario SSH de ambas EC2 privadas. |
| `PRIVATE_EC2_SSH_KEY` | Clave privada PEM completa que la EC2 publica usara para conectarse a las EC2 privadas. |

### Repositorio

| Secret | Descripcion |
|---|---|
| `REPO_URL` | URL publica del repositorio que debe clonarse en la EC2 privada. |

### Aplicacion y base de datos

| Secret | Descripcion |
|---|---|
| `POSTGRES_USER` | Usuario de PostgreSQL en produccion. |
| `POSTGRES_PASSWORD` | Password de PostgreSQL en produccion. |
| `DB_NAME_VENTAS` | Nombre de la base de datos usada por backend ventas. |
| `DB_NAME_DESPACHOS` | Nombre de la base de datos usada por backend despachos. |

## Supuestos

- La EC2 publica puede conectarse por red privada a la EC2 privada por SSH en el puerto `22`.
- La EC2 privada tiene salida a internet para hacer `git pull` y `docker compose pull`.
- La EC2 privada puede instalar paquetes desde repositorios de Ubuntu si `git`, `docker` o Docker Compose no estan presentes.
- El workflow detectara si Docker requiere `sudo` antes de ejecutar comandos Docker.
- El repositorio publico es accesible desde la EC2 privada con `REPO_URL`.
- El Security Group permite trafico HTTP/API hacia los puertos publicados que correspondan, pero no permite acceso externo directo a PostgreSQL.

## Riesgos tecnicos

- Copiar una clave privada a la EC2 publica aumenta el riesgo operativo si no se protegen permisos y limpieza.
- Si la EC2 privada no tiene salida a internet, no podra descargar imagenes de Docker Hub ni actualizar el repositorio.
- Si la EC2 privada no puede instalar paquetes desde repositorios de Ubuntu, la instalacion automatica de `git`, `docker` o Docker Compose fallara.
- Si el deploy requiere `sudo` y el usuario no tiene permisos sin password, el pipeline quedara bloqueado.
- Si `REPO_URL` no apunta a un repositorio publico accesible desde internet, el clone o pull fallara.
- Si `init-db/01_init.sh` crea bases hardcodeadas distintas a `DB_NAME_VENTAS` y `DB_NAME_DESPACHOS`, el arranque puede fallar cuando se usen nombres personalizados.
- Si PostgreSQL ya tiene un volumen creado, los scripts de `init-db` no se reejecutan automaticamente.
- Como el deploy operativo usa `latest`, el rollback requiere retaguear una imagen previa o ajustar temporalmente el compose para usar `<commit-sha>`.
- Si GitHub Actions no limpia la clave privada en la EC2 publica, quedara material sensible persistente en el bastion.

## Criterios de aceptacion

- Dado un push a `main`, cuando se ejecuta el workflow, entonces se construyen y pushean ambas imagenes a Docker Hub.
- Dado que el build de una imagen falla, cuando corre el pipeline, entonces no se ejecuta el deploy.
- Dado que las imagenes fueron publicadas, cuando inicia el deploy, entonces GitHub Actions se conecta primero a la EC2 publica.
- Dado que la conexion a la EC2 publica funciona, cuando continua el deploy, entonces la EC2 publica se conecta a ambas EC2 privadas.
- Dado que la EC2 privada no tiene `git`, `docker` o Docker Compose, cuando corre el deploy, entonces instala las herramientas faltantes en Ubuntu.
- Dado que Docker requiere `sudo`, cuando corre el deploy, entonces los comandos Docker se ejecutan con `sudo`.
- Dado que Docker no requiere `sudo`, cuando corre el deploy, entonces los comandos Docker se ejecutan sin `sudo`.
- Dado que el proyecto no existe en la EC2 privada, cuando corre el deploy, entonces el repositorio se clona.
- Dado que el proyecto ya existe en la EC2 privada, cuando corre el deploy, entonces el repositorio se actualiza sin borrar volumenes.
- Dado que `.env` se genera desde secrets, cuando se ejecuta Docker Compose, entonces los backends reciben las variables `DB_ENDPOINT`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`.
- Dado un despliegue exitoso, cuando se consulta `docker compose ps` en la EC2 privada de ventas, entonces `postgres` y `backend-ventas` aparecen corriendo.
- Dado un despliegue exitoso, cuando se consulta `docker compose ps` en la EC2 privada de despachos, entonces `postgres` y `backend-despachos` aparecen corriendo.
- Dado un despliegue repetido, cuando termina el pipeline, entonces los datos de PostgreSQL persisten.
- Dado que se publica `5432` en Docker Compose, cuando se revisa el Security Group, entonces no existe acceso externo directo a PostgreSQL.
- El workflow no imprime secretos ni claves privadas en logs.

## Preguntas abiertas

1. Confirmar que los usuarios Ubuntu de ambas EC2 son `ubuntu`, o indicar los valores finales para `PUBLIC_EC2_USER` y `PRIVATE_EC2_USER`.
2. Confirmar si se permite instalar Docker desde repositorios oficiales de Docker o solo desde paquetes disponibles en Ubuntu.
3. Confirmar que el usuario de la EC2 privada tiene permisos `sudo` sin password si la instalacion de dependencias o Docker lo requiere.
4. Confirmar que los nombres de base de datos en secrets seran `ventas_db` y `despachos_db`, o si el script `init-db` tambien debe parametrizarse.

## Cambios sugeridos para design.md

- Definir el mecanismo exacto de salto SSH: comando SSH anidado, `ProxyJump`, o script remoto ejecutado desde el bastion.
- Definir si la clave `PRIVATE_EC2_SSH_KEY` se escribe en disco y se elimina al terminar, o si se usa forwarding/agent.
- Definir el bloque de deteccion `sudo`: probar `docker ps` sin `sudo`; si falla, usar `sudo docker ps`.
- Definir instalacion idempotente de `git`, Docker Engine y Docker Compose para Ubuntu.
- Definir comandos idempotentes para clonar o actualizar el repositorio en la EC2 privada.
- Definir validaciones finales del deploy con `docker compose ps`, logs breves y checks HTTP.
- Definir si se debe parametrizar `init-db/01_init.sh` para respetar `DB_NAME_VENTAS` y `DB_NAME_DESPACHOS`.
