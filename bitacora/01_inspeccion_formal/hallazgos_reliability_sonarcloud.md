# Hallazgos de Reliability — Análisis SonarCloud

**Repositorio auditado:** google/gson (fork: samurestrepo/gson_fork00)
**Fecha:** 29 de julio de 2026
**Registrado por:** Samuel
**Fuente:** SonarCloud, proyecto samurestrepo_gson_fork00

---

## Resumen de calidad — Quality Gate (Sonar Way, sin umbrales custom aún)

| Métrica           | Valor                        | Rating                 |
| ----------------- | ---------------------------- | ---------------------- |
| Reliability       | 17 issues abiertos           | **E**                  |
| Security          | 3 issues abiertos            | **C**                  |
| Maintainability   | 807 code smells              | **A**                  |
| Coverage          | 12.3% (7.1k líneas a cubrir) | Sin umbral configurado |
| Duplications      | 1.6% (26k líneas)            | Sin umbral configurado |
| Security Hotspots | 0 pendientes                 | —                      |

**Nota:** Coverage y Duplications aparecen como "No conditions set" — el Quality Gate por defecto de Sonar no evalúa estas métricas. Se definirán umbrales propios en el Anexo ISO/IEC 25010.

## Desglose de severidad — Reliability (17 issues)

| Severidad | Cantidad |
| --------- | -------- |
| Blocker   | 2        |
| High      | 3        |
| Medium    | 9        |
| Low       | 3        |
| Info      | 0        |

**Hallazgo clave:** el rating E se explica por la sola presencia de 2 issues Blocker (Sonar asigna el rating más bajo si existe al menos uno, independientemente del resto).

---

## Detalle de issues Blocker

### Blocker 1 — `LinkedTreeMap.java`

| Campo                              | Detalle                                                                                                                                                       |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Regla Sonar**                    | `javabugs:S2259` — "Null pointers should not be dereferenced"                                                                                                 |
| **Módulo**                         | Core (`gson`) — clase interna usada ampliamente en la librería                                                                                                |
| **Descripción**                    | Riesgo de`NullPointerException` detectado por análisis estático                                                                                               |
| **Estado de verificación**         | ⏳ PENDIENTE — esta regla es conocida por generar falsos positivos cuando existe una validación previa que el analizador no rastrea correctamente en su flujo |
| **Impacto en adopción si es real** | Alto — es una clase interna core, no aislada                                                                                                                  |
| **Próximo paso**                   | Revisar línea exacta señalada por Sonar y el código circundante para determinar si es bug real o falso positivo                                               |

### Blocker 2 — `ParseBenchmark.java`

| Campo                      | Detalle                                                                                                |
| -------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Regla Sonar**            | `java:S2095` — "Resources should be closed"                                                            |
| **Módulo**                 | `metrics` — herramienta interna de benchmarking, no parte de la API pública                            |
| **Descripción**            | Recurso (stream/reader) abierto sin cierre explícito ni try-with-resources; riesgo de fuga de recursos |
| **Estado de verificación** | Bug de patrón real (gestión de recursos), no requiere verificación de falso positivo                   |
| **Impacto en adopción**    | Bajo — no afecta a consumidores externos de la librería, solo a herramientas internas de desarrollo    |

---

## Actualización — Blocker 1: `LinkedTreeMap.java` (S2259) — VEREDICTO FINAL

**Estado:** Falso positivo confirmado

**Línea exacta:** `Node<K, V> leftLeft = left.left;` dentro de `rebalance()`, rama `else if (delta == 2)`.

**Justificación técnica:**
`delta` se calcula como `leftHeight - rightHeight`, donde `leftHeight = 0` si `left == null`.
Dado que `rightHeight >= 0` siempre, `delta == 2` solo es alcanzable si `leftHeight >= 2`,
lo cual es matemáticamente incompatible con `left == null`. El invariante del algoritmo
de balanceo AVL garantiza que `left` no puede ser nulo en este punto del flujo.

**Por qué Sonar lo marca de todos modos:** el análisis estático de SonarJava rastrea
nulabilidad símbolo por símbolo, pero no infiere relaciones aritméticas entre variables
derivadas (no conecta "leftHeight ≥ 2" con "left no puede ser null"). Este es un patrón
de falso positivo documentado en la comunidad de Sonar para la regla S2259.

**Decisión de la interventoría:** No se recomienda corrección de código (fuera de alcance
del proyecto). Se recomienda documentar como excepción justificada del Quality Gate,
o suprimir puntualmente con `@SuppressWarnings` + comentario explicativo si el equipo
mantenedor decide adoptarlo.

**Impacto en el dictamen final:** Reduce el hallazgo de "2 Blocker reales" a
"1 Blocker real (ParseBenchmark) + 1 falso positivo verificado (LinkedTreeMap)".
Esto es evidencia de rigor auditor: no se acepta el rating de Sonar sin verificación humana.
