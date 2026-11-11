package br.edu.ufma.jackcompiler;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JackCompilerTest {
    @TempDir Path directory;
    private static final String SOURCE = "class Main { function void main() { do Output.printInt(7); return; } }";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    private int run(String... args) {
        return JackCompiler.run(args, new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8));
    }

    @Test void compilesSingleFileAndReplacesExistingOutput() throws Exception {
        Path source = directory.resolve("Main.jack");
        Files.writeString(source, SOURCE);
        Files.writeString(directory.resolve("Main.vm"), "old output");
        assertEquals(0, run(source.toString()));
        assertEquals("function Main.main 0\npush constant 7\ncall Output.printInt 1\npop temp 0\npush constant 0\nreturn",
                Files.readString(directory.resolve("Main.vm")));
        assertEquals("", errors.toString(StandardCharsets.UTF_8));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Main.vm"));
    }

    @Test void compilesDirectoryInOrderWithoutDescendingOrCopyingOtherFiles() throws Exception {
        Files.writeString(directory.resolve("B.jack"), "class B { function void f() { return; } }");
        Files.writeString(directory.resolve("A.jack"), "class A { function void f() { return; } }");
        Files.writeString(directory.resolve("ignore.txt"), "invalid");
        Path nested = Files.createDirectory(directory.resolve("nested"));
        Files.writeString(nested.resolve("Bad.jack"), "invalid");
        assertEquals(0, run(directory.toString()));
        try (var files = Files.list(directory)) {
            assertEquals(java.util.List.of("A.vm", "B.vm"), files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".vm")).sorted().toList());
        }
        assertFalse(Files.exists(nested.resolve("Bad.vm")));
        String log = output.toString(StandardCharsets.UTF_8);
        assertTrue(log.indexOf("A.vm") < log.indexOf("B.vm"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"class Bad {", "class Bad { @ }",
            "class Bad { function void f() { let missing = 1; return; } }"})
    void compilationErrorsPreserveAllOutputs(String invalid) throws Exception {
        Files.writeString(directory.resolve("Main.jack"), SOURCE);
        Files.writeString(directory.resolve("Main.vm"), "preserved");
        Path bad = directory.resolve("ZBad.jack");
        Files.writeString(bad, invalid);
        assertEquals(1, run(directory.toString()));
        assertEquals("preserved", Files.readString(directory.resolve("Main.vm")));
        assertFalse(Files.exists(directory.resolve("ZBad.vm")));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains(bad.toString()));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("linha"));
        assertEquals("", output.toString(StandardCharsets.UTF_8));
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void rejectsOutputSymlinksBeforeWritingAnything(boolean existing) throws Exception {
        Files.writeString(directory.resolve("A.jack"), "class A { function void f() { return; } }");
        Files.writeString(directory.resolve("Main.jack"), SOURCE);
        Path target = directory.resolve("target.txt");
        if (existing) Files.writeString(target, "preserved");
        Path link = directory.resolve("Main.vm");
        Files.createSymbolicLink(link, target);
        assertEquals(1, run(directory.toString()));
        assertFalse(Files.exists(directory.resolve("A.vm")));
        assertTrue(Files.isSymbolicLink(link));
        if (existing) assertEquals("preserved", Files.readString(target));
        else assertFalse(Files.exists(target));
    }

    @Test void rejectsInvalidArgumentsAndInputs() throws Exception {
        assertEquals(1, run());
        assertEquals(1, run("one", "two"));
        assertEquals(1, run(directory.resolve("missing.jack").toString()));
        assertEquals(1, run(directory.toString()));
        Path text = directory.resolve("input.txt");
        Files.writeString(text, SOURCE);
        assertEquals(1, run(text.toString()));
    }

    @Test void rejectsDirectoryAsOutputBeforeWritingAnything() throws Exception {
        Files.writeString(directory.resolve("Main.jack"), SOURCE);
        Files.createDirectory(directory.resolve("Main.vm"));
        assertEquals(1, run(directory.toString()));
        assertTrue(Files.isDirectory(directory.resolve("Main.vm")));
    }

    @Test void mainReturnsNonzeroExitCodeOnInvalidInput() throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = Path.of(JackCompiler.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Process process = new ProcessBuilder(javaExecutable, "-cp", classes, JackCompiler.class.getName())
                .redirectErrorStream(true).start();
        assertTrue(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(1, process.exitValue());
        assertTrue(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).contains("Uso:"));
    }
}
