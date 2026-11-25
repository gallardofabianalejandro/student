---
name: apply-renovate-prs
description: >
  Aplica en el working tree local los cambios de todos los PRs abiertos de Renovate de un repo de GitHub, para
  compilar y probar juntas todas las actualizaciones de dependencias. Solo toca archivos locales: nunca commitea,
  pushea, mergea ni cierra nada. Usar cuando se pide aplicar, consolidar o probar los PRs de Renovate.
disable-model-invocation: true
---

# Aplicar PRs de Renovate en local

Junta los PRs abiertos de Renovate y los aplica al working tree, para que las actualizaciones se construyan y
prueben en una sola corrida. El usuario decide después qué commitear.

## Restricciones duras
- **Nunca** correr `git commit`, `git push`, `git merge`, `git cherry-pick`, `git rebase`, `gh pr merge`,
  `gh pr close` ni `gh pr review`. No crear ramas ni tags salvo que el usuario lo pida.
- Solo comandos `gh` de lectura: `gh pr list`, `gh pr view`, `gh pr diff`, `gh api` con GET.
- El resultado queda como cambios sin commitear; informar qué se aplicó.

## 1. Precondiciones
```bash
gh auth status
git rev-parse --show-toplevel
git status --short --branch
git fetch origin
```
- Si falta `gh` o no está autenticado, parar y avisar.
- Si ya hay cambios sin commitear, mostrarlos y preguntar si seguir encima o si el usuario prefiere guardarlos
  antes. No hacer stash ni descartar nada por cuenta propia.
- Anotar la rama y `git rev-parse HEAD` para el informe: sirven para deshacer.

## 2. Descubrir los PRs
```bash
gh pr list --app renovate --state open --limit 100 \
  --json number,title,headRefName,isDraft,mergeable,files,url
```
Si no devuelve nada, probar por autor o por rama `renovate/`:
```bash
gh pr list --state open --limit 100 --json number,title,headRefName,author,isDraft,url \
  --jq '[.[] | select((.author.login | test("renovate"; "i")) or (.headRefName | startswith("renovate/")))]'
```
El Dependency Dashboard es un *issue*, no un PR: ignorarlo. Los PRs en borrador se excluyen salvo pedido.
Mostrar la lista al usuario antes de aplicar.

## 3. Orden
1. Parches, después menores, después mayores (los mayores son los que más a menudo piden cambios de código).
2. Los grupos de Renovate (`spring-boot`, `testcontainers`, `all-minor-patch`) antes que los PRs sueltos que se les superponen.

Llevar una tabla `PR → paquete → versión vieja → versión nueva` leyendo el título del PR.

## 4. Aplicar cada PR
Guardar el parche en el scratchpad de la sesión, no en `/tmp`:
```bash
gh pr diff <n> --patch > "<scratchpad>/renovate-<n>.patch"
git apply --3way --whitespace=nowarn "<scratchpad>/renovate-<n>.patch"
git reset --quiet
```
- **Aplicó limpio:** seguir.
- **Marcadores de conflicto (`<<<<<<<`):** resolver a mano. Para dependencias casi siempre es quedarse con la
  versión más alta de cada una. Comprobar que no quede ninguno: `git grep -n '^<<<<<<<\|^>>>>>>>'`.
- **El parche no aplica:** no forzar. Leer `gh pr diff <n>` y hacer la misma edición a mano sobre el manifiesto.
- Los lockfiles de varios PRs se contradicen: aplicar solo los manifiestos y regenerar el lockfile una vez.

## 5. Ediciones compañeras (Renovate no las ve)
Revisar después de cada PR relacionado:
- **`.sdkmanrc`** (y `.mise.toml` si el cambio es de herramientas): Renovate no tiene un manager para `.sdkmanrc`.
  Si un PR sube Maven, Java o Node, subir la misma versión ahí. Mantener el formato SDKMAN (`java=25.0.4-tem`,
  no `25`). Cambiar solo lo que el PR subió y, si la versión no está publicada, no tocar el archivo y avisarlo.
- **Imágenes en `TestcontainersConfig.java`:** el `renovate.json` ya trae un manager para `parse("imagen:tag")`;
  si el PR de una imagen no lo tocó, igual verificar que el tag del código coincida con el de `compose.yml`,
  conservando el sufijo (`16-alpine`, no `16`). Otros tags en `src/test/**` y `application-test.yml` también.
- **`compose.yml`:** tags sin fijar (`:latest`) no los actualiza Renovate; no inventar una versión.
- Cambiar solo la imagen que algún PR subió; nunca subir una imagen de test por cuenta propia.

## 6. Regenerar y verificar
```bash
mvn -B -q -DskipTests verify     # valida que todo resuelve
task test                        # formatea y corre toda la suite (necesita Docker para Testcontainers)
```
Si falla: identificar qué PR lo causó (casi siempre un mayor), leer las notas del PR con `gh pr view <n>`
y hacer el cambio mínimo de código. Si el arreglo es grande o dudoso, revertir solo los archivos de ese PR
(`git checkout -- <archivos>`) y reportarlo como omitido con su motivo.
**Nunca** hacer pasar un test debilitando o borrando una aserción.

## 7. Informe
- Repo, rama base y SHA inicial.
- Tabla de cada PR: número, título y resultado (`aplicado`, `aplicado con resolución manual`, `aplicado a mano`, `omitido` + motivo).
- Ediciones compañeras hechas o pendientes.
- Comandos de verificación corridos y su resultado real, con el fallo si lo hubo.
- Archivos cambiados (`git status --short`).
- Recordatorio de que no se commiteó nada y cómo revisar y deshacer:
```bash
git diff              # revisar todo lo aplicado
git checkout -- .     # descartar todo
```

Adaptado de `sivaprasadreddy/sivalabs-agent-skills` (licencia MIT).
