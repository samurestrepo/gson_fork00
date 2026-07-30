# Análisis Cuantitativo — Pareto y Densidad de Defectos

**Repositorio auditado:** google/gson (fork: samurestrepo/gson_fork00)
**Fuente:** SonarCloud, análisis del 29 de julio de 2026
**Fecha:** 29 de julio de 2026

---

## 1. Densidad de defectos

**Fórmula:** Defectos / KLOC (miles de líneas de código)

| Tipo de conteo                                 | Cantidad                             | KLOC | Densidad               |
| ---------------------------------------------- | ------------------------------------ | ---- | ---------------------- |
| Defectos funcionales (Bugs + Vulnerabilidades) | 17 (Reliability) + 3 (Security) = 20 | 26   | **0.77 defectos/KLOC** |
| Issues totales (incluyendo Code Smells)        | 20 + 807 = 827                       | 26   | 31.8 issues/KLOC       |

**Nota metodológica:** se separan intencionalmente "defectos funcionales" (bugs/vulnerabilidades,
que representan riesgo de comportamiento incorrecto) de "code smells" (mantenibilidad,
riesgo de costo futuro, no de falla). La densidad de **0.77 defectos/KLOC** es la cifra
relevante para el dictamen de fiabilidad; benchmarks de industria para código maduro
suelen ubicarse entre 0.5–1.5 defectos/KLOC, por lo que gson se encuentra en un
rango aceptable pese al rating E.

---

## 2. Análisis de Pareto — por severidad (ponderación de riesgo)

**Ponderación estándar usada:** Blocker = 10, High = 5, Medium = 2, Low = 1
(refleja el peso relativo del esfuerzo/riesgo de cada severidad, no solo el conteo)

| Severidad | Cantidad | Peso | Puntaje | % del riesgo total | % acumulado |
| --------- | -------- | ---- | ------- | ------------------ | ----------- |
| Blocker   | 2        | 10   | 20      | 35.7%              | 35.7%       |
| Medium    | 9        | 2    | 18      | 32.1%              | 67.8%\*     |
| High      | 3        | 5    | 15      | 26.8%              | 94.6%       |
| Low       | 3        | 1    | 3       | 5.4%               | 100%        |

_(ordenado por puntaje descendente, no por severidad nominal, para reflejar el orden real de Pareto)_

**Lectura de Pareto:** los 2 issues Blocker + 3 High (5 issues, **29.4% del total de 17**)
concentran **62.5% del puntaje de riesgo ponderado** (20+15 = 35 de 56 puntos). Esto es
una aproximación razonable al principio 80/20: una minoría de hallazgos concentra la
mayoría del riesgo relevante para el dictamen.

## 3. Ajuste tras verificación humana (dato clave del informe)

Como se documentó en la bitácora, **1 de los 2 Blocker fue verificado como falso positivo**
(`LinkedTreeMap.java`, S2259). Recalculando el Pareto con solo defectos confirmados como reales:

| Severidad (confirmados) | Cantidad | Peso | Puntaje |
| ----------------------- | -------- | ---- | ------- |
| Blocker (real)          | 1        | 10   | 10      |
| High                    | 3        | 5    | 15      |
| Medium                  | 9        | 2    | 18      |
| Low                     | 3        | 1    | 3       |

Puntaje total real: 46 (vs. 56 sin verificar) → **una reducción del 17.9% en riesgo
ponderado** solo por aplicar verificación humana sobre el output automatizado de Sonar.

**Este es el hallazgo más importante de esta sección para el dictamen:** confiar
ciegamente en el rating crudo de la herramienta sobreestima el riesgo real en casi
un 18%. Es evidencia directa de por qué el proceso de auditoría (no solo la herramienta)
determina la calidad del veredicto.

---

## 4. Métricas DORA (estimación cualitativa)

Dado que DORA requiere datos históricos de despliegues/incidentes que no están disponibles
vía SonarCloud ni en el alcance de este análisis, se documenta como **limitación explícita**:

| Métrica DORA             | Disponibilidad                 | Nota                                                                                                                                                                                    |
| ------------------------ | ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Frecuencia de despliegue | No medible desde este análisis | Requeriría historial de releases de GitHub                                                                                                                                              |
| Lead time for changes    | No medible                     | Requeriría timestamps de PR → merge → release                                                                                                                                           |
| Time to restore service  | No medible                     | Requeriría historial de incidentes/hotfixes                                                                                                                                             |
| Change failure rate      | Proxy aproximado disponible    | 2 de 4 jobs del CI oficial excluyen módulos frágiles (`test-jpms`, `test-shrinker`) — indicio indirecto de inestabilidad conocida en ciertos módulos, no una tasa de fallo real medible |

**Declaración para el informe:** las métricas DORA completas no son calculables con las
fuentes de datos gratuitas disponibles en el alcance de este proyecto (GitHub público +
SonarCloud free tier); se documenta como limitación metodológica, no como omisión.
