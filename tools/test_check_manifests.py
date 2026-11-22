"""Pruebas de check_manifests (reglas sobre manifiestos ya renderizados). Correr: python tools/test_check_manifests.py"""
import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import check_manifests as cm  # noqa: E402


def cfg(**data):
    return {"kind": "ConfigMap", "metadata": {"name": "student-config"}, "data": data}


def dep(env=None, env_from=None):
    container = {"name": "student", "env": env or [], "envFrom": env_from or []}
    return {"kind": "Deployment", "metadata": {"name": "student"},
            "spec": {"template": {"spec": {"containers": [container]}}}}


POSTGRES = {"kind": "Deployment", "metadata": {"name": "postgres"},
            "spec": {"template": {"spec": {"containers": [{"name": "postgres"}]}}}}
PDB = {"kind": "PodDisruptionBudget", "metadata": {"name": "student"}}


class BaseRules(unittest.TestCase):
    def test_base_whenObservabilityOnAndPdb_thenNoErrors(self):
        self.assertEqual([], cm.check_base([cfg(OBSERVABILITY_ENABLED="true"), dep(), PDB]))

    def test_base_whenOnlyTracingFlag_thenReportsMissingObservability(self):
        errors = cm.check_base([cfg(TRACING_ENABLED="true"), dep(), PDB])
        self.assertTrue(any("OBSERVABILITY_ENABLED" in e for e in errors))

    def test_base_whenNoPdb_thenReportsPdb(self):
        errors = cm.check_base([cfg(OBSERVABILITY_ENABLED="true"), dep()])
        self.assertTrue(any("PodDisruptionBudget" in e for e in errors))


class BaseIsolation(unittest.TestCase):
    OK = [cfg(OBSERVABILITY_ENABLED="true"), dep(), PDB]

    def test_base_whenContainsSecret_thenReports(self):
        secret = {"kind": "Secret", "metadata": {"name": "student-secrets"}}
        self.assertTrue(any("Secret" in e for e in cm.check_base(self.OK + [secret])))

    def test_base_whenContainsPostgresDeployment_thenReports(self):
        self.assertTrue(any("postgres" in e for e in cm.check_base(self.OK + [POSTGRES])))

    def test_base_whenGrafanaCloudProfileLeaks_thenReports(self):
        docs = [cfg(OBSERVABILITY_ENABLED="true", SPRING_PROFILES_ACTIVE="production,grafana-cloud"), dep(), PDB]
        self.assertTrue(any("grafana-cloud" in e for e in cm.check_base(docs)))


class CredentialLeaks(unittest.TestCase):
    def test_noCredentials_whenOnlySecretRef_thenNoErrors(self):
        docs = [cfg(A="1"), dep(env_from=[{"secretRef": {"name": "grafana-cloud"}}])]
        self.assertEqual([], cm.check_no_credentials(docs))

    def test_credentials_whenAuthKeyInConfigMap_thenReports(self):
        self.assertTrue(cm.check_no_credentials([cfg(GRAFANA_OTLP_AUTH="Basic abc")]))

    def test_credentials_whenTokenInAnySecret_thenReports(self):
        secret = {"kind": "Secret", "metadata": {"name": "otro"}, "stringData": {"GRAFANA_OTLP_TOKEN": "x"}}
        self.assertTrue(cm.check_no_credentials([secret]))

    def test_credentials_whenBasicHeaderInEnvValue_thenReports(self):
        docs = [dep(env=[{"name": "ANYTHING", "value": "Basic MTIzOnRva2Vu"}])]
        self.assertTrue(cm.check_no_credentials(docs))


class Placeholders(unittest.TestCase):
    def test_placeholders_whenChangeMePresent_thenReturnsLines(self):
        text = "a: 1\nDB_URL: jdbc:postgresql://CHANGE_ME:5432/student\n"
        self.assertEqual(["DB_URL: jdbc:postgresql://CHANGE_ME:5432/student"], cm.find_placeholders(text))

    def test_placeholders_whenNone_thenEmpty(self):
        self.assertEqual([], cm.find_placeholders("a: 1\nb: 2\n"))


class LocalRules(unittest.TestCase):
    def test_local_whenObservabilityOff_thenNoErrors(self):
        self.assertEqual([], cm.check_local([cfg(OBSERVABILITY_ENABLED="false"), dep()]))

    def test_local_whenObservabilityOn_thenReports(self):
        self.assertTrue(cm.check_local([cfg(OBSERVABILITY_ENABLED="true"), dep()]))


class LocalCloudRules(unittest.TestCase):
    OK_ENV = [{"name": "MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED", "value": "true"}]
    OK_FROM = [{"secretRef": {"name": "grafana-cloud"}}]

    def test_localCloud_whenProfileSecretRefAndMetricsOn_thenNoErrors(self):
        docs = [cfg(SPRING_PROFILES_ACTIVE="production,grafana-cloud"), dep(self.OK_ENV, self.OK_FROM)]
        self.assertEqual([], cm.check_local_cloud(docs))

    def test_localCloud_whenPostgresDeploymentComesFirst_thenStillFindsStudent(self):
        docs = [POSTGRES, cfg(SPRING_PROFILES_ACTIVE="production,grafana-cloud"), dep(self.OK_ENV, self.OK_FROM)]
        self.assertEqual([], cm.check_local_cloud(docs))

    def test_localCloud_whenMetricsEnvLeftOff_thenReports(self):
        env = [{"name": "MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED", "value": "false"}]
        docs = [cfg(SPRING_PROFILES_ACTIVE="production,grafana-cloud"), dep(env, self.OK_FROM)]
        self.assertTrue(any("METRICS" in e for e in cm.check_local_cloud(docs)))

    def test_localCloud_whenSecretIsVersioned_thenReports(self):
        secret = {"kind": "Secret", "metadata": {"name": "grafana-cloud"}}
        docs = [cfg(SPRING_PROFILES_ACTIVE="production,grafana-cloud"), dep(self.OK_ENV, self.OK_FROM), secret]
        self.assertTrue(any("versionado" in e for e in cm.check_local_cloud(docs)))

    def test_localCloud_whenNoSecretRef_thenReports(self):
        docs = [cfg(SPRING_PROFILES_ACTIVE="production,grafana-cloud"), dep(self.OK_ENV)]
        self.assertTrue(any("secretRef" in e for e in cm.check_local_cloud(docs)))


if __name__ == "__main__":
    unittest.main()
