# Persistencia y comportamiento en runtime

## JPA
- Las entidades `*Entity` viven en `infrastructure.persistence`; el dominio no las conoce.
- El ID se asigna en el dominio (`UUID.randomUUID()` al crear), no con `GenerationType.IDENTITY`: IDENTITY desactiva
  el batching de inserts y el `batch_size` configurado quedaria inerte. Con ID asignado conviene `@Version` o
  `Persistable`: sin eso Spring Data trata `save()` como update y hace un SELECT extra antes de insertar.
- Nombres de tabla y columna explicitos; enums con `@Enumerated(STRING)`.
- Auditoria (`created_by`, `created_date`, `last_modified_*`) la da `BaseAuditableEntity` del starter de persistencia.
- `@Transactional` va en los metodos del adapter (con `readOnly = true` en lecturas), no en el dominio.
- `open-in-view: false` y `ddl-auto: validate`; el esquema lo maneja solo Flyway.
- Auto-commit del pool apagado: una escritura fuera de una transaccion no se confirma nunca.
- Consultas propias con `@Query` o metodos con nombre significativo; proyecciones para lecturas; paginar siempre.

## Flyway
- `V<n>__descripcion.sql` versionadas e inmutables una vez aplicadas; nunca editar una migracion ya desplegada.
- Los seeds repetibles (`R__*.sql`) solo bajo el perfil `local`.
- `CREATE INDEX CONCURRENTLY` lo rechaza Postgres dentro de una transaccion: va solo en su archivo y con la
  migracion configurada sin transaccion (`executeInTransaction=false`). Una restriccion nueva sobre una tabla grande:
  `NOT VALID` y despues `VALIDATE` en otra migracion.
- Renombrar o borrar una columna se hace en dos releases (agregar y migrar; borrar despues, cuando nadie la lee).
- No usar `baseline-on-migrate: true` para tapar un esquema desconocido; hacer el baseline una vez, a mano.

## Procesos que reintentan o corren en varias replicas
- Toda replica dispara un `@Scheduled`. Decidir: corre en todas, en una sola (lock distribuido) o reclamando items
  persistidos. El cron de Spring tiene seis campos (segundos primero); fijar la zona horaria y no confiar en que
  se recuperen disparos perdidos tras una caida.
- Inyectar un `Clock`; asi los tests no dependen de la hora real.
- Idempotencia: la clave sale determinista de los inputs (`IdempotencyKeyGenerator.of(...)`, nunca UUID al azar) e
  incluye tenant y operacion si los hay. Reclamarla con una sola sentencia atomica (INSERT o UPDATE condicional
  verificando una fila afectada); leer y despues escribir deja pasar a dos replicas.
- No capturar la violacion de unicidad y seguir dentro de una transaccion ya abortada.
- Estado que importa se persiste antes de avanzar de paso; nunca solo en memoria.
- Un efecto despues del commit no es durable: si no puede perderse, usar outbox.
- Reintentar fuera de la transaccion, no adentro; un reintento que repite una escritura necesita idempotencia.

## Config y logs
- `@ConfigurationProperties` con record, nunca `@Value`.
- Logs con SLF4J (`private static final Logger LOG`); jamas secretos, tokens ni PAN completos; con MDC para trazar.
