#!/usr/bin/env python3
"""Imprime la cabecera 'Basic <base64>' para Grafana Cloud a partir de GRAFANA_OTLP_INSTANCE_ID y GRAFANA_OTLP_TOKEN.

Se usa desde `task run_cloud`; nunca escribe el token en un archivo.
"""
import base64
import os
import sys

instance = os.environ.get("GRAFANA_OTLP_INSTANCE_ID")
token = os.environ.get("GRAFANA_OTLP_TOKEN")
if not instance or not token:
    sys.exit("Faltan GRAFANA_OTLP_INSTANCE_ID y/o GRAFANA_OTLP_TOKEN en el entorno")
print("Basic " + base64.b64encode(f"{instance}:{token}".encode()).decode())
