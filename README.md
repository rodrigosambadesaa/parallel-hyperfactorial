# Parallel Hyperfactorial

Proyecto Java 17/Eclipse para calcular:

```text
H(n) = 1^1 × 2^2 × ... × n^n
```

Incluye un motor entero exacto FLINT/GMP con respaldo Java puro y una continuación
compleja con precisión arbitraria certificada mediante FLINT/Arb.

## Enteros exactos: FLINT/GMP

El motor factoriza implícitamente el resultado. Para cada primo calcula:

```text
v_p(H(n)) = Σ(j≥1) p^j q(q+1)/2,  q = floor(n/p^j)
```

Después eleva cada primo a su exponente exacto y usa un árbol de producto que
combina primero operandos de tamaño parecido. FLINT/GMP ejecuta las potencias y
multiplicaciones gigantes. Sin el backend nativo sigue disponible la criba y el
árbol ponderado Java con `BigInteger` y `ForkJoinPool`.

API:

```java
BigInteger value = ParallelHyperfactorial.hyperfactorial(n);
```

Salida decimal directa para resultados gigantes:

```bash
java -cp target/classes programas.ParallelHyperfactorial 10000 hyperfactorial-10000.txt
```

El archivo temporal se valida por longitud y SHA-256 antes de moverlo de forma
atómica al destino. Esto evita la costosa ruta `BigInteger.toString()`.

## Complejos certificados

La rama principal usada es:

```text
H(z) = exp(z log Γ(z+1) - log G(z+1))
```

Los decimales de entrada se convierten directamente a racionales exactos. Arb
evalúa `log Γ` y `log G` con aritmética de bolas complejas; el programa acepta
los dígitos únicamente cuando el intervalo completo determina un redondeo único.

```java
ComplexHyperfactorial.CertifiedResult result =
        ComplexHyperfactorial.hyperfactorial(
                "1.111111111111111111111111111111111111111i", 100_000L);
```

```bash
java -cp target/classes programas.ComplexHyperfactorial \
  1.111111111111111111111111111111111111111i \
  100000 hyperfactorial-complex-100000.txt
```

Los primeros dígitos verificados son:

```text
0.4895316414038854964227133800163145564815682084020001845705183197379803...
-0.4845344786538150886184160408749807311323448759130317674788257762254994... i
```

No existe un techo artificial pequeño de precisión: se recibe un `long` y se
devuelven cadenas decimales. Los límites efectivos son los recursos disponibles y
los tamaños máximos de las estructuras de Java/Python/FLINT.

## Instalación y Eclipse

Requisitos: Java 17, Maven, Python 3 y Eclipse con m2e.

```bash
python3 -m pip install -r requirements.txt
mvn verify
```

En Eclipse: **File → Import → Maven → Existing Maven Projects** y seleccionar este
repositorio. Si el Python deseado no se llama `python3` o `python`, definir
`PYTHON`.

`requirements.txt` fija `python-flint==0.8.0`. Los hilos nativos se controlan
con `HYPERFACTORIAL_THREADS`.

## Verificación

Las pruebas comparan los resultados nativos con referencias Java exactas, verifican
los archivos por SHA-256 y comprueban el ejemplo complejo conocido. GitHub Actions
instala FLINT y ejecuta `mvn verify` en cada cambio.
