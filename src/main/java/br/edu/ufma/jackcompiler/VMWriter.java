
package br.edu.ufma.jackcompiler;

import java.util.Objects;
import java.util.Set;

/** Escreve comandos da linguagem de máquina virtual do nand2tetris. */
public final class VMWriter {

    private static final Set<String> SEGMENTS = Set.of(
            "constant",
            "argument",
            "local",
            "static",
            "this",
            "that",
            "pointer",
            "temp"
    );

    private static final Set<String> ARITHMETIC_COMMANDS = Set.of(
            "add",
            "sub",
            "neg",
            "eq",
            "gt",
            "lt",
            "and",
            "or",
            "not"
    );

    private final StringBuilder output = new StringBuilder();

    public void writePush(String segment, int index) {
        validateSegment(segment, index, false);
        append("push " + segment + " " + index);
    }

    public void writePop(String segment, int index) {
        validateSegment(segment, index, true);
        append("pop " + segment + " " + index);
    }

    public void writeArithmetic(String command) {
        Objects.requireNonNull(
                command,
                "O comando aritmético não pode ser nulo.");

        if (!ARITHMETIC_COMMANDS.contains(command)) {
            throw new IllegalArgumentException(
                    "Comando aritmético inválido: " + command);
        }

        append(command);
    }

    public void writeLabel(String label) {
        append("label " + requireName(label, "rótulo"));
    }

    public void writeGoto(String label) {
        append("goto " + requireName(label, "rótulo"));
    }

    public void writeIf(String label) {
        append("if-goto " + requireName(label, "rótulo"));
    }

    public void writeCall(String name, int argumentCount) {
        validateCount(argumentCount, "quantidade de argumentos");

        append(
                "call "
                + requireName(name, "subrotina")
                + " "
                + argumentCount
        );
    }

    public void writeFunction(String name, int localCount) {
        validateCount(localCount, "quantidade de variáveis locais");

        append(
                "function "
                + requireName(name, "função")
                + " "
                + localCount
        );
    }

    public void writeReturn() {
        append("return");
    }

    /** Retorna todo o código gerado, com um comando por linha. */
    public String toCode() {
        return output.toString();
    }

    @Override
    public String toString() {
        return toCode();
    }

    private void append(String command) {
        if (!output.isEmpty()) {
            output.append('\n');
        }

        output.append(command);
    }

    private void validateSegment(
            String segment,
            int index,
            boolean isPop) {

        Objects.requireNonNull(
                segment,
                "O segmento não pode ser nulo.");

        if (!SEGMENTS.contains(segment)) {
            throw new IllegalArgumentException(
                    "Segmento VM inválido: " + segment);
        }

        validateCount(index, "índice do segmento");

        if (isPop && "constant".equals(segment)) {
            throw new IllegalArgumentException(
                    "Não existe o comando pop constant.");
        }

        if ("pointer".equals(segment) && index > 1) {
            throw new IllegalArgumentException(
                    "O segmento pointer aceita somente os índices 0 e 1.");
        }

        if ("temp".equals(segment) && index > 7) {
            throw new IllegalArgumentException(
                    "O segmento temp aceita índices de 0 a 7.");
        }
    }

    private void validateCount(int value, String description) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "A " + description + " não pode ser negativa.");
        }
    }

    private String requireName(String value, String description) {
        Objects.requireNonNull(
                value,
                "O nome da " + description + " não pode ser nulo.");

        if (value.isBlank()
                || value.chars().anyMatch(Character::isWhitespace)) {

            throw new IllegalArgumentException(
                    "Nome inválido para " + description + ": " + value);
        }

        return value;
    }
}
