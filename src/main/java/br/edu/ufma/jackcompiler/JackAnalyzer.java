package br.edu.ufma.jackcompiler;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Ponto de entrada da entrega léxica; futuramente integrará o parser. */
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
        if (args.length < 1 || args.length > 2) {
            err.println("Uso: JackAnalyzer <arquivo.jack|diretório> [diretório de saída]");
            return 1;
        }

        try {
            Path input = Path.of(args[0]);
            Path output = args.length == 2 ? Path.of(args[1]) : Path.of("target", "tokens");
            List<Path> sources = sourceFiles(input);
            Files.createDirectories(output);

            for (Path source : sources) {
                String xml;
                try {
                    xml = TokenXmlWriter.toXml(new JackTokenizer(
                            Files.readString(source, StandardCharsets.UTF_8)));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(source + ": " + exception.getMessage(), exception);
                }
                String name = source.getFileName().toString();
                Path destination = output.resolve(name.substring(0, name.length() - 5) + "T.xml");
                if (Files.exists(destination)) {
                    throw new IOException("A saída já existe: " + destination
                            + ". Use outro diretório ou remova a saída anterior.");
                }
                Files.writeString(destination, xml, StandardCharsets.UTF_8);
                out.println("Gerado: " + destination);
            }
            return 0;
        } catch (IOException | IllegalArgumentException exception) {
            err.println("Erro: " + exception.getMessage());
            return 1;
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
