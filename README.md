# student

Generado por `microservice-hexagonal-archetype` sobre `platform-parent` +
`platform-bom` (Spring Boot 4.2 / Java 25).

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

La telemetría está apagada por defecto y se puede mandar a **dos destinos**. Con `OBSERVABILITY_ENABLED=true` se encienden a la vez métricas, trazas y logs por OTLP.

| Destino | Cómo se arranca | Qué hace falta |
|---|---|---|
| **Grafana local** (profile `observability` del compose) | `task run_obs` | Docker. Grafana en http://localhost:3000 (admin / admin) |
| **Grafana Cloud** | `task run_cloud` | Variables de entorno de usuario: `GRAFANA_OTLP_ENDPOINT` (termina en `/otlp`), `GRAFANA_OTLP_INSTANCE_ID` y `GRAFANA_OTLP_TOKEN` |

```bash
task obs_up      # solo la pila local, sin la app
task obs_down    # la baja; Postgres sigue
```

- `run_cloud` arma la cabecera `Authorization` con `tools/grafana_auth.py` y activa el perfil `grafana-cloud` (`application-grafana-cloud.yml`). El token nunca se escribe en un archivo.
- Sin las variables, la app no arranca con ese perfil, a propósito.
- Los logs salen por el appender de logback de `logback-spring.xml`, que conecta con OpenTelemetry al arrancar.
- Con la telemetría apagada no hay avisos aunque no haya colector.

## Kubernetes: minikube y GKE

| Qué | Comando | Notas |
|---|---|---|
| minikube con Postgres como pod | `task k8s_local` | Overlay `deployment/local`; telemetría apagada |
| minikube enviando a Grafana Cloud | `task k8s_cloud` | Overlay `deployment/local-cloud`; crea antes el Secret `grafana-cloud` con `task k8s_grafana_secret` (usa las mismas variables que `run_cloud`) |
| GKE | `task k8s_apply CONTEXT=<contexto>` | Aplica `deployment/base`; aborta si el contexto actual de kubectl no es el indicado |
| Revisar los manifiestos | `task k8s_check` | Renderiza `base`, `local` y `local-cloud` y comprueba telemetría, PDB y que el Secret no esté versionado (también corre en el CI) |

- Las tareas contra minikube abortan si el contexto de kubectl no es `minikube`, para no aplicar por error a un cluster real.
- skaffold necesita `minikube` en el PATH: sin él construye la imagen pero no la carga en el cluster y el pod queda en `ErrImagePull`. `task doctor` avisa si falta.
- `base` enciende métricas, trazas y logs con `OBSERVABILITY_ENABLED=true` y apunta a un collector en el cluster (`CHANGE_ME` hasta definirlo). El `PodDisruptionBudget` evita que un drain o un rolling update baje más de un pod.
- El Secret `grafana-cloud` nunca se versiona: lo crea la tarea a partir de las variables de entorno.

## Seguridad de la cadena de suministro y calidad

| Qué | Dónde | Notas |
|---|---|---|
| Actions fijadas por SHA | `.github/workflows/*.yml` | Cada `uses:` lleva el SHA completo y la versión en un comentario; Renovate (`helpers:pinGitHubActionDigests`) los mantiene al día |
| Permisos mínimos | cada workflow | `contents: read` y checkout sin credenciales persistentes (CodeQL suma `security-events: write`); `PACKAGES_TOKEN` solo en los pasos de Maven |
| Mutation testing | `task mutation` y `mutation.yml` (semanal) | PIT sobre `domain/` y `application/`; no corre en `verify` ni bloquea PR |
| Hooks de git | `task hooks_install` | Antes de cada commit revisa los manifiestos de `deployment/` y el formato Java |

- **Por qué se fija por SHA:** en marzo de 2026 se comprometió la action `trivy-action` (76 de 77 tags apuntados a código que roba secretos del CI). Un tag se puede mover; un SHA no. Por eso tampoco se usa Trivy como action de terceros.
- **Revisión de dependencias en PR:** requiere activar *Dependency graph* en Settings > Advanced Security (hoy está desactivado y la API responde 404). Con eso activo se agrega un workflow con `actions/dependency-review-action` fijado por SHA.
- **Escaneo de secretos:** al ser un repo público, GitHub Secret Scanning con push protection cubre el repo sin sumar una herramienta al CI.

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
