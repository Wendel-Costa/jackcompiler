
package br.edu.ufma.jackcompiler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Tabela de símbolos Jack com dois escopos: classe e subrotina.
 *
 * <p>Os índices são independentes por tipo de variável. Ao iniciar uma
 * subrotina, somente os símbolos locais e os respectivos contadores de
 * {@code argument} e {@code local} são reiniciados.</p>
 */
public final class SymbolTable {

    /** Tipos de variável reconhecidos pela tabela de símbolos. */
    public enum Kind {
        STATIC,
        FIELD,
        ARG,
        VAR
    }

    /** Informação necessária para resolver um identificador na VM. */
    public record Symbol(String name, String type, Kind kind, int index) {
        public Symbol {
            Objects.requireNonNull(name, "O nome do símbolo não pode ser nulo.");
            Objects.requireNonNull(type, "O tipo do símbolo não pode ser nulo.");
            Objects.requireNonNull(kind, "A categoria do símbolo não pode ser nula.");

            if (index < 0) {
                throw new IllegalArgumentException(
                        "O índice do símbolo não pode ser negativo.");
            }
        }
    }

    private final Map<String, Symbol> classScope = new LinkedHashMap<>();
    private final Map<String, Symbol> subroutineScope = new LinkedHashMap<>();
    private final Map<Kind, Integer> counters =
            new java.util.EnumMap<>(Kind.class);

    public SymbolTable() {
        resetCounters();
    }

    /** Limpa ambos os escopos e reinicia todos os índices. */
    public void startClass() {
        classScope.clear();
        subroutineScope.clear();
        resetCounters();
    }

    /** Inicia um novo escopo de subrotina sem apagar os campos da classe. */
    public void startSubroutine() {
        subroutineScope.clear();
        counters.put(Kind.ARG, 0);
        counters.put(Kind.VAR, 0);
    }

    /**
     * Declara um identificador no escopo correspondente e retorna o símbolo
     * criado. Uma declaração duplicada no mesmo escopo é rejeitada.
     */
    public Symbol define(String name, String type, Kind kind) {
        Objects.requireNonNull(name, "O nome do símbolo não pode ser nulo.");
        Objects.requireNonNull(type, "O tipo do símbolo não pode ser nulo.");
        Objects.requireNonNull(kind, "A categoria do símbolo não pode ser nula.");

        if (name.isBlank() || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome e tipo do símbolo não podem ser vazios.");
        }

        Map<String, Symbol> scope =
                isClassKind(kind) ? classScope : subroutineScope;

        if (scope.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Identificador já declarado neste escopo: " + name);
        }

        int index = counters.get(kind);

        Symbol symbol = new Symbol(name, type, kind, index);

        scope.put(name, symbol);
        counters.put(kind, index + 1);

        return symbol;
    }

    /** Quantidade de variáveis declaradas de uma categoria. */
    public int varCount(Kind kind) {
        Objects.requireNonNull(
                kind,
                "A categoria do símbolo não pode ser nula.");

        return counters.get(kind);
    }

    /**
     * Procura primeiro no escopo da subrotina e depois no escopo da classe.
     */
    public Optional<Symbol> resolve(String name) {
        Objects.requireNonNull(
                name,
                "O nome do símbolo não pode ser nulo.");

        Symbol local = subroutineScope.get(name);

        return Optional.ofNullable(
                local != null ? local : classScope.get(name));
    }

    /** Retorna a categoria ou null se o identificador não existir. */
    public Kind kindOf(String name) {
        return resolve(name).map(Symbol::kind).orElse(null);
    }

    /** Retorna o tipo ou null se o identificador não existir. */
    public String typeOf(String name) {
        return resolve(name).map(Symbol::type).orElse(null);
    }

    /** Retorna o índice ou -1 se o identificador não existir. */
    public int indexOf(String name) {
        return resolve(name).map(Symbol::index).orElse(-1);
    }

    private boolean isClassKind(Kind kind) {
        return kind == Kind.STATIC || kind == Kind.FIELD;
    }

    private void resetCounters() {
        for (Kind kind : Kind.values()) {
            counters.put(kind, 0);
        }
    }
}
