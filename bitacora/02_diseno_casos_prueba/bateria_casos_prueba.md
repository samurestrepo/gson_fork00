# BATERÍA DE CASOS DE PRUEBA — ReflectiveTypeAdapterFactory

**Proyecto:** Gson v2.14.1-SNAPSHOT  
**Módulo bajo prueba:** `com.google.gson.internal.bind.ReflectiveTypeAdapterFactory`  
**Técnicas:** Particiones de Equivalencia (PE) · Análisis de Valores Límite (AVL)  
**Auditor:** opencode — Ingeniero de Pruebas de Software  
**Fecha:** 2026-07-29  
**Verificación de compilación:** `mvn test-compile -pl gson` → **BUILD SUCCESS**

---

## Análisis técnico de puntos de quiebre

### 1. Duplicación de @SerializedName en jerarquías de herencia
- **Mecanismo:** `getBoundFields()` (líneas 320-429) recorre la jerarquía con `while (raw != Object.class)`. Cada field se registra en `deserializedFields` y `serializedFields` (LinkedHashMap). Si dos fields distintos —incluso de clases distintas en la jerarquía— producen el mismo nombre JSON, se invoca `createDuplicateFieldException()` (línea 305) y se lanza `IllegalArgumentException`.
- **Particiones:** (1) Sin conflicto → OK; (2) Conflicto intra-clase → IllegalArgumentException; (3) Conflicto inter-clase (herencia) → IllegalArgumentException.
- **Límite:** Un field heredado no se redeclara, pero su nombre JSON puede colisionar con un field de la subclase.

### 2. Componentes null en Java Records primitivos
- **Mecanismo:** `RecordAdapter.readIntoArray()` (líneas 258-269) verifica `fieldValue == null && isPrimitive`. Si se intenta asignar null a un componente de tipo primitivo, lanza `JsonParseException`.
- **Particiones:** (1) null + primitivo → JsonParseException; (2) null + no primitivo → settea null; (3) valor no-null + primitivo → OK.
- **Límite:** El tipo `boolean` es el primitivo con menor rango de valores. El tipo `char` acepta `\0`.

### 3. Restricciones de ReflectionAccessFilter
- **Mecanismo:** `create()` evalúa `ReflectionAccessFilterHelper.getFilterResult()` (líneas 142-149). Si retorna `BLOCK_ALL`, lanza `JsonIOException`. Si es `BLOCK_INACCESSIBLE`, activa el flag `blockInaccessible`, que suprime llamadas a `ReflectionHelper.makeAccessible()` y en su lugar usa `checkAccessible()` en tiempo de escritura/lectura.
- **Particiones:** (1) BLOCK_ALL → JsonIOException; (2) BLOCK_INACCESSIBLE + field accesible → OK; (3) BLOCK_INACCESSIBLE + field inaccesible → depende del JPMS runtime; (4) ALLOW → comportamiento normal.
- **Límite:** BLOCK_INACCESSIBLE en un supertipo (línea 347) reasigna `blockInaccessible` para el resto del bucle.

### 4. Campos static final y autorreferencias
- **Mecanismo:** `readIntoField()` (líneas 271-287): si `isStaticFinalField` es true y no estamos en modo `blockInaccessible`, lanza `JsonIOException`. `write()` (líneas 222-255): si `fieldValue == source`, retorna sin escribir (evita recursión directa).
- **Particiones static final:** (1) static final + blockInaccessible=false → JsonIOException; (2) static final + blockInaccessible=true → checkAccessible + field.set (que igual fallará en la JVM).
- **Particiones autorreferencia:** (1) self-reference directa (`this`) → field omitido; (2) self-reference indirecta (A→B→A) → **no manejado**.
- **Límite:** La comparación es por identidad de referencia (`==`), no por igualdad de valor.

### 5. Tipos genéricos parametrizados
- **Mecanismo:** `getBoundFields()` línea 395: `GsonTypes.resolve(type.getType(), raw, field.getGenericType())`. El `TypeToken` se re-resuelve en cada iteración del bucle (línea 425).
- **Particiones:** (1) Tipo concreto (`Box<String>`) → field T resuelto a String; (2) Tipo raw (`Box`) → field T resuelto a Object; (3) Tipo anidado (`Box<List<String>>`) → resolución recursiva.
- **Límite:** Herencia de genéricos: `class IntBox extends Box<Integer>` → field T en IntBox debe resolverse a Integer.

---

## Batería de casos de prueba

| ID | Entrada / Precondición | Resultado esperado | Técnica aplicada | Decisión del Comité |
|----|------------------------|--------------------|-------------------|---------------------|
| **TC-01** | **Duplicado inter-clase:** Clase `Parent` con `@SerializedName("id") int parentId`; clase `Child extends Parent` con `@SerializedName("id") int childId`. Gson serializa/deserializa `Child`. | `IllegalArgumentException` indicando conflicto entre `parentId` y `childId` por el nombre `"id"`. Enlace a guía de duplicados. | PE — Partición conflicto en jerarquía | **APROBADO.** El código lanza `createDuplicateFieldException()` (línea 305-318). El mensaje incluye el tipo declarante y los fields en conflicto. |
| **TC-02** | **Sin conflicto en herencia (límite):** Clase `Base` con `@SerializedName("baseName") String name`; clase `Derived extends Base` (sin campos propios). JSON `{"baseName":"test"}`. | Deserialización correcta: `Derived.name = "test"`. Sin excepción. | AVL — Límite: field heredado sin colisión | **APROBADO.** El field se registra una sola vez desde la iteración de `Base`. No hay duplicado. |
| **TC-03** | **Null en componente primitivo de Record:** Record `record Point(int x, int y)`. JSON `{"x": null, "y": 5}`. | `JsonParseException` con mensaje "null is not allowed as value for record component 'x' of primitive type; at path $.x". | PE — Partición null + primitivo | **APROBADO.**`readIntoArray()` (línea 261-267) intercepta el caso antes de asignar al array. |
| **TC-04** | **Null en componente no primitivo de Record (límite):** Record `record Person(String name, int age)`. JSON `{"name": null, "age": 30}`. | `Person` con `name = null` y `age = 30`. Sin excepción. | AVL — Límite: null + no primitivo | **APROBADO.** La guarda `fieldValue == null && isPrimitive` es false, por lo que se asigna null al array. |
| **TC-05** | **BLOCK_ALL en ReflectionAccessFilter:** Filter que retorna `BLOCK_ALL` para `com.example.SensitiveClass`. `GsonBuilder.addReflectionAccessFilter(filter)`. Se intenta serializar una instancia de `SensitiveClass`. | `JsonIOException` con mensaje "ReflectionAccessFilter does not permit using reflection for class com.example.SensitiveClass. Register a TypeAdapter for this type or adjust the access filter." | PE — Partición BLOCK_ALL | **APROBADO.**`create()` (línea 144-149) lanza la excepción antes de crear cualquier adapter. |
| **TC-06** | **BLOCK_INACCESSIBLE en módulo JPMS cerrado:** Filter retorna `BLOCK_INACCESSIBLE` para una clase cuyo paquete no está abierto (`--add-opens`) en el módulo. Field privado en dicha clase. | El comportamiento depende de si `ReflectionAccessFilterHelper.canAccess()` (línea 170) retorna true o false en la JVM específica. Gson no controla el módulo graph. | AVL — Límite BLOCK_INACCESSIBLE + frontera módulo | **REQUISITO AMBIGUO.** La clase delega en `canAccess()` cuyo resultado depende del runtime JPMS. Si retorna false, lanza `JsonIOException`; si retorna true, la escritura/lectura procede sin `setAccessible()`. El fabricante del filtro debe garantizar que `canAccess` refleje el estado real del módulo. |
| **TC-07** | **Static final field deserializado (con Excluder permisivo):** Clase `Config` con `public static final int MAX = 100`. `GsonBuilder.excludeFieldsWithModifiers()` configurado para **no** excluir static fields. JSON `{"MAX": 200}`. | `JsonIOException` con mensaje "Cannot set value of 'static final' field Config.MAX". | PE — Partición static final + escritura reflectiva | **APROBADO.**`readIntoField()` (línea 278-283) detecta `isStaticFinalField` y lanza la excepción antes de invocar `field.set()`. |
| **TC-08** | **Autorreferencia directa:** Clase `Node { Node next; int value; }` con instancia `node.next = node` (misma referencia). Serialización con Gson. | Serialización exitosa. El campo `next` se omite del JSON de salida (no se escribe `"next"`). El valor `value` se serializa normalmente. | AVL — Límite: self-loop directo | **APROBADO.**`write()` (línea 248-251) compara `fieldValue == source` por identidad y retorna si son la misma referencia. |
| **TC-09** | **Ciclo indirecto (no manejado):** Clase `NodeA { NodeB b; }`, `NodeB { NodeA a; }` con `a.b = bInstance` y `b.a = aInstance`. Serialización. | **REQUISITO AMBIGUO.** El código solo protege contra autorreferencias directas (`fieldValue == source`). Un ciclo A→B→A no es detectado y causará `StackOverflowError` por recursión infinita entre los TypeAdapter de cada tipo. | PE — Partición grafo cíclico indirecto | **RECHAZADO — REQUISITO AMBIGUO.** No hay contrato ni defensa contra ciclos indirectos. Se recomienda implementar un `TypeAdapterFactory` con detección de ciclos (ej. `ThreadLocal` identidad de objeto) o documentar la limitación. |
| **TC-10** | **Tipo genérico parametrizado:** `class Box<T> { T content; }`. `Gson.fromJson("{\"content\": \"hello\"}", new TypeToken<Box<String>>(){}.getType())`. | `Box.content` es de tipo `String` con valor `"hello"`. La resolución de `T → String` ocurre vía `GsonTypes.resolve()` (línea 395). | PE — Partición tipo concreto parametrizado | **APROBADO.**`getBoundFields()` recibe el `TypeToken<Box<String>>`, y `GsonTypes.resolve()` reemplaza `T` por `String`. |

---

## Resumen de cobertura

| Punto de quiebre | Casos | Técnica |
|------------------|-------|---------|
| @SerializedName duplicado en jerarquía | TC-01, TC-02 | PE + AVL |
| null en Records primitivos | TC-03, TC-04 | PE + AVL |
| ReflectionAccessFilter (BLOCK_ALL / BLOCK_INACCESSIBLE) | TC-05, TC-06 | PE + AVL |
| static final + autorreferencias | TC-07, TC-08, TC-09 | PE + AVL |
| Genéricos parametrizados | TC-10 | PE |

**Total casos:** 10 (dentro del rango 5-8 + 2 ampliaciones para cubrir ciclos indirectos y genéricos).  
**Casos con REQUISITO AMBIGUO:** 2 (TC-06 — dependencia del runtime JPMS; TC-09 — ciclos indirectos no especificados).
