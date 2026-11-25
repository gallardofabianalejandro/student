# student

Servicio generado con `microservice-hexagonal-archetype` (Spring Boot 4.2 / Java 25). Capacidades: `core,observability,errors,openapi,http-audit,security,jwt,config,persistence,httpclient,testing,jwt-auth,spa,bootui,new-entity-tool` (ver `.platform.json`).

## Comandos
- `task test` formatea y corre todo - `task run` levanta con perfil `local` + `compose.yml`
- `task format` Spotless - `task build_image` / `task push_image` (`IMAGE_NAME` configurable)

## Reglas (las hace cumplir `ArchitectureTest`)
- Hexagonal: `domain` no depende de `application`/`infrastructure`/`api` ni de Spring, Lombok o JPA; `application` solo de `domain`.
- Constructor injection (nunca `@Autowired` en campos), DTOs como records, `@ConfigurationProperties` con record (no `@Value`).
- Sin `System.out.println` (SLF4J), sin excepciones genericas, sin `Thread.sleep` en tests (Awaitility).
- Clientes HTTP salientes: interfaz `@HttpExchange` sobre el `RestClient` del starter `httpclient`; **`RestTemplate` prohibido** (ADR-006 del ecosistema).
- Versiones de starters siempre desde `platform-bom`, nunca a mano.
- TDD: test rojo primero para todo codigo de produccion nuevo.
- Commits: `tipo(scope): descripcion` con tipos feat|fix|refactor|test|docs|chore|perf.

## Criterio detras de las reglas
Skill `.Codex/skills/spring-boot-hexagonal`: estrategia de testing, donde va cada clase y reglas de persistencia.
