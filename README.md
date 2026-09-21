# Parallel Hyperfactorial

Exact arbitrary-precision hyperfactorial calculator written in Java.

The hyperfactorial of a non-negative integer `n` is:

```text
H(n) = 1^1 × 2^2 × 3^3 × ... × n^n
```

## Implementation

The public API accepts and returns `BigInteger`:

```java
BigInteger result = ParallelHyperfactorial.hyperfactorial(n);
```

For practical inputs, the program factors the result into prime powers:

```text
H(n) = product of p^E(p) for every prime p <= n
```

For each prime `p`, its exponent is calculated exactly as:

```text
E(p) = sum of p^j × q × (q + 1) / 2
q = floor(n / p^j)
```

The prime powers are multiplied through a weight-balanced parallel product tree using Java's `ForkJoinPool`. Exponents are represented by `BigInteger`; binary exponentiation is used when an exponent does not fit in an `int`.

Inputs greater than `Integer.MAX_VALUE` use an exact divide-and-conquer fallback whose range boundaries and exponents remain `BigInteger`.

## Requirements

- Java 17 or newer
- Eclipse IDE with Java Development Tools

## Import into Eclipse

1. Open **File → Import**.
2. Select **General → Existing Projects into Workspace**.
3. Select the cloned repository as the root directory.
4. Choose **parallel-hyperfactorial** and finish the import.
5. Run `src/programas/ParallelHyperfactorial.java` as a Java application.

## Practical limits

The API does not impose an artificial `int` or `long` limit on the input, but an exact hyperfactorial grows extraordinarily quickly. For example, `H(1,000)` already has roughly 1.39 million decimal digits, while `H(1,000,000)` would have roughly 2.89 trillion digits.

Memory, execution time, `BigInteger` implementation limits and Java's maximum `String` length therefore impose much smaller practical limits than the method signature.
