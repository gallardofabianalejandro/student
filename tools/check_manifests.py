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


def check_base(docs):
    errors = []
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


def render(overlay):
    out = subprocess.run(["kubectl", "kustomize", str(ROOT / "deployment" / overlay)],
                         capture_output=True, text=True, check=False)
    if out.returncode != 0:
        raise RuntimeError(f"{overlay}: kustomize fallo\n{out.stderr}")
    return [d for d in yaml.safe_load_all(out.stdout) if d]


def main():
    errors = []
    for overlay, check in CHECKS.items():
        try:
            errors += check(render(overlay))
        except RuntimeError as exc:
            errors.append(str(exc))
    for error in errors:
        print("ERROR", error, file=sys.stderr)
    if not errors:
        print(f"OK: {', '.join(CHECKS)} renderizan y son coherentes")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
