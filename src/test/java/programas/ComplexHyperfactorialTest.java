package programas;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

class ComplexHyperfactorialTest {

    @Test
    void knownImaginaryValueIsCertified() throws Exception {
        assumeTrue(NativeHyperfactorial.isAvailable());
        ComplexHyperfactorial.CertifiedResult result = ComplexHyperfactorial.hyperfactorial(
                "1.111111111111111111111111111111111111111i", 80);
        assertTrue(result.certified());
        assertTrue(result.real().startsWith(
                "0.4895316414038854964227133800163145564815682084020001845705183197379803"));
        assertTrue(result.imaginary().startsWith(
                "-0.4845344786538150886184160408749807311323448759130317674788257762254994"));
    }
}
