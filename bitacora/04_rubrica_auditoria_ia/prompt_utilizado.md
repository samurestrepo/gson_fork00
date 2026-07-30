# PROMPT PARA OPENCODE - TAREA 4: AUDITORÍA DE SALIDAS DE IA (RÚBRICA TABLA 4.4)

**Rol:** Actúa como Auditor General de Sistemas de Gestión de IA (SGIA) e Interventor Técnico de Calidad.

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`. NO crees carpetas adicionales en la raíz.

---

### OBJETIVO
Evaluar cuantitativa y cualitativamente la totalidad de las salidas generadas durante la **Tarea 3 (Corridas 1 y 2, incluyendo los diagnósticos de IDE y CI/CD)** aplicando la **Rúbrica de Auditoría de IA (Tabla 4.4)**.

---

### CRITERIOS DE EVALUACIÓN

Evalúa cada uno de los 6 criterios asignando un nivel:
* **Nivel 1 (Rechazar):** La salida contiene errores graves, alucinaciones o rompe el build.
* **Nivel 2 (Corregir y usar):** La salida requirió ajustes menores para funcionar correctamente.
* **Nivel 3 (Usar):** La salida es correcta, segura y lista para producción sin modificaciones.

**Criterios a auditar:**
1. **Corrección funcional:** Basado en la evidencia de logs (`BUILD SUCCESS` en las 4,619+ pruebas y reactor multimódulo).
2. **Seguridad:** Ausencia de fugas de memoria o vulnerabilidades en accesos reflectivos (`ReflectiveTypeAdapterFactory`).
3. **Calidad estructural:** Adherencia a los estándares de diseño y arquitectura de Gson.
4. **Dependencias:** Uso exclusivo de librerías permitidas (`JUnit 4`, `Google Truth`) sin alucinaciones de código.
5. **Calidad de las pruebas:** Validez de las aserciones sobre límites, excepciones y licencias.
6. **Trazabilidad:** Registro completo de prompts, logs y validaciones en la bitácora.

---

### ESTRUCTURA DE SALIDA REQUERIDA

Genera un documento en Markdown estructurado de la siguiente forma:

1. **Matriz Consolidada de Auditoría (Tabla 4.4):**
   | Criterio de Auditoría | Nivel Asignado (1, 2 o 3) | Evidencia Técnica e Impacto en la Interventoría |

2. **Justificación Detallada por Criterio:** Explicación técnica de cada calificación dada.

3. **Dictamen Global de Interventoría:** 
   * Estado de Aprobación (*Aprobado*, *Aprobado con Observaciones*, o *Rechazado*).
   * Calificación cuantitativa ponderada en escala de **1.0 a 5.0**.

---

### REGISTRO DE EVIDENCIAS

Crea la subcarpeta `gson_fork00\bitacora\04_rubrica_auditoria_ia\` y guarda:
1. `prompt_utilizado.md`: El contenido exacto de este prompt.
2. `evaluacion_rubrica_tabla_4.4.md`: El informe final de auditoría con la matriz diligenciada y el dictamen global.
