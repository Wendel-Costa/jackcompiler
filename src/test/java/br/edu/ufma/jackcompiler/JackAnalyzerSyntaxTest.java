package br.edu.ufma.jackcompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class JackAnalyzerSyntaxTest {

    @TempDir
    Path temporary;

    private final ByteArrayOutputStream outputLog = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorLog = new ByteArrayOutputStream();

    @Test
    void shouldGenerateBothXmlFilesFromOneUtf8Source() throws Exception {
        Path source = temporary.resolve("Main.jack");
        String content = "class Main { function void main() { do Output.printString(\"Olá & <Jack>\"); return; } }";
        Files.writeString(source, content);
        Path output = temporary.resolve("saída com espaços");

        assertEquals(0, run(source.toString(), output.toString()));
        assertEquals(content, Files.readString(source));
        assertEquals(TokenXmlWriter.toXml(new JackTokenizer(content)), Files.readString(output.resolve("MainT.xml")));
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(output.resolve("MainP.xml").toFile());
        assertEquals("class", document.getDocumentElement().getTagName());
        assertEquals(" Olá & <Jack> ", document.getElementsByTagName("stringConstant").item(0).getTextContent());
        assertEquals(1, document.getElementsByTagName("doStatement").getLength());
        assertEquals(1, document.getElementsByTagName("returnStatement").getLength());
        assertTrue(outputLog.toString(StandardCharsets.UTF_8).contains("MainT.xml"));
        assertTrue(outputLog.toString(StandardCharsets.UTF_8).contains("MainP.xml"));
        assertEquals("", errorLog.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldGenerateExactSyntaxXmlForEmptyClass() throws Exception {
        Path source = temporary.resolve("Main.jack");
        Files.writeString(source, "class Main {}");
        Path output = temporary.resolve("out");

        assertEquals(0, run(source.toString(), output.toString()));
        assertEquals("<class>\n  <keyword> class </keyword>\n  <identifier> Main </identifier>\n"
                + "  <symbol> { </symbol>\n  <symbol> } </symbol>\n</class>\n",
                Files.readString(output.resolve("MainP.xml")));
    }

    @Test
    void shouldProcessDirectoryWithoutRecursingOrReadingOtherExtensions() throws Exception {
        Path sources = Files.createDirectory(temporary.resolve("sources"));
        Files.writeString(sources.resolve("A.jack"), "class A {}");
        Files.writeString(sources.resolve("B.jack"), "class B {}");
        Files.writeString(sources.resolve("ignore.txt"), "@");
        Files.createDirectory(sources.resolve("nested"));
        Files.writeString(sources.resolve("nested/Invalid.jack"), "@");
        Path output = temporary.resolve("out");

        assertEquals(0, run(sources.toString(), output.toString()));
        try (var files = Files.list(output)) {
            assertEquals(List.of("AP.xml", "AT.xml", "BP.xml", "BT.xml"),
                    files.map(path -> path.getFileName().toString()).sorted().toList());
        }
    }

    @Test
    void shouldReportSyntaxErrorWithoutWritingEitherXml() throws Exception {
        Path source = temporary.resolve("Invalid.jack");
        Files.writeString(source, "class Invalid {\n    function void main() {\n        return\n    }\n}");
        Path output = temporary.resolve("out");

        assertEquals(1, run(source.toString(), output.toString()));
        String errors = errorLog.toString(StandardCharsets.UTF_8);
        assertTrue(errors.contains(source.toString()));
        assertTrue(errors.contains("linha 4, coluna 5"));
        assertTrue(errors.contains("SEMICOLON"));
        assertEquals("", outputLog.toString(StandardCharsets.UTF_8));
        assertFalse(Files.exists(output.resolve("InvalidT.xml")));
        assertFalse(Files.exists(output.resolve("InvalidP.xml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"T", "P"})
    void shouldProtectEitherExistingOutputBeforeWritingTheOther(String suffix) throws Exception {
        Path source = temporary.resolve("Main.jack");
        Files.writeString(source, "class Main {}");
        Path output = Files.createDirectory(temporary.resolve("out"));
        Path reference = output.resolve("Main" + suffix + ".xml");
        Files.writeString(reference, "referência preservada");

        assertEquals(1, run(source.toString(), output.toString()));
        assertEquals("referência preservada", Files.readString(reference));
        try (var files = Files.list(output)) {
            assertEquals(1, files.count());
        }
        assertTrue(errorLog.toString(StandardCharsets.UTF_8).contains(reference.toString()));
    }

    @Test
    void shouldKeepTokenOnlyModeIndependentOfSyntaxAndExistingParserOutput() throws Exception {
        Path source = temporary.resolve("Fragment.jack");
        Files.writeString(source, "\"Olá\"");
        Path output = Files.createDirectory(temporary.resolve("out"));
        Files.writeString(output.resolve("FragmentP.xml"), "referência");

        assertEquals(0, run("--tokens", source.toString(), output.toString()));
        assertEquals("<tokens>\n<stringConstant> Olá </stringConstant>\n</tokens>\n",
                Files.readString(output.resolve("FragmentT.xml")));
        assertEquals("referência", Files.readString(output.resolve("FragmentP.xml")));
        assertEquals(1, run("--tokens"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldRunMainWithModeSpecificDefaultOutputDirectory(boolean tokensOnly) throws Exception {
        Path source = temporary.resolve("Main.jack");
        Files.writeString(source, "class Main {}");
        List<String> arguments = new ArrayList<>();
        if (tokensOnly) {
            arguments.add("--tokens");
        }
        arguments.add(source.toString());
        assertEquals(0, runProcess(arguments));
        Path output = temporary.resolve(tokensOnly ? "target/tokens" : "target/xml");
        assertTrue(Files.exists(output.resolve("MainT.xml")));
        assertEquals(!tokensOnly, Files.exists(output.resolve("MainP.xml")));
    }

    @Test
    void shouldExitWithFailureForSyntaxErrorInRealProcess() throws Exception {
        Path source = temporary.resolve("Invalid.jack");
        Files.writeString(source, "class Invalid {");

        assertEquals(1, runProcess(List.of(source.toString())));
        String log = Files.readString(temporary.resolve("process.log"));
        assertTrue(log.contains("Invalid.jack"));
        assertTrue(log.contains("Erro sintático"));
        assertFalse(log.contains("Exception in thread"));
        assertFalse(Files.exists(temporary.resolve("target/xml/InvalidT.xml")));
        assertFalse(Files.exists(temporary.resolve("target/xml/InvalidP.xml")));
    }

    private int runProcess(List<String> arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", Path.of("target/classes").toAbsolutePath().toString(),
                JackAnalyzer.class.getName()));
        command.addAll(arguments);
        Process process = new ProcessBuilder(command).directory(temporary.toFile())
                .redirectErrorStream(true).redirectOutput(temporary.resolve("process.log").toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "A CLI não terminou no prazo");
            return process.exitValue();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private int run(String... arguments) {
        return JackAnalyzer.run(arguments, new PrintStream(outputLog, true, StandardCharsets.UTF_8),
                new PrintStream(errorLog, true, StandardCharsets.UTF_8));
    }
}
