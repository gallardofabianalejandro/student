---
name: spring-boot-hexagonal
description: >
  Convenciones de testing, arquitectura hexagonal y persistencia para servicios Spring Boot 4.1 / Java 25
  generados con microservice-hexagonal-archetype. Usar al escribir o revisar tests (unitarios, slices,
  smoke, e2e, Testcontainers), al agregar casos de uso, controllers, adapters JPA o migraciones Flyway,
  y al decidir en que nivel testear algo.
---

# Spring Boot hexagonal

Las reglas duras las hace cumplir `ArchitectureTest` y el build. Este skill explica el criterio detras;
leer solo la referencia del area en la que se trabaja.

| Estoy haciendo... | Leer |
|---|---|
| Decidir que nivel de test escribir, o la suite se volvio lenta | [references/testing-strategy.md](references/testing-strategy.md) |
| Escribir un test de controller, JSON, repositorio o cliente HTTP | [references/testing-slices.md](references/testing-slices.md) |
| Conectar un container o preparar datos de test | [references/testcontainers-and-data.md](references/testcontainers-and-data.md) |
| Agregar una clase y no se donde va, o falla `ArchitectureTest` | [references/architecture-rules.md](references/architecture-rules.md) |
| Entidad JPA, repositorio, migracion Flyway, `@Scheduled`, idempotencia | [references/persistence-and-runtime.md](references/persistence-and-runtime.md) |

## Reglas de bolsillo
- Test rojo primero (TDD) y de a un criterio de aceptacion por vez; no escribir todos los tests y despues implementar.
- Base de datos real con Testcontainers, nunca H2. Un solo container para toda la suite.
- `@SpringBootTest` completo solo para el smoke test y para un par de e2e; el resto en slices.
- Nombre de test: `metodo_whenCondicion_thenResultado`. Clases de test package-private. Sin `Thread.sleep` (Awaitility).
- Cero 'tests ejecutados' no es exito: confirmar que `Tests run` sea mayor que cero.

## Origen
La estrategia de testing se adapto de `sivaprasadreddy/sivalabs-agent-skills` (licencia MIT), que a su vez
cita a Paul Bakker (Netflix) y a Philip Riecks. La parte de arquitectura y persistencia es propia de este
template: el skill original organiza por modulos y aca se organiza por capas hexagonales.
