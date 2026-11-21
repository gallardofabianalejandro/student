"""Pruebas de grafana_check. Correr: python tools/test_grafana_check.py"""
import base64
import http.server
import os
import pathlib
import subprocess
import sys
import threading
import unittest

TOOL = pathlib.Path(__file__).with_name("grafana_check.py")


class _Handler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        self.rfile.read(int(self.headers.get("Content-Length", 0)))
        header = self.headers.get("Authorization", "")
        decoded = base64.b64decode(header[6:]).decode() if header.startswith("Basic ") else ""
        ok = decoded == "123:bien"
        code = (204 if self.path.endswith("/v1/logs") else 200) if ok else 401
        self.send_response(code)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, *args):
        pass


def run(env_extra, base_env=None):
    env = {k: v for k, v in os.environ.items() if not k.startswith("GRAFANA_")}
    env.update(env_extra)
    return subprocess.run([sys.executable, str(TOOL)], capture_output=True, text=True, env=env)


class GrafanaCheckTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), _Handler)
        cls.port = cls.server.server_address[1]
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()

    def env(self, token):
        return {"GRAFANA_OTLP_ENDPOINT": f"http://127.0.0.1:{self.port}/otlp",
                "GRAFANA_OTLP_INSTANCE_ID": "123", "GRAFANA_OTLP_TOKEN": token}

    def test_main_whenCredentialsAccepted_thenExitZeroAndShowsCodes(self):
        out = run(self.env("bien"))
        self.assertEqual(out.returncode, 0, out.stdout + out.stderr)
        self.assertIn("metrics", out.stdout)
        self.assertIn("204", out.stdout)

    def test_main_whenCredentialsRejected_thenExitOne(self):
        out = run(self.env("mal"))
        self.assertEqual(out.returncode, 1)
        self.assertIn("401", out.stdout)

    def test_main_whenVariablesMissing_thenExitTwoNamingThemWithoutValues(self):
        out = run({"GRAFANA_OTLP_TOKEN": "secreto-que-no-debe-salir"})
        self.assertEqual(out.returncode, 2)
        self.assertIn("GRAFANA_OTLP_ENDPOINT", out.stdout + out.stderr)
        self.assertNotIn("secreto-que-no-debe-salir", out.stdout + out.stderr)

    def test_main_whenTokenPresent_thenNeverPrintsIt(self):
        out = run(self.env("bien"))
        self.assertNotIn("Basic ", out.stdout + out.stderr)
        self.assertNotIn("123:bien", out.stdout + out.stderr)


if __name__ == "__main__":
    unittest.main()
