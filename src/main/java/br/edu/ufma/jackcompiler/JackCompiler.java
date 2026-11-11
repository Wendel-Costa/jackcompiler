package br.edu.ufma.jackcompiler;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** CLI que compila arquivos Jack em código VM na mesma pasta dos fontes. */
public final class JackCompiler {

    private record CompiledFile(Path destination, String code) {
    }

    private JackCompiler() {
    }

    public static void main(String[] args) {
        int result = run(args, System.out, System.err);
        if (result != 0) {
            System.exit(result);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 1) {
            err.println("Uso: JackCompiler <arquivo.jack|diretório>");
            return 1;
        }

        try {
            List<CompiledFile> compiled = new ArrayList<>();
            for (Path source : sourceFiles(Path.of(args[0]))) {
                String code;
                try {
                    String content = Files.readString(source, StandardCharsets.UTF_8);
                    code = new JackCompilationEngine(new JackTokenizer(content)).compileClass();
                } catch (IOException | IllegalArgumentException | JackSyntaxException
                        | JackCompilationException exception) {
                    throw new IOException(source + ": " + exception.getMessage(), exception);
                }

                String name = source.getFileName().toString();
                Path destination = source.resolveSibling(name.substring(0, name.length() - 5) + ".vm");
                requireRegularOutput(destination);
                compiled.add(new CompiledFile(destination, code));
            }

            // Erros de compilação em qualquer fonte não alteram as saídas existentes.
            // Falhas de I/O durante a gravação ainda podem deixar saídas parciais.
            for (CompiledFile file : compiled) {
                Files.writeString(file.destination(), file.code(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                out.println("Gerado: " + file.destination());
            }
            return 0;
        } catch (IOException | IllegalArgumentException exception) {
            err.println("Erro: " + exception.getMessage());
            return 1;
        }
    }

    private static void requireRegularOutput(Path destination) throws IOException {
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("A saída deve ser um arquivo regular, sem link simbólico: " + destination);
        }
    }

    private static List<Path> sourceFiles(Path input) throws IOException {
        if (Files.isRegularFile(input) && input.toString().endsWith(".jack")) {
            return List.of(input);
        }
        if (Files.isDirectory(input)) {
            try (Stream<Path> files = Files.list(input)) {
                List<Path> sources = files.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".jack"))
                        .sorted().toList();
                if (!sources.isEmpty()) {
                    return sources;
                }
            }
            throw new IllegalArgumentException("Nenhum arquivo .jack encontrado em " + input);
        }
        throw new IllegalArgumentException("Informe um arquivo .jack ou diretório válido: " + input);
    }
}
