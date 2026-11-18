import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Genera un CRUD completo (dominio, casos de uso, REST paginado, JPA + migracion Flyway y pruebas) para una entidad.
 *
 * <pre>
 *   java tools/NewEntity.java Alumno nombre:String edad:Integer email:String? fechaIngreso:LocalDate
 *   java tools/NewEntity.java Curso titulo:String precio:BigDecimal --path=cursos
 * </pre>
 *
 * Tipos: String, Integer, Long, BigDecimal, Boolean, LocalDate, Instant, UUID. Un {@code ?} al final hace opcional el campo.
 * Se ejecuta en la raiz del servicio; lee el paquete base y las capacidades de {@code .platform.json}.
 * Nunca pisa archivos: si alguno ya existe, aborta sin escribir nada.
 */
public class NewEntity {

    private static final Set<String> TYPES = Set.of(
            "String", "Integer", "Long", "BigDecimal", "Boolean", "LocalDate", "Instant", "UUID");
    private static final Set<String> RESERVED = Set.of(
            "id", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate", "class", "int", "long", "new",
            "package", "public", "private", "static", "default", "return", "switch", "this", "void", "enum", "record");

    public static void main(String[] args) throws IOException {
        try {
            run(args);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws IOException {
        if (args.length < 2 || args[0].startsWith("-")) {
            throw new IllegalArgumentException("""
                    uso: java tools/NewEntity.java <Entidad> <campo:Tipo[?]>... [--path=segmento]
                    ejemplo: java tools/NewEntity.java Alumno nombre:String edad:Integer email:String? fechaIngreso:LocalDate
                    tipos: %s""".formatted(String.join(", ", new TreeSet<>(TYPES))));
        }
        var spec = Spec.parse(args);
        var project = Project.read(Path.of("."));
        var files = Generator.generate(spec, project);

        var existing = files.keySet().stream().filter(Files::exists).toList();
        if (!existing.isEmpty()) {
            throw new IllegalStateException("ya existen estos archivos, no se pisa nada:\n  "
                    + existing.stream().map(Path::toString).collect(Collectors.joining("\n  ")));
        }
        for (var entry : files.entrySet()) {
            Files.createDirectories(entry.getKey().getParent());
            Files.writeString(entry.getKey(), entry.getValue(), StandardCharsets.UTF_8);
        }
        System.out.println("Entidad " + spec.entity() + ": " + files.size() + " archivos generados:");
        files.keySet().forEach(p -> System.out.println("  " + p));
        System.out.println("""

                Siguiente: task format && task verify   (o: mvn spotless:apply verify)
                Endpoint:  /api/v1/%s""".formatted(spec.path()));
    }

    // ---------------------------------------------------------------- entrada

    record Field(String name, String type, boolean optional) {

        static Field parse(String raw) {
            var m = Pattern.compile("([a-z][A-Za-z0-9]*):([A-Za-z]+)(\\?)?").matcher(raw);
            if (!m.matches()) {
                throw new IllegalArgumentException("campo invalido '" + raw + "': usar nombre:Tipo o nombre:Tipo?");
            }
            var name = m.group(1);
            var type = m.group(2);
            if (RESERVED.contains(name)) {
                throw new IllegalArgumentException("el nombre de campo '" + name + "' esta reservado");
            }
            if (!TYPES.contains(type)) {
                throw new IllegalArgumentException("tipo no soportado '" + type + "'. Tipos: " + new TreeSet<>(TYPES));
            }
            return new Field(name, type, m.group(3) != null);
        }

        String column() {
            return snake(name);
        }

        String sqlType() {
            return switch (type) {
                case "String" -> "VARCHAR(255)";
                case "Integer" -> "INTEGER";
                case "Long" -> "BIGINT";
                case "BigDecimal" -> "NUMERIC(12, 2)";
                case "Boolean" -> "BOOLEAN";
                case "LocalDate" -> "DATE";
                case "Instant" -> "TIMESTAMP WITH TIME ZONE";
                default -> "UUID";
            };
        }

        String importName() {
            return switch (type) {
                case "BigDecimal" -> "java.math.BigDecimal";
                case "LocalDate" -> "java.time.LocalDate";
                case "Instant" -> "java.time.Instant";
                case "UUID" -> "java.util.UUID";
                default -> null;
            };
        }

        /** Expresion Java de un valor de ejemplo; {@code alt} da uno distinto (para probar la actualizacion). */
        String sample(boolean alt) {
            return switch (type) {
                case "String" -> alt ? "\"otro-" + name + "\"" : "\"valor-" + name + "\"";
                case "Integer" -> alt ? "7" : "3";
                case "Long" -> alt ? "70L" : "30L";
                case "BigDecimal" -> alt ? "new BigDecimal(\"99.50\")" : "new BigDecimal(\"10.25\")";
                case "Boolean" -> alt ? "false" : "true";
                case "LocalDate" -> alt ? "LocalDate.of(2027, 2, 20)" : "LocalDate.of(2026, 1, 15)";
                case "Instant" -> alt ? "Instant.parse(\"2027-02-20T11:00:00Z\")" : "Instant.parse(\"2026-01-15T10:00:00Z\")";
                default -> alt ? "UUID.fromString(\"00000000-0000-0000-0000-0000000000b2\")"
                        : "UUID.fromString(\"00000000-0000-0000-0000-0000000000a1\")";
            };
        }

        String json(boolean alt) {
            return switch (type) {
                case "String" -> alt ? "\"otro-" + name + "\"" : "\"valor-" + name + "\"";
                case "Integer" -> alt ? "7" : "3";
                case "Long" -> alt ? "70" : "30";
                case "BigDecimal" -> alt ? "99.5" : "10.25";
                case "Boolean" -> alt ? "false" : "true";
                case "LocalDate" -> alt ? "\"2027-02-20\"" : "\"2026-01-15\"";
                case "Instant" -> alt ? "\"2027-02-20T11:00:00Z\"" : "\"2026-01-15T10:00:00Z\"";
                default -> alt ? "\"00000000-0000-0000-0000-0000000000b2\"" : "\"00000000-0000-0000-0000-0000000000a1\"";
            };
        }
    }

    record Spec(String entity, List<Field> fields, String path) {

        static Spec parse(String[] args) {
            var entity = args[0];
            if (!entity.matches("[A-Z][A-Za-z0-9]*")) {
                throw new IllegalArgumentException("la entidad debe empezar con mayuscula y ser alfanumerica: '" + entity + "'");
            }
            var fields = new ArrayList<Field>();
            String path = null;
            for (int i = 1; i < args.length; i++) {
                if (args[i].startsWith("--path=")) {
                    path = args[i].substring("--path=".length()).replaceAll("^/+|/+$", "");
                } else {
                    fields.add(Field.parse(args[i]));
                }
            }
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("hace falta al menos un campo");
            }
            var duplicated = fields.stream().collect(Collectors.groupingBy(Field::name, Collectors.counting())).entrySet().stream()
                    .filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
            if (!duplicated.isEmpty()) {
                throw new IllegalArgumentException("campos repetidos: " + duplicated);
            }
            if (path == null || path.isBlank()) {
                path = plural(kebab(entity));
            }
            if (!path.matches("[a-z0-9-]+")) {
                throw new IllegalArgumentException("--path solo admite minusculas, digitos y guiones");
            }
            return new Spec(entity, List.copyOf(fields), path);
        }

        /** Campo por el que se ordenan los listados: el primero obligatorio, o el primero. */
        Field sortField() {
            return fields.stream().filter(f -> !f.optional()).findFirst().orElse(fields.get(0));
        }
    }

    record Project(String basePackage, boolean persistence, boolean secured, boolean oidc, Path root) {

        static Project read(Path root) throws IOException {
            var file = root.resolve(".platform.json");
            if (!Files.exists(file)) {
                throw new IllegalStateException("no encuentro .platform.json: ejecutar en la raiz de un servicio generado");
            }
            var json = Files.readString(file, StandardCharsets.UTF_8);
            var pkg = Pattern.compile("\"basePackage\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
            var caps = Pattern.compile("\"capabilities\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
            if (!pkg.find() || !caps.find()) {
                throw new IllegalStateException(".platform.json no tiene basePackage y capabilities");
            }
            var list = List.of(caps.group(1).split(","));
            var secured = list.contains("web-ui") || list.contains("jwt-auth") || list.contains("oidc-auth");
            return new Project(pkg.group(1), list.contains("persistence"), secured, list.contains("oidc-auth"), root);
        }

        Path java(String sub, String file) {
            return root.resolve("src/main/java").resolve(basePackage.replace('.', '/')).resolve(sub).resolve(file);
        }

        Path test(String sub, String file) {
            return root.resolve("src/test/java").resolve(basePackage.replace('.', '/')).resolve(sub).resolve(file);
        }

        int nextMigrationVersion() throws IOException {
            var dir = root.resolve("src/main/resources/db/migration");
            if (!Files.isDirectory(dir)) {
                return 1;
            }
            try (var stream = Files.list(dir)) {
                return stream.map(p -> p.getFileName().toString())
                        .map(n -> Pattern.compile("V(\\d+)__.*\\.sql").matcher(n))
                        .filter(java.util.regex.Matcher::matches)
                        .mapToInt(m -> Integer.parseInt(m.group(1)))
                        .max().orElse(0) + 1;
            }
        }
    }

    // ---------------------------------------------------------------- nombres

    static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    static String kebab(String camel) {
        return snake(camel).replace('_', '-');
    }

    static String lowerFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    static String upperFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String plural(String word) {
        if (word.matches(".*[^aeiou]y")) {
            return word.substring(0, word.length() - 1) + "ies";
        }
        if (word.matches(".*(s|x|z|ch|sh)")) {
            return word + "es";
        }
        return word + "s";
    }

    // ---------------------------------------------------------------- generacion

    static final class Generator {

        static Map<Path, String> generate(Spec spec, Project project) throws IOException {
            var out = new LinkedHashMap<Path, String>();
            var g = new Generator(spec, project);
            g.domain(out);
            g.application(out);
            g.api(out);
            g.infrastructure(out);
            g.tests(out);
            return out;
        }

        private static final String TEXT_HELPERS = """

    private static String requiredText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        var text = value.strip();
        if (text.length() > max) {
            throw new IllegalArgumentException(field + " no puede superar " + max + " caracteres");
        }
        return text;
    }

    private static String optionalText(String value, String field, int max) {
        return value == null || value.isBlank() ? null : requiredText(value, field, max);
    }
""";

        private final Spec spec;
        private final Project project;
        private final String e;
        private final String v;

        private Generator(Spec spec, Project project) {
            this.spec = spec;
            this.project = project;
            this.e = spec.entity();
            this.v = lowerFirst(spec.entity());
        }

        private String render(String template) {
            return template
                    .replace("@@PKG@@", project.basePackage())
                    .replace("@@E@@", e)
                    .replace("@@v@@", v)
                    .replace("@@PATH@@", spec.path())
                    .replace("@@TABLE@@", snake(e));
        }

        private static String imports(Set<String> names) {
            return names.stream().map(n -> "import " + n + ";").collect(Collectors.joining("\n"));
        }

        private Set<String> typeImports() {
            var set = new TreeSet<String>();
            spec.fields().stream().map(Field::importName).filter(n -> n != null).forEach(set::add);
            return set;
        }

        private String components() {
            return spec.fields().stream().map(f -> f.type() + " " + f.name()).collect(Collectors.joining(", "));
        }

        private String componentsWithId() {
            return "UUID id, " + components();
        }

        private String names(String prefix) {
            return spec.fields().stream().map(f -> prefix + f.name() + (prefix.isEmpty() ? "" : "()"))
                    .collect(Collectors.joining(", "));
        }

        private String samples(boolean alt) {
            return spec.fields().stream().map(f -> f.sample(alt)).collect(Collectors.joining(", "));
        }

        private String json(boolean alt) {
            return spec.fields().stream().map(f -> "\"" + f.name() + "\":" + f.json(alt)).collect(Collectors.joining(","));
        }

        // ------------------------------------------------------------ domain

        private void domain(Map<Path, String> out) {
            var imports = new TreeSet<>(typeImports());
            imports.add("java.util.Objects");
            imports.add("java.util.UUID");
            var validations = new StringBuilder();
            var hasText = false;
            var hasBigDecimal = false;
            for (var f : spec.fields()) {
                switch (f.type()) {
                    case "String" -> {
                        hasText = true;
                        validations.append("        %s = %s(%s, \"%s\", 255);\n".formatted(
                                f.name(), f.optional() ? "optionalText" : "requiredText", f.name(), f.name()));
                    }
                    case "BigDecimal" -> {
                        hasBigDecimal = true;
                        validations.append(f.optional()
                                ? "        if (%1$s != null) {\n            %1$s = %1$s.setScale(2, RoundingMode.HALF_UP);\n        }\n".formatted(f.name())
                                : "        Objects.requireNonNull(%1$s, \"%1$s es obligatorio\");\n        %1$s = %1$s.setScale(2, RoundingMode.HALF_UP);\n".formatted(f.name()));
                    }
                    default -> {
                        if (!f.optional()) {
                            validations.append("        Objects.requireNonNull(%1$s, \"%1$s es obligatorio\");\n".formatted(f.name()));
                        }
                    }
                }
            }
            if (hasBigDecimal) {
                imports.add("java.math.RoundingMode");
            }
            var helpers = hasText ? TEXT_HELPERS : "";

            out.put(project.java("domain/model", e + ".java"), render("""
                    package @@PKG@@.domain.model;

                    %s

                    /** Generado por tools/NewEntity.java. Record inmutable: las invariantes se validan en el constructor. */
                    public record @@E@@(%s) {

                        public @@E@@ {
                            Objects.requireNonNull(id, "id es obligatorio");
                    %s    }

                        public static @@E@@ create(@@E@@Data data) {
                            return new @@E@@(UUID.randomUUID(), %s);
                        }

                        /** Copia con los datos nuevos y el mismo id. */
                        public @@E@@ with(@@E@@Data data) {
                            return new @@E@@(id, %s);
                        }
                    %s}
                    """).formatted(imports(imports), componentsWithId(), validations, names("data."), names("data."), helpers));

            var dataImports = new TreeSet<>(typeImports());
            out.put(project.java("domain/model", e + "Data.java"), render("""
                    package @@PKG@@.domain.model;
                    %s
                    /** Datos editables de @@E@@ (lo que llega al crear o actualizar); la validacion la hace @@E@@. */
                    public record @@E@@Data(%s) {
                    }
                    """).formatted(dataImports.isEmpty() ? "\n" : "\n" + imports(dataImports) + "\n", components()));

            out.put(project.java("domain/exception", e + "NotFoundException.java"), render("""
                    package @@PKG@@.domain.exception;

                    import java.util.UUID;

                    public class @@E@@NotFoundException extends RuntimeException {

                        public @@E@@NotFoundException(UUID id) {
                            super("@@E@@ no encontrado: " + id);
                        }
                    }
                    """));

            port("in", "Create" + e + "UseCase", """
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.@@E@@Data;

                    public interface Create@@E@@UseCase {

                        @@E@@ create(@@E@@Data data);
                    }
                    """, out);
            port("in", "Get" + e + "UseCase", """
                    import @@PKG@@.domain.model.@@E@@;
                    import java.util.UUID;

                    public interface Get@@E@@UseCase {

                        @@E@@ getById(UUID id);
                    }
                    """, out);
            port("in", "List" + e + "UseCase", """
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.Paged;

                    public interface List@@E@@UseCase {

                        Paged<@@E@@> list(int page, int size);
                    }
                    """, out);
            port("in", "Update" + e + "UseCase", """
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.@@E@@Data;
                    import java.util.UUID;

                    public interface Update@@E@@UseCase {

                        @@E@@ update(UUID id, @@E@@Data data);
                    }
                    """, out);
            port("in", "Delete" + e + "UseCase", """
                    import java.util.UUID;

                    public interface Delete@@E@@UseCase {

                        void delete(UUID id);
                    }
                    """, out);
            port("out", e + "Repository", """
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.Paged;
                    import java.util.Optional;
                    import java.util.UUID;

                    /** Puerto de salida -- la implementacion va en infrastructure/persistence/. */
                    public interface @@E@@Repository {

                        @@E@@ save(@@E@@ @@v@@);

                        Optional<@@E@@> findById(UUID id);

                        /** Pagina ordenada por @@SORT@@; {@code page} arranca en 0. */
                        Paged<@@E@@> findAll(int page, int size);

                        /** @return true si existia y se borro. */
                        boolean deleteById(UUID id);
                    }
                    """.replace("@@SORT@@", spec.sortField().name()), out);
        }

        private void port(String dir, String name, String body, Map<Path, String> out) {
            out.put(project.java("domain/port/" + dir, name + ".java"),
                    render("package @@PKG@@.domain.port." + dir + ";\n\n" + body));
        }

        // ------------------------------------------------------------ application

        private void application(Map<Path, String> out) {
            out.put(project.java("application/usecase", e + "UseCaseImpl.java"), render("""
                    package @@PKG@@.application.usecase;

                    import @@PKG@@.domain.exception.@@E@@NotFoundException;
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.@@E@@Data;
                    import @@PKG@@.domain.model.Paged;
                    import @@PKG@@.domain.port.in.Create@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Delete@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Get@@E@@UseCase;
                    import @@PKG@@.domain.port.in.List@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Update@@E@@UseCase;
                    import @@PKG@@.domain.port.out.@@E@@Repository;
                    import java.util.UUID;
                    import org.springframework.stereotype.Service;

                    @Service
                    public class @@E@@UseCaseImpl implements Create@@E@@UseCase, Get@@E@@UseCase, List@@E@@UseCase,
                            Update@@E@@UseCase, Delete@@E@@UseCase {

                        private final @@E@@Repository repository;

                        public @@E@@UseCaseImpl(@@E@@Repository repository) {
                            this.repository = repository;
                        }

                        @Override
                        public @@E@@ create(@@E@@Data data) {
                            return repository.save(@@E@@.create(data));
                        }

                        @Override
                        public @@E@@ getById(UUID id) {
                            return repository.findById(id).orElseThrow(() -> new @@E@@NotFoundException(id));
                        }

                        @Override
                        public Paged<@@E@@> list(int page, int size) {
                            return repository.findAll(page, size);
                        }

                        @Override
                        public @@E@@ update(UUID id, @@E@@Data data) {
                            return repository.save(getById(id).with(data));
                        }

                        @Override
                        public void delete(UUID id) {
                            if (!repository.deleteById(id)) {
                                throw new @@E@@NotFoundException(id);
                            }
                        }
                    }
                    """));

            var imports = new TreeSet<>(typeImports());
            imports.add("java.util.UUID");
            out.put(project.java("application/dto", e + "Dto.java"), render("""
                    package @@PKG@@.application.dto;

                    import @@PKG@@.domain.model.@@E@@;
                    %s

                    public record @@E@@Dto(%s) {

                        public static @@E@@Dto from(@@E@@ @@v@@) {
                            return new @@E@@Dto(@@v@@.id(), %s);
                        }
                    }
                    """).formatted(imports(imports), componentsWithId(), names(v + ".")));
        }

        // ------------------------------------------------------------ api

        private String requestComponents() {
            return spec.fields().stream().map(f -> {
                var ann = new ArrayList<String>();
                switch (f.type()) {
                    case "String" -> {
                        if (!f.optional()) {
                            ann.add("@NotBlank");
                        }
                        ann.add("@Size(max = 255)");
                    }
                    case "BigDecimal" -> {
                        if (!f.optional()) {
                            ann.add("@NotNull");
                        }
                        ann.add("@Digits(integer = 10, fraction = 2)");
                    }
                    default -> {
                        if (!f.optional()) {
                            ann.add("@NotNull");
                        }
                    }
                }
                return String.join(" ", ann) + " " + f.type() + " " + f.name();
            }).collect(Collectors.joining(",\n        "));
        }

        private void api(Map<Path, String> out) {
            var requestImports = new TreeSet<>(typeImports());
            requestImports.add("@@PKG@@.domain.model.@@E@@Data");
            var needs = spec.fields();
            if (needs.stream().anyMatch(f -> f.type().equals("String") && !f.optional())) {
                requestImports.add("jakarta.validation.constraints.NotBlank");
            }
            if (needs.stream().anyMatch(f -> !f.type().equals("String") && !f.optional())) {
                requestImports.add("jakarta.validation.constraints.NotNull");
            }
            if (needs.stream().anyMatch(f -> f.type().equals("String"))) {
                requestImports.add("jakarta.validation.constraints.Size");
            }
            if (needs.stream().anyMatch(f -> f.type().equals("BigDecimal"))) {
                requestImports.add("jakarta.validation.constraints.Digits");
            }
            out.put(project.java("api/request", e + "Request.java"), render("""
                    package @@PKG@@.api.request;

                    %s

                    /** Cuerpo de POST y PUT. Las anotaciones validan el formato; las reglas del negocio viven en el dominio. */
                    public record @@E@@Request(
                            %s) {

                        public @@E@@Data toData() {
                            return new @@E@@Data(%s);
                        }
                    }
                    """).formatted(imports(requestImports.stream().map(this::render).collect(Collectors.toCollection(TreeSet::new))),
                    requestComponents(), names("")));

            var responseImports = new TreeSet<>(typeImports());
            responseImports.add("java.util.UUID");
            out.put(project.java("api/response", e + "Response.java"), render("""
                    package @@PKG@@.api.response;

                    import @@PKG@@.application.dto.@@E@@Dto;
                    %s

                    public record @@E@@Response(%s) {

                        public static @@E@@Response from(@@E@@Dto dto) {
                            return new @@E@@Response(dto.id(), %s);
                        }
                    }
                    """).formatted(imports(responseImports), componentsWithId(), names("dto.")));

            out.put(project.java("api/rest", e + "Controller.java"), render("""
                    package @@PKG@@.api.rest;

                    import com.nacionservicios.emv.core.paging.PageResponse;
                    import @@PKG@@.api.request.@@E@@Request;
                    import @@PKG@@.api.response.@@E@@Response;
                    import @@PKG@@.application.dto.@@E@@Dto;
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.port.in.Create@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Delete@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Get@@E@@UseCase;
                    import @@PKG@@.domain.port.in.List@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Update@@E@@UseCase;
                    import jakarta.validation.Valid;
                    import jakarta.validation.constraints.Max;
                    import jakarta.validation.constraints.Min;
                    import java.util.UUID;
                    import org.springframework.http.ResponseEntity;
                    import org.springframework.web.bind.annotation.DeleteMapping;
                    import org.springframework.web.bind.annotation.GetMapping;
                    import org.springframework.web.bind.annotation.PathVariable;
                    import org.springframework.web.bind.annotation.PostMapping;
                    import org.springframework.web.bind.annotation.PutMapping;
                    import org.springframework.web.bind.annotation.RequestBody;
                    import org.springframework.web.bind.annotation.RequestMapping;
                    import org.springframework.web.bind.annotation.RequestParam;
                    import org.springframework.web.bind.annotation.RestController;
                    import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

                    /** Generado por tools/NewEntity.java. Solo mapeo request/response: la logica va en los use cases. */
                    @RestController
                    @RequestMapping("/api/v1/@@PATH@@")
                    public class @@E@@Controller {

                        private final Create@@E@@UseCase createUseCase;
                        private final Get@@E@@UseCase getUseCase;
                        private final List@@E@@UseCase listUseCase;
                        private final Update@@E@@UseCase updateUseCase;
                        private final Delete@@E@@UseCase deleteUseCase;

                        public @@E@@Controller(Create@@E@@UseCase createUseCase, Get@@E@@UseCase getUseCase,
                                List@@E@@UseCase listUseCase, Update@@E@@UseCase updateUseCase, Delete@@E@@UseCase deleteUseCase) {
                            this.createUseCase = createUseCase;
                            this.getUseCase = getUseCase;
                            this.listUseCase = listUseCase;
                            this.updateUseCase = updateUseCase;
                            this.deleteUseCase = deleteUseCase;
                        }

                        @PostMapping
                        public ResponseEntity<@@E@@Response> create(@Valid @RequestBody @@E@@Request request) {
                            var created = toResponse(createUseCase.create(request.toData()));
                            var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
                            return ResponseEntity.created(location).body(created);
                        }

                        @GetMapping
                        public PageResponse<@@E@@Response> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
                            var result = listUseCase.list(page, size);
                            return PageResponse.of(result.content().stream().map(@@E@@Controller::toResponse).toList(), result.page(),
                                    result.size(), result.totalElements());
                        }

                        @GetMapping("/{id}")
                        public @@E@@Response getById(@PathVariable UUID id) {
                            return toResponse(getUseCase.getById(id));
                        }

                        @PutMapping("/{id}")
                        public @@E@@Response update(@PathVariable UUID id, @Valid @RequestBody @@E@@Request request) {
                            return toResponse(updateUseCase.update(id, request.toData()));
                        }

                        @DeleteMapping("/{id}")
                        public ResponseEntity<Void> delete(@PathVariable UUID id) {
                            deleteUseCase.delete(id);
                            return ResponseEntity.noContent().build();
                        }

                        private static @@E@@Response toResponse(@@E@@ @@v@@) {
                            return @@E@@Response.from(@@E@@Dto.from(@@v@@));
                        }
                    }
                    """));

            out.put(project.java("api/rest", e + "ExceptionHandler.java"), render("""
                    package @@PKG@@.api.rest;

                    import @@PKG@@.domain.exception.@@E@@NotFoundException;
                    import org.springframework.core.Ordered;
                    import org.springframework.core.annotation.Order;
                    import org.springframework.http.HttpStatus;
                    import org.springframework.http.ProblemDetail;
                    import org.springframework.web.bind.annotation.ExceptionHandler;
                    import org.springframework.web.bind.annotation.RestControllerAdvice;

                    /** Traduce las excepciones de dominio de @@E@@ a respuestas HTTP (RFC 9457). */
                    @RestControllerAdvice(assignableTypes = @@E@@Controller.class)
                    @Order(Ordered.HIGHEST_PRECEDENCE)
                    public class @@E@@ExceptionHandler {

                        @ExceptionHandler(@@E@@NotFoundException.class)
                        ProblemDetail notFound(@@E@@NotFoundException ex) {
                            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
                            problem.setProperty("errorCode", "@@UPPER@@_NOT_FOUND");
                            return problem;
                        }
                    }
                    """.replace("@@UPPER@@", snake(e).toUpperCase())));
        }

        // ------------------------------------------------------------ infrastructure

        private void infrastructure(Map<Path, String> out) throws IOException {
            if (!project.persistence()) {
                var sortGetter = spec.sortField().name();
                out.put(project.java("infrastructure/persistence", "InMemory" + e + "Repository.java"), render("""
                        package @@PKG@@.infrastructure.persistence;

                        import @@PKG@@.domain.model.@@E@@;
                        import @@PKG@@.domain.model.Paged;
                        import @@PKG@@.domain.port.out.@@E@@Repository;
                        import java.util.Comparator;
                        import java.util.Map;
                        import java.util.Optional;
                        import java.util.UUID;
                        import java.util.concurrent.ConcurrentHashMap;
                        import org.springframework.stereotype.Repository;

                        /** Adaptador sin base de datos: NO persiste entre reinicios. Con la capacidad persistence se usa JPA. */
                        @Repository
                        public class InMemory@@E@@Repository implements @@E@@Repository {

                            private final Map<UUID, @@E@@> store = new ConcurrentHashMap<>();

                            @Override
                            public @@E@@ save(@@E@@ @@v@@) {
                                store.put(@@v@@.id(), @@v@@);
                                return @@v@@;
                            }

                            @Override
                            public Optional<@@E@@> findById(UUID id) {
                                return Optional.ofNullable(store.get(id));
                            }

                            @Override
                            public Paged<@@E@@> findAll(int page, int size) {
                                var sorted = store.values().stream()
                                        .sorted(Comparator.comparing(@@E@@::@@SORT@@, Comparator.nullsLast(Comparator.naturalOrder())))
                                        .toList();
                                var content = sorted.stream().skip((long) page * size).limit(size).toList();
                                return new Paged<>(content, page, size, sorted.size());
                            }

                            @Override
                            public boolean deleteById(UUID id) {
                                return store.remove(id) != null;
                            }
                        }
                        """.replace("@@SORT@@", sortGetter)));
                return;
            }

            var entityImports = new TreeSet<>(typeImports());
            entityImports.add("java.util.UUID");
            var fieldsCode = new StringBuilder();
            var apply = new StringBuilder();
            for (var f : spec.fields()) {
                var column = switch (f.type()) {
                    case "String" -> "@Column(name = \"%s\", nullable = %s, length = 255)".formatted(f.column(), f.optional());
                    case "BigDecimal" -> "@Column(name = \"%s\", nullable = %s, precision = 12, scale = 2)".formatted(f.column(), f.optional());
                    default -> "@Column(name = \"%s\", nullable = %s)".formatted(f.column(), f.optional());
                };
                fieldsCode.append("    ").append(column)
                        .append("\n    private ").append(f.type()).append(' ').append(f.name()).append(";\n\n");
                apply.append("        this.%1$s = %2$s.%1$s();\n".formatted(f.name(), v));
            }
            var entityBody = fieldsCode.toString();

            out.put(project.java("infrastructure/persistence", e + "Entity.java"), render("""
                    package @@PKG@@.infrastructure.persistence;

                    import @@PKG@@.domain.model.@@E@@;
                    import com.nacionservicios.emv.persistence.audit.BaseAuditableEntity;
                    import jakarta.persistence.Column;
                    import jakarta.persistence.Entity;
                    import jakarta.persistence.Id;
                    import jakarta.persistence.Table;
                    %s

                    /** Entidad JPA (clase, porque JPA necesita constructor sin argumentos); el modelo de dominio es un record. */
                    @Entity
                    @Table(name = "@@TABLE@@")
                    public class @@E@@Entity extends BaseAuditableEntity {

                        @Id
                        private UUID id;

                    %s    protected @@E@@Entity() {
                        }

                        static @@E@@Entity from(@@E@@ @@v@@) {
                            var entity = new @@E@@Entity();
                            entity.id = @@v@@.id();
                            entity.apply(@@v@@);
                            return entity;
                        }

                        void apply(@@E@@ @@v@@) {
                    %s    }

                        @@E@@ toDomain() {
                            return new @@E@@(id, %s);
                        }
                    }
                    """).formatted(imports(entityImports), entityBody, apply,
                    spec.fields().stream().map(Field::name).collect(Collectors.joining(", "))));

            out.put(project.java("infrastructure/persistence", e + "JpaRepository.java"), render("""
                    package @@PKG@@.infrastructure.persistence;

                    import java.util.UUID;
                    import org.springframework.data.jpa.repository.JpaRepository;

                    public interface @@E@@JpaRepository extends JpaRepository<@@E@@Entity, UUID> {
                    }
                    """));

            out.put(project.java("infrastructure/persistence", "Jpa" + e + "RepositoryAdapter.java"), render("""
                    package @@PKG@@.infrastructure.persistence;

                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.Paged;
                    import @@PKG@@.domain.port.out.@@E@@Repository;
                    import java.util.Optional;
                    import java.util.UUID;
                    import org.springframework.data.domain.PageRequest;
                    import org.springframework.data.domain.Sort;
                    import org.springframework.stereotype.Repository;
                    import org.springframework.transaction.annotation.Transactional;

                    /** Adaptador JPA del puerto @@E@@Repository (tabla @@TABLE@@, migracion Flyway V@@VERSION@@). */
                    @Repository
                    public class Jpa@@E@@RepositoryAdapter implements @@E@@Repository {

                        private final @@E@@JpaRepository jpa;

                        public Jpa@@E@@RepositoryAdapter(@@E@@JpaRepository jpa) {
                            this.jpa = jpa;
                        }

                        @Override
                        @Transactional
                        public @@E@@ save(@@E@@ @@v@@) {
                            var entity = jpa.findById(@@v@@.id()).map(existing -> {
                                existing.apply(@@v@@);
                                return existing;
                            }).orElseGet(() -> @@E@@Entity.from(@@v@@));
                            return jpa.saveAndFlush(entity).toDomain();
                        }

                        @Override
                        @Transactional(readOnly = true)
                        public Optional<@@E@@> findById(UUID id) {
                            return jpa.findById(id).map(@@E@@Entity::toDomain);
                        }

                        @Override
                        @Transactional(readOnly = true)
                        public Paged<@@E@@> findAll(int page, int size) {
                            var result = jpa.findAll(PageRequest.of(page, size, Sort.by("@@SORT@@")));
                            return new Paged<>(result.getContent().stream().map(@@E@@Entity::toDomain).toList(), page, size,
                                    result.getTotalElements());
                        }

                        @Override
                        @Transactional
                        public boolean deleteById(UUID id) {
                            if (!jpa.existsById(id)) {
                                return false;
                            }
                            jpa.deleteById(id);
                            return true;
                        }
                    }
                    """.replace("@@SORT@@", spec.sortField().name()).replace("@@VERSION@@", String.valueOf(project.nextMigrationVersion()))));

            var columns = new StringBuilder();
            for (var f : spec.fields()) {
                columns.append("    %-18s %s%s,\n".formatted(f.column(), f.sqlType(), f.optional() ? "" : " NOT NULL"));
            }
            var version = project.nextMigrationVersion();
            out.put(project.root().resolve("src/main/resources/db/migration").resolve("V" + version + "__create_" + snake(e) + ".sql"),
                    "CREATE TABLE IF NOT EXISTS " + snake(e) + " (\n"
                            + "    id                 UUID PRIMARY KEY,\n"
                            + columns
                            + "    created_by         VARCHAR(255) NOT NULL,\n"
                            + "    created_date       TIMESTAMP WITH TIME ZONE NOT NULL,\n"
                            + "    last_modified_by   VARCHAR(255),\n"
                            + "    last_modified_date TIMESTAMP WITH TIME ZONE\n);\n");
        }

        // ------------------------------------------------------------ tests

        private void tests(Map<Path, String> out) {
            var imports = new TreeSet<>(typeImports());
            imports.add("java.util.UUID");
            var requiredText = spec.fields().stream().filter(f -> f.type().equals("String") && !f.optional()).findFirst();
            var firstRequired = spec.fields().stream().filter(f -> !f.optional()).findFirst();

            var domainTests = new StringBuilder();
            if (firstRequired.isPresent()) {
                var f = firstRequired.get();
                var args = spec.fields().stream().map(x -> x.name().equals(f.name()) ? "null" : x.sample(false)).collect(Collectors.joining(", "));
                domainTests.append(render("""

                            @Test
                            void constructor_whenRequiredFieldIsMissing_thenThrows() {
                                assertThatThrownBy(() -> new @@E@@(ID, %s)).isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
                            }
                        """).formatted(args));
            }
            if (requiredText.isPresent()) {
                var f = requiredText.get();
                var args = spec.fields().stream().map(x -> x.name().equals(f.name()) ? "\"x\".repeat(256)" : x.sample(false)).collect(Collectors.joining(", "));
                domainTests.append(render("""

                            @Test
                            void constructor_whenTextTooLong_thenThrows() {
                                assertThatThrownBy(() -> new @@E@@(ID, %s)).isInstanceOf(IllegalArgumentException.class);
                            }
                        """).formatted(args));
            }
            out.put(project.test("domain", e + "Test.java"), render("""
                    package @@PKG@@.domain;

                    import static org.assertj.core.api.Assertions.assertThat;
                    import static org.assertj.core.api.Assertions.assertThatThrownBy;

                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.@@E@@Data;
                    %s
                    import org.junit.jupiter.api.Test;

                    /** Unit puro, sin Spring. */
                    class @@E@@Test {

                        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

                        @Test
                        void create_whenData_thenGeneratesNewIdAndKeepsValues() {
                            var data = new @@E@@Data(%s);

                            var first = @@E@@.create(data);
                            var second = @@E@@.create(data);

                            assertThat(first.id()).isNotEqualTo(second.id());
                            assertThat(first.%s()).isEqualTo(second.%s());
                        }

                        @Test
                        void with_whenNewData_thenKeepsIdAndReplacesTheRest() {
                            var original = new @@E@@(ID, %s);

                            var changed = original.with(new @@E@@Data(%s));

                            assertThat(changed.id()).isEqualTo(ID);
                            assertThat(changed.%s()).isEqualTo(new @@E@@(ID, %s).%s());
                        }
                    %s}
                    """).formatted(imports(imports), samples(false), spec.fields().get(0).name(), spec.fields().get(0).name(),
                    samples(false), samples(true), spec.fields().get(0).name(), samples(true), spec.fields().get(0).name(),
                    domainTests));

            out.put(project.test("domain", e + "UseCaseImplTest.java"), render("""
                    package @@PKG@@.domain;

                    import static org.assertj.core.api.Assertions.assertThat;
                    import static org.assertj.core.api.Assertions.assertThatThrownBy;
                    import static org.mockito.ArgumentMatchers.any;
                    import static org.mockito.Mockito.mock;
                    import static org.mockito.Mockito.verify;
                    import static org.mockito.Mockito.when;

                    import @@PKG@@.application.usecase.@@E@@UseCaseImpl;
                    import @@PKG@@.domain.exception.@@E@@NotFoundException;
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.@@E@@Data;
                    import @@PKG@@.domain.model.Paged;
                    import @@PKG@@.domain.port.out.@@E@@Repository;
                    %s
                    import java.util.List;
                    import java.util.Optional;
                    import org.junit.jupiter.api.Test;

                    class @@E@@UseCaseImplTest {

                        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

                        private final @@E@@Repository repository = mock(@@E@@Repository.class);
                        private final @@E@@UseCaseImpl useCase = new @@E@@UseCaseImpl(repository);

                        private static @@E@@ existing() {
                            return new @@E@@(ID, %s);
                        }

                        @Test
                        void create_whenValidData_thenSavesAndReturns() {
                            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                            var created = useCase.create(new @@E@@Data(%s));

                            assertThat(created.id()).isNotNull();
                            verify(repository).save(any(@@E@@.class));
                        }

                        @Test
                        void getById_whenExists_thenReturnsIt() {
                            when(repository.findById(ID)).thenReturn(Optional.of(existing()));

                            assertThat(useCase.getById(ID)).isEqualTo(existing());
                        }

                        @Test
                        void getById_whenNotFound_thenThrows() {
                            when(repository.findById(ID)).thenReturn(Optional.empty());

                            assertThatThrownBy(() -> useCase.getById(ID)).isInstanceOf(@@E@@NotFoundException.class);
                        }

                        @Test
                        void list_whenCalled_thenDelegatesPaging() {
                            var page = new Paged<>(List.of(existing()), 1, 5, 6);
                            when(repository.findAll(1, 5)).thenReturn(page);

                            assertThat(useCase.list(1, 5)).isEqualTo(page);
                        }

                        @Test
                        void update_whenExists_thenSavesChangesKeepingId() {
                            when(repository.findById(ID)).thenReturn(Optional.of(existing()));
                            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                            var updated = useCase.update(ID, new @@E@@Data(%s));

                            assertThat(updated.id()).isEqualTo(ID);
                        }

                        @Test
                        void update_whenNotFound_thenThrows() {
                            when(repository.findById(ID)).thenReturn(Optional.empty());

                            assertThatThrownBy(() -> useCase.update(ID, new @@E@@Data(%s))).isInstanceOf(@@E@@NotFoundException.class);
                        }

                        @Test
                        void delete_whenExists_thenDeletes() {
                            when(repository.deleteById(ID)).thenReturn(true);

                            useCase.delete(ID);

                            verify(repository).deleteById(ID);
                        }

                        @Test
                        void delete_whenNotFound_thenThrows() {
                            when(repository.deleteById(ID)).thenReturn(false);

                            assertThatThrownBy(() -> useCase.delete(ID)).isInstanceOf(@@E@@NotFoundException.class);
                        }
                    }
                    """).formatted(imports(imports), samples(false), samples(false), samples(true), samples(true)));

            // ---- controller (slice)
            var secured = project.secured();
            var csrfImport = secured ? "import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;\nimport org.springframework.security.test.context.support.WithMockUser;\n" : "";
            var csrf = secured ? ".with(csrf())" : "";
            var mockUser = secured ? "@WithMockUser\n" : "";
            var webMvc = project.oidc()
                    ? "@WebMvcTest(controllers = @@E@@Controller.class, properties = \"app.oidc.issuer-uri=http://keycloak.test/realms/ecosystem\")"
                    : "@WebMvcTest(@@E@@Controller.class)";
            out.put(project.test("api", e + "ControllerTest.java"), render("""
                    package @@PKG@@.api;

                    import static org.mockito.ArgumentMatchers.any;
                    import static org.mockito.ArgumentMatchers.eq;
                    import static org.mockito.Mockito.doThrow;
                    import static org.mockito.Mockito.verify;
                    import static org.mockito.Mockito.when;
                    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
                    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
                    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
                    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
                    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
                    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
                    %s
                    import @@PKG@@.api.rest.@@E@@Controller;
                    import @@PKG@@.domain.exception.@@E@@NotFoundException;
                    import @@PKG@@.domain.model.@@E@@;
                    import @@PKG@@.domain.model.@@E@@Data;
                    import @@PKG@@.domain.model.Paged;
                    import @@PKG@@.domain.port.in.Create@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Delete@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Get@@E@@UseCase;
                    import @@PKG@@.domain.port.in.List@@E@@UseCase;
                    import @@PKG@@.domain.port.in.Update@@E@@UseCase;
                    %s
                    import java.util.List;
                    import org.junit.jupiter.api.Test;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
                    import org.springframework.http.MediaType;
                    import org.springframework.test.context.bean.override.mockito.MockitoBean;
                    import org.springframework.test.web.servlet.MockMvc;
                    import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

                    %s
                    %sclass @@E@@ControllerTest {

                        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
                        private static final String BODY = \"\"\"
                                {%s}\"\"\";

                        @Autowired
                        private MockMvc mockMvc;

                        @MockitoBean
                        private Create@@E@@UseCase createUseCase;

                        @MockitoBean
                        private Get@@E@@UseCase getUseCase;

                        @MockitoBean
                        private List@@E@@UseCase listUseCase;

                        @MockitoBean
                        private Update@@E@@UseCase updateUseCase;

                        @MockitoBean
                        private Delete@@E@@UseCase deleteUseCase;

                        private static @@E@@ item() {
                            return new @@E@@(ID, %s);
                        }

                        private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder) {
                            return builder%s.contentType(MediaType.APPLICATION_JSON).content(BODY);
                        }

                        @Test
                        void getById_whenExists_thenReturns200() throws Exception {
                            when(getUseCase.getById(ID)).thenReturn(item());

                            mockMvc.perform(get("/api/v1/@@PATH@@/" + ID))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.id").value(ID.toString()));
                        }

                        @Test
                        void getById_whenNotFound_thenReturns404() throws Exception {
                            when(getUseCase.getById(ID)).thenThrow(new @@E@@NotFoundException(ID));

                            mockMvc.perform(get("/api/v1/@@PATH@@/" + ID)).andExpect(status().isNotFound());
                        }

                        @Test
                        void list_whenDefaults_thenReturnsPageWithMetadata() throws Exception {
                            when(listUseCase.list(0, 20)).thenReturn(new Paged<>(List.of(item(), item()), 0, 20, 2));

                            mockMvc.perform(get("/api/v1/@@PATH@@"))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.content.length()").value(2))
                                    .andExpect(jsonPath("$.totalElements").value(2))
                                    .andExpect(jsonPath("$.last").value(true));
                        }

                        @Test
                        void list_whenSizeAboveLimit_thenReturns400() throws Exception {
                            mockMvc.perform(get("/api/v1/@@PATH@@").param("size", "101")).andExpect(status().isBadRequest());
                        }

                        @Test
                        void create_whenValidBody_thenReturns201WithLocation() throws Exception {
                            when(createUseCase.create(any(@@E@@Data.class))).thenReturn(item());

                            mockMvc.perform(json(post("/api/v1/@@PATH@@")))
                                    .andExpect(status().isCreated())
                                    .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/@@PATH@@/" + ID)));
                        }

                        @Test
                        void create_whenBodyIsEmpty_thenReturns4xxAndDoesNotCallUseCase() throws Exception {
                            mockMvc.perform(post("/api/v1/@@PATH@@")%s.contentType(MediaType.APPLICATION_JSON).content("{}"))
                                    .andExpect(status().is4xxClientError());

                            org.mockito.Mockito.verifyNoInteractions(createUseCase);
                        }

                        @Test
                        void update_whenExists_thenReturns200() throws Exception {
                            when(updateUseCase.update(eq(ID), any(@@E@@Data.class))).thenReturn(item());

                            mockMvc.perform(json(put("/api/v1/@@PATH@@/" + ID))).andExpect(status().isOk());
                        }

                        @Test
                        void update_whenNotFound_thenReturns404() throws Exception {
                            when(updateUseCase.update(eq(ID), any(@@E@@Data.class))).thenThrow(new @@E@@NotFoundException(ID));

                            mockMvc.perform(json(put("/api/v1/@@PATH@@/" + ID))).andExpect(status().isNotFound());
                        }

                        @Test
                        void delete_whenExists_thenReturns204() throws Exception {
                            mockMvc.perform(delete("/api/v1/@@PATH@@/" + ID)%s).andExpect(status().isNoContent());

                            verify(deleteUseCase).delete(ID);
                        }

                        @Test
                        void delete_whenNotFound_thenReturns404() throws Exception {
                            doThrow(new @@E@@NotFoundException(ID)).when(deleteUseCase).delete(ID);

                            mockMvc.perform(delete("/api/v1/@@PATH@@/" + ID)%s).andExpect(status().isNotFound());
                        }
                    }
                    """).formatted(csrfImport, imports(imports), render(webMvc), mockUser, json(false), samples(false), csrf, csrf, csrf, csrf));

            // ---- integracion con la base (solo con persistence)
            if (project.persistence()) {
                var firstField = spec.fields().get(0);
                out.put(project.test("", e + "RepositoryIntegrationTest.java"), render("""
                        package @@PKG@@;

                        import static org.assertj.core.api.Assertions.assertThat;

                        import @@PKG@@.domain.model.@@E@@;
                        import @@PKG@@.domain.port.out.@@E@@Repository;
                        %s
                        import org.junit.jupiter.api.BeforeEach;
                        import org.junit.jupiter.api.Test;
                        import org.springframework.beans.factory.annotation.Autowired;
                        import org.springframework.boot.test.context.SpringBootTest;
                        import org.springframework.context.annotation.Import;
                        import org.springframework.jdbc.core.JdbcTemplate;
                        import org.springframework.test.context.ActiveProfiles;
                        import org.springframework.transaction.support.TransactionTemplate;

                        /** El CRUD contra PostgreSQL real (Testcontainers): tabla, migracion, paginacion y auditoria. */
                        @SpringBootTest
                        @Import(TestcontainersConfig.class)
                        @ActiveProfiles("test")
                        class @@E@@RepositoryIntegrationTest {

                            private final @@E@@Repository repository;
                            private final JdbcTemplate jdbc;
                            private final TransactionTemplate transaction;

                            @Autowired
                            @@E@@RepositoryIntegrationTest(@@E@@Repository repository, JdbcTemplate jdbc, TransactionTemplate transaction) {
                                this.repository = repository;
                                this.jdbc = jdbc;
                                this.transaction = transaction;
                            }

                            @BeforeEach
                            void cleanTable() {
                                // El pool trabaja con auto-commit apagado: sin transaccion el delete nunca se confirma.
                                transaction.executeWithoutResult(_ -> jdbc.update("delete from @@TABLE@@"));
                            }

                            private static @@E@@ item(%s) {
                                return new @@E@@(UUID.randomUUID(), %s);
                            }

                            @Test
                            void save_whenNewItem_thenPersistedWithAuditColumns() {
                                var saved = repository.save(item());

                                assertThat(repository.findById(saved.id())).contains(saved);
                                var row = jdbc.queryForMap("select created_by, created_date from @@TABLE@@ where id = ?", saved.id());
                                assertThat(row.get("created_by")).isNotNull();
                                assertThat(row.get("created_date")).isNotNull();
                            }

                            @Test
                            void save_whenExistingId_thenUpdatesTheSameRow() {
                                var saved = repository.save(item());

                                repository.save(new @@E@@(saved.id(), %s));

                                assertThat(jdbc.queryForObject("select count(*) from @@TABLE@@", Long.class)).isEqualTo(1L);
                                assertThat(repository.findById(saved.id())).get().extracting(@@E@@::%s).isEqualTo(%s);
                            }

                            @Test
                            void findAll_whenSeveralItems_thenPaged() {
                                repository.save(item());
                                repository.save(item());
                                repository.save(item());

                                var first = repository.findAll(0, 2);
                                var second = repository.findAll(1, 2);

                                assertThat(first.content()).hasSize(2);
                                assertThat(second.content()).hasSize(1);
                                assertThat(first.totalElements()).isEqualTo(3);
                            }

                            @Test
                            void deleteById_whenExists_thenRemovesRowAndReportsIt() {
                                var saved = repository.save(item());

                                assertThat(repository.deleteById(saved.id())).isTrue();
                                assertThat(repository.deleteById(saved.id())).isFalse();
                                assertThat(jdbc.queryForObject("select count(*) from @@TABLE@@", Long.class)).isZero();
                            }
                        }
                        """).formatted(imports(imports), "", samples(false), samples(true), firstField.name(), firstField.sample(true)));
            }
        }
    }
}
