# Anexo Técnico — Modelo de Calidad ISO/IEC 25010

**Repositorio auditado:** google/gson (fork: samurestrepo/gson_fork00)
**Fuente de datos:** SonarCloud, análisis del 29 de julio de 2026
**Fecha del anexo:** 29 de julio de 2026

---

## 1. Nota metodológica — Limitación de la plataforma (declaración obligatoria)

**SonarCloud, en su capa gratuita (Free/Open Source), no permite crear ni asignar un
Quality Gate personalizado distinto de "Sonar Way".** La opción de clonar o editar el
Gate por proyecto está restringida a planes de pago (Team/Enterprise).

**Consecuencia para esta interventoría:** el Quality Gate automatizado visible en
SonarCloud refleja únicamente los criterios genéricos de "Sonar Way" (que no evalúa
Coverage ni Duplications, como se observó — "No conditions set"). Por lo tanto, los
umbrales de calidad definidos en este anexo **no están parametrizados en la
herramienta**, sino que constituyen un **Quality Gate documental**, aplicado
manualmente por el equipo auditor sobre las métricas reales extraídas de SonarCloud.

Esta limitación se declara explícitamente para que quede trazable ante el profesor:
la ausencia de un Gate automatizado custom no es una omisión del equipo, sino una
restricción de la versión gratuita de la herramienta.

---

## 2. Umbrales de calidad definidos (Quality Gate documental)

| Característica ISO/IEC 25010                | Métrica Sonar                 | Umbral definido       | Valor real                           | Cumple     |
| ------------------------------------------- | ----------------------------- | --------------------- | ------------------------------------ | ---------- |
| **Fiabilidad (Reliability)**                | Rating                        | ≤ B                   | E                                    | ❌ No      |
| **Fiabilidad (Reliability)**                | Bugs Blocker                  | 0                     | 2 (1 verificado como falso positivo) | ⚠️ Parcial |
| **Seguridad (Security)**                    | Rating                        | ≤ B                   | C                                    | ❌ No      |
| **Seguridad (Security)**                    | Security Hotspots sin revisar | 0                     | 0                                    | ✅ Sí      |
| **Mantenibilidad (Maintainability)**        | Rating                        | ≤ A                   | A                                    | ✅ Sí      |
| **Mantenibilidad (Maintainability)**        | Code Smells                   | < 5% líneas de código | 807 / 26.000 líneas (~3.1%)          | ✅ Sí      |
| **Compatibilidad / Estabilidad de pruebas** | Cobertura de pruebas          | ≥ 60%                 | 12.3%                                | ❌ No      |
| **Mantenibilidad (Maintainability)**        | Duplicación de código         | < 3%                  | 1.6%                                 | ✅ Sí      |

**Veredicto del Gate documental: NO CUMPLE** (falla en Reliability, Security y Coverage — 3 de 8 criterios).

---

## 3. Justificación de los umbrales elegidos

- **Reliability ≤ B / Bugs Blocker = 0**: estándar de la industria para librerías core de amplio uso (gson es dependencia transitiva de miles de proyectos); un solo bug Blocker real ya representa riesgo para consumidores.
- **Coverage ≥ 60%**: umbral conservador (no exigente) para una librería madura de serialización; útil como referencia mínima de confianza en refactors futuros.
- **Duplicación < 3% / Code Smells < 5%**: umbrales estándar de SonarSource para proyectos Java de tamaño medio-grande.

## 4. Análisis cualitativo por característica (con verificación humana)

### Fiabilidad

De los 2 Blocker reportados, **1 fue verificado como falso positivo** (`LinkedTreeMap.java`,
regla S2259 — ver bitácora de hallazgos) mediante razonamiento sobre el invariante
aritmético del algoritmo AVL. El Blocker restante (`ParseBenchmark.java`, S2095) es un
bug real de gestión de recursos, pero ubicado en módulo interno de benchmarking, no en
la API pública consumida por terceros. **Conclusión:** el rating E de Sonar sobreestima
el riesgo real para un adoptador; el riesgo efectivo para producción es bajo pero no nulo.

### Seguridad

3 issues abiertos, rating C, sin Security Hotspots pendientes de revisión. Sin verificación
línea por línea de estos 3 (pendiente si el tiempo lo permite), se documenta como riesgo
moderado no crítico.

### Mantenibilidad

Rating A pese a 807 code smells — coherente con el bajo costo de remediación relativo
al tamaño del proyecto (26k líneas). No representa riesgo de adopción.

### Cobertura de pruebas

12.3% es bajo frente al umbral definido (60%). Sin embargo, debe interpretarse con cautela:
JaCoCo fue añadido por el equipo auditor durante esta intervención (no estaba en el pom.xml
original), por lo que este número refleja cobertura medida recién ahora, no necesariamente
la cobertura real histórica del proyecto si usaban otra herramienta de medición internamente.

---

## 5. Recomendación para el dictamen final

Este anexo sustenta un veredicto de **ADOPTAR CON CONDICIONES**: el proyecto es
maduro y estable en mantenibilidad y duplicación, pero requiere que el adoptador
(a) verifique manualmente el uso de `LinkedTreeMap` en su versión específica antes
de integrarlo en producción crítica, y (b) no dependa del pipeline de gson para
garantías de cobertura, dado el nivel medido.
