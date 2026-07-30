# PROMPT PARA OPENCODE - TAREA 2: DISEÑO DE CASOS DE PRUEBA

**Rol:** Actúa como un Ingeniero de Pruebas de Software y Auditor de Calidad.

**UBICACIÓN DE TRABAJO:**
Usa la carpeta existente `gson_fork00\bitacora`. NO crees carpetas de bitácora adicionales en la raíz.

**VERIFICACIÓN DE ENTORNO PREVIA:**
Antes de diseñar, confirma la compilación del módulo principal ejecutando:
`mvn test-compile -pl gson`
*(Si requiere dependencias de proyecto, ejecuta primero `mvn clean install -DskipTests`)*.

**Objetivo:** Diseñar entre 5 y 8 casos de prueba especializados para `ReflectiveTypeAdapterFactory.java` aplicando **Particiones de Equivalencia** y **Análisis de Valores Límite**.

**Instrucciones de Diseño:**
1. Analiza el código fuente en `gson/src/main/java/com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java`.
2. Identifica puntos de quiebre en:
   - Duplicación de `@SerializedName` en jerarquías de herencia.
   - Componentes `null` en Java Records primitivos.
   - Restricciones de `ReflectionAccessFilter` (`BLOCK_ALL`, `BLOCK_INACCESSIBLE`).
   - Campos `static final` y autorreferencias (grafos cíclicos).
   - Tipos genéricos parametrizados (`Box<T>`).

**Regla de Dominio Estricta:**
Si el contrato de la clase no define explícitamente el comportamiento para un caso específico o es ambiguo, márcalo explícitamente como **'REQUISITO AMBIGUO'** en la columna de resultado esperado.

**Estructura de Salida Requerida:**
Presenta la batería de pruebas exclusivamente en la siguiente tabla Markdown:
| ID | Entrada / Precondición | Resultado esperado | Técnica aplicada | Decisión del Comité |

**Registro de Evidencias:**
Crea la subcarpeta `gson_fork00\bitacora\02_diseno_casos_prueba\` y guarda:
1. `prompt_utilizado.md`: Contenido exacto de este prompt.
2. `bateria_casos_prueba.md`: Documento formal con el análisis técnico y la tabla resultante.
