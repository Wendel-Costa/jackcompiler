package br.edu.ufma.jackcompiler;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Compilação dos fontes oficiais; não substitui a execução no VM Emulator. */
class Project11IntegrationTest {
    @TempDir Path directory;

    @ParameterizedTest(name = "{0}: compila todas as classes")
    @CsvSource({"Seven,Main", "ConvertToBin,Main", "Square,Main Square SquareGame",
            "Average,Main", "Pong,Ball Bat Main PongGame", "ComplexArrays,Main"})
    void compilesOfficialProgram(String program, String classNames) throws Exception {
        for (String name : classNames.split(" ")) {
            try (var source = getClass().getResourceAsStream("/nand2tetris/Project11/" + program + "/" + name + ".jack")) {
                assertNotNull(source, "Fonte oficial ausente: " + program + "/" + name);
                Files.copy(source, directory.resolve(name + ".jack"));
            }
        }
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        assertEquals(0, JackCompiler.run(new String[]{directory.toString()},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(errors)), errors.toString());
        Set<String> expected = new HashSet<>();
        for (String name : classNames.split(" ")) {
            expected.add(name + ".vm");
            String code = Files.readString(directory.resolve(name + ".vm"), StandardCharsets.UTF_8);
            assertFalse(code.isBlank());
            assertTrue(code.startsWith("function " + name + "."));
            validateVm(code);
        }
        try (var files = Files.list(directory)) {
            assertEquals(expected, files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".vm")).collect(java.util.stream.Collectors.toSet()));
        }
        if (program.equals("Seven")) {
            assertEquals("function Main.main 0\npush constant 1\npush constant 2\npush constant 3\ncall Math.multiply 2\nadd\ncall Output.printInt 1\npop temp 0\npush constant 0\nreturn",
                    Files.readString(directory.resolve("Main.vm")));
        }
    }

    private void validateVm(String code) {
        Set<String> labels = new HashSet<>();
        Set<String> jumps = new HashSet<>();
        Set<String> functions = new HashSet<>();
        String scope = "";
        for (String line : code.lines().toList()) {
            assertTrue(line.matches("(push (constant|argument|local|static|this|that|pointer|temp) [0-9]+)"
                    + "|(pop (argument|local|static|this|that|pointer|temp) [0-9]+)"
                    + "|((add|sub|neg|eq|gt|lt|and|or|not|return))"
                    + "|((label|goto|if-goto) [A-Za-z_.$:][A-Za-z0-9_.$:]*)"
                    + "|((function|call) [A-Za-z_.$:][A-Za-z0-9_.$:]* [0-9]+)"), line);
            String[] parts = line.split(" ");
            if (parts[0].equals("function")) {
                scope = parts[1];
                assertTrue(functions.add(scope), "Função duplicada: " + scope);
            } else if (parts[0].equals("label")) {
                assertTrue(labels.add(scope + "$" + parts[1]), "Rótulo duplicado: " + line);
            } else if (parts[0].equals("goto") || parts[0].equals("if-goto")) {
                jumps.add(scope + "$" + parts[1]);
            } else if (parts[0].equals("push") || parts[0].equals("pop")) {
                int index = Integer.parseInt(parts[2]);
                if (parts[1].equals("pointer")) assertTrue(index <= 1, line);
                if (parts[1].equals("temp")) assertTrue(index <= 7, line);
                if (parts[1].equals("constant")) assertTrue(index <= 32767, line);
            }
        }
        assertTrue(labels.containsAll(jumps), "Salto para rótulo inexistente: " + jumps);
    }
}
