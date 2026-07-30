# PROMPT PARA OPENCODE - TAREA 3 (CORRIDA 2): ANÁLISIS DE SEGURIDAD Y SUITE COMPLETA

**Rol:** Actúa como Especialista en Ciberseguridad Java y Auditor de Código.

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`.

**Objetivo:** 
Analizar el método `getBoundFields()` en `ReflectiveTypeAdapterFactory.java` buscando posibles vulnerabilidades de elusión (*bypass*) de `ReflectionAccessFilter` al navegar en jerarquías profundas mediante el bucle `while (raw != Object.class)`.

**PASO OBLIGATORIO DE EJECUCIÓN EN CONSOLA:**
Para verificar la estabilidad y que no existan regresiones en el módulo principal, ejecuta:
`mvn test -pl gson`

**Registro de Evidencias:**
Crea la subcarpeta `gson_fork00\bitacora\03_corridas_agente\corrida_02\` y guarda:
1. `prompt_corrida_02.md`: Este prompt exacto.
2. `ejecucion.log`: Log textual de la consola con los resultados globales de Maven (`Tests run`, `Failures`, `Errors`).
3. `analisis_generado.md`: El informe técnico de seguridad detallando si la vulnerabilidad existe o no.
4. `verificacion_humana_02.md`: Evaluación del auditor confirmando la validez del análisis y el estado de la suite de pruebas.
