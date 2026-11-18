# Build multi-etapa. Resuelve platform-parent, el BOM y los starters desde GitHub Packages (ver docs/PUBLICAR_Y_CONSUMIR.md
# del monorepo del ecosistema). Necesita el usuario y un token con permiso read:packages; el token viaja como secreto de
# BuildKit y no queda en la imagen:
#   docker build --build-arg GH_PACKAGES_USER=<usuario> --secret id=GH_PACKAGES_TOKEN,env=GH_PACKAGES_TOKEN .
# Alternativa que no necesita contenedor de build: `mvn jib:dockerBuild`.
# Imagen no-alpine: frontend-maven-plugin descarga el Node de glibc, que no corre en musl.
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app
ARG GH_PACKAGES_USER
COPY .mvn ./.mvn
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 --mount=type=secret,id=GH_PACKAGES_TOKEN,env=GH_PACKAGES_TOKEN \
    mvn -B dependency:go-offline
COPY src ./src
COPY frontend ./frontend
RUN --mount=type=cache,target=/root/.m2 --mount=type=secret,id=GH_PACKAGES_TOKEN,env=GH_PACKAGES_TOKEN \
    mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
