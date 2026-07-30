# PROMPT PARA OPENCODE - TAREA 1: INSPECCIÓN FORMAL DE MÓDULO CRÍTICO (AUTO-REPARABLE)

**Rol:** Actúa como un Comité Evaluador e Interventor Técnico de Calidad de Software (ISO/IEC 25010 e ISO/IEC 5055).

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`. NO crees carpetas adicionales en la raíz.

---

### PASO 1: GARANTIZAR COMPILACIÓN E INSTALACIÓN DE MÓDULOS (OBLIGATORIO)

Para resolver las dependencias del reactor multimódulo y JPMS (`module not found: com.google.gson`), ejecuta inmediatamente:

1. Primero intenta la instalación y compilación global:
   `mvn clean install -DskipTests`

2. Si la orden anterior presenta algún conflicto secundario en submódulos de pruebas integradas (como GraalVM o ProGuard), ejecuta la compilación focalizada sobre el módulo núcleo:
   `mvn clean test-compile -pl gson`

3. Confirma que el resultado final sea **BUILD SUCCESS**.

---

### PASO 2: INSPECCIÓN FORMAL DEL MÓDULO CRÍTICO

Una vez asegurada la compilación, realiza la inspección técnica estática del archivo:
`gson/src/main/java/com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java`

Evalúa y documenta los siguientes 4 puntos:
1. **Complejidad Ciclomática y Riesgos:** Puntos de ramificación en `create()`, `getBoundFields()` y `createBoundField()`.
2. **Seguridad y Accesibilidad Reflectiva:** Gestión de `ReflectionAccessFilter`, `setAccessible()` y compatibilidad con JPMS / Java 17+.
3. **Manejo de Tipos Modernos:** Robustez del soporte de Java Records (`RecordAdapter`) y resolución de genéricos (`GsonTypes.resolve`).
4. **Matriz de Hallazgos:** Presenta la tabla formal:
   `| Criterio | Observación Técnica | Nivel de Riesgo (Bajo/Medio/Alto) |`

---

### REGISTRO DE EVIDENCIAS:

Crea la subcarpeta `gson_fork00\bitacora\01_inspeccion_formal\` y guarda obligatoriamente:
1. `prompt_utilizado.md`: El contenido exacto de este prompt.
2. `informe_inspeccion_formal.md`: El informe final de inspección que INCLUYA al inicio el log exitoso (`BUILD SUCCESS`) del comando Maven ejecutado y la matriz de hallazgos.
