#!/usr/bin/env python3
"""Renderiza los overlays de deployment/ con kustomize y revisa que sigan coherentes entre si.

Falla si base pierde la telemetria completa o el PDB, si local la enciende, o si local-cloud
deja de usar el perfil grafana-cloud / versiona el Secret. Requiere kubectl y PyYAML.
Se corre con `task k8s_check` y en el CI (workflow manifests).
"""
import pathlib
import subprocess
import sys

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
METRICS_ENV = "MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED"


def _kind(docs, kind):
    return [d for d in docs if d.get("kind") == kind]


def _config(docs):
    found = [d for d in _kind(docs, "ConfigMap") if d["metadata"]["name"] == "student-config"]
    return found[0].get("data", {}) if found else {}


def _container(docs):
    student = [d for d in _kind(docs, "Deployment") if d["metadata"]["name"] == "student"][0]
    return student["spec"]["template"]["spec"]["containers"][0]


CREDENTIAL_KEYS = ("GRAFANA_OTLP_AUTH", "GRAFANA_OTLP_TOKEN")


def _walk(node):
    """Recorre claves y valores de un documento YAML ya parseado."""
    if isinstance(node, dict):
        for key, value in node.items():
            yield key
            yield from _walk(value)
    elif isinstance(node, list):
        for item in node:
            yield from _walk(item)
    else:
        yield node


def check_no_credentials(docs):
    """Ningun documento renderizado puede traer la credencial de Grafana Cloud (ni su clave ni una cabecera Basic)."""
    errors = []
    for doc in docs:
        label = f"{doc.get('kind')}/{doc.get('metadata', {}).get('name')}"
        for item in _walk(doc):
            if item in CREDENTIAL_KEYS and doc.get("kind") != "Deployment":
                errors.append(f"{label}: trae la clave {item}; la credencial solo vive en el Secret creado con `task k8s_grafana_secret`")
            elif isinstance(item, str) and item.startswith("Basic "):
                errors.append(f"{label}: contiene una cabecera 'Basic ...' versionada")
    return errors


def check_base(docs):
    errors = []
    if _kind(docs, "Secret"):
        errors.append("base: no debe traer ningun Secret (los crea quien despliega)")
    extra = [d["metadata"]["name"] for d in _kind(docs, "Deployment") if d["metadata"]["name"] != "student"]
    if extra:
        errors.append(f"base: solo debe tener el Deployment student; sobra {', '.join(extra)} (eso es de local)")
    if "grafana-cloud" in _config(docs).get("SPRING_PROFILES_ACTIVE", ""):
        errors.append("base: no debe activar el perfil grafana-cloud (es del overlay local-cloud)")
    if _config(docs).get("OBSERVABILITY_ENABLED") != "true":
        errors.append("base: OBSERVABILITY_ENABLED debe ser 'true' (con solo TRACING_ENABLED salen trazas pero no metricas ni logs)")
    if not _kind(docs, "PodDisruptionBudget"):
        errors.append("base: falta el PodDisruptionBudget")
    return errors


def check_local(docs):
    if _config(docs).get("OBSERVABILITY_ENABLED") != "false":
        return ["local: OBSERVABILITY_ENABLED debe ser 'false' (minikube no tiene collector)"]
    return []


def check_local_cloud(docs):
    errors = []
    if "grafana-cloud" not in _config(docs).get("SPRING_PROFILES_ACTIVE", "").split(","):
        errors.append("local-cloud: SPRING_PROFILES_ACTIVE debe incluir el perfil grafana-cloud")
    container = _container(docs)
    env = {e["name"]: e.get("value") for e in container.get("env", [])}
    if env.get(METRICS_ENV) != "true":
        errors.append(f"local-cloud: {METRICS_ENV} debe ser 'true' (el overlay local lo apaga y la variable gana sobre el YAML del perfil)")
    refs = [r["secretRef"]["name"] for r in container.get("envFrom", []) if "secretRef" in r]
    if "grafana-cloud" not in refs:
        errors.append("local-cloud: falta el secretRef grafana-cloud en envFrom")
    if any(d["metadata"]["name"] == "grafana-cloud" for d in _kind(docs, "Secret")):
        errors.append("local-cloud: el Secret grafana-cloud esta versionado; debe crearse con `task k8s_grafana_secret`")
    return errors


CHECKS = {"base": check_base, "local": check_local, "local-cloud": check_local_cloud}


def find_placeholders(text):
    """Lineas con un valor CHANGE_ME sin completar."""
    return [line.strip() for line in text.splitlines() if "CHANGE_ME" in line]


def render(overlay):
    out = subprocess.run(["kubectl", "kustomize", str(ROOT / "deployment" / overlay)],
                         capture_output=True, text=True, check=False)
    if out.returncode != 0:
        raise RuntimeError(f"{overlay}: kustomize fallo\n{out.stderr}")
    return [d for d in yaml.safe_load_all(out.stdout) if d]


def placeholders_main(path):
    """`check_manifests.py --placeholders <ruta>`: falla si el overlay renderizado trae CHANGE_ME."""
    out = subprocess.run(["kubectl", "kustomize", path], capture_output=True, text=True, check=False)
    if out.returncode != 0:
        print(f"ERROR kustomize fallo en {path}\n{out.stderr}", file=sys.stderr)
        return 1
    found = find_placeholders(out.stdout)
    for line in found:
        print(f"ERROR {path} tiene un valor sin completar: {line}", file=sys.stderr)
    return 1 if found else 0


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--placeholders":
        return placeholders_main(sys.argv[2])
    errors = []
    for overlay, check in CHECKS.items():
        try:
            docs = render(overlay)
            errors += check(docs) + [f"{overlay}: {e}" for e in check_no_credentials(docs)]
        except (RuntimeError, IndexError, KeyError) as exc:
            errors.append(f"{overlay}: no se pudo verificar ({type(exc).__name__}: {exc})")
        except FileNotFoundError:
            errors.append("falta kubectl en el PATH (hace falta para renderizar con kustomize)")
            break
    for error in errors:
        print("ERROR", error, file=sys.stderr)
    if not errors:
        print(f"OK: {', '.join(CHECKS)} renderizan y son coherentes")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
