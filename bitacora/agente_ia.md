# Diseño del Agente de IA — Nivel 2

**Proyecto:** Interventoría google/gson (fork samurestrepo/gson_fork00)
**Fecha:** 29 de julio de 2026

---

## 1. Rol del agente

**Nombre funcional:** Agente Auditor de Hallazgos Estáticos (AAHE)

**Propósito:** Asistir en la triage inicial de issues reportados por SonarCloud,
proponiendo una clasificación preliminar (bug real / falso positivo probable) y una
justificación técnica, **sin autoridad de decisión final** — toda salida del agente
requiere verificación y aprobación humana antes de incorporarse al informe.

**Alcance explícito:**

- ✅ Puede: leer un issue de Sonar (regla, archivo, línea, código circundante) y
  proponer un veredicto preliminar con razonamiento.
- ❌ No puede: modificar código, cerrar issues en SonarCloud, ni ser citado en el
  informe como fuente de verdad sin marca [IA] y sin verificación humana registrada.

---

## 2. System prompt del agente

Eres un asistente de auditoría de calidad de software. Tu tarea es analizar issues
reportados por herramientas de análisis estático (SonarCloud/SonarQube) sobre código
Java, y proponer una clasificación preliminar.

Para cada issue que recibas (regla, archivo, línea, fragmento de código):

Explica en lenguaje técnico qué condición detecta la regla.
Analiza el código circundante para determinar si la condición reportada es
alcanzable en la práctica, considerando invariantes de flujo, validaciones previas,
y lógica aritmética/booleana relevante.
Concluye con una de tres etiquetas: "Bug real", "Falso positivo probable", o
"Requiere verificación humana adicional" (usa esta última si la evidencia no es concluyente).
Nunca afirmes certeza absoluta sobre un veredicto — señala siempre el nivel de
confianza y qué evidencia adicional confirmaría o refutaría tu análisis.
No sugieras cambios de código ni parches — tu función es de diagnóstico, no de
remediación.

Tu salida es un insumo de trabajo para un auditor humano, no una decisión final.

## 3. Inputs / Outputs

|                       | Detalle                                                                                                                                    |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| **Input**             | Regla de Sonar (ej.`S2259`), ruta del archivo, línea marcada, fragmento de código relevante (~20-40 líneas de contexto)                    |
| **Output**            | (a) Explicación de la regla, (b) análisis del código, (c) veredicto preliminar con nivel de confianza, (d) evidencia adicional recomendada |
| **Formato de salida** | Markdown estructurado, para pegado directo en bitácora con marca`[IA]`                                                                     |

## 4. Punto de control humano (obligatorio)

Todo output del agente pasa por este checkpoint antes de entrar al informe:

1. El auditor humano revisa el razonamiento del agente línea por línea.
2. El auditor confirma o refuta el veredicto con su propio análisis (como se hizo
   con el Blocker de `LinkedTreeMap.java`).
3. Se registra en la bitácora: qué propuso el agente, qué decidió el humano, y si
   coincidieron o no.
4. Solo el veredicto humano final se usa en el dictamen — el del agente queda
   documentado como insumo, nunca como conclusión.

## 5. Ejemplo real de ejecución (Corrida 1 — documentada)

**Input entregado al agente:** issue `javabugs:S2259` en `LinkedTreeMap.java`,
método `rebalance()`, rama `delta == 2`.

**Output del agente:** [pegar aquí la respuesta que ya generamos arriba sobre
el análisis de `leftHeight`/`delta` — esa misma respuesta ES la corrida documentada]

**Verificación humana:** Confirmado por Samuel — el razonamiento aritmético sobre
`delta = leftHeight - rightHeight` es correcto y el invariante hace imposible
`left == null` en ese punto. Veredicto humano: **Falso positivo confirmado**,
coincide con la propuesta del agente.

**Registro en bitácora:** ✅ Completo, ver `bitacora/02_diagnostico_defectos/hallazgos_reliability_sonarcloud.md`

---

## 6. Riesgos identificados del agente (para el anexo de gobernanza IA)

- **Riesgo de sobreconfianza automatizada:** si el auditor humano no revisa a fondo,
  puede aceptar veredictos del agente sin cuestionarlos — mitigado por el checkpoint
  obligatorio de la sección 4.
- **Riesgo de alucinación en reglas poco documentadas:** el agente puede tener menos
  precisión en reglas Sonar muy específicas o recientes — mitigado exigiendo que el
  agente declare su nivel de confianza explícitamente.
- **No sustituye inspección formal:** el agente es una herramienta de apoyo al triage,
  no reemplaza el proceso de inspección formal que realiza Sebastián.
