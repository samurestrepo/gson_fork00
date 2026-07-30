# INFORME DE INSPECCIÓN FORMAL DE MÓDULO CRÍTICO

**Proyecto:** Gson (com.google.code.gson) v2.14.1-SNAPSHOT  
**Módulo inspeccionado:** `ReflectiveTypeAdapterFactory.java`  
**Ubicación:** `gson/src/main/java/com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java`  
**Estándares de referencia:** ISO/IEC 25010, ISO/IEC 5055  
**Fecha:** 2026-07-29 15:21 UTC-5  
**Comité Evaluador:** opencode — Interventor Técnico de Calidad de Software

---

## 1. VERIFICACIÓN DE COMPILACIÓN

### Comando ejecutado
```
mvn clean install -DskipTests
```

### Salida exitosa del reactor Maven
```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Gson Parent 2.14.1-SNAPSHOT:
[INFO]
[INFO] Gson Parent ........................................ SUCCESS [ 17.285 s]
[INFO] Gson ............................................... SUCCESS [01:12 min]
[INFO] Test: Java Platform Module System (JPMS) ........... SUCCESS [  7.827 s]
[INFO] Test: GraalVM Native Image ......................... SUCCESS [  2.403 s]
[INFO] Test: Code shrinking (ProGuard / R8) ............... SUCCESS [ 39.604 s]
[INFO] Gson Extras ........................................ SUCCESS [ 11.669 s]
[INFO] Gson Metrics ....................................... SUCCESS [  8.091 s]
[INFO] Gson Protobuf Support .............................. SUCCESS [ 19.222 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:59 min
[INFO] Finished at: 2026-07-29T15:21:16-05:00
```

**Resultado: BUILD SUCCESS** — Los 8 módulos del reactor compilaron sin errores.  
Se instalaron los artefactos en el repositorio local Maven (~/.m2), resolviendo las dependencias JPMS.

---

## 2. INSPECCIÓN ESTÁTICA — ANÁLISIS TÉCNICO

### 2.1 Complejidad Ciclomática y Riesgos

#### Método `create()` (líneas 110-166)
| Punto de ramificación | Línea | Descripción |
|-----------------------|-------|-------------|
| `!Object.class.isAssignableFrom(raw)` | 113 | Rechaza tipos primitivos → return null |
| `ReflectionHelper.isAnonymousOrNonStaticLocal(raw)` | 119 | Adapter nulo para clases anónimas/locales |
| `filterResult == FilterResult.BLOCK_ALL` | 144 | Lanza JsonIOException si el filtro bloquea todo |
| `ReflectionHelper.isRecord(raw)` | 154 | Desvía a RecordAdapter |
| Default | 163-165 | FieldReflectionAdapter con constructor |

**McCabe ciclomático:** 5 (bajo-moderado)  
**Observación:** Estructura lineal con 4 puntos de decisión en secuencia. Sin anidamiento profundo. Riesgo bajo.

#### Método `getBoundFields()` (líneas 320-429)
| Punto de ramificación | Línea | Descripción |
|-----------------------|-------|-------------|
| `raw.isInterface()` | 322 | Retorno temprano |
| `while (raw != Object.class)` | 332 | Loop de jerarquía de herencia |
| `raw != originalRaw && fields.length > 0` | 336 | Verifica filtro en supertipos |
| `filterResult == FilterResult.BLOCK_ALL` | 339 | Bloqueo de supertipo |
| `!serialize && !deserialize` → continue | 353 | Salta campos excluidos |
| `isRecord` → `Modifier.isStatic` | 366 | Manejo de campos estáticos en records |
| `!blockInaccessible` (accessor) | 371 | makeAccessible condicional |
| `accessor.hasAnnotation && !field.hasAnnotation` | 379 | @SerializedName en accessor |
| `!blockInaccessible && accessor == null` | 391 | makeAccessible condicional field |
| `deserialize` → loop + null check | 408-416 | Registro + detección duplicados |
| `serialize` → put + null check | 418-423 | Registro + detección duplicados |

**McCabe ciclomático estimado:** 14-16 (alto)  
**Riesgos identificados:**
- **ALTO**: Mutación del parámetro `type` dentro del bucle (línea 425: `type = TypeToken.get(...)`). Esto es una mala práctica que puede confundir el análisis de flujo y ocasionar comportamientos inesperados si el tipo genérico resuelto no es el esperado.
- **MEDIO**: Reasignación de `blockInaccessible` según el filtro del supertipo (línea 347). Un supertipo con `BLOCK_INACCESSIBLE` puede sobrescribir `BLOCK_ALL` del tipo original, creando una falsa sensación de seguridad.
- **MEDIO**: El bucle `while` puede procesar toda la jerarquía de clases; en jerarquías profundas el rendimiento podría degradarse.

#### Método `createBoundField()` (líneas 181-289)
| Punto de ramificación | Línea | Descripción |
|-----------------------|-------|-------------|
| `annotation != null` | 197 | @JsonAdapter presente |
| `mapped == null` | 204 | Fallback a adapter por defecto |
| `serialize` | 211 | Selecciona writeTypeAdapter |
| `blockInaccessible` (write) | 223 | Verificación accesso reflectivo |
| `accessor == null` (write) | 224 | Usa field o accessor |
| `accessor != null` (write) | 234 | Invoca accessor method |
| `InvocationTargetException` catch | 237 | Manejo de excepción reflectiva |
| `fieldValue == source` (write) | 249 | Protección recursión directa |
| `fieldValue == null && isPrimitive` (readIntoArray) | 261 | Validación null primitivo record |
| `fieldValue != null \|\| !isPrimitive` (readIntoField) | 275 | Setteo condicional |
| `blockInaccessible` (readIntoField) | 276 | Verificación accesso |
| `isStaticFinalField` (readIntoField) | 278 | Rechazo static final |

**McCabe ciclomático estimado:** 10-12 (moderado-alto, distribuido entre el método principal y 3 métodos anónimos)  
**Observación:** La creación de una **clase anónima** con 3 métodos abstractos (`write`, `readIntoArray`, `readIntoField`) concentra la complejidad en un solo punto. Refactorizar a una clase separada mejoraría la mantenibilidad. Riesgo **medio**.

---

### 2.2 Seguridad y Accesibilidad Reflectiva

#### ReflectionAccessFilter
- El filtro se evalúa en dos puntos: `create()` (línea 142-149) y `getBoundFields()` (línea 337-348) para supertipos.
- Tres estados: `BLOCK_ALL`, `BLOCK_INACCESSIBLE`, `ALLOW`.
- `BLOCK_ALL` lanza `JsonIOException` con mensaje informativo + enlace a guía.
- `BLOCK_INACCESSIBLE` difiere la verificación de accesibilidad a tiempo de ejecución.

#### setAccessible() / makeAccessible()
- `ReflectionHelper.makeAccessible()` se invoca bajo guarda `!blockInaccessible`:
  - Para accessor methods de records (línea 372)
  - Para fields (línea 392)
- **Riesgo MEDIO**: Bajo JPMS sin `--add-opens`, `makeAccessible()` lanzará `InaccessibleObjectException`. Aunque `BLOCK_INACCESSIBLE` está diseñado para este escenario, un filter mal configurado (`ALLOW`) combinado con un módulo cerrado causará fallo en tiempo de ejecución.

#### checkAccessible() (líneas 168-179)
- Verifica accesibilidad sin hacer `setAccessible`.
- Se usa en modo `BLOCK_INACCESSIBLE` tanto para fields como para accessor methods.

#### Método `checkAccessible` (líneas 168-179)
- Invoca `ReflectionAccessFilterHelper.canAccess()`.
- Aplica a `Member` genérico (Field o Method) y determina si el objeto destino es accesible.

**Hallazgo MEDIO**: En `getBoundFields()` línea 347, la reasignación de `blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE` para supertipos puede degradar el modo de seguridad. Si el tipo original está en modo `BLOCK_ALL` pero un supertipo reporta `BLOCK_INACCESSIBLE`, se pierde la protección.

---

### 2.3 Manejo de Tipos Modernos — Java Records y Genéricos

#### RecordAdapter (líneas 571-671)
- **Arquitectura correcta**: usa patrón Adapter<A> con acumulador `Object[]` para construir el record vía constructor canónico.
- **Mapeo field → componente**: usa `componentIndices` (HashMap<String, Integer>) poblado desde `ReflectionHelper.getRecordComponentNames()`.
- **Inicialización de primitivos**: `PRIMITIVE_DEFAULTS` (líneas 608-619) provee valores zero para tipos primitivos, previniendo NPEs en constructores de record.
- **Validación null en primitivos**: `readIntoArray()` (línea 261-267) lanza `JsonParseException` si se intenta deserializar null en un componente primitivo.

**Riesgos:**
- **MEDIO**: `finalize()` (línea 646-671) envuelve `InvocationTargetException` en `RuntimeException` en lugar de `JsonParseException`. Esto rompe la cadena de manejo de errores de Gson y puede ser confuso para el usuario.
- **BAJO**: `readField()` asume fieldName == componentName. Si un framework de ofuscación renombra fields pero no los componentes del record (o viceversa), el mapeo fallaría silenciosamente con `IllegalStateException`.

#### Resolución de Genéricos (líneas 395, 425)
- `GsonTypes.resolve(type.getType(), raw, field.getGenericType())`: resuelve el tipo parametrizado de cada field considerando la herencia.
- `GsonTypes.resolve(type.getType(), raw, raw.getGenericSuperclass())`: resuelve el supertipo parametrizado para continuar la navegación.
- **Correcto pero riesgoso**: La reasignación de `type` (línea 425) dentro del bucle `while` es frágil. Si la resolución del supertipo genérico falla o devuelve un tipo inesperado, la siguiente iteración podría usar un `raw` incorrecto.

---

## 3. MATRIZ DE HALLAZGOS

| Criterio | Observación Técnica | Nivel de Riesgo |
|----------|---------------------|-----------------|
| **CC-01** — Mutación de parámetro `type` en `getBoundFields()` línea 425 | El parámetro `type` se reasigna dentro del bucle `while`, lo que viola buenas prácticas de inmutabilidad y puede ocultar bugs de resolución genérica | **Alto** |
| **CC-02** — Alta complejidad ciclomática en `getBoundFields()` | ~14-16 puntos de decisión. Dificulta pruebas de cobertura completa y mantenimiento correctivo | **Medio** |
| **CC-03** — Clase anónima grande en `createBoundField()` | La clase anónima con 3 métodos (write, readIntoArray, readIntoField) concentra alta complejidad en una sola construcción | **Medio** |
| **CC-04** — Reasignación de `blockInaccessible` para supertipos | `getBoundFields()` línea 347: el modo de accesibilidad puede degradarse al procesar supertipos con filtros diferentes | **Medio** |
| **SEG-01** — JPMS: makeAccessible() sin --add-opens | Si el filter es ALLOW y el módulo no abre el paquete, makeAccessible() lanza InaccessibleObjectException en JPMS | **Medio** |
| **SEG-02** — Error reflectivo envuelto como RuntimeException | `RecordAdapter.finalize()` (línea 663) envuelve `InvocationTargetException` en RuntimeException en vez de JsonParseException, rompiendo la jerarquía de excepciones de Gson | **Medio** |
| **SEG-03** — Duplicados detectados en runtime | `createDuplicateFieldException()` (línea 305) lanza IllegalArgumentException solo hasta que se procesa un field duplicado; no hay validación previa | **Bajo** |
| **REC-01** — RecordAdapter: mapeo field → componente por nombre | Asume que `fieldName` coincide con `componentName`. Podría fallar con ofuscación. Por ahora es seguro porque Java Records garantiza esta correspondencia | **Bajo** |
| **REC-02** — Primitivos en Records: defaults correctos | `PRIMITIVE_DEFAULTS` maneja los 8 tipos primitivos. La clonación en `createAccumulator()` evita efectos laterales entre deserializaciones | **Bajo** |
| **REC-03** — @SerializedName en accessor methods | Detección temprana (línea 379-385) de anotación mal ubicada con mensaje claro. Buen control defensivo | **Bajo** |
| **GEN-01** — Resolución de genéricos correcta | Uso de `GsonTypes.resolve()` para field types y supertipos. La lógica es correcta aunque la mutación de `type` (CC-01) introduce fragilidad | **Bajo** |

---

## 4. CONCLUSIONES Y RECOMENDACIONES

### Resumen
- **Hallazgos Altos:** 1 (CC-01 — mutación de parámetro)
- **Hallazgos Medios:** 5 (CC-02, CC-03, CC-04, SEG-01, SEG-02)
- **Hallazgos Bajos:** 5 (SEG-03, REC-01, REC-02, REC-03, GEN-01)

### No conformidades ISO/IEC 25010
| Atributo de Calidad | Afectado por | Impacto |
|---------------------|--------------|---------|
| **Mantenibilidad (ISO 25010)** | CC-01, CC-02, CC-03 | Dificulta el análisis de impacto y las pruebas |
| **Fiabilidad (ISO 25010)** | CC-04, SEG-02 | Pueden ocurrir fallos inesperados bajo JPMS o con records |
| **Seguridad (ISO 25010)** | SEG-01, CC-04 | El bypass del modo BLOCK_ALL en supertipos reduce la protección |

### Recomendaciones
1. **Refactorizar `getBoundFields()`**: Extraer la lógica del bucle `while` a un método separado y evitar la mutación del parámetro `type` usando una variable local.
2. **Preservar `blockInaccessible`**: No reasignar el modo de accesibilidad para supertipos; usar la configuración más restrictiva.
3. **Corregir excepción en RecordAdapter**: Cambiar `RuntimeException` por `JsonParseException` en `finalize()`.
4. **Extraer clase anónima**: Convertir el BoundField anónimo en `createBoundField()` a una clase interna estática con nombre para mejorar legibilidad y testabilidad.
5. **Documentar prerequisitos JPMS**: Agregar documentación sobre los `--add-opens` requeridos para cada paquete cuando se usa Gson bajo JPMS.

---

*Informe generado por el Comité Evaluador e Interventor Técnico de Calidad de Software (ISO/IEC 25010 e ISO/IEC 5055)*
*Archivos en: `bitacora\01_inspeccion_formal\`*
