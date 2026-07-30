# PROMPT PARA OPENCODE - INSPECCIÓN, DIAGNÓSTICO Y RESOLUCIÓN DE ERRORES DE ENTORNO/IDE

**Rol:** Actúa como un Ingeniero Principal de Herramientas y Calidad de Software Java (DevOps / IDE Integrations Specialist).

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`. NO crees carpetas raíz adicionales.

---

### OBJETIVO
Inspeccionar, corregir y documentar los ~529 errores/advertencias detectados en el editor de código (VS Code / OpenCode LSP) en archivos como `Java17RecordTest.java`, `NumberLimitsTest.java`, `ObjectTest.java`, etc., logrando armonía total entre la ejecución por consola (`BUILD SUCCESS`) y el análisis en tiempo real del editor.

---

### PASOS A EJECUTAR

1. **ANÁLISIS DE CAUSA RAÍZ (DIAGNÓSTICO):**
   - Examina por qué los archivos bajo `gson/src/test/java/com/google/gson/functional/` (ej. `Java17RecordTest.java`) reportan errores de sintaxis (como el uso de `record`, `@SerializedName` en accesores o características de Java 17+).
   - Verifica la configuración del nivel de lenguaje Java del proyecto en la carpeta `.vscode/settings.json` o en la definición de propiedades del `pom.xml`.

2. **APLICACIÓN DE SOLUCIONES (REFACTORIZACIÓN Y CONFIGURACIÓN):**
   - Configura o ajusta el entorno del proyecto (creando/actualizando `.vscode/settings.json` si es necesario) para asegurar que el Servidor de Lenguaje Java (JDT/LSP) reconozca la JDK 21 instalada (`C:\Program Files\Java\jdk-21.0.11`) y habilite la compatibilidad con Java 17/21 en los tests.
   - Si existen errores reales de sintaxis, imports faltantes o desordenados en clases de prueba recién generadas o modificadas, corrige el código fuente.
   - Ejecuta una recompilación y validación de pruebas en el módulo principal:
     `mvn clean test-compile -pl gson`

3. **DOCUMENTACIÓN EN BITÁCORA:**
   Crea la subcarpeta `gson_fork00\bitacora\03_corridas_agente\diagnostico_errores_ide\` y genera el archivo `informe_diagnostico_y_solucion_errores.md` estructurado de la siguiente forma:

   - **1. Resumen Ejecutivo:** Descripción del fenómeno (Consola en SUCCESS vs IDE en ERROR).
   - **2. Diagnóstico Técnico por Archivo/Patrón:**
     * Explicación de por qué `Java17RecordTest.java` marcaba errores sintácticos.
     * Explicación de errores en `NumberLimitsTest.java`, `ObjectTest.java` y `Gson.java`.
     * Causa raíz: Mapeo de Nivel de Lenguaje (Java 8 Baseline vs Java 17/21 Test Execution).
   - **3. Acciones Correctivas Aplicadas:**
     * Cambios en código fuente (si aplicó).
     * Cambios en configuración de entorno (`.vscode/settings.json`, `pom.xml`, etc.).
   - **4. Estado Final y Verificación:** Captura del log de Maven confirmando `BUILD SUCCESS`.

---

### REGISTRO DE EVIDENCIAS

En la carpeta `gson_fork00\bitacora\03_corridas_agente\diagnostico_errores_ide\` guarda:
1. `prompt_utilizado.md`: El contenido exacto de este prompt.
2. `informe_diagnostico_y_solucion_errores.md`: El documento con el diagnóstico detallado, las causas explicadas y la solución aplicada.
