package br.edu.ufma.jackcompiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class OfficialSyntaxTest {

    @TempDir
    Path temporary;

    @ParameterizedTest(name = "Parser contra XML sintático oficial: {0}")
    @ValueSource(strings = {"Main", "Square", "SquareGame"})
    void shouldMatchOfficialSyntaxTree(String name) throws Exception {
        String actual = new JackParser(new JackTokenizer(resource(name + ".jack"))).parseClass();
        assertOfficialXml(name, actual);
    }

    @ParameterizedTest(name = "CLI contra XML sintático oficial: {0}")
    @ValueSource(strings = {"Main", "Square", "SquareGame"})
    void shouldWriteOfficialSyntaxTreeThroughCli(String name) throws Exception {
        Path source = temporary.resolve(name + ".jack");
        Files.writeString(source, resource(name + ".jack"), StandardCharsets.UTF_8);
        Path output = temporary.resolve("output");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int result = JackAnalyzer.run(new String[]{source.toString(), output.toString()},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(errors, true, StandardCharsets.UTF_8));

        assertEquals(0, result, errors.toString(StandardCharsets.UTF_8));
        assertOfficialXml(name, Files.readString(output.resolve(name + "P.xml"), StandardCharsets.UTF_8));
    }

    private void assertOfficialXml(String name, String actual) throws Exception {
        String expected = resource(name + "P.xml");
        // Confere também se a saída é um documento XML válido, além da comparação integral.
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(actual)));
        assertEquals("class", document.getDocumentElement().getTagName());
        assertEquals(normalizeLayout(expected), normalizeLayout(actual), name);
    }

    private String normalizeLayout(String xml) {
        // Cada tag ocupa uma linha. Remove somente indentação e diferenças CRLF/CR/LF.
        // Espaços e entidades DENTRO dos tokens são preservados, inclusive em strings.
        return xml.lines().map(String::stripLeading).collect(Collectors.joining("\n"));
    }

    private String resource(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/nand2tetris/Square/" + name)) {
            assertNotNull(input, "Referência oficial ausente: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
