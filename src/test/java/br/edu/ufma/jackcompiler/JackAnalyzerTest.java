package br.edu.ufma.jackcompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JackAnalyzerTest {

    @TempDir
    Path temporary;

    @Test
    void shouldReadUtf8FileAndWriteTokensInSeparateDirectory() throws Exception {
        Path source = temporary.resolve("Main.jack");
        Files.writeString(source, "\"Olá\"");
        Path output = temporary.resolve("saída com espaços");
        assertEquals(0, JackAnalyzer.run(new String[]{"--tokens", source.toString(), output.toString()},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())));
        assertEquals("<tokens>\n<stringConstant> Olá </stringConstant>\n</tokens>\n",
                Files.readString(output.resolve("MainT.xml")));
        assertEquals("\"Olá\"", Files.readString(source));
    }

    @Test
    void shouldProcessOnlyJackFilesInDirectory() throws Exception {
        Path sources = Files.createDirectory(temporary.resolve("sources"));
        Files.writeString(sources.resolve("Main.jack"), "class Main {}");
        Files.writeString(sources.resolve("Square.jack"), "class Square {}");
        Files.writeString(sources.resolve("ignore.txt"), "@");
        Path output = temporary.resolve("tokens");
        assertEquals(0, run(sources, output));
        assertTrue(Files.exists(output.resolve("MainT.xml")));
        assertTrue(Files.exists(output.resolve("SquareT.xml")));
        assertFalse(Files.exists(output.resolve("ignoreT.xml")));
    }

    @Test
    void shouldReportSourceAndLineForLexicalErrorWithoutWritingXml() throws Exception {
        Path source = temporary.resolve("Invalid.jack");
        Files.writeString(source, "\n/* aberto");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        Path output = temporary.resolve("tokens");
        int result = JackAnalyzer.run(new String[]{source.toString(), output.toString()},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(errors));
        assertEquals(1, result);
        assertTrue(errors.toString().contains("Invalid.jack"));
        assertTrue(errors.toString().contains("linha 2"));
        assertFalse(Files.exists(output.resolve("InvalidT.xml")));
    }

    @Test
    void shouldRejectMissingInputEmptyDirectoryAndWrongExtension() throws Exception {
        Path text = temporary.resolve("Main.txt");
        Files.writeString(text, "class Main {}");
        Path empty = Files.createDirectory(temporary.resolve("empty"));
        Path output = temporary.resolve("tokens");
        assertEquals(1, run(temporary.resolve("missing.jack"), output));
        assertEquals(1, run(empty, output));
        assertEquals(1, run(text, output));
        assertEquals(1, JackAnalyzer.run(new String[0], System.out,
                new PrintStream(new ByteArrayOutputStream())));
    }

    @Test
    void shouldProtectExistingReferenceXml() throws Exception {
        Path source = temporary.resolve("Main.jack");
        Files.writeString(source, "class Main {}");
        Path reference = temporary.resolve("MainT.xml");
        Files.writeString(reference, "referência");
        assertEquals(1, run(source, temporary));
        assertEquals("referência", Files.readString(reference));
    }

    private int run(Path source, Path output) {
        return JackAnalyzer.run(new String[]{source.toString(), output.toString()},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));
    }
}
