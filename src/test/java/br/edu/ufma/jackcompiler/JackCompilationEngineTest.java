
package br.edu.ufma.jackcompiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JackCompilationEngineTest {

    @Test
    void shouldCompileArithmeticAndDiscardDoReturnValue() {
        String source = """
                class Main {
                    function void main() {
                        do Output.printInt(1 + 2 * 3);
                        return;
                    }
                }
                """;

        String vm = compile(source);

        assertEquals(String.join("\n",
                "function Main.main 0",
                "push constant 1",
                "push constant 2",
                "add",
                "push constant 3",
                "call Math.multiply 2",
                "call Output.printInt 1",
                "pop temp 0",
                "push constant 0",
                "return"), vm);
    }

    @Test
    void shouldCompileIfElseAndWhileWithUniqueLabels() {
        String source = """
                class Main {
                    function void main() {
                        var int i;
                        let i = 0;
                        while (i < 3) {
                            if (i = 1) {
                                let i = i + 1;
                            } else {
                                let i = i + 2;
                            }
                        }
                        return;
                    }
                }
                """;

        String vm = compile(source);

        assertTrue(vm.contains("label WHILE_EXP0"));
        assertTrue(vm.contains("if-goto IF_TRUE1"));
        assertTrue(vm.contains("label IF_FALSE1"));
        assertTrue(vm.contains("goto WHILE_EXP0"));
        assertTrue(vm.contains("label WHILE_END0"));
    }

    @Test
    void shouldCompileMethodArgumentsWithImplicitThisArgument() {
        String source = """
                class Main {
                    method int sum(int a, int b) {
                        return a + b;
                    }
                }
                """;

        String vm = compile(source);

        assertEquals(String.join("\n",
                "function Main.sum 0",
                "push argument 0",
                "pop pointer 0",
                "push argument 1",
                "push argument 2",
                "add",
                "return"), vm);
    }

    @Test
    void shouldAllocateConstructorUsingNumberOfFields() {
        String source = """
                class Point {
                    field int x, y;
                    constructor Point new(int initialX, int initialY) {
                        let x = initialX;
                        let y = initialY;
                        return this;
                    }
                }
                """;

        String vm = compile(source);

        assertTrue(vm.contains(
                "push constant 2\ncall Memory.alloc 1\npop pointer 0"));

        assertTrue(vm.endsWith("push pointer 0\nreturn"));
    }

    @Test
    void shouldPassObjectAsFirstArgumentInMethodCall() {
        String source = """
                class Caller {
                    function void run(Square square) {
                        do square.draw();
                        return;
                    }
                }
                """;

        String vm = compile(source);

        assertTrue(vm.contains(
                "push argument 0\ncall Square.draw 1"));
    }

    @Test
    void shouldCompileArrayReadAndWriteWithCorrectStackOrder() {
        String source = """
                class Arrays {
                    function void update(Array values, int index, int value) {
                        let values[index] = value;
                        do Output.printInt(values[index]);
                        return;
                    }
                }
                """;

        String vm = compile(source);

        assertTrue(vm.contains(String.join("\n",
                "push argument 0",
                "push argument 1",
                "add",
                "push argument 2",
                "pop temp 0",
                "pop pointer 1",
                "push temp 0",
                "pop that 0")));

        assertTrue(vm.contains(String.join("\n",
                "push argument 0",
                "push argument 1",
                "add",
                "pop pointer 1",
                "push that 0",
                "call Output.printInt 1")));
    }

    @Test
    void shouldCreateStringAndAppendEachCharacter() {
        String source = """
                class Main {
                    function void main() {
                        do Output.printString("Hi");
                        return;
                    }
                }
                """;

        String vm = compile(source);

        assertTrue(vm.contains(String.join("\n",
                "push constant 2",
                "call String.new 1",
                "push constant 72",
                "call String.appendChar 2",
                "push constant 105",
                "call String.appendChar 2",
                "call Output.printString 1")));
    }

    @Test
    void shouldCompileNestedArrayIndexes() {
        String source = """
                class Complex {
                    function int calculate(Array a, Array b) {
                        return a[b[a[3]]];
                    }
                }
                """;

        String vm = compile(source);

        assertTrue(vm.contains("pop pointer 1\npush that 0"));
        assertTrue(vm.contains(
                "push that 0\nadd\npop pointer 1\npush that 0"));
    }

    private String compile(String source) {
        return new JackCompilationEngine(
                new JackTokenizer(source)).compileClass();
    }
}
