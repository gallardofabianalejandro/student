# student

Generado por `microservice-hexagonal-archetype` sobre `platform-parent` +
`platform-bom` (Spring Boot 4.1 / Java 25).

## Estructura

```
src/main/java/dev.fabian.gallardo.student
  domain/{model,port/in,port/out,exception}   sin Lombok, sin deps externas
  application/{usecase,dto}                    records + @Service
  infrastructure/persistence                   adaptador de SampleItemRepository (JPA + PostgreSQL con persistence; en memoria sin ella)
  api/{rest,request,response}                  controller + records de entrada/salida
```

El ejemplo `SampleItem*` es un CRUD completo (domain → application → infrastructure → api):
`POST/GET/PUT/DELETE /api/v1/sample-items` y `GET /api/v1/sample-items?page=0&size=20` (paginado, con metadatos).
Con `persistence` guarda en la tabla `sample_item` (migración `V2__create_sample_item.sql`, con auditoría); el nombre es único (409 si se repite).
Está escrito con Java 25: records con validación en el constructor, `_` en lambdas y Spring Boot 4. **Reemplazarlo por la lógica de negocio real y borrar
el ejemplo** una vez que el primer caso de uso real esté andando.

## Arrancar

```bash
task run          # perfil local + dependencias de compose.yml (spring-boot-docker-compose)
task run_tc       # alternativa: dependencias vía Testcontainers (TestApplication)
task test         # formatea (Spotless) y corre todo, incluido ArchitectureTest
```

Sin Task instalado: `make run`, `make test`, `make format`. `/actuator/health` debe responder `UP`.

## Qué trae el entorno

| Archivo | Para qué |
|---|---|
| `.platform.json` | Manifiesto: capacidades, paquete, imagen. Permite regenerar/auditar drift |
| `Taskfile.yml` / `Makefile` | Comandos comunes (`IMAGE_NAME` configurable en `build_image`/`push_image`) |
| `compose.yml` | Dependencias locales según capacidades |
| `src/test/.../TestcontainersConfig` | Mismas dependencias para tests (`@ServiceConnection`) |
| `ArchitectureTest` | ArchUnit + Taikai: hexagonal, sin `@Autowired` en campos, sin ciclos |
| `deployment/base` | kustomize (ConfigMap, Deployment no-preemptible, Service) |
| `render.yaml` | Blueprint de Render para pruebas (usa `PORT`) |
| `.github/workflows/ci.yml`, `renovate.json`, `.sdkmanrc`, `.mise.toml` | CI, actualización de dependencias, JDK+Maven (`.sdkmanrc` para Linux/Mac con SDKMAN, `.mise.toml` para Windows con mise) |

Cobertura JaCoCo por capa (domain 90 / application 80 / infrastructure 60 / api 70) en modo advertencia
(`haltOnFailure=false`): pasar a `true` cuando el servicio tenga cobertura real.

**Dependencias del ecosistema:** `platform-parent`, el BOM y los starters se leen de GitHub Packages (privado). Definir `GH_PACKAGES_USER` y `GH_PACKAGES_TOKEN` (token clasico con `read:packages`); `.mvn/settings.xml` ya trae el repositorio. Detalle en `docs/PUBLICAR_Y_CONSUMIR.md` del monorepo del ecosistema.

**JDK en Windows (sin WSL):** instalar [mise](https://mise.jdx.dev) (`winget install jdx.mise`) y correr
`mise install` en la raíz del servicio. En IntelliJ 2026.1+ el plugin
[Mise](https://plugins.jetbrains.com/plugin/24904-mise) debería fijar el SDK del proyecto desde la config
de mise. Sin probar todavía: si no detecta `.mise.toml` (con punto), elegir el JDK a mano en
Project Structure → SDK (`%LOCALAPPDATA%\mise\installs\java\`).

**Autenticacion (`jwt-auth`):** si se genero con esa capacidad, `POST /api/auth/login` devuelve el access token en el cuerpo y el refresh token en la cookie `refresh_token` (HttpOnly, SameSite=Strict, Path=/api/auth). `POST /api/auth/refresh` exige esa cookie y el header `X-Requested-With`. `GET /api/auth/me` y el resto de `/api/**` piden `Authorization: Bearer`. `POST /api/auth/register` solo existe con `app.auth.registration-enabled=true` (activo en el perfil `local`). Fuera de `local` definir `JWT_SECRET` (minimo 32 bytes): en Render lo genera el blueprint; en Kubernetes sale del Secret `<servicio>-secrets` (clave `JWT_SECRET`), que no se versiona: `kubectl create secret generic <servicio>-secrets --from-literal=JWT_SECRET="$(openssl rand -base64 48)"`. Con `persistence`, el mismo Secret lleva tambien `DB_USERNAME` y `DB_PASSWORD` (`--from-literal=DB_USERNAME=... --from-literal=DB_PASSWORD=...`): sin ellos el servicio usaria los defaults de desarrollo.

**Front Angular (`spa`):** vive en `frontend/` (Angular 22, Tailwind 4, Vitest). `mvn verify` descarga Node 24.15, hace `npm ci`, corre los tests de Angular, compila y copia el resultado a `static/` dentro del jar; `-Dskip.frontend=true` lo saltea. Para desarrollar el front con recarga en vivo: backend en el puerto 8080 (perfil `local`) y en otra terminal `cd frontend && npm start` (Node 24; `mise install` lo deja listo), que sirve en `http://localhost:4200` y reenvia `/api` al backend. El access token vive solo en memoria y la sesion sobrevive al reload por la cookie de refresh. Imagen: `mvn jib:dockerBuild`, o el `Dockerfile` multi-etapa (ver su cabecera). Si agregas una ruta en `frontend/src/app/app.routes.ts`, agregala tambien en `api/spa/AngularForwardController`. La autenticacion esta aislada en `AuthService` y los dos interceptores.

## Agregar un starter

Ver `catalog.json` en la raíz del monorepo `spring-boot-ecosystem-4x` para la
lista completa de starters disponibles y su prefijo de properties. Agregar la
dependencia al `pom.xml` (sin versión — la resuelve `platform-bom`) y las
properties bajo `app.infra.<starter>.*` en `application.yml`.

### Si el servicio consume mensajería

Este esqueleto es REST-only: no genera ningún listener. Al agregar
`kafka-spring-boot-starter` o `pubsub-spring-boot-starter`, el listener NO se
escribe a mano — se extiende la clase base del starter
(`AbstractIdempotentKafkaListener<T>` / `AbstractIdempotentPubsubListener<T>`),
que ya resuelve idempotencia, deserialización, MDC, ack/nack y el manejo de
error. La subclase implementa solo `handle(...)` y `payloadType()`, y delega en
el caso de uso de `application/usecase` — nunca lleva lógica de negocio, igual
que un controller. Ver el README del starter correspondiente.

## Agregar una entidad (CRUD completo)

```bash
task new_entity -- Alumno nombre:String edad:Integer email:String? fechaIngreso:LocalDate
# o sin Task: java tools/NewEntity.java Alumno nombre:String edad:Integer
```

Genera dominio, casos de uso, REST paginado (`/api/v1/alumnos`), JPA + migración Flyway (con `persistence`) y pruebas.
Tipos: String, Integer, Long, BigDecimal, Boolean, LocalDate, Instant, UUID; `?` al final = opcional; `--path=cursos` cambia el endpoint.
No pisa archivos existentes. Después: `task format` y `task verify`.

## Observabilidad opcional (Grafana, Loki, Tempo, Prometheus)

La pila `lgtm` del `compose.yml` es opcional y no arranca con `task run`. La telemetría de la app también está apagada por defecto.

```bash
task obs_up      # levanta Grafana en http://localhost:3000 (admin / admin)
task run_obs     # obs_up + la app con OBSERVABILITY_ENABLED=true (métricas, trazas y logs por OTLP)
task obs_down    # la baja; Postgres sigue
```

`OBSERVABILITY_ENABLED=true` enciende los tres exportadores a la vez. Con el `lgtm` apagado no hay avisos. Verificado: llegan métricas (unas 500 series) y trazas; el envío de logs por OTLP todavía no está confirmado.

## Pedidos `.http` para probar el servicio

La carpeta `http/` trae pedidos listos para IntelliJ (cliente HTTP integrado; tambien sirve la extension REST Client de VS Code, sin los scripts de guardado de token). Elegir el entorno `local` y apretar el boton verde de cada pedido.

- `http/api.http`: salud, OpenAPI y el CRUD de ejemplo. Guarda el id creado en `{{itemId}}`.
- `http/auth.http` (solo con `jwt-auth`): registro, login, `me`, refresh y logout. El login guarda el token en `{{token}}`.
- `http/http-client.env.json`: direccion y usuario de prueba del entorno `local`.

Orden: arrancar el servicio (perfil `local`), correr registro y login, despues el resto.

## Perfiles, Flyway y login

| Perfil | Flyway | Login |
|---|---|---|
| `local` | carga `db/migration` + `db/seed` (datos de ejemplo, repetible e idempotente) y permite `clean` | se puede apagar |
| `test` | solo `db/migration` (sin datos de ejemplo) | segun la prueba |
| `production` | solo `db/migration`; `clean` prohibido; la validacion sigue activa | **no se puede apagar** |

- Datos de ejemplo: `src/main/resources/db/seed/R__seed_sample_item.sql`. Agregá los tuyos en esa carpeta (solo con perfil local).
- Rehacer la base local: `task db_reset`.
- Apagar el login (solo desarrollo): `AUTH_ENABLED=false` o `app.auth.enabled=false`. La API queda abierta y no existen `/api/auth/*`.
  Con el perfil `production` o `prod` el servicio **no arranca** si el login está apagado.
