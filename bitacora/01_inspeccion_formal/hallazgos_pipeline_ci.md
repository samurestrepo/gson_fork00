# Hallazgos técnicos — Configuración de pipeline CI/CD

**Repositorio auditado:** google/gson (fork: samurestrepo/gson_fork00)
**Fecha:** 29 de julio de 2026
**Registrado por:** Samuel

---

## Hallazgo 1 — Fallo en módulo `test-jpms` (Java Platform Module System)

| Campo                           | Detalle                                                                                                                                                                                                                |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Clasificación**               | Defecto (fault) — presente en el código/configuración, no atribuible al fork                                                                                                                                           |
| **Descripción**                 | El módulo de pruebas JPMS falla durante`mvn test`/`mvn verify`, deteniendo el reactor de Maven y saltando módulos posteriores (Gson Extras, Gson Metrics, Gson Protobuf Support) si no se usa `-fae`.                  |
| **Evidencia de pre-existencia** | El workflow oficial (`build.yml`) ya excluye `test-jpms` explícitamente en el job `verify-reproducible-build`: `mvn artifact:check-buildplan --projects '!metrics,!test-graal-native-image,!test-jpms,!test-shrinker'` |
| **Mitigación aplicada**         | Flag`-fae` (fail-at-end) en build local y en el pipeline, para que el análisis continúe sobre el resto de módulos.                                                                                                     |
| **Fuera de alcance corregirlo** | Sí — el proyecto de interventoría excluye intervención directa sobre el código.                                                                                                                                        |

## Hallazgo 2 — Ausencia de plugin de cobertura (JaCoCo)

| Campo             | Detalle                                                                                                                      |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **Clasificación** | Brecha de configuración (no defecto del proyecto original, sino requisito faltante para la auditoría)                        |
| **Descripción**   | El`pom.xml` no traía configurado JaCoCo; sin esto, SonarCloud no genera métricas de cobertura de código.                     |
| **Acción tomada** | Se agregó`jacoco-maven-plugin` v0.8.12 (goals `prepare-agent` y `report`) + property `sonar.coverage.jacoco.xmlReportPaths`. |
| **Relevancia**    | Necesario para completar métricas de mantenibilidad/confiabilidad del Anexo ISO/IEC 25010.                                   |

## Hallazgo 3 — Violaciones de formato (Spotless)

| Campo                          | Detalle                                                                                                                                                                  |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Clasificación**              | No es un defecto del proyecto — es evidencia de un gate de calidad de estilo activo y estricto                                                                           |
| **Descripción**                | El plugin`spotless-maven-plugin` rechazó el build por espacios en blanco al final de línea en `pom.xml` y `build.yml`, introducidos al editar/pegar configuración nueva. |
| **Relevancia para el informe** | Indicador positivo de madurez de proceso: el proyecto enforce estilo de código automáticamente en cada build.                                                            |
| **Resuelto con**               | `mvn spotless:apply`                                                                                                                                                     |

## Hallazgo 4 — Fallo de resolución de dependencia en `test-shrinker` (R8/ProGuard)

| Campo                           | Detalle                                                                                                                                                                    |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Clasificación**               | Defecto de dependencia externa / fragilidad de infraestructura de build                                                                                                    |
| **Descripción**                 | El módulo`test-shrinker` no resuelve el artefacto `com.android.tools:r8:jar:9.1.31` — falla de descriptor de plugin.                                                       |
| **Evidencia de pre-existencia** | El`build.yml` oficial ya excluye `test-shrinker` en varios jobs por incompatibilidades conocidas de JDK/ProGuard.                                                          |
| **Mitigación aplicada**         | Exclusión del módulo (`--projects '!test-shrinker'`), replicando el mismo patrón del CI oficial.                                                                           |
| **Pendiente de verificar**      | Si el fallo persiste también en GitHub Actions (no solo en entorno local) — si persiste ahí, es evidencia más fuerte de fragilidad real del proyecto y no de la red local. |

---

## Pendientes de esta bitácora

- [ ] Capturas de pantalla de cada error (anexo de evidencia)
- [ ] Confirmar si Hallazgo 4 persiste en el pipeline de GitHub Actions
- [ ] Resultado final del pipeline en Actions
- [ ] Veredicto del Quality Gate en SonarCloud
