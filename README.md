# Parallel Hyperfactorial

Java 17 calculator providing two complementary hyperfactorial implementations:

- an exact `BigInteger` implementation for non-negative integers;
- an arbitrary-precision analytic continuation for real and complex decimal arguments.

For a non-negative integer `n`:

```text
H(n) = 1^1 × 2^2 × 3^3 × ... × n^n
```

## Exact integer implementation

The integer API accepts and returns `BigInteger`:

```java
BigInteger result = ParallelHyperfactorial.hyperfactorial(n);
```

For practical inputs, it factors the result into prime powers and multiplies them through a weight-balanced parallel product tree using `ForkJoinPool`. Inputs greater than `Integer.MAX_VALUE` use an exact divide-and-conquer fallback whose boundaries and exponents remain `BigInteger`.

Run:

```text
programas.ParallelHyperfactorial
```

## Arbitrary-precision complex implementation

For non-integer and complex arguments, the program evaluates the principal analytic continuation:

```text
H(z) = exp(z LogGamma(z + 1) - LogBarnesG(z + 1))
```

The API receives a `BigComplex` and an explicit decimal precision:

```java
MathContext context = new MathContext(200, RoundingMode.HALF_EVEN);
BigComplex z = BigComplex.valueOf(
        BigDecimal.ZERO,
        BigDecimalMath.toBigDecimal("1.111111111111111111111111111111111111111")
);

BigComplex result = ComplexHyperfactorial.hyperfactorial(z, context);
```

Run:

```text
programas.ComplexHyperfactorial
```

Accepted console formats include:

```text
3.5
2i
-i
3.5+2.25i
3.5-2.25i
1e-20+2e-5i
```

The parser never converts through `double`. Very long decimal components are read with `BigDecimalMath.toBigDecimal(String)`.

### Example

For 200 significant digits:

```text
H(1.111111111111111111111111111111111111111i) =
0.48953164140388549642271338001631455648156820840200018457051831973798032005078357064287005130067287470691848458563061842894420386596879117766030697718759288253611838518905480450134244450392430832175896
- 0.48453447865381508861841604087498073113234487591303176747882577622549949699957175495978017639802409544769945663542822185300101531099139000616884728805548544543671218079878445324613099798002181206731225 i
```

### Numerical method

- `BigComplex`, elementary complex functions and arbitrary-precision decimal operations come from `ch.obermuhlner:big-math:2.3.2`.
- `LogGamma` is evaluated using recurrence and Stirling's asymptotic expansion.
- `LogBarnesG` is evaluated through its functional equation and asymptotic expansion.
- Bernoulli numbers use an incremental Akiyama-Tanigawa cache with an extended internal `MathContext`.
- The Kinkelin constant `zeta'(-1)` is calculated dynamically with Euler-Maclaurin summation instead of being stored with a fixed number of digits.

## Requirements

- Java 17 or newer
- Eclipse IDE with Java Development Tools and Maven Integration for Eclipse (`m2e`)
- Maven, when building outside Eclipse

The external dependency is declared in `pom.xml` and downloaded from Maven Central.

## Import into Eclipse

1. Open **File → Import**.
2. Select **Maven → Existing Maven Projects**.
3. Select the cloned repository as the root directory.
4. Choose `parallel-hyperfactorial` and finish the import.
5. If necessary, select **Maven → Update Project** from the project's context menu.
6. Run either main class as a Java application.

## Precision and practical limits

“Arbitrary precision” means that the caller chooses a finite number of significant decimal digits. A transcendental complex result generally has infinitely many non-repeating digits, so no program can return all of them.

Input length and requested precision are not restricted to `double`, `int` or `long` numeric ranges, but Java ultimately remains limited by available memory, execution time, `BigDecimal` scale and array-size limits. Increasing the requested precision also increases the number of asymptotic terms and the cost of the elementary complex operations.

The exact integer hyperfactorial grows even faster: `H(1,000)` already has roughly 1.39 million decimal digits, while `H(1,000,000)` would have roughly 2.89 trillion digits.
