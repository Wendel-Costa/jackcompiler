package br.edu.ufma.jackcompiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class JackCompilationEngineRegressionTest {
    private String compile(String source) {
        return new JackCompilationEngine(new JackTokenizer(source)).compileClass();
    }

    @ParameterizedTest
    @CsvSource({"+,add", "-,sub", "*,call Math.multiply 2", "/,call Math.divide 2",
            "&,and", "|,or", "<,lt", ">,gt", "=,eq"})
    void translatesEveryBinaryOperator(String operator, String instruction) {
        assertEquals("function Main.f 0\npush constant 6\npush constant 2\n" + instruction + "\nreturn",
                compile("class Main { function int f() { return 6 " + operator + " 2; } }"));
    }

    @ParameterizedTest
    @CsvSource({"true,push constant 1;neg", "false,push constant 0", "null,push constant 0",
            "-2,push constant 2;neg", "~0,push constant 0;not", "(2+3)*4,push constant 2;push constant 3;add;push constant 4;call Math.multiply 2"})
    void translatesConstantsUnaryOperatorsAndParentheses(String expression, String expected) {
        assertEquals("function Main.f 0\n" + expected.replace(';', '\n') + "\nreturn",
                compile("class Main { function int f() { return " + expression + "; } }"));
    }

    @Test void resolvesShadowingAndResetsLocalIndexesBetweenSubroutines() {
        assertEquals("function Main.f 1\npush constant 1\npop local 0\npush local 0\nreturn\n"
                + "function Main.g 1\npush static 0\npop local 0\npush local 0\nreturn",
                compile("class Main { static int x; function int f() { var int x; let x=1; return x; } "
                        + "function int g() { var int y; let y=x; return y; } }"));
    }

    @Test void passesThisBeforeExplicitArgumentsOfUnqualifiedMethod() {
        assertEquals("function Main.f 0\npush argument 0\npop pointer 0\npush pointer 0\npush argument 1\ncall Main.g 2\nreturn\n"
                + "function Main.g 0\npush argument 0\npop pointer 0\npush argument 1\nreturn",
                compile("class Main { method int f(int x) { return g(x); } method int g(int x) { return x; } }"));
    }

    @Test void preservesArrayDestinationAcrossArrayReadOnRightHandSide() {
        assertEquals("function Main.f 0\npush argument 0\npush constant 1\nadd\npush argument 1\npush constant 2\nadd\npop pointer 1\npush that 0\npop temp 0\npop pointer 1\npush temp 0\npop that 0\npush constant 0\nreturn",
                compile("class Main { function void f(Array a, Array b) { let a[1]=b[2]; return; } }"));
    }

    @ParameterizedTest @ValueSource(strings = {
            "class Main { function void f() { let x=1; return; } }",
            "class Main { field int x,x; }",
            "class Main { function void f(int x) { var int x; return; } }"})
    void reportsSemanticErrorsWithPosition(String source) {
        JackCompilationException error = assertThrows(JackCompilationException.class, () -> compile(source));
        assertEquals(1, error.getLine());
        assertTrue(error.getColumn() > 0);
    }
}
