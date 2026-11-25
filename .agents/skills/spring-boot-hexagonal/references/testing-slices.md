# Slices de test (Boot 4.2)

En Boot 4 las anotaciones de test se movieron a modulos `spring-boot-<tecnologia>-test`. Imports verificados
contra 4.2.0:

| Anotacion | Paquete | Artefacto |
|---|---|---|
| `@WebMvcTest`, `@AutoConfigureMockMvc` | `org.springframework.boot.webmvc.test.autoconfigure` | `spring-boot-webmvc-test` |
| `@DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure` | `spring-boot-data-jpa-test` |
| `@AutoConfigureTestDatabase` | `org.springframework.boot.jdbc.test.autoconfigure` | `spring-boot-jdbc-test` |
| `@RestClientTest` | `org.springframework.boot.restclient.test.autoconfigure` | `spring-boot-restclient-test` |
| `@AutoConfigureRestTestClient` | `org.springframework.boot.resttestclient.autoconfigure` | `spring-boot-resttestclient` |
| `@JsonTest` | `org.springframework.boot.test.autoconfigure.json` | `spring-boot-test-autoconfigure` |
| `@MockitoBean` | `org.springframework.test.context.bean.override.mockito` | spring-test |
| `MockMvcTester`, `RestTestClient` | `org.springframework.test.web.servlet.assertj` y `...servlet.client` | spring-test |

Si falta un artefacto, agregarlo en `test` desde `platform-bom`; no fijar versiones a mano.

## Controller: `@WebMvcTest`
- Levanta solo la capa web. Cada use case que el controller inyecta va como `@MockitoBean` o el contexto no arranca.
- Los controllers dependen de los ports de entrada (`CreateXUseCase`...), asi que se mockean esos, no repositorios.
- La seguridad no se aplica sola en el slice: con login encendido usar `@WithMockUser` y `csrf()` en POST/PUT/DELETE.
- Afirmar codigo HTTP, cuerpo y `errorCode` del `ProblemDetail`, no solo que 'no explota'.
- Preferir `MockMvcTester` (AssertJ fluido) en codigo nuevo; `MockMvc` sigue siendo valido.

## Repositorio: base real
- Probar el adapter contra Postgres de Testcontainers con `@ServiceConnection` (ver `testcontainers-and-data.md`).
- Con `@DataJpaTest` agregar `@AutoConfigureTestDatabase(replace = NONE)` para no reemplazar Postgres por H2, e
  `@Import` del adapter y de `TestcontainersConfig`. Si el adapter necesita auditoria o beans del starter de
  persistencia, el slice puede no traerlos: en ese caso usar `@SpringBootTest` con el contexto completo.
- Que probar: consultas propias, restricciones unicas, orden y paginacion, columnas de auditoria. No probar
  los metodos que Spring Data genera solos.

## JSON y clientes HTTP
- `@JsonTest` para el contrato de un DTO (nombres de campos, fechas, nulos). Boot 4 usa Jackson 3
  (`tools.jackson.databind.json.JsonMapper`); las anotaciones siguen en `com.fasterxml.jackson.annotation`.
- `@RestClientTest` + `MockRestServiceServer` es lo primero a probar para un `RestClient`; WireMock solo si
  hace falta un puerto real (reintentos, timeouts, fallas intermitentes).

## Subconjunto a medida
`@SpringBootTest(classes = {...})` mas una anotacion de slice arma un contexto con solo los componentes listados:
rapido como un unitario pero con el framework real. Escribirlo una vez como meta-anotacion y reutilizarlo; cada
`@TestConfiguration` o `@MockitoBean` distinto parte el cache de contexto.

## E2E
`@SpringBootTest(webEnvironment = RANDOM_PORT)` con Postgres real y `RestTestClient` (`@AutoConfigureRestTestClient`)
para pasar por toda la pila, incluida la seguridad. Con login encendido el test se autentica primero con la API
real. Dejar uno o dos: son los mas lentos.
