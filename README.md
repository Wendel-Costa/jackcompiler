# JackCompiler

Projeto acadêmico em **Java 17**, baseado na linguagem Jack do nand2tetris.
Esta entrega implementa o analisador léxico. Parser, Compilation Engine e
código VM serão desenvolvidos nas próximas etapas.

## Dupla

| Nome | Matrícula |
|---|---|
| Wendel Davi Reis Costa | 2023035735 |
| Yasmin Mendes Ferreira | 2022003156 |

## Compilação

Requisitos: JDK 17 ou superior e Maven 3.9 ou superior. Confira `java -version`,
`javac -version` e `mvn -version`. A primeira execução precisa de internet para
baixar as dependências declaradas no `pom.xml`, incluindo JUnit 5.

Abra a pasta `jackcompiler` no VS Code e execute no terminal integrado:

```sh
mvn clean verify
```

Isso compila, executa os testes e gera o JAR em `target/`.
No macOS com Java 17 instalado pelo Homebrew, se Java não estiver no PATH:

```sh
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

Maven também precisa estar instalado e disponível no PATH.

## Execução

Depois de compilar, processe um arquivo ou todos os arquivos `.jack` de uma pasta:

```sh
java -cp target/classes br.edu.ufma.jackcompiler.JackAnalyzer src/test/resources/nand2tetris/Square/Main.jack target/exemplo-main
java -cp target/classes br.edu.ufma.jackcompiler.JackAnalyzer src/test/resources/nand2tetris/Square target/exemplo-square
```

O segundo argumento é o diretório de saída; se omitido, usa `target/tokens`.
`Main.jack` produz `MainT.xml`. Leitura e escrita usam UTF-8. A busca na pasta
não é recursiva e ignora outras extensões. Caminhos com espaços devem estar entre aspas.

A CLI recusa sobrescrever XMLs existentes para proteger as referências. Para
repetir, escolha outra pasta de saída ou remova as saídas anteriores após revisão.
Sucesso retorna código 0; entrada inválida, erro léxico ou erro de arquivo retorna 1.
Erros léxicos informam arquivo e linha em stderr. Em uma pasta, arquivos anteriores
à falha podem ter sido gerados; o arquivo com erro léxico não produz XML parcial.

## Reconhecimento e ruído

`nextToken()` retorna tipo, lexema e linha. Reconhece 21 palavras reservadas,
19 símbolos (incluindo colchetes), inteiros de 0 a 32767, strings e identificadores.
Identificadores começam com letra ASCII ou `_`, e continuam com letras, números
ou `_`. Strings preservam Unicode e espaços, removem as aspas delimitadoras,
não interpretam escapes e não aceitam quebras de linha. `-12` gera dois tokens.

Espaços, tabs, quebras de linha, `//`, `/* ... */` e `/** ... */` são ignorados.
Comentários de bloco não são aninhados: o primeiro `*/` fecha o comentário.
`/` permanece divisão quando não inicia comentário. Marcadores dentro de strings
são preservados. LF, CRLF e CR contam como uma quebra de linha.
Strings e comentários sem fechamento, inteiros fora da faixa e caracteres
inesperados causam erro léxico. EOF encerra a leitura internamente.

## Estratégia de testes: XML e JUnit 5

Optamos pelo XML oficial, complementado por testes unitários. Cada token ocupa
uma linha com espaços internos nas tags, dentro de `<tokens>` e `</tokens>`.
EOF é omitido. `<`, `>`, `&` e `"` são escapados como `&lt;`, `&gt;`, `&amp;`
e `&quot;`. Exemplo:

```xml
<tokens>
<keyword> let </keyword>
<identifier> x </identifier>
<symbol> = </symbol>
<integerConstant> 7 </integerConstant>
<symbol> ; </symbol>
</tokens>
```

Execute:

```sh
mvn test
```

São **46 casos executados**:

| Classe | Casos | Cobertura |
|---|---:|---|
| JackTokenizerTest | 34 | Todas as palavras reservadas e símbolos; identificadores; limites; strings; ruído; linhas; erros |
| TokenXmlWriterTest | 4 | Categorias, tags, espaços, escapes e ausência de EOF |
| JackAnalyzerTest | 5 | Arquivo UTF-8, pasta, saída, erros e proteção das referências |
| OfficialTokensTest | 3 | XML integral de Main, Square e SquareGame |

O teste oficial lê os três fontes em `src/test/resources/nand2tetris/Square/`
e compara o resultado com os respectivos `*T.xml` extraídos do pacote oficial.
Essas referências são independentes: não são geradas pelo nosso scanner.
A asserção de igualdade integral detecta tokens extras, ausentes, fora de ordem,
tipos incorretos e diferenças de lexema, tags e escapes.
Normaliza **somente finais de linha** (CRLF/CR para LF); não remove espaços nem
altera conteúdo de strings. Origem, autoria e SHA-256 estão em
[ORIGEM.md](src/test/resources/nand2tetris/ORIGEM.md).

Para gerar XMLs e repetir a comparação via `diff`:

```sh
sh scripts/validate-official.sh
```

O script requer shell POSIX e `diff` (macOS/Linux ou Git Bash), executa `mvn test`
e cria uma pasta nova em `target/` a cada execução. Os testes Maven também
funcionam no Windows sem depender do script.

## Organização e reutilização

```text
jackcompiler/
├── src/main/java/br/edu/ufma/jackcompiler/
│   ├── TokenType.java
│   ├── Token.java
│   ├── JackTokenizer.java
│   ├── TokenXmlWriter.java
│   └── JackAnalyzer.java
├── src/test/java/br/edu/ufma/jackcompiler/
├── src/test/resources/nand2tetris/
├── scripts/validate-official.sh
├── .gitignore
├── .gitattributes
├── pom.xml
└── README.md
```

O tokenizer recebe uma String e permanece independente de arquivos e XML.
O parser poderá consumir `nextToken()` diretamente. `TokenXmlWriter` serializa
para validação; `JackAnalyzer` cuida da CLI e dos arquivos. Os tipos detalhados
em `TokenType` podem ser reutilizados pelo analisador sintático.

## Referências e entrega

- [Projeto 10: padrão de tokens e validação](https://www.nand2tetris.org/project10).
- [Pacote oficial de testes](https://www.nand2tetris.org/software).
- Enunciado da Entrega Parcial 1 da disciplina.

Os fontes e XMLs de teste oficiais são materiais de Nisan e Schocken, com os
avisos de autoria preservados. A implementação Java é separada desses materiais.

Antes de entregar, preencha as matrículas, execute `mvn clean verify`, revise as
alterações e registre cada etapa com uma mensagem significativa. Confira pelo
menos 10 commits com `git log --oneline`. Envie pelo VS Code para o GitHub.
Apenas um membro envia o ZIP no SIGAA e insere o link do repositório nos comentários.
Use o ZIP do GitHub após o envio, ou inclua `.git` no ZIP local para preservar
histórico. Não inclua `target/` nem arquivos pessoais do editor.
