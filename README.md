# 🚀 DevOps Backend — Sistema de Ventas y Despachos

Proyecto completo de **microservicios Spring Boot + Frontend React**, completamente contenedorizado con Docker y preparado para despliegue automático en AWS EC2 mediante pipeline CI/CD en GitHub Actions.

---

## 📋 Tabla de Contenidos
- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Servicios](#servicios)
- [Inicio Rápido](#inicio-rápido)
- [Frontend React](#frontend-react)
- [Conectarse a PostgreSQL](#conectarse-a-postgresql)
- [Estructura de Datos](#estructura-de-datos)
- [Pipeline CI/CD](#pipeline-cicd)
- [Variables de Entorno](#variables-de-entorno)
- [Configuración EC2](#configuración-ec2)
- [Decisiones Técnicas](#decisiones-técnicas)

---

## 🏗️ Arquitectura

```
┌──────────────────────────────────────────────────────────────────┐
│                   Entorno Local / AWS EC2                        │
│                                                                  │
│  ┌─────────────────┐   ┌──────────────────────────┐             │
│  │ Frontend React  │   │  backend-ventas          │             │
│  │ Vite + Axios    │   │  Spring Boot 3.4.4       │             │
│  │ :5173           │──→│  Java 17 + JPA           │             │
│  └─────────────────┘   │  :8082 (interno :8080)   │             │
│                        └────────┬─────────────────┘             │
│                                 │                                │
│                        ┌────────▼─────────────────┐             │
│                        │  backend-despachos       │             │
│                        │  Spring Boot 3.4.4       │             │
│                        │  Java 17 + JPA           │             │
│                        │  :8081                   │             │
│                        └────────┬─────────────────┘             │
│                                 │                                │
│                        ┌────────▼──────────┐                    │
│                        │  PostgreSQL 16    │  ← volumen         │
│                        │  :5432            │    persistente     │
│                        │  └─ devops_db     │    postgres_data   │
│                        └───────────────────┘                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 💻 Stack Tecnológico

### Backend
- **Spring Boot** 3.4.4 (Spring Framework 6.2.5)
- **Java** 17 (Eclipse Temurin JRE Alpine)
- **Spring Data JPA** + Hibernate 6.6.11
- **PostgreSQL** Driver 42.7.5
- **SpringDoc OpenAPI** 2.7.0 (Swagger UI)
- **Lombok** 1.18.36
- **Maven** Wrapper

### Frontend
- **React** 18.2.0
- **Vite** 5.2.10
- **Axios** 1.6.8
- **React Router DOM** 6.24.1
- **TailwindCSS** 3.x
- **SweetAlert2** 11.11.0

### Base de Datos
- **PostgreSQL** 16-alpine
- **Persistencia:** Volumen Docker `postgres_data`
- **Bases de datos:** `devops_db`

### DevOps
- **Docker** + **Docker Compose** v2
- **GitHub Actions** (CI/CD)
- **Docker Hub** (Registry)
- **AWS EC2** (Producción)

---

## 🎯 Servicios

| Servicio | Puerto Local | Puerto Contenedor | Base de datos | Swagger UI | Descripción |
|---|---|---|---|---|---|
| **backend-ventas** | `8082` | `8080` | `devops_db` | [/swagger-ui.html](http://localhost:8082/swagger-ui.html) | API de órdenes de compra/ventas |
| **backend-despachos** | `8081` | `8081` | `devops_db` | [/swagger-ui.html](http://localhost:8081/swagger-ui.html) | API de despachos y logística |
| **PostgreSQL** | `5432` | `5432` | `devops_db` | — | Base de datos centralizada |
| **Frontend** | `5173` | — | — | — | Interfaz de usuario React |

> ⚠️ **Nota importante:** El puerto de ventas se cambió de `8080` → `8082` debido a conflicto con Oracle TNS Listener en el entorno de desarrollo local.

---

## 🚀 Inicio Rápido

### Requisitos previos
- **Docker Desktop** >= 24.0
- **Docker Compose** v2.x
- **Node.js** >= 18 (para el frontend)
- **Git**

### Pasos para levantar el proyecto completo

```bash
# 1. Clonar el repositorio
git clone https://github.com/MartRomeros/devops_backend.git
cd devops_backend

# 2. Crear el archivo de variables de entorno
cp .env.example .env
# Editar .env con tus valores (la contraseña por defecto es: Gonzalo2026#)

# 3. Levantar PostgreSQL y ambos backends
docker compose up -d

# 4. Verificar que los servicios están corriendo
docker compose ps
# Deberías ver: postgres_db (healthy), backend_ventas (Up), backend_despachos (Up)

# 5. Ver logs de los backends (opcional)
docker compose logs -f backend-ventas backend-despachos

# 6. Probar los endpoints de las APIs
curl http://localhost:8082/api/v1/ventas
curl http://localhost:8081/api/v1/despachos
```

### Levantar el Frontend

```bash
# En una terminal separada
cd front_despacho

# Instalar dependencias (solo la primera vez)
npm install

# Iniciar servidor de desarrollo
npm run dev

# El frontend estará disponible en http://localhost:5173
```

### URLs de acceso

- **Frontend:** http://localhost:5173
- **API Ventas:** http://localhost:8082/api/v1/ventas
- **API Despachos:** http://localhost:8081/api/v1/despachos
- **Swagger Ventas:** http://localhost:8082/swagger-ui.html
- **Swagger Despachos:** http://localhost:8081/swagger-ui.html

### Detener los servicios

```bash
# Detener todos los contenedores
docker compose down

# Para eliminar también el volumen de datos PostgreSQL (¡DESTRUCTIVO!)
docker compose down -v
```

---

## ⚛️ Frontend React

El frontend está construido con **React 18** + **Vite** y se conecta a ambos backends:

### Características
- **Gestión de ventas:** Visualización de órdenes de compra
- **Gestión de despachos:** Control de entregas, cierre de despachos, intentos de entrega
- **UI moderna:** TailwindCSS con diseño responsive
- **Validación de formularios:** React Hook Form
- **Alertas interactivas:** SweetAlert2

### Estructura Frontend
```
front_despacho/
├── src/
│   ├── componentes/
│   │   ├── CrudAdmin/
│   │   │   ├── TableCompras.jsx      ← Conecta a API Ventas (8082)
│   │   │   ├── TableDespachos.jsx    ← Conecta a API Despachos (8081)
│   │   │   ├── FormDespacho.jsx
│   │   │   └── FormCierreDespacho.jsx
│   │   └── Layouts/
│   │       ├── Navbar.jsx
│   │       ├── Footer.jsx
│   │       └── Carrusel.jsx
│   ├── Routes/
│   │   └── AppRoutes.jsx
│   └── main.jsx
├── package.json
└── vite.config.js
```

### Configuración de conexión

El frontend se conecta directamente a las APIs usando Axios:

```javascript
// TableCompras.jsx - Conecta a backend-ventas
axios.get("http://localhost:8082/api/v1/ventas")

// TableDespachos.jsx - Conecta a backend-despachos
axios.get("http://localhost:8081/api/v1/despachos")
```

---

## 🗄️ Conectarse a PostgreSQL

### Opción 1: pgAdmin 4 (Recomendado)

**Pasos para conectar:**

1. **Crear nuevo servidor:**
   - Clic derecho en `Servers` → `Register` → `Server...`

2. **Tab "General":**
   - **Name:** `PostgreSQL Docker` (o cualquier nombre)

3. **Tab "Connection":**
   - **Host name/address:** `localhost` (o `127.0.0.1`)
   - **Port:** `5432`
   - **Maintenance database:** `postgres`
   - **Username:** `postgres`
   - **Password:** `Gonzalo2026#`
   - ✅ Marcar **"Save password?"**

4. **Tab "SSL":**
   - **SSL mode:** `Disable`

5. **Guardar:**
   - Clic en `Save`

**Navegar a las bases de datos:**
```
PostgreSQL Docker
  └── Databases
      ├── postgres (base de datos del sistema)
      └── devops_db
          └── Schemas
              └── public
                  └── Tables
                      ├── productos
                      ├── venta
                      └── despachos
```

### Opción 2: psql (Línea de comandos)

```bash
# Conectarse a devops_db
docker exec -it postgres_db psql -U postgres -d devops_db

# Comandos útiles dentro de psql:
\dt                  # Listar tablas
\d venta            # Describir tabla venta
SELECT * FROM venta; # Ver datos

\q                  # Salir
```

```bash
# Conectarse a devops_db
docker exec -it postgres_db psql -U postgres -d devops_db

\dt
\d despachos
SELECT * FROM despachos;
```

### Opción 3: Clientes GUI alternativos

- **DBeaver Community:** https://dbeaver.io
- **DataGrip (JetBrains):** https://www.jetbrains.com/datagrip
- **Azure Data Studio:** https://azure.microsoft.com/products/data-studio

**Cadena de conexión:**
```
postgresql://postgres:Gonzalo2026%23@localhost:5432/devops_db
```

---

## 📊 Estructura de Datos

### Base de datos: `devops_db`

#### Tabla: `productos`
Catálogo de productos disponible para ventas.

| Campo | Tipo | Descripción |
|---|---|---|
| `idProducto` | BIGINT (PK) | ID único del producto |
| `nombreProducto` | VARCHAR(255) | Nombre del producto |
| `descripcionProducto` | VARCHAR(255) | Descripción del producto |
| `precioProducto` | INTEGER | Precio del producto |
| `stockProducto` | INTEGER | Stock disponible |

**Entidad JPA:**
```java
@Entity
@Table(name = "productos")
public class Producto {
    @Id
    private Long idProducto;
    private String nombreProducto;
    private String descripcionProducto;
    private int precioProducto;
    private int stockProducto;
}
```

#### Tabla: `venta`
Almacena las órdenes de compra/ventas.

| Campo | Tipo | Descripción |
|---|---|---|
| `id_venta` | BIGINT (PK) | ID único de la compra |
| `direccion_compra` | VARCHAR(255) | Dirección de entrega |
| `valor_compra` | INTEGER | Monto de la compra |
| `fecha_compra` | DATE | Fecha de la compra |
| `despacho_generado` | BOOLEAN | Indica si ya existe despacho |

#### Tabla: `despachos`
Almacena los despachos asociados a compras existentes.

| Campo | Tipo | Descripción |
|---|---|---|
| `id_despacho` | BIGINT (PK) | ID único del despacho |
| `id_compra` | BIGINT (FK) | Referencia a la orden de compra |
| `direccion_compra` | VARCHAR(255) | Dirección de entrega |
| `fecha_despacho` | DATE | Fecha del despacho |
| `patente_camion` | VARCHAR(10) | Patente del vehículo |
| `entregado` | BOOLEAN | Estado de entrega (true/false) |
| `intento` | INTEGER | Número de intentos de entrega |

**Entidad JPA:**
```java
@Entity
@Table(name = "despachos")
public class Despacho {
    @Id
    @Column(name = "id_despacho")
    private Long idDespacho;

    @Column(name = "id_compra")
    private Long idCompra;
    @Column(name = "direccion_compra")
    private String direccionCompra;
    @Column(name = "fecha_despacho")
    private LocalDate fechaDespacho;
    @Column(name = "patente_camion")
    private String patenteCamion;
    @Column(name = "valor_compra")
    private Long valorCompra;
    private boolean entregado;
    private int intento;
}
```

### Persistencia de datos

Los datos se almacenan en un **volumen Docker nombrado** (`postgres_data`):
- ✅ **Persisten** entre `docker compose down` y `docker compose up`
- ✅ **Persisten** ante actualizaciones de contenedores
- ❌ **Se eliminan** solo con `docker compose down -v`

```bash
# Ver volúmenes
docker volume ls

# Inspeccionar volumen
docker volume inspect proyecto-semestral_postgres_data
```

---

## 🔐 Variables de Entorno

Copia `.env.example` como `.env` y configura los valores:

```bash
cp .env.example .env
```

### Variables disponibles

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `POSTGRES_USER` | Usuario de PostgreSQL | `postgres` |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL | `Gonzalo2026#` |
| `DB_NAME` | Nombre de la BD centralizada | `devops_db` |
| `DOCKERHUB_USERNAME` | Tu usuario de Docker Hub | `tu_usuario_dockerhub` |

### Ejemplo de `.env`

```ini
POSTGRES_USER=postgres
POSTGRES_PASSWORD=Gonzalo2026#
DB_NAME=devops_db
DOCKERHUB_USERNAME=tu_usuario_dockerhub
```

> ⚠️ **Importante:** El archivo `.env` está en `.gitignore` para proteger credenciales sensibles.

---

## 🔄 Pipeline CI/CD

```
push → rama main
         │
         ▼
   GitHub Actions
         │
    ┌────┴─────────────────────────────┐
    │  Job 1: build-and-push           │
    │  • Buildx multi-platform         │
    │  • Build backend-ventas          │
    │  • Build backend-despachos       │
    │  • Push a Docker Hub con :latest │
    │    y :<commit-sha>               │
    │  • Cache de capas con GHA        │
    └────┬─────────────────────────────┘
         │
    ┌────▼─────────────────────────────┐
    │  Job 2: deploy                   │
    │  • SSH a EC2 pública (bastion)   │
    │  • SSH bastion -> EC2 privada    │
    │  • Instala git/docker si falta   │
    │  • Verifica Docker Compose       │
    │  • PostgreSQL + ambos backends   │
    │  • en una sola EC2 privada       │
    │  • SSH: docker image prune -f    │
    └──────────────────────────────────┘
```

### Secrets requeridos en GitHub

Ve a **Settings → Secrets and variables → Actions** y agrega:

| Secret | Descripción | Ejemplo |
|---|---|---|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub | `miusuario` |
| `DOCKERHUB_TOKEN` | Access token de Docker Hub | `dckr_pat_xxx...` |
| `PUBLIC_EC2_HOST` | IP pública o DNS de la EC2 pública (bastion) | `54.123.45.67` |
| `PUBLIC_EC2_USER` | Usuario SSH de la EC2 pública | `ubuntu` |
| `PUBLIC_EC2_SSH_KEY` | Clave privada PEM para entrar a la EC2 pública | `-----BEGIN RSA...` |
| `PRIVATE_EC2_BACKEND_HOST` | IP privada o DNS privado de la EC2 backend | `10.0.2.15` |
| `PRIVATE_EC2_USER` | Usuario SSH de la EC2 privada backend | `ubuntu` |
| `PRIVATE_EC2_SSH_KEY` | Clave privada PEM para salto EC2 pública -> EC2 privada | `-----BEGIN RSA...` |
| `REPO_URL` | URL pública del repositorio a clonar/actualizar | `https://github.com/usuario/devops_backend.git` |
| `POSTGRES_USER` | Usuario de PostgreSQL en producción | `postgres` |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL en producción | `tu_password_seguro` |
| `DB_NAME` | Nombre BD centralizada en producción | `devops_db` |

### Workflow del pipeline

El pipeline se activa automáticamente al hacer `push` a la rama `main`:

```bash
git checkout main
git push origin main
```

**Pasos del pipeline:**

1. **Build & Push:**
   - Construye imágenes Docker para ambos backends
   - Tagea con `:latest` y `:<commit-sha>`
   - Sube las imágenes a Docker Hub
   - Usa cache de GitHub Actions para acelerar builds

2. **Deploy:**
   - Conecta a EC2 pública (bastion) y desde ahí a la EC2 privada de backend
   - Instala `git`, `docker` y `docker compose` en la EC2 privada solo si faltan
   - Detecta automáticamente si Docker requiere `sudo`
   - Clona/actualiza el repositorio en la EC2 privada
   - Crea archivo `.env` en la EC2 privada con los secrets
   - Levanta `postgres`, `backend-ventas` y `backend-despachos`
   - Limpia imágenes antiguas para liberar espacio

---

## ☁️ Configuración EC2

### Requisitos de la instancia

- **Tipo:** t2.medium o superior (recomendado)
- **AMI:** Ubuntu 22.04 LTS (pública y privada)
- **Storage:** 20 GB mínimo
- **Security Group:** Configurar puertos

### Instalación de Docker en EC2

```bash
# Ubuntu 22.04
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu

# Cerrar sesión y volver a conectar para aplicar cambios
exit
```

### Security Group

Configurar reglas de entrada:

| Puerto | Protocolo | Origen | Descripción |
|---|---|---|---|
| 22 | TCP | Tu IP | SSH |
| 8081 | TCP | 0.0.0.0/0 | API Despachos |
| 8082 | TCP | 0.0.0.0/0 | API Ventas |
| 5173 | TCP | 0.0.0.0/0 | Frontend (opcional) |

> ⚠️ El puerto 5432 de PostgreSQL **NO** debe exponerse externamente. Solo es accesible dentro de la red interna Docker (`backend-net`).

### Verificar despliegue en EC2

```bash
# Conectarse vía SSH
ssh -i tu-clave.pem ubuntu@<EC2_IP>

# Verificar contenedores
docker compose ps

# Ver logs
docker compose logs -f

# Probar endpoints
curl http://localhost:8081/api/v1/despachos
curl http://localhost:8082/api/v1/ventas
```

---

## 📁 Estructura del Repositorio

```
devops_backend/
├── .github/
│   └── workflows/
│       └── deploy.yml                    # Pipeline CI/CD con GitHub Actions
│
├── back-Despachos_SpringBoot/
│   └── Springboot-API-REST-DESPACHO/
│       ├── Dockerfile                    # Multi-stage build para despachos
│       ├── .dockerignore
│       ├── pom.xml                       # Maven + PostgreSQL driver
│       ├── mvnw                          # Maven wrapper
│       ├── mvnw.cmd
│       └── src/
│           ├── main/
│           │   ├── java/com/citt/
│           │   │   ├── SpringbootApiRestDespachoApplication.java
│           │   │   ├── config/
│           │   │   │   ├── CorsConfig.java
│           │   │   │   └── OpenApiConfig.java
│           │   │   ├── controller/
│           │   │   │   └── DespachoController.java
│           │   │   ├── exceptions/
│           │   │   │   └── DespachoNotFoundException.java
│           │   │   └── persistence/
│           │   │       ├── entity/Despacho.java
│           │   │       ├── repository/DespachoRepository.java
│           │   │       └── services/DespachoService*.java
│           │   └── resources/
│           │       └── application.properties    # PostgreSQL config
│           └── test/
│
├── back-Ventas_SpringBoot/
│   └── Springboot-API-REST/
│       ├── Dockerfile                    # Multi-stage build para ventas
│       ├── .dockerignore
│       ├── pom.xml                       # Maven + PostgreSQL driver
│       ├── mvnw
│       ├── mvnw.cmd
│       └── src/
│           ├── main/
│           │   ├── java/com/citt/
│           │   │   ├── SpringbootApiRestApplication.java
│           │   │   ├── config/
│           │   │   ├── controller/
│           │   │   │   └── VentaController.java
│           │   │   ├── exceptions/
│           │   │   └── persistence/
│           │   │       ├── entity/Venta.java
│           │   │       ├── repository/VentaRepository.java
│           │   │       └── services/VentaService*.java
│           │   └── resources/
│           │       └── application.properties    # PostgreSQL config
│           └── test/
│
├── front_despacho/
│   ├── src/
│   │   ├── componentes/
│   │   │   ├── CrudAdmin/
│   │   │   │   ├── TableCompras.jsx      # Axios → localhost:8082
│   │   │   │   ├── TableDespachos.jsx    # Axios → localhost:8081
│   │   │   │   ├── FormDespacho.jsx
│   │   │   │   └── FormCierreDespacho.jsx
│   │   │   └── Layouts/
│   │   │       ├── Navbar.jsx
│   │   │       └── Footer.jsx
│   │   ├── Routes/
│   │   │   └── AppRoutes.jsx
│   │   └── main.jsx
│   ├── package.json
│   ├── vite.config.js
│   └── tailwind.config.js
│
├── docker-compose.yml                    # Orquestación PostgreSQL + backends
├── script.sql                            # Esquema y datos iniciales PostgreSQL
├── .env.example                          # Template de variables de entorno
├── .gitignore                            # Excluye .env, target/, node_modules/
└── README.md                             # Este archivo
```

---

## 🛠️ Decisiones Técnicas

### Arquitectura y Diseño

| Decisión | Justificación |
|---|---|
| **PostgreSQL en lugar de MySQL** | Mayor robustez para entornos productivos, mejor soporte para tipos de datos complejos, cumplimiento ACID más estricto, y compatibilidad nativa con JSON para futura expansión |
| **Multi-stage Dockerfile** | La imagen final solo contiene el JRE y el JAR compilado; reduce el tamaño de imagen **~60%** y elimina herramientas de build de producción |
| **eclipse-temurin:17-jdk-alpine (build)** | Imagen oficial de Adoptium con Alpine Linux; balance entre velocidad de build y seguridad |
| **eclipse-temurin:17-jre-alpine (runtime)** | Solo incluye Java Runtime Environment; imagen final **~200 MB** vs **~450 MB** con JDK completo |
| **Usuario no-root en contenedor** | Principio de mínimo privilegio; mitigación de **OWASP A05 Security Misconfiguration** |
| **Volumen nombrado `postgres_data`** | Desacopla los datos del ciclo de vida del contenedor; los datos **persisten** ante actualizaciones de código |

### Base de Datos

| Decisión | Justificación |
|---|---|
| **PostgreSQL 16-alpine** | Versión LTS estable + imagen Alpine (tamaño reducido ~80 MB) |
| **Base de datos única `devops_db`** | Simplifica despliegue y permite coherencia directa entre ventas y despachos |
| **Script de inicialización `script.sql`** | Define explícitamente tablas y datos seed para un entorno reproducible |
| **`service_healthy` en depends_on** | Garantiza que PostgreSQL esté **completamente listo** (`pg_isready`) antes de que los backends intenten conectarse; evita errores de conexión en frío |
| **Hibernate `ddl-auto=none`** | Evita que Hibernate altere tablas seedadas y deja a `script.sql` como fuente del esquema |

### Build y Deployment

| Decisión | Justificación |
|---|---|
| **Maven Wrapper (`mvnw`)** | No requiere instalación global de Maven; versión exacta controlada por el proyecto |
| **`chmod +x mvnw` en Dockerfile** | Alpine Linux no preserva permisos de ejecución; fix explícito necesario |
| **Exclusión de `.mvn/` en `.dockerignore` removida** | Maven wrapper **requiere** el directorio `.mvn/wrapper/` para funcionar |
| **Cache GHA en Buildx** | Reduce tiempo de build en **~70%** al reutilizar capas de dependencias Maven |
| **Tag `:latest` + `:<commit-sha>`** | `:latest` para deploys rápidos; `:<commit-sha>` para trazabilidad y rollback fácil |
| **`--no-deps --force-recreate`** | Actualiza **solo** los contenedores de backend sin recrear PostgreSQL ni perder datos |

### Desarrollo Local

| Decisión | Justificación |
|---|---|
| **Puerto 8082 para ventas (externo)** | Oracle TNS Listener ocupa el puerto 8080 en el entorno de desarrollo; evita conflicto |
| **Puerto 8080 para ventas (interno)** | Spring Boot mantiene su configuración por defecto; solo se mapea externamente a 8082 |
| **Red Docker `backend-net`** | Aislamiento de red; los backends se comunican con PostgreSQL por nombre de servicio (`postgres`) |

### Frontend

| Decisión | Justificación |
|---|---|
| **Vite en lugar de Create React App** | Build **~10x más rápido**, Hot Module Replacement instantáneo, bundle optimizado |
| **TailwindCSS utility-first** | Desarrollo rápido de UI responsive sin CSS custom; purge automático reduce tamaño final |
| **Axios para HTTP** | API más simple que fetch nativo; interceptores globales para manejo de errores |

### Seguridad

| Decisión | Justificación |
|---|---|
| **`.env` en `.gitignore`** | Evita commit accidental de credenciales; cada entorno mantiene su propia configuración |
| **Secrets de GitHub Actions** | Credenciales encriptadas; nunca aparecen en logs ni en código |
| **Puerto 5432 no expuesto en EC2** | PostgreSQL solo accesible dentro de la red Docker; reduce superficie de ataque |
| **Token de Docker Hub en lugar de contraseña** | Tokens con permisos granulares; pueden revocarse sin cambiar la contraseña de la cuenta |

---

## 🐛 Troubleshooting

### Backend no se conecta a PostgreSQL

```bash
# Ver logs del backend
docker compose logs backend-ventas

# Verificar que PostgreSQL está healthy
docker compose ps

# Probar conexión manual
docker exec -it backend_ventas curl postgres:5432
```

### Frontend no puede llamar a las APIs

**Error:** `ERR_CONNECTION_REFUSED`

**Solución:** Verifica que los backends están corriendo:
```bash
curl http://localhost:8081/api/v1/despachos
curl http://localhost:8082/api/v1/ventas
```

### Puerto 8080 ya está en uso

**Error:** `Bind for 0.0.0.0:8080 failed: port is already allocated`

**Solución:** Cambia el puerto externo en `docker-compose.yml`:
```yaml
backend-ventas:
  ports:
    - "8082:8080"  # Ya implementado en este proyecto
```

### Pipeline CI/CD falla en EC2

**Error:** `Permission denied (publickey)`

**Solución:** Verifica que los secrets `PUBLIC_EC2_SSH_KEY` y `PRIVATE_EC2_SSH_KEY` contienen las claves PEM **completas**, y que `PRIVATE_EC2_BACKEND_HOST` es alcanzable desde la EC2 pública:
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...todo el contenido...
-----END RSA PRIVATE KEY-----
```

### Datos de PostgreSQL se perdieron

**Causa:** Se ejecutó `docker compose down -v` (elimina volúmenes)

**Solución:** Usar solo `docker compose down` para preservar datos.

---

## 📚 Recursos Adicionales

- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **PostgreSQL Docs:** https://www.postgresql.org/docs/16/index.html
- **Docker Compose Docs:** https://docs.docker.com/compose/
- **GitHub Actions:** https://docs.github.com/actions
- **React + Vite:** https://vitejs.dev/guide/

---

## 👥 Equipo de Desarrollo

**Proyecto Semestral — Contenedorización y DevOps**

---

## 📄 Licencia

Este proyecto es de uso académico.

---

**✅ Todo listo para desplegar en AWS EC2 con GitHub Actions.**

Para iniciar el despliegue, haz push a la rama `main`:
```bash
git add .
git commit -m "feat: complete PostgreSQL migration with Docker + CI/CD"
git push origin main
```
