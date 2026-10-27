package br.edu.ufma.jackcompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JackAnalyzerIntegrationTest {

    @TempDir
    Path temporary;

    private final ByteArrayOutputStream messages = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @Test
    void shouldIntegrateDeclarationsAndAllStatementsInOneClass() throws Exception {
        String content = """
                class Box {
                    static int count;
                    field Array data;
                    field int size;
                    constructor Box new(int n) {
                        let data = Array.new(n);
                        let size = n;
                        return this;
                    }
                    method void fill(int value) {
                        var int i;
                        let i = 0;
                        while (i < size) {
                            if (value < 0) { let data[i] = -value; }
                            else { let data[i] = value; }
                            let i = i + 1;
                        }
                        do Output.printInt(size);
                        return;
                    }
                    function int twice(int x) { return x + x; }
                }
                """;
        Path source = temporary.resolve("Box.jack");
        Path output = temporary.resolve("out");
        Files.writeString(source, content);

        assertEquals(0, run(source, output));
        assertEquals(content, Files.readString(source));
        assertEquals("", errors.toString(StandardCharsets.UTF_8));
        assertTrue(Files.exists(output.resolve("BoxT.xml")));
        var xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(output.resolve("BoxP.xml").toFile());
        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("3", xpath.evaluate("count(/class/classVarDec)", xml));
        assertEquals("3", xpath.evaluate("count(/class/subroutineDec)", xml));
        assertEquals("constructor", xpath.evaluate("normalize-space(/class/subroutineDec[1]/keyword[1])", xml));
        assertEquals("method", xpath.evaluate("normalize-space(/class/subroutineDec[2]/keyword[1])", xml));
        assertEquals("function", xpath.evaluate("normalize-space(/class/subroutineDec[3]/keyword[1])", xml));
        assertEquals("1", xpath.evaluate("count(//varDec)", xml));
        assertEquals("6", xpath.evaluate("count(//letStatement)", xml));
        assertEquals("1", xpath.evaluate("count(//whileStatement/statements/ifStatement)", xml));
        assertEquals("1", xpath.evaluate("count(//doStatement)", xml));
        assertEquals("3", xpath.evaluate("count(//returnStatement)", xml));
        assertEquals("this", xpath.evaluate(
                "normalize-space(/class/subroutineDec[1]//returnStatement/expression/term/keyword)", xml));
        assertEquals("0", xpath.evaluate("count(/class/subroutineDec[2]//returnStatement/expression)", xml));
    }

    @ParameterizedTest(name = "erro estrutural: {1}")
    @MethodSource("invalidStructures")
    void shouldReportSameSyntaxPositionThroughParserAndCli(String marked, String expected) throws Exception {
        // O marcador fixa a posição esperada sem depender da posição calculada pelo tokenizer.
        int marker = marked.indexOf('§');
        String before = marked.substring(0, marker);
        int line = (int) before.chars().filter(c -> c == '\n').count() + 1;
        int column = before.length() - before.lastIndexOf('\n');
        String content = marked.replace("§", "");
        JackSyntaxException exception = assertThrows(JackSyntaxException.class,
                () -> new JackParser(new JackTokenizer(content)).parseClass());
        assertEquals(line, exception.getLine());
        assertEquals(column, exception.getColumn());
        assertTrue(exception.getMessage().contains("esperado " + expected), exception.getMessage());

        Path source = temporary.resolve("Invalid.jack");
        Path output = temporary.resolve("out");
        Files.writeString(source, content);
        assertEquals(1, run(source, output));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains(source + ": " + exception.getMessage()));
        assertEquals("", messages.toString(StandardCharsets.UTF_8));
        assertNoOutput(output, "Invalid");
    }

    private static Stream<Arguments> invalidStructures() {
        return Stream.of(
                Arguments.of("§", "CLASS"),
                Arguments.of("// sem classe\n§", "CLASS"),
                Arguments.of("class §{ }", "IDENT"),
                Arguments.of("class Main {§", "RBRACE"),
                Arguments.of("class Main {}\n§class Other {}", "EOF"),
                Arguments.of("class Main {} §}", "EOF"),
                Arguments.of("class Main { function void f(§", "RPAREN"),
                Arguments.of("class Main { function void f(int x, §) {} }", "tipo"),
                Arguments.of("class Main { function void f() { var int §; } }", "IDENT"),
                Arguments.of("class Main { function void f() { return; §var int x; } }", "RBRACE"),
                Arguments.of("class Main { function void f() {} §field int x; }", "RBRACE"),
                Arguments.of("class Main { function void f() { §else {} } }", "RBRACE"),
                Arguments.of("class Main { function void f() { return; }§", "RBRACE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void shouldKeepErrorLocationAfterCommentsAndDifferentLineEndings(String newline) throws Exception {
        String content = String.join(newline,
                "class Main {", "/* comentário", "   continua */", "function void f() {",
                "  // comentário de linha", "  let x = ;", "}", "}");
        JackSyntaxException exception = assertThrows(JackSyntaxException.class,
                () -> new JackParser(new JackTokenizer(content)).parseClass());
        assertEquals(6, exception.getLine());
        assertEquals(11, exception.getColumn());
        Path source = temporary.resolve("Invalid.jack");
        Files.writeString(source, content);
        Path output = temporary.resolve("out");
        assertEquals(1, run(source, output));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("linha 6, coluna 11"));
        assertNoOutput(output, "Invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"class B {", "class B { /* aberto"})
    void shouldStopDirectoryAtFirstInvalidFileAndKeepEarlierOutputs(String invalid) throws Exception {
        Path sources = Files.createDirectory(temporary.resolve("sources"));
        // Criação fora de ordem para verificar a ordenação dos nomes pela CLI.
        Files.writeString(sources.resolve("C.jack"), "class C {}");
        Files.writeString(sources.resolve("B.jack"), invalid);
        Files.writeString(sources.resolve("A.jack"), "class A {}");
        Path output = temporary.resolve("out");

        assertEquals(1, run(sources, output));
        try (var files = Files.list(output)) {
            assertEquals(List.of("AP.xml", "AT.xml"),
                    files.map(path -> path.getFileName().toString()).sorted().toList());
        }
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("B.jack"));
        assertFalse(messages.toString(StandardCharsets.UTF_8).contains("CP.xml"));
        assertEquals(invalid, Files.readString(sources.resolve("B.jack")));
    }

    @Test
    void shouldAllowRetryAfterFixingSourceWithoutRemovingPartialXml() throws Exception {
        Path source = temporary.resolve("Main.jack");
        Path output = temporary.resolve("out");
        Files.writeString(source, "class Main {");
        assertEquals(1, run(source, output));
        assertNoOutput(output, "Main");

        Files.writeString(source, "class Main {}");
        messages.reset();
        errors.reset();
        assertEquals(0, run(source, output));
        assertTrue(Files.exists(output.resolve("MainT.xml")));
        assertTrue(Files.exists(output.resolve("MainP.xml")));
        assertEquals("", errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldReportOutputPathThatIsAFileWithoutOverwritingIt() throws Exception {
        Path source = temporary.resolve("Main.jack");
        Files.writeString(source, "class Main {}");
        Path output = temporary.resolve("out");
        Files.writeString(output, "preservar");

        assertEquals(1, run(source, output));
        assertEquals("preservar", Files.readString(output));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Erro:"));
        assertEquals("", messages.toString(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"T", "P"})
    void shouldRejectDanglingOutputLinksBeforeWritingEitherXml(String suffix) throws Exception {
        Path source = temporary.resolve("Main.jack");
        Files.writeString(source, "class Main {}");
        Path output = Files.createDirectory(temporary.resolve("out"));
        Path missingTarget = temporary.resolve("missing.xml");
        Path destination = output.resolve("Main" + suffix + ".xml");
        try {
            Files.createSymbolicLink(destination, missingTarget);
        } catch (UnsupportedOperationException | IOException | SecurityException exception) {
            assumeTrue(false, "Ambiente sem suporte/permissão para links simbólicos: " + exception);
        }

        assertEquals(1, run(source, output));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("A saída já existe: " + destination));
        assertEquals("", messages.toString(StandardCharsets.UTF_8));
        assertEquals(missingTarget, Files.readSymbolicLink(destination));
        assertFalse(Files.exists(missingTarget));
        try (var files = Files.list(output)) {
            assertEquals(List.of(destination), files.toList());
        }
    }

    private void assertNoOutput(Path directory, String name) {
        assertFalse(Files.exists(directory.resolve(name + "T.xml")));
        assertFalse(Files.exists(directory.resolve(name + "P.xml")));
    }

    private int run(Path source, Path output) {
        return JackAnalyzer.run(new String[]{source.toString(), output.toString()},
                new PrintStream(messages, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8));
    }
}
