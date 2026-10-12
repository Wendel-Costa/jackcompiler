#!/bin/sh
set -eu

cd "$(dirname "$0")/.."
mvn test
mkdir -p target
output=$(mktemp -d target/validacao-XXXXXX)
reference=src/test/resources/nand2tetris/Square

java -cp target/classes br.edu.ufma.jackcompiler.JackAnalyzer "$reference" "$output"

for name in Main Square SquareGame; do
    # A referência oficial tem CRLF. Não removemos espaços dos tokens.
    tr -d '\r' < "$reference/${name}T.xml" > "$output/${name}.expected.xml"
    diff -u "$output/${name}.expected.xml" "$output/${name}T.xml"
    printf '%s: XML igual à referência oficial\n' "$name"
done

printf 'Validação concluída. Saídas em %s\n' "$output"
