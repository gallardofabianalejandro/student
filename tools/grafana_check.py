#!/usr/bin/env python3
"""Comprueba el acceso a Grafana Cloud: variables presentes y respuesta de las tres rutas OTLP.

Usa GRAFANA_OTLP_ENDPOINT, GRAFANA_OTLP_INSTANCE_ID y GRAFANA_OTLP_TOKEN. Nunca imprime el token ni la cabecera.
Sale 0 si las tres rutas aceptan (200/204), 1 si alguna rechaza o no responde, 2 si faltan variables.
"""
import base64
import os
import sys
import urllib.error
import urllib.request

REQUIRED = ("GRAFANA_OTLP_ENDPOINT", "GRAFANA_OTLP_INSTANCE_ID", "GRAFANA_OTLP_TOKEN")
PATHS = ("v1/metrics", "v1/traces", "v1/logs")


def probe(url, header):
    request = urllib.request.Request(url, data=b"", method="POST",
                                     headers={"Content-Type": "application/x-protobuf", "Authorization": header})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return response.status
    except urllib.error.HTTPError as error:
        return error.code
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        return f"sin respuesta ({type(error).__name__})"


def main():
    missing = [name for name in REQUIRED if not os.environ.get(name)]
    if missing:
        print("Faltan variables de entorno: " + ", ".join(missing))
        return 2
    credentials = f"{os.environ['GRAFANA_OTLP_INSTANCE_ID']}:{os.environ['GRAFANA_OTLP_TOKEN']}".encode()
    header = "Basic " + base64.b64encode(credentials).decode()
    endpoint = os.environ["GRAFANA_OTLP_ENDPOINT"].rstrip("/")
    ok = True
    for path in PATHS:
        status = probe(f"{endpoint}/{path}", header)
        accepted = status in (200, 204)
        ok = ok and accepted
        print(f"{path.split('/')[-1]:8} -> {status} {'OK' if accepted else 'RECHAZADO'}")
    print("Acceso a Grafana Cloud: " + ("correcto" if ok else "con problemas (401/403: revisa instance ID y token)"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
