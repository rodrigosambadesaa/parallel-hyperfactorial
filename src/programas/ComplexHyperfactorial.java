package programas;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Scanner;

import ch.obermuhlner.math.big.BigComplex;
import ch.obermuhlner.math.big.BigComplexMath;

/**
 * Arbitrary-precision analytic continuation of the hyperfactorial to complex
 * arguments.
 *
 * <pre>
 * H(z) = exp(z Log(Gamma(z + 1)) - Log(G(z + 1)))
 * </pre>
 *
 * <p>{@code G} denotes the Barnes G-function. Principal complex logarithms
 * are used, matching the principal value convention of the external
 * big-math library.</p>
 */
public final class ComplexHyperfactorial {

    private static final int GUARD_DIGITS = 18;

    private ComplexHyperfactorial() {
        // Utility class
    }

    /**
     * Interactive entry point.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== ARBITRARY-PRECISION COMPLEX HYPERFACTORIAL ===");
        System.out.println("Accepted examples: 3.5, 2i, -i, 3.5-2.25i, 1e-20+2e-5i");
        System.out.println("Type q to quit.");

        try (Scanner keyboard = new Scanner(System.in)) {
            while (true) {
                System.out.print(System.lineSeparator() + "Enter z: ");
                if (!keyboard.hasNextLine()) {
                    return;
                }

                String input = keyboard.nextLine().trim();
                if (input.equalsIgnoreCase("q")) {
                    return;
                }

                System.out.print("Significant decimal digits: ");
                if (!keyboard.hasNextLine()) {
                    return;
                }

                String precisionText = keyboard.nextLine().trim();

                try {
                    int precision = Integer.parseInt(precisionText);
                    if (precision <= 0) {
                        throw new IllegalArgumentException("Precision must be a positive integer");
                    }

                    BigComplex z = ComplexInputParser.parse(input);
                    MathContext context = new MathContext(precision, RoundingMode.HALF_EVEN);

                    long start = System.nanoTime();
                    BigComplex result = hyperfactorial(z, context);
                    long end = System.nanoTime();

                    System.out.println("H(" + input + ") =");
                    System.out.println(format(result));
                    System.out.printf("Calculation time: %.3f s%n", (end - start) / 1_000_000_000.0);
                } catch (NumberFormatException e) {
                    System.out.println("Error: invalid number or precision: " + e.getMessage());
                } catch (IllegalArgumentException | ArithmeticException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (OutOfMemoryError e) {
                    System.err.println("Error: not enough JVM heap memory for the requested precision.");
                    return;
                }
            }
        }
    }

    /**
     * Calculates the principal analytic continuation of the hyperfactorial.
     *
     * @param z arbitrary-precision complex argument
     * @param resultContext requested significant-digit precision
     * @return {@code H(z)} rounded to {@code resultContext}
     */
    public static BigComplex hyperfactorial(BigComplex z, MathContext resultContext) {
        if (z == null) {
            throw new IllegalArgumentException("The argument cannot be null");
        }
        if (resultContext == null || resultContext.getPrecision() <= 0) {
            throw new IllegalArgumentException("A finite positive output precision is required");
        }

        if (z.equals(BigComplex.ZERO)) {
            return BigComplex.ONE;
        }

        if (z.isReal()
                && z.re.signum() < 0
                && z.re.stripTrailingZeros().scale() <= 0) {
            throw new ArithmeticException(
                    "The selected principal continuation is singular at negative integers"
            );
        }

        MathContext workContext = new MathContext(
                Math.addExact(resultContext.getPrecision(), GUARD_DIGITS),
                resultContext.getRoundingMode()
        );

        BigComplex zPlusOne = z.add(BigDecimal.ONE, workContext);
        BigComplex logGamma = ComplexLogGamma.log(zPlusOne, workContext);
        BigComplex logBarnesG = BarnesG.log(zPlusOne, workContext);

        BigComplex logarithm = z.multiply(logGamma, workContext)
                .subtract(logBarnesG, workContext);

        return BigComplexMath.exp(logarithm, workContext).round(resultContext);
    }

    private static String format(BigComplex value) {
        BigDecimal real = normalizeZero(value.re);
        BigDecimal imaginary = normalizeZero(value.im);
        String sign = imaginary.signum() < 0 ? " - " : " + ";

        return real.stripTrailingZeros().toPlainString()
                + sign
                + imaginary.abs().stripTrailingZeros().toPlainString()
                + " i";
    }

    private static BigDecimal normalizeZero(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value;
    }
}
