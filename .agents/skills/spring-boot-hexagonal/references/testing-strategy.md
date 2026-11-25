# Estrategia de testing

Los niveles se complementan, no se reemplazan: cada uno atrapa fallas que los otros no ven.

| Nivel | Que prueba | Cuando |
|---|---|---|
| Unitario (sin Spring) | Logica de `domain` y de casos de uso, con los ports mockeados o con fakes | Siempre que haya logica |
| Slice | El framework real alrededor de pocos componentes: mapeo HTTP, JSON, SQL, seguridad | La mayoria de los tests que tocan Spring |
| Smoke | Que el contexto completo arranca | Uno por servicio (`ApplicationContextTest`) |
| E2E | Un request por toda la pila contra Postgres real | Uno o dos, flujos criticos (`AuthFlowIntegrationTest`) |

Un test unitario puede dar 100% de cobertura y aun asi no darse cuenta de que falta un `@RestController`, que el
mapeo a la tabla no coincide o que la config de seguridad bloquea la ruta. Por eso hace falta algo que arranque Spring.

## Costo de contexto
Levantar el contexto completo tarda segundos y crece con la app. Spring reutiliza un contexto entre clases de test
solo si la configuracion es identica. Lo que **parte el cache** y fuerza otro arranque:
- `@MockitoBean` o `@MockitoSpyBean` distintos entre clases.
- Propiedades sueltas (`@TestPropertySource`, `properties = ...`) o `@ActiveProfiles` distintos.
- `@DirtiesContext`: descarta el contexto; arreglar la causa del aislamiento en lugar de usarlo.

Que hacer:
- Mocks comunes en una clase base abstracta, no uno por clase.
- Config compartida en `application-test.yml` con `@ActiveProfiles("test")`.
- Un container por suite (bean `@ServiceConnection` compartido en `TestcontainersConfig`), no uno por clase.

## Mockear en los bordes
Mockear lo externo, lento o pago (HTTP de terceros, reloj, azar) y dejar real todo lo demas. Si hace falta mockear
tres capas para probar una, el test esta en el nivel equivocado. Al afirmar el estado de un port, preferir un fake
en memoria antes que `verify()`; reservar `verify()` para probar un efecto lateral (se envio el mensaje, se cobro).

## Anti-patrones
- Test tautologico: el valor esperado se recalcula con la misma logica que el codigo. Usar un literal independiente.
- Acoplado a la implementacion: falla al refactorizar aunque el comportamiento no cambie.
- Cortar por capas horizontales: escribir todos los tests y despues todo el codigo. Un test, una implementacion, repetir.
- H2 o base en memoria: da falsa confianza sobre SQL, tipos y restricciones de Postgres.
- Test que pasa 'ejecutando cero tests' o que se salta en silencio en CI.
