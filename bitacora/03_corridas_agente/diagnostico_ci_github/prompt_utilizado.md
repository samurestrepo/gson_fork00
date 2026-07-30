# PROMPT PARA OPENCODE - DIAGNÓSTICO Y REPARACIÓN DE PIPELINE DE CI/CD (GITHUB ACTIONS)

**Rol:** Actúa como Ingeniero DevOps Principal y Especialista en CI/CD Java.

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`. NO crees carpetas adicionales en la raíz.

---

### OBJETIVO
Reproducir localmente los fallos reportados por GitHub Actions (`Build on JDK 21`, `GraalVM` y `SonarCloud`), reparar la clase `ReflectiveTypeAdapterFactoryBoundaryTest.java` (y cualquier archivo modificado), y asegurar que el comando global de Maven pase al 100% antes del próximo push.

---

### PASOS A EJECUTAR

1. **VERIFICACIÓN DE FORMATO Y LICENCIAS (SPOTLESS):**
   - Ejecuta el verificador de estilo del proyecto Gson:
     `mvn spotless:check`
   - Si detecta errores de formato o falta del encabezado de licencia Apache 2.0 en `ReflectiveTypeAdapterFactoryBoundaryTest.java`, aplica el formato automático:
     `mvn spotless:apply`

2. **COMPILACIÓN Y EJECUCIÓN MULTIMÓDULO COMPLETA (SIMULACIÓN CI):**
   - Para resolver las dependencias JPMS y GraalVM que fallaron en GitHub, instala los artefactos y ejecuta la validación global:
     `mvn clean install`
   - Si algún submódulo (`test-jpms` o `gson-extras`) arroja error por visibilidad de paquetes o módulos de la nueva clase de prueba, ajusta la visibilidad de la clase (`public`) o sus imports.

3. **DOCUMENTACIÓN DEL HALLAZGO EN BITÁCORA:**
   Crea la subcarpeta `gson_fork00\bitacora\03_corridas_agente\diagnostico_ci_github\` y genera el archivo `informe_fallo_ci_github.md` estructurado así:

   - **1. Análisis del Fallo en CI:**
     * Explicación de por qué `Build Gson subset` pasó mientras `JDK 21/25` y `SonarCloud` fallaron.
     * Causa raíz identificada (ej. Reglas de Spotless, encabezado de licencia faltante, o dependencias del módulo `test-jpms`).
   - **2. Correcciones Aplicadas:**
     * Ajustes en `ReflectiveTypeAdapterFactoryBoundaryTest.java` (licencia Apache 2.0, formato Spotless, visibilidad).
     * Comandos Maven ejecutados para validar localmente.
   - **3. Evidencia de Solución Local:**
     * Log de `mvn spotless:check` arrojando SUCCESS.
     * Log de `mvn clean install` arrojando BUILD SUCCESS en todos los submódulos.

---

### REGISTRO DE EVIDENCIAS

En `gson_fork00\bitacora\03_corridas_agente\diagnostico_ci_github\` guarda:
1. `prompt_utilizado.md`: El contenido exacto de este prompt.
2. `informe_fallo_ci_github.md`: El informe de diagnóstico y solución para la auditoría de CI/CD.
