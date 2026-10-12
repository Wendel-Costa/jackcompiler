package br.edu.ufma.jackcompiler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class OfficialTokensTest {

    @ParameterizedTest(name = "XML oficial: {0}")
    @ValueSource(strings = {"Main", "Square", "SquareGame"})
    void shouldMatchEveryOfficialTokenInOrder(String name) throws IOException {
        String source = resource(name + ".jack");
        String expected = resource(name + "T.xml");
        String actual = TokenXmlWriter.toXml(new JackTokenizer(source));

        // Somente CRLF/CR vira LF. Espaços nos lexemas, tags e ordem são preservados.
        assertEquals(normalizeLineEndings(expected), normalizeLineEndings(actual), name);
    }

    private String resource(String name) throws IOException {
        String path = "/nand2tetris/Square/" + name;
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, "Recurso ausente: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String normalizeLineEndings(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }
}
