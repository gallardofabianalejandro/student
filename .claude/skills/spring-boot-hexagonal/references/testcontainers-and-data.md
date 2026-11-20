# Testcontainers y datos de test

## Cableado: `@ServiceConnection` primero
`TestcontainersConfig` declara los containers como beans con `@ServiceConnection`; Boot lee host, puerto y
credenciales del container y configura la conexion solo. No registrar propiedades a mano.

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(parse("postgres:16-alpine"));
    }
}
```

Usar paquete `org.testcontainers.postgresql.PostgreSQLContainer` y los artefactos `testcontainers-postgresql` y
`testcontainers-junit-jupiter` (Testcontainers 2.x). Ademas hace falta el driver `org.postgresql:postgresql`:
el modulo de Testcontainers maneja el container, no el driver JDBC.

## Donde va cada valor
| El valor es... | Va en... |
|---|---|
| Conexion a un container que tiene `ConnectionDetails` de Boot (JDBC, Redis, Mongo, Kafka, RabbitMQ) | Nowhere: `@ServiceConnection` |
| Solo se conoce cuando el container arranca y no tiene factory (puerto de un MailHog) | Bean `DynamicPropertyRegistrar` |
| Fijo y conocido al escribir el test (`ddl-auto`, flags, TTL) | `application-test.yml` |

Anti-patrones: poner config estatica en un registrador dinamico; fijar puertos del host (`withFixedExposedPorts`,
`5432` a mano) porque choca en corridas paralelas.

## Sin Docker
Un test con Testcontainers falla de forma dura si no hay Docker. En local puede saltarse con una condicion de JUnit;
**en CI nunca se salta en silencio**: seria falsa confianza, igual que probar con H2. Asegurar Docker en el runner.

## Datos de test
- Preferir fixtures SQL con `@Sql("classpath:/test-data.sql")` (se ejecutan antes de cada metodo por defecto).
- Para limpiar entre tests usar un script `@Sql` de limpieza en lugar de `JdbcTemplate` suelto.
- El pool trabaja con auto-commit apagado: un `delete` sin transaccion nunca se confirma. Si se usa `JdbcTemplate`
  directo, envolverlo en `TransactionTemplate`; con `@Sql` ya lo resuelve Spring.
- Para armar datos por codigo, usar los casos de uso o ports del propio servicio, no el repositorio ni SQL a mano:
  asi el setup respeta las reglas de negocio.
- Fixtures deterministas e independientes del orden de ejecucion.
