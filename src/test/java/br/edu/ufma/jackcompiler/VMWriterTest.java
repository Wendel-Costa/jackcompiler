
package br.edu.ufma.jackcompiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VMWriterTest {

    @Test
    void shouldWritePushArithmeticAndCallCommands() {
        VMWriter writer = new VMWriter();

        writer.writePush("constant", 5);
        writer.writeArithmetic("add");
        writer.writeCall("Math.abs", 1);

        assertEquals(
                "push constant 5\nadd\ncall Math.abs 1",
                writer.toCode());
    }

    @Test
    void shouldWriteAllCommandFamilies() {
        VMWriter writer = new VMWriter();

        writer.writePop("local", 2);
        writer.writeLabel("WHILE_EXP0");
        writer.writeGoto("WHILE_EXP0");
        writer.writeIf("IF_TRUE0");
        writer.writeFunction("Main.main", 1);
        writer.writeReturn();

        assertEquals(
                String.join(
                        "\n",
                        "pop local 2",
                        "label WHILE_EXP0",
                        "goto WHILE_EXP0",
                        "if-goto IF_TRUE0",
                        "function Main.main 1",
                        "return"),
                writer.toCode());
    }

    @Test
    void shouldRejectInvalidSegmentsAndCommands() {
        VMWriter writer = new VMWriter();

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.writePush("global", 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.writePop("constant", 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.writePush("pointer", 2));

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.writeArithmetic("multiply"));
    }

    @Test
    void shouldRejectNegativeIndexesAndArgumentCounts() {
        VMWriter writer = new VMWriter();

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.writePush("local", -1));

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.writeCall("Main.test", -1));

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.writeFunction("Main.test", -1));
    }
}
