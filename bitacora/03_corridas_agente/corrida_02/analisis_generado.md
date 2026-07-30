# Informe Técnico de Seguridad: Análisis de `getBoundFields()` en `ReflectiveTypeAdapterFactory`

## Alcance

- **Archivo analizado:** `gson/src/main/java/com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java`
- **Método objetivo:** `getBoundFields()` (líneas 320-429)
- **Vector de ataque analizado:** Bypass de `ReflectionAccessFilter` mediante navegación en jerarquías profundas vía `while (raw != Object.class)`

## Resumen de Hallazgos

**No se encontró una vulnerabilidad explotable** que permita eludir `ReflectionAccessFilter.BLOCK_ALL` o `BLOCK_INACCESSIBLE` en la implementación actual. A continuación se detalla el análisis completo.

---

## Anatomía del Control de Acceso

El sistema de filtros actúa en **dos niveles**:

### Nivel 1: Clase solicitada — Método `create()` (líneas 142-150)

```java
FilterResult filterResult =
    ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
if (filterResult == FilterResult.BLOCK_ALL) {
    throw new JsonIOException(
        "ReflectionAccessFilter does not permit using reflection for "
            + raw + ". Register a TypeAdapter for this type or adjust the access filter.");
}
boolean blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
```

- Se evalúa el filtro para la **clase concreta** que se va a serializar/deserializar.
- Si es `BLOCK_ALL`, se lanza `JsonIOException` inmediatamente.
- Si es `BLOCK_INACCESSIBLE`, se marca la bandera `blockInaccessible = true`.

### Nivel 2: Superclases — Bucle `while (raw != Object.class)` en `getBoundFields()` (líneas 331-348)

```java
Class<?> originalRaw = raw;
while (raw != Object.class) {
    Field[] fields = raw.getDeclaredFields();

    // For inherited fields, check if access to their declaring class is allowed
    if (raw != originalRaw && fields.length > 0) {
        FilterResult filterResult =
            ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
        if (filterResult == FilterResult.BLOCK_ALL) {
            throw new JsonIOException(
                "ReflectionAccessFilter does not permit using reflection for "
                    + raw + " (supertype of " + originalRaw
                    + "). Register a TypeAdapter for this type or adjust the access filter.");
        }
        blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
    }
```

- Solo se evalúa para **superclases** (`raw != originalRaw`).
- Solo se evalúa si la superclase **tiene campos declarados** (`fields.length > 0`).
- Si es `BLOCK_ALL`, se lanza `JsonIOException`.
- Si es `BLOCK_INACCESSIBLE`, se actualiza la bandera para ese nivel de jerarquía.

---

## Evaluación del Vector de Ataque

### Escenario hipotético de bypass

Se plantea que un atacante podría crear una subclase que extienda una clase bloqueada para evadir el filtro:

```java
// Clase sensible bloqueada por ReflectionAccessFilter
class ClaseSensible {
    String secreto = "clasificado";
}

// Subclase "inocente" que intenta evadir el filtro
class SubclaseInocente extends ClaseSensible {
    String dato = "publico";
}
```

**Flujo de ejecución:**

1. `create(SubclaseInocente)` evalúa el filtro para `SubclaseInocente` → `ALLOW` (porque el filtro solo bloquea `ClaseSensible`)
2. `getBoundFields()` recorre la jerarquía:
   - **Iteración 1:** `raw = SubclaseInocente (originalRaw)` → procesa campo `dato`
   - **Iteración 2:** `raw = ClaseSensible` → `raw != originalRaw` ✅, `fields.length > 0` ✅
     - Se evalúa el filtro → `BLOCK_ALL` → **SE LANZA JsonIOException** 🚫

**Conclusión:** El bypass NO funciona para `BLOCK_ALL`. La verificación en el Nivel 2 detecta que la superclase está bloqueada y lanza la excepción.

### Escenario con superclase sin campos (cases extremos)

El guarda `fields.length > 0` en línea 336 evita evaluar el filtro cuando una superclase no tiene campos declarados. ¿Es esto un riesgo?

```java
class Abuelo {
    String secreto = "clasificado";
}

class Padre extends Abuelo {
    // Sin campos propios
}

class Hijo extends Padre {
    String dato = "publico";
}
```

**Flujo con filtro que bloquea `Padre`:**

1. `create(Hijo)` → `ALLOW`
2. `getBoundFields()`:
   - Iteración 1: `raw = Hijo` → procesa `dato`
   - Iteración 2: `raw = Padre` → `raw != originalRaw` ✅, `fields.length == 0` → **filtro NO evaluado** ⚠️
   - Iteración 3: `raw = Abuelo` → evaluado normalmente → `secreto` procesado según su filtro

**Riesgo:** Si `Padre` tiene `BLOCK_ALL` pero **no tiene campos**, el filtro se salta. Sin embargo:
- No hay campos propios de `Padre` que proteger, por lo que no hay fuga de información.
- Los campos de `Abuelo` están protegidos por su propia evaluación de filtro en la iteración 3.
- **Conclusión:** Es una optimización segura, no una vulnerología.

### Escenario con `BLOCK_INACCESSIBLE`

El flag `blockInaccessible` se actualiza por cada superclase, pero SOLO si `fields.length > 0`:

```java
if (raw != originalRaw && fields.length > 0) {
    // ...
    blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
}
```

Si una superclase intermedia tiene `BLOCK_INACCESSIBLE` y `fields.length == 0`, el flag no se actualiza. Pero como la clase no tiene campos, no hay miembros sobre los cuales aplicar el flag. Al llegar a la siguiente superclase, el filtro se evalúa con su propio resultado. **No hay bypass.**

---

## Análisis de Casos de Prueba Relacionados

El archivo `ReflectionAccessFilterTest.java` (532 líneas) incluye pruebas exhaustivas para:
- `BLOCK_ALL_JAVA` (línea 129)
- `BLOCK_ALL` con serializer parcial (línea 411)
- `BLOCK_ALL` con interfaces de colección (línea 450)
- Delegación de filtros con herencia (línea 204)
- `BLOCK_INACCESSIBLE` (línea 127)

Ninguna de estas pruebas logra eludir el filtro, lo que respalda el análisis de que **no existe un bypass explotable**.

---

## Vulnerabilidades Potenciales Identificadas (No explotables)

| ID | Descripción | Severidad | Explotable |
|----|-------------|-----------|------------|
| V1 | Salto de filtro `BLOCK_ALL` en superclase sin campos (`fields.length == 0`) | Baja | No — no hay datos que proteger |
| V2 | `blockInaccessible` no actualizado en superclase sin campos | Baja | No — no hay miembros que requieran el flag |
| V3 | El filtro solo se evalúa para la clase concreta en `create()`, no para toda la jerarquía | Media | No — compensado por verificación en `getBoundFields()` |

---

## Conclusión Final

**No se encontró una vulnerabilidad explotable de bypass de `ReflectionAccessFilter`** en el método `getBoundFields()`.

La implementación:
1. Verifica `BLOCK_ALL` para la clase solicitada en `create()` (Nivel 1).
2. Verifica `BLOCK_ALL` para cada superclase con campos en `getBoundFields()` (Nivel 2).
3. En tiempo de ejecución, `checkAccessible()` valida que los miembros sean accesibles (Nivel 3, en `createBoundField()`).

El diseño de defensa en profundidad (3 niveles de verificación) hace que la elusión del filtro requiera manipular la propia lista de filtros registrados, lo cual no es posible desde una posición de atacante que solo controla las clases a serializar/deserializar.

**Calificación de seguridad: 9/10** — La única observación menor es que `blockInaccessible` podría no actualizarse para superclases sin campos, pero esto es intrascendente en la práctica.
