package programas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeHyperfactorialTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void nativeBinaryMatchesPureJava() throws Exception {
        assumeTrue(NativeHyperfactorial.isAvailable());
        BigInteger expected = ParallelHyperfactorial.hyperfactorial(BigInteger.valueOf(999))
                .multiply(BigInteger.valueOf(1_000).pow(1_000));
        assertEquals(expected, NativeHyperfactorial.calculate(1_000).value());
    }

    @Test
    void decimalFileIsVerified() throws Exception {
        assumeTrue(NativeHyperfactorial.isAvailable());
        Path output = temporaryDirectory.resolve("hyper-1000.txt");
        NativeHyperfactorial.DecimalFileResult result =
                NativeHyperfactorial.writeDecimal(1_000, output);
        assertEquals(result.digits(), Files.size(output));
        assertTrue(result.sha256().matches("[0-9a-f]{64}"));
    }
}
