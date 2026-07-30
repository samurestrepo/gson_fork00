# PROMPT PARA OPENCODE - TAREA 3 (CORRIDA 1): GENERACIÓN Y EJECUCIÓN REAL DE PRUEBAS

**Rol:** Actúa como Desarrollador Senior de Software y Especialista en Testing Java.

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`. NO crees carpetas adicionales en la raíz.

**Objetivo:** 
Crear la clase de prueba `ReflectiveTypeAdapterFactoryBoundaryTest.java` en la ruta `gson/src/test/java/com/google/gson/functional/` que evalúe los casos límite de `ReflectiveTypeAdapterFactory`.

**Requisitos Técnicos del Código:**
1. Usa **JUnit 4** e **Google Truth** (`assertThat(...)`).
2. Implementa casos de prueba para:
   - Colisión de `@SerializedName` en herencia (debe esperar `IllegalArgumentException`).
   - Evaluación de `ReflectionAccessFilter.BLOCK_ALL` (debe esperar `JsonIOException`).

**PASO OBLIGATORIO DE EJECUCIÓN EN CONSOLA:**
Una vez creado el archivo Java, ejecuta en la terminal del proyecto:
`mvn test -pl gson -Dtest=ReflectiveTypeAdapterFactoryBoundaryTest`

*(Nota: Usa el parámetro `-pl gson` para evitar fallos de resolución en submódulos JPMS).*

**Registro de Evidencias:**
Crea la subcarpeta `gson_fork00\bitacora\03_corridas_agente\corrida_01\` y guarda:
1. `prompt_corrida_01.md`: Este prompt exacto.
2. `ejecucion.log`: Log textual y completo de la consola al ejecutar el comando `mvn test`.
3. `codigo_generado\ReflectiveTypeAdapterFactoryBoundaryTest.java`: Copia del código creado.
4. `verificacion_humana_01.md`: Informe de verificación humana indicando si la prueba compiló y pasó a la primera (`BUILD SUCCESS`), o los errores encontrados.
