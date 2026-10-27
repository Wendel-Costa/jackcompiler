package br.edu.ufma.jackcompiler;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

/** CLI para gerar XML de tokens e da árvore sintática Jack. */
public final class JackAnalyzer {

    private JackAnalyzer() {
    }

    public static void main(String[] args) {
        int result = run(args, System.out, System.err);
        if (result != 0) {
            System.exit(result);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        boolean tokensOnly = args.length > 0 && args[0].equals("--tokens");
        int offset = tokensOnly ? 1 : 0;
        if (args.length - offset < 1 || args.length - offset > 2) {
            err.println("Uso: JackAnalyzer [--tokens] <arquivo.jack|diretório> [diretório de saída]");
            return 1;
        }

        try {
            Path input = Path.of(args[offset]);
            Path output = args.length - offset == 2 ? Path.of(args[offset + 1])
                    : Path.of("target", tokensOnly ? "tokens" : "xml");
            List<Path> sources = sourceFiles(input);
            Files.createDirectories(output);

            for (Path source : sources) {
                String tokenXml;
                String syntaxXml = null;
                try {
                    String content = Files.readString(source, StandardCharsets.UTF_8);
                    tokenXml = TokenXmlWriter.toXml(new JackTokenizer(content));
                    if (!tokensOnly) {
                        syntaxXml = new JackParser(new JackTokenizer(content)).parseClass();
                    }
                } catch (IllegalArgumentException | JackSyntaxException exception) {
                    throw new IllegalArgumentException(source + ": " + exception.getMessage(), exception);
                }
                String name = source.getFileName().toString();
                String baseName = name.substring(0, name.length() - 5);
                Path tokenDestination = output.resolve(baseName + "T.xml");
                Path syntaxDestination = output.resolve(baseName + "P.xml");
                requireNewOutput(tokenDestination);
                if (!tokensOnly) {
                    requireNewOutput(syntaxDestination);
                }
                Files.writeString(tokenDestination, tokenXml, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                out.println("Gerado: " + tokenDestination);
                if (!tokensOnly) {
                    Files.writeString(syntaxDestination, syntaxXml, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                    out.println("Gerado: " + syntaxDestination);
                }
            }
            return 0;
        } catch (IOException | IllegalArgumentException exception) {
            err.println("Erro: " + exception.getMessage());
            return 1;
        }
    }

    private static void requireNewOutput(Path destination) throws IOException {
        // Um link com destino ausente também ocupa o nome de saída.
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("A saída já existe: " + destination
                    + ". Use outro diretório ou remova a saída anterior.");
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
