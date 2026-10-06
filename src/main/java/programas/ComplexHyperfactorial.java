package programas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Scanner;

/** Certified arbitrary-precision complex continuation of the hyperfactorial. */
public final class ComplexHyperfactorial {

    private static final String WORKER_RESOURCE = "/certified_hyperfactorial.py";

    private ComplexHyperfactorial() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length >= 2) {
            long precision = parsePrecision(args[1]);
            CertifiedResult result = hyperfactorial(args[0], precision);
            if (args.length >= 3) {
                Path output = Path.of(args[2]);
                try (BufferedWriter writer = Files.newBufferedWriter(output,
                        StandardCharsets.UTF_8)) {
                    writeResult(args[0], result, writer);
                }
                System.out.println("Result written to " + output.toAbsolutePath());
                printCertificate(result);
            } else {
                writeResult(args[0], result,
                        new java.io.OutputStreamWriter(System.out, StandardCharsets.UTF_8));
            }
            return;
        }

        System.out.println("=== CERTIFIED COMPLEX HYPERFACTORIAL ===");
        System.out.println("H(z) = exp(z log Γ(z+1) - log G(z+1))");
        System.out.println("FLINT/Arb ball arithmetic certifies every returned digit.");
        try (Scanner keyboard = new Scanner(System.in)) {
            System.out.print("Enter z: ");
            String input = keyboard.nextLine().trim();
            System.out.print("Significant decimal digits: ");
            long precision = parsePrecision(keyboard.nextLine().trim());
            System.out.print("Output file (leave empty for console): ");
            String outputText = keyboard.nextLine().trim();

            CertifiedResult result = hyperfactorial(input, precision);
            if (outputText.isEmpty()) {
                writeResult(input, result,
                        new java.io.OutputStreamWriter(System.out, StandardCharsets.UTF_8));
            } else {
                Path output = Path.of(outputText);
                try (BufferedWriter writer = Files.newBufferedWriter(output,
                        StandardCharsets.UTF_8)) {
                    writeResult(input, result, writer);
                }
                System.out.println("Result written to " + output.toAbsolutePath());
                printCertificate(result);
            }
        }
    }

    public static CertifiedResult hyperfactorial(String input, long precision)
            throws IOException, InterruptedException {
        ComplexInputParser.ParsedComplex parsed = ComplexInputParser.parse(input);
        return hyperfactorial(parsed.real(), parsed.imaginary(), precision);
    }

    public static CertifiedResult hyperfactorial(String real, String imaginary, long precision)
            throws IOException, InterruptedException {
        if (precision <= 0) {
            throw new IllegalArgumentException("Precision must be positive");
        }
        ComplexInputParser.ParsedComplex parsed = ComplexInputParser.parse(
                real + signedImaginary(imaginary) + "i");
        Path worker = extractWorker();
        try {
            Process process = new ProcessBuilder(findPython(), worker.toString(),
                    parsed.real(), parsed.imaginary(), Long.toString(precision))
                    .redirectErrorStream(true).start();
            Map<String, String> fields = new HashMap<>();
            StringBuilder diagnostics = new StringBuilder();
            try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int separator = line.indexOf('\t');
                    if (separator > 0) {
                        fields.put(line.substring(0, separator), line.substring(separator + 1));
                    } else {
                        diagnostics.append(line).append('\n');
                    }
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("The FLINT/Arb worker failed with exit code "
                        + exitCode + ":\n" + diagnostics);
            }
            boolean certified = Boolean.parseBoolean(require(fields, "CERTIFIED"));
            if (!certified) {
                throw new IOException("The worker did not certify a unique interval rounding");
            }
            CertifiedResult result = new CertifiedResult(
                    require(fields, "REAL"), require(fields, "IMAGINARY"), certified,
                    Long.parseLong(require(fields, "PRECISION_DIGITS")),
                    Long.parseLong(require(fields, "WORKING_DIGITS")),
                    Long.parseLong(require(fields, "REAL_ACCURACY_BITS")),
                    Long.parseLong(require(fields, "IMAGINARY_ACCURACY_BITS")),
                    Double.parseDouble(require(fields, "TIME_SECONDS")),
                    require(fields, "SHA256"));
            String localHash = sha256(result.real() + "\n" + result.imaginary() + "\n");
            if (!localHash.equals(result.sha256())) {
                throw new IOException("The worker result failed its SHA-256 integrity check");
            }
            return result;
        } finally {
            Files.deleteIfExists(worker);
        }
    }

    private static String signedImaginary(String imaginary) {
        return imaginary.startsWith("-") || imaginary.startsWith("+")
                ? imaginary : "+" + imaginary;
    }

    private static long parsePrecision(String value) {
        try {
            long precision = Long.parseLong(value);
            if (precision <= 0) {
                throw new IllegalArgumentException("Precision must be positive");
            }
            return precision;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Precision must be a positive 64-bit integer", exception);
        }
    }

    private static Path extractWorker() throws IOException {
        try (InputStream input = ComplexHyperfactorial.class
                .getResourceAsStream(WORKER_RESOURCE)) {
            if (input == null) {
                throw new IOException("Bundled certified hyperfactorial worker not found");
            }
            Path worker = Files.createTempFile("certified-hyperfactorial-", ".py");
            Files.copy(input, worker, StandardCopyOption.REPLACE_EXISTING);
            return worker;
        }
    }

    private static String findPython() throws IOException, InterruptedException {
        String configured = System.getenv("PYTHON");
        if (configured != null && !configured.isBlank() && canRun(configured)) {
            return configured;
        }
        for (String candidate : new String[] {"python3", "python"}) {
            if (canRun(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Python 3 was not found");
    }

    private static boolean canRun(String command) throws InterruptedException {
        try {
            Process process = new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true).start();
            try (InputStream output = process.getInputStream()) {
                output.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return process.waitFor() == 0;
        } catch (IOException exception) {
            return false;
        }
    }

    private static String require(Map<String, String> fields, String name) throws IOException {
        String value = fields.get(name);
        if (value == null) {
            throw new IOException("Worker omitted field " + name);
        }
        return value;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void writeResult(String input, CertifiedResult result, Writer writer)
            throws IOException {
        writer.write("H(" + input + ") =\n");
        writer.write(result.real());
        writer.write(result.imaginary().startsWith("-") ? " -\n" : " +\n");
        writer.write(result.imaginary().startsWith("-")
                ? result.imaginary().substring(1) : result.imaginary());
        writer.write(" i\n");
        writer.flush();
    }

    private static void printCertificate(CertifiedResult result) {
        System.out.println("Certified: " + result.certified());
        System.out.println("Requested digits: " + result.precisionDigits());
        System.out.println("Working digits: " + result.workingDigits());
        System.out.printf("Calculation time: %.3f s%n", result.timeSeconds());
        System.out.println("SHA-256: " + result.sha256());
    }

    public record CertifiedResult(String real, String imaginary, boolean certified,
                                  long precisionDigits, long workingDigits,
                                  long realAccuracyBits, long imaginaryAccuracyBits,
                                  double timeSeconds, String sha256) {
    }
}
