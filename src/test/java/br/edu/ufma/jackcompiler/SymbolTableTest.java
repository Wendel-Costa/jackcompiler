
package br.edu.ufma.jackcompiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SymbolTableTest {

    @Test
    void shouldAssignIndependentIndexesForEachKind() {
        SymbolTable table = new SymbolTable();

        assertEquals(
                0,
                table.define("count", "int", SymbolTable.Kind.STATIC).index());

        assertEquals(
                0,
                table.define("value", "int", SymbolTable.Kind.FIELD).index());

        assertEquals(
                1,
                table.define("enabled", "boolean", SymbolTable.Kind.FIELD).index());

        assertEquals(
                1,
                table.define("total", "int", SymbolTable.Kind.STATIC).index());
    }

    @Test
    void shouldResolveSubroutineScopeBeforeClassScope() {
        SymbolTable table = new SymbolTable();

        table.define("value", "int", SymbolTable.Kind.FIELD);

        table.startSubroutine();

        table.define("value", "boolean", SymbolTable.Kind.VAR);

        assertEquals("boolean", table.typeOf("value"));
        assertEquals(SymbolTable.Kind.VAR, table.kindOf("value"));
        assertEquals(0, table.indexOf("value"));
    }

    @Test
    void shouldPreserveClassScopeWhenStartingSubroutine() {
        SymbolTable table = new SymbolTable();

        table.define("field", "Square", SymbolTable.Kind.FIELD);

        table.startSubroutine();

        table.define("argument", "int", SymbolTable.Kind.ARG);
        table.define("local", "int", SymbolTable.Kind.VAR);

        table.startSubroutine();

        assertTrue(table.resolve("field").isPresent());
        assertFalse(table.resolve("argument").isPresent());

        assertEquals(0, table.varCount(SymbolTable.Kind.ARG));
        assertEquals(0, table.varCount(SymbolTable.Kind.VAR));
    }

    @Test
    void shouldRejectDuplicateNamesInsideTheSameScope() {
        SymbolTable table = new SymbolTable();

        table.define("x", "int", SymbolTable.Kind.FIELD);

        assertThrows(
                IllegalArgumentException.class,
                () -> table.define("x", "boolean", SymbolTable.Kind.STATIC));

        table.startSubroutine();

        table.define("x", "int", SymbolTable.Kind.ARG);

        assertThrows(
                IllegalArgumentException.class,
                () -> table.define("x", "int", SymbolTable.Kind.VAR));
    }

    @Test
    void shouldReturnEmptyForUnknownIdentifiers() {
        SymbolTable table = new SymbolTable();

        assertTrue(table.resolve("missing").isEmpty());
        assertEquals(-1, table.indexOf("missing"));
    }
}
