package programas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class ParallelHyperfactorialTest {

    @Test
    void matchesSequentialReferenceThroughThreeHundred() {
        BigInteger expected = BigInteger.ONE;
        for (int n = 0; n <= 300; n++) {
            if (n >= 2) {
                expected = expected.multiply(BigInteger.valueOf(n).pow(n));
            }
            assertEquals(expected,
                    ParallelHyperfactorial.hyperfactorial(BigInteger.valueOf(n)),
                    "Mismatch at n=" + n);
        }
    }

    @Test
    void rejectsNegativeAndNullArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> ParallelHyperfactorial.hyperfactorial(BigInteger.valueOf(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> ParallelHyperfactorial.hyperfactorial(null));
    }
}
