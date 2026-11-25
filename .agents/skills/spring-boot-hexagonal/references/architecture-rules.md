# Arquitectura hexagonal y reglas de `ArchitectureTest`

## Donde va cada clase
```
<basePackage>
  api/             request, response (records), rest (controllers y *ExceptionHandler), spa
  application/     usecase (implementa los ports de entrada), dto
  domain/          model, exception, port/in (un caso de uso por interfaz), port/out
  infrastructure/  persistence (*Entity, *JpaRepository, Jpa*RepositoryAdapter), config, security
```
- Un controller depende de un port de entrada (`CreateXUseCase`), nunca de un repositorio ni de un adapter.
- Un caso de uso depende de ports de salida (`XRepository`), nunca de JPA ni de Spring Data.
- El adapter de `infrastructure` implementa el port de salida y traduce entre `*Entity` y modelo de dominio.
- Dominio puro: records y clases Java, sin Spring, sin Lombok, sin `jakarta.persistence`.

## Que verifica `ArchitectureTest`
Reglas ArchUnit:
- `domain` no depende de `application`, `infrastructure`, `api`, `org.springframework`, `jakarta.persistence` ni `lombok`.
- `application` no depende de `infrastructure` ni de `api`.
- Las `@Entity` viven solo en `infrastructure.persistence`; los `@RestController` solo en `api`.
- Sin inyeccion por campo, sin `System.out`, sin excepciones genericas.
- Ninguna clase de produccion usa `RestTemplate`, `RestTemplateBuilder` ni `RestTemplateCustomizer`: los clientes HTTP salientes son interfaces `@HttpExchange` sobre `RestClient` (ADR-006).

Reglas Taikai:
- Sin APIs deprecadas, sin ciclos de imports, clases utilitarias `final` con constructor privado.
- Constantes con convencion (`LOG`, no `log`), interfaces sin prefijo `I`.
- Tests package-private (`.*Test(s)?`) y sin `@Disabled`.
- Sin `@Autowired` en campos; la clase `Application` en el paquete base.
- Configuraciones nombradas `*Config` o `*Guard`; controllers nombrados `*Controller` y sin depender de otros controllers.
- Un repositorio no depende de servicios.

## Lo que NO verifica (decidido a proposito)
- Controllers package-private: no se exige porque obligaria a mover cada test de controller a su mismo paquete y
  Spring no lo necesita. Si se quiere minimizar visibilidad, hacerlo junto con la reubicacion de los tests.
- `@Controller` de vistas o de forward a la SPA: son legitimos, por eso no se exige `@RestController`.
- Donde se abre la transaccion: es convencion de `persistence-and-runtime.md`, no una regla de build.

## Si falla `ArchitectureTest`
Leer el mensaje: indica la clase y la regla. Arreglar el codigo, no relajar la regla. Si la regla de verdad no
aplica a un caso, excluir esa clase puntual en el test con un comentario de una linea que diga por que (como
`JwtSecurityConfig`, que hereda `throws Exception` de `HttpSecurity.build()`).

## Errores
Los errores de dominio extienden `BaseDomainException` con un `ErrorCode` y se traducen a `ProblemDetail` (RFC 9457)
por el handler del starter de errores; cada feature suma su `*ExceptionHandler` solo para mapear sus excepciones.
Nunca devolver el mensaje de una excepcion inesperada al cliente.
