# 📁 Documento de Contexto para Interventoría Técnica

## 1. Resumen General del Proyecto

* **Nombre / Repositorio:** Google Gson v2.14.1-SNAPSHOT (fork)
* **Stack Tecnológico:** Java 8+ (source), Java 11+ (tests), Maven multi-módulo, Error Prone (lint estático), Spotless (Google Java Style), JUnit 4 + Google Truth (testing), ProGuard/R8 (shrinking), OSGi, JPMS
* **Propósito del Sistema:** Biblioteca de serialización/deserialización JSON ↔ Java. Permite convertir objetos Java arbitrarios a su representación JSON y viceversa, usando un sistema extensible de TypeAdapters con soporte para anotaciones, genéricos y estrategias de reflexión.
* **Arquitectura:** Basada en **cadena de TypeAdapterFactories** (Chain-of-Responsibility). El flujo es: `GsonBuilder` → `Gson` (orquestador + caché thread-safe) → `TypeAdapterFactory` → `TypeAdapter` (lectura/escritura). Soporta dos modelos de representación JSON: streaming (`JsonReader`/`JsonWriter`) y árbol DOM (`JsonElement`/`JsonObject`/`JsonArray`/`JsonPrimitive`/`JsonNull`). Los módulos son: `gson` (core), `extras`, `proto`, `metrics`, `test-jpms`, `test-graal-native-image`, `test-shrinker`.

## 2. Módulo Crítico Seleccionado

* **Ruta/Ubicación del Módulo:** `gson/src/main/java/com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java` (673 líneas)
* **Justificación de Criticidad:** Este módulo constituye el **corazón del mecanismo de serialización automática** de Gson. Es quien procesa cualquier objeto Java arbitrario del que no exista un TypeAdapter registrado, usando reflexión para inspeccionar sus campos y construir adaptadores dinámicamente. Su complejidad ciclomática es alta (~40 puntos de ramificación), maneja lógica sensible de seguridad (acceso reflectivo a campos/constructores, filtros de acceso, `Unsafe`), y gestiona casos límite como herencia, tipos anidados, clases estáticas internas, records (Java 16+), tipos genéricos, anotaciones cíclicas, arrays, colecciones, y control de duplicados. Una falla aquí compromete la totalidad de las operaciones `toJson()`/`fromJson()` del sistema para tipos no registrados explícitamente (el caso de uso más común). Depende de ~10 clases internas (ConstructorConstructor, Excluder, GsonTypes, ReflectionHelper, etc.) y es consumido por el flujo principal de Gson.
* **Responsabilidad Principal:** Crear dinámicamente un `TypeAdapter` para cualquier tipo de objeto Java (no primitivo) utilizando reflexión sobre sus campos, procesando anotaciones (`@SerializedName`, `@JsonAdapter`, `@Expose`, `@Since`/`@Until`), resolviendo tipos genéricos, y manejando correctamente la serialización/deserialización incluyendo herencia, records, y modificación de acceso.

## 3. Radiografía Técnica del Módulo Crítico

### Componentes / Clases / Funciones clave

| Clase/Componente | Rol | Métodos principales |
|---|---|---|
| **`ReflectiveTypeAdapterFactory`** | Factory principal que implementa `TypeAdapterFactory.create()`. Decide si puede manejar el tipo y delega la construcción del adapter. | `create(Gson, TypeToken)`, `getBoundFields(...)`, `createBoundField(...)`, `includeField(Field, boolean)`, `getFieldNames(Field)`, `checkAccessible(Object, Member)` |
| **`Adapter<T, A>`** | Clase base abstracta para los adapters producidos; hereda de `TypeAdapter<T>`. Maneja el bucle de serialización/deserialización. | `write(JsonWriter, T)`, `read(JsonReader)`, `createAccumulator()` (abstracto), `readField(A, JsonReader, BoundField)` (abstracto), `finalize(A)` (abstracto) |
| **`FieldReflectionAdapter<T>`** | Implementación concreta de `Adapter` para clases Java normales. Usa un `ObjectConstructor<T>` para construir instancias y escribe/lee campos vía reflexión. | `createAccumulator()`, `readField(...)`, `finalize(...)` |
| **`RecordAdapter<T>`** | Implementación concreta de `Adapter` para Java Records. Construye instancias usando el constructor canónico y pasa argumentos en orden. | `createAccumulator()` (clona defaults), `readField(...)` (mapea field → índice constructor), `finalize(...)` (invoca constructor con `newInstance`) |
| **`BoundField`** | Clase abstracta interna que representa el binding entre un campo Java y su nombre JSON. Cada campo serializable/deserializable produce una instancia. | `write(JsonWriter, Object)`, `readIntoArray(JsonReader, int, Object[])`, `readIntoField(JsonReader, Object)` |
| **`FieldsData`** | Contenedor con los mapas de campos serializables y deserializables. | `EMPTY` (singleton), constructor |

### Flujo de Datos y Firmas de Métodos

**Entrada al módulo:**
```
ReflectiveTypeAdapterFactory.create(Gson gson, TypeToken<T> type) → TypeAdapter<T> | null
```
- `type`: Tipo Java a serializar/deserializar (nunca null).
- Retorna `null` si `rawType` es primitivo (delega en otro factory).
- Retorna un adapter dummy (null-safe) para clases anónimas/locales no estáticas.
- Retorna `RecordAdapter<T>` si `rawType` es un Java Record.
- Retorna `FieldReflectionAdapter<T>` para cualquier otro objeto.

**Flujo de deserialización (`read`):**
```
JsonReader (JSON tokens) → Adapter.read(JsonReader)
  → Adapter.createAccumulator()
  → beginObject()
    → loop: in.nextName() → lookup in deserializedFields map
      → BoundField.readIntoField(reader, target) o readIntoArray(reader, index, target)
      → typeAdapter.read(reader) [delegación recursiva]
  → endObject()
  → Adapter.finalize(accumulator) → T
```

**Flujo de serialización (`write`):**
```
T (Java object) → Adapter.write(JsonWriter, T)
  → out.beginObject()
    → loop over serializedFields:
      → BoundField.write(writer, source)
        → accessor.invoke(source) O field.get(source)
        → writer.name(serializedName)
        → writeTypeAdapter.write(writer, fieldValue)
  → out.endObject()
```

**Parámetros de `createBoundField`:**
- `Gson context` - instancia de Gson para obtener adapters hijos
- `Field field` - campo reflectivo
- `Method accessor` - método accessor (solo para Records)
- `String serializedName` - nombre JSON del campo
- `TypeToken<?> fieldType` - tipo resuelto del campo (incluyendo genéricos)
- `boolean serialize` - si debe incluirse en serialización
- `boolean blockInaccessible` - si debe bloquear acceso reflectivo

**Excepciones lanzadas:**
- `JsonIOException`:
  - `ReflectionAccessFilter` bloquea reflexión para el tipo (BLOCK_ALL)
  - Campo/constructor no accesible y no se permite hacerlo accesible
  - Accessor lanza excepción (`InvocationTargetException`)
  - Intento de setear campo `static final`
  - `@SerializedName` en accessor method de Record (no soportado)
- `JsonParseException`: valor null para componente de Record de tipo primitivo
- `JsonSyntaxException` (envuelve `IllegalStateException`): error de parseo en `read()`
- `IllegalArgumentException`: nombres JSON duplicados entre campos
- `RuntimeException`: error al invocar constructor de Record (`InstantiationException`, `IllegalArgumentException`, `InvocationTargetException`)

## 4. Matriz de Variables y Reglas de Negocio (Para Diseño de Pruebas)

### Parámetros y campos evaluados en `getBoundFields`

| Parámetro/Campo | Reglas de validación | Valores límite / observaciones |
|---|---|---|
| `Class<?> raw` (tipo raw) | Si es `Object.class` → detiene recorrido de herencia. Si es interface → retorna `FieldsData.EMPTY`. | **Límite**: recorrido jerarquía hasta `Object.class` inclusive. |
| `Field[] fields` (campos declarados) | Cada campo: evaluar `includeField(f, serialize)`. Para herencia con `raw != originalRaw`: re-evaluar `ReflectionAccessFilter`. | **Límite**: clase sin campos (vacío), clase con 1 campo, clase con N campos, clase con campos duplicados por herencia. |
| `serializedName` (nombre JSON) | Se obtiene de `@SerializedName.value` o `fieldNamingPolicy.translateName()`. Alternates de `@SerializedName.alternate()` o `fieldNamingPolicy.alternateNames()`. | **Límite**: `@SerializedName` ausente, cadena vacía, espacios, caracteres especiales, Unicode. |
| `Modifier.isStatic(field)` (en Record) | Si es static → `deserialize = false` (se ignora para deserialización). | **Partición**: campo static vs campo de instancia. |
| `GsonTypes.resolve(...)` (tipo genérico) | Resuelve el tipo del campo considerando parámetros de tipo de la clase contenedora. | **Límite**: tipo simple, tipo parametrizado (`List<String>`), tipo variable (`T`), tipo wildcard (`? extends X`), tipo recursivo. |
| `ReflectionAccessFilter` | `BLOCK_ALL` → error. `BLOCK_INACCESSIBLE` → verifica acceso. `ALLOW` → permite `setAccessible`. | **Particiones**: `ALLOW`, `BLOCK_INACCESSIBLE`, `BLOCK_ALL` para tipo y supertipos. |
| `isRecord` (flag) | Modifica comportamiento: usa accessor method en vez de field.get(). Para constructor usa `ReflectionHelper.getCanonicalRecordConstructor()`. | **Límite**: Record con 0 componentes, 1 componente, N componentes, Record con genéricos, Record con herencia. |
| `duplicate field names` | Si dos campos mapean al mismo nombre JSON → `IllegalArgumentException`. | **Invalido**: `@SerializedName` duplicado, herencia con mismo nombre. |

### Valores Límites Identificados

| Tipo de entrada | Valores válidos | Valores inválidos / límite |
|---|---|---|
| Tipo de objeto (`rawType`) | Toda clase que extiende `Object` y no es primitiva, anónima, local no estática, ni bloqueada por filtro. | `null` (no llega porque es checked en `Gson.getAdapter`); primitivos (`int`, `boolean`, etc.); clase anónima; clase local no estática. |
| Campos (`Field`) | Campos con visibilidad pública, protegida, paquete, privada. Campos static, transient. | Campos `static final` (no se pueden setear por reflexión → `JsonIOException`). Campos sintéticos (excluidos por `Excluder`). |
| Nombres JSON (`serializedName`) | Cadena no nula; puede contener cualquier carácter Unicode. Para alternates: lista de Strings. | `""` (vacío); nombres que colisionan entre campos. |
| Valor de campo (`fieldValue`) | Cualquier objeto Java incluyendo null. | Para primitivas: `null` en deserialización de Records → `JsonParseException`. |
| Anotaciones | `@SerializedName`, `@JsonAdapter`, `@Expose` (vía Excluder), `@Since`, `@Until` (vía Excluder). | `@SerializedName` en accessor method de Record sin anotación en field → `JsonIOException`. |
| Tipos parametrizados | `List<String>`, `Map<K,V>`, tipos recursivos (`Node<T>`), wildcards. | Tipos mal formados (raw types sin parámetros). |
| Ciclos/grafos | Referencias circulares manejadas por `FutureTypeAdapter` + `ThreadLocal`. | Objeto que se referencia a sí mismo directamente → el adapter omite escritura (`fieldValue == source` check). |

### Particiones de Equivalencia

| Clase válida | Clase inválida |
|---|---|
| Clase con constructor sin args (público o accesible) | Clase abstracta o interfaz |
| Clase con campos de tipos básicos (String, int, boolean) | Clase con campos `static final` → error en deserialización |
| Clase con herencia (campos en superclase) | Clase anónima o local no estática → adapter null-safe |
| Java Record con N componentes | Record con constructor privado (acceso denegado) |
| Clase con `@SerializedName` en campos | Clase con dos campos con mismo `@SerializedName` → excepción |
| Clase con genéricos (`Container<T>`) | Clase filtrada por `ReflectionAccessFilter.BLOCK_ALL` |
| Clase con `@JsonAdapter` en campo | Clase con `requireExpose` activado y campo sin `@Expose` |
| Objeto con valor null en campo no primitivo | Primitiva con null en deserialización de Record |
| Clase sin campos (vacia) | Referencia circular directa (`field == source`) |

## 5. Prerequisitos y Entorno de Ejecución (Para Agente de IA / Test Suite)

### Requisitos de Entorno

- **JDK:** Java 17+ (requerido para compilar, compatibilidad Java 8 source)
- **Build:** Maven 3.9+ (proyecto multi-módulo)
- **Dependencias de prueba:** JUnit 4.13.2, Google Truth 1.4.5, Guava 33.6.0-jre
- **OS:** Independiente (Windows/Linux/macOS)
- **Memoria:** 512 MB heap mínimo (recomendado 2 GB para suite completa)

### Datos de Prueba / Fixtures Necesarios

Para probar `ReflectiveTypeAdapterFactory` en aislamiento se requiere:

1. **Clases de prueba** que cubran:
   - Objetos simples (POJO con constructor default + getters/setters)
   - Objetos con herencia (clase base + subclase)
   - Objetos con genéricos (`class Box<T> { T content; }`)
   - Objetos con anotaciones (`@SerializedName(alternate = [...])`, `@JsonAdapter`, `@Expose`)
   - Records Java (`record Point(int x, int y)`)
   - Clases sin constructor sin args
   - Clases con campos `static final`
   - Clases con referencias circulares
   - Clases con tipos complejos anidados (`Map<String, List<CustomType>>`)

2. **Archivos JSON** de ejemplo (strings): representaciones válidas e inválidas para cada fixture.

### Mocks Necesarios

| Dependencia | Tipo de mock | Razón |
|---|---|---|
| `Gson` | Mock/Stub completo | Dependencia principal para obtener adapters hijos vía `getAdapter()` |
| `ConstructorConstructor` | Mock parcial | Puede usarse real pero aislar la creación de instancias facilita testing |
| `Excluder` | Stub | Fácil de instanciar (tiene constantes como `Excluder.DEFAULT`) |
| `FieldNamingStrategy` | Stub/Lambda | Función simple `Field → String` |
| `JsonAdapterAnnotationTypeAdapterFactory` | Mock | Para probar `@JsonAdapter` sin depender del pipeline completo |
| `ReflectionAccessFilter` | Stub | Lista de filtros; se puede pasar `Collections.emptyList()` |
| `JsonReader` / `JsonWriter` | Reales (sin mock) | Son de bajo nivel y baratos de instanciar |

### Configuración Mínima de Prueba

```java
// Setup mínimo para probar el factory de forma aislada
Gson gson = new Gson(); // o mock
ConstructorConstructor constructorConstructor = new ConstructorConstructor(
    Collections.emptyMap(), true, Collections.emptyList());
Excluder excluder = Excluder.DEFAULT;
FieldNamingStrategy fieldNamingPolicy = FieldNamingPolicy.IDENTITY;
JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory =
    new JsonAdapterAnnotationTypeAdapterFactory(constructorConstructor);

ReflectiveTypeAdapterFactory factory = new ReflectiveTypeAdapterFactory(
    constructorConstructor,
    fieldNamingPolicy,
    excluder,
    jsonAdapterFactory,
    Collections.emptyList());

TypeAdapter<MyPOJO> adapter = factory.create(gson, TypeToken.get(MyPOJO.class));
// Luego: adapter.toJson(myPojo) / adapter.fromJson(jsonString)
```

### Variables de Entorno Relevantes

- Ninguna variable de entorno específica es requerida.
- Opcional: `-Dgson.useJdkUnsafe=false` para probar sin `Unsafe` (configurable vía `GsonBuilder`).
- Opcional: `--add-opens java.base/java.lang=ALL-UNNAMED` para JVM > 16 si se prueba con reflexión profunda.
