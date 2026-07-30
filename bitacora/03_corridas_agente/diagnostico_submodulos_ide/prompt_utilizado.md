# PROMPT PARA OPENCODE - DIAGNÓSTICO DE ERRORES RESIDUALES EN SUBMÓDULOS (GSON, TEST-JPMS, TEST-GRAAL)

**Rol:** Actúa como Especialista en Herramientas de IDE Java y Diagnóstico de Espacios de Trabajo Multi-Módulo.

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`.

---

### OBJETIVO
Inspeccionar las carpetas `gson`, `test-jpms` y `test-graal-native-image` para identificar qué advertencias o errores específicos está reportando el servidor de lenguaje (LSP), confirmar que la compilación de Maven pasa sin problemas en las tres carpetas y documentar el hallazgo.

---

### PASOS A EJECUTAR

1. **VERIFICACIÓN MULTIMÓDULO FOCALIZADA EN CONSOLA:**
   Ejecuta la compilación de los tres submódulos involucrados para reconfirmar el estado real del proyecto:
   `mvn clean test-compile -pl gson,test-jpms,test-graal-native-image`

2. **INSPECCIÓN DE ARCHIVOS CONFIGURATIVOS:**
   - Revisa `test-jpms/src/test/java/module-info.java` y confirma que la visibilidad del módulo `com.google.gson` esté correcta.
   - Revisa si `test-graal-native-image` requiere algún ajuste en su ruta de clases (`classpath`) de pruebas.

3. **REGISTRO Y DOCUMENTACIÓN:**
   Crea la subcarpeta `gson_fork00\bitacora\03_corridas_agente\diagnostico_submodulos_ide\` y guarda el archivo `informe_diagnostico_submodulos.md` detallando:
   - Estado de la compilación en consola (`BUILD SUCCESS`).
   - Explicación técnica de por qué el IDE marca alertas visuales en módulos JPMS y GraalVM (desincronización de caché/referencias cruzadas inter-módulo).
   - Confirmación de que no existen errores reales de código.

---

### REGISTRO DE EVIDENCIAS

En `gson_fork00\bitacora\03_corridas_agente\diagnostico_submodulos_ide\` guarda:
1. `prompt_utilizado.md`: Este prompt exacto.
2. `informe_diagnostico_submodulos.md`: El informe técnico.
