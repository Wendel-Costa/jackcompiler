# JackCompiler — Analisador e gerador de código VM

Projeto acadêmico desenvolvido em **Java 17 ou superior** para a linguagem Jack do curso [Nand2Tetris](https://www.nand2tetris.org/). O repositório reúne o analisador léxico, o analisador sintático com geração de XML e o motor de compilação que traduz programas Jack para código da máquina virtual (`.vm`).

A implementação desta entrega corresponde ao **Project 11 — Code Generation**: cada classe Jack é compilada para um arquivo VM, que pode ser executado no VM Emulator do Nand2Tetris.

## Integrantes

| Integrante | Matrícula |
|---|---|
| Wendel Davi Reis Costa | 2023035735 |
| Yasmin Mendes Ferreira | 2022003156 |

## Funcionalidades

- Análise léxica de palavras reservadas, símbolos, identificadores, constantes inteiras e strings.
- Ignora espaços, quebras de linha e comentários de linha e de bloco.
- Análise sintática por descida recursiva, com geração de XML de tokens e de estrutura sintática.
- Geração de código VM para declarações, expressões, comandos, subrotinas, objetos, arrays e strings.
- Tabela de símbolos com escopo de classe e de subrotina.
- Compilação de um arquivo `.jack` ou de todos os arquivos `.jack` diretamente presentes em um diretório.
- Testes unitários, testes de regressão e testes de integração com os seis programas do Project 11.

## Requisitos de ambiente

- **JDK 17 ou superior** (é necessário ter `java` e `javac`).
- **Maven 3.9 ou superior**.
- Git, caso o projeto seja obtido por clone.
- Internet na primeira execução do Maven para baixar os plugins e a dependência JUnit 5 declarados no `pom.xml`.

O projeto usa `maven.compiler.release=17`. O JAR não é configurado como executável com todas as dependências; por isso, os exemplos de execução abaixo usam diretamente `target/classes` e o nome completo da classe principal.

## Obter o repositório

Se ainda não tiver o repositório na máquina:

```bash
git clone https://github.com/Wendel-Costa/jackcompiler.git
cd jackcompiler
```

Se já tiver o projeto, entre na pasta que contém o arquivo `pom.xml`:

```bash
cd /caminho/para/jackcompiler
```

Todos os comandos Maven e Java deste README devem ser executados a partir dessa pasta raiz.

## Compilar e executar os testes

Para limpar os arquivos gerados anteriormente, compilar o código e executar a suíte de testes:

```bash
mvn clean verify
```

O Maven compila as classes principais e de teste, executa os testes JUnit 5 e percorre as etapas de verificação do ciclo de build. As classes compiladas ficam em:

```text
target/classes/
target/test-classes/
```

Para executar somente os testes, sem limpar o diretório `target/`:

```bash
mvn test
```

Para conferir os XMLs oficiais do Project 10 usando o script do repositório:

```bash
sh scripts/validate-official.sh
```

Esse script executa os testes Maven e utiliza `diff` para comparar as saídas XML com as referências incluídas no repositório. Ele requer um shell compatível com POSIX e o utilitário `diff`.

## Compilar programas Jack para VM

A classe responsável pela linha de comando da geração VM é:

```text
br.edu.ufma.jackcompiler.JackCompiler
```

### Compilar um único arquivo

Formato:

```bash
java -cp target/classes br.edu.ufma.jackcompiler.JackCompiler caminho/para/Classe.jack
```

Exemplo:

```bash
java -cp target/classes br.edu.ufma.jackcompiler.JackCompiler src/test/resources/nand2tetris/Project11/Seven/Main.jack
```

A saída será criada ao lado do fonte:

```text
src/test/resources/nand2tetris/Project11/Seven/
├── Main.jack
└── Main.vm
```

**Atenção:** esse exemplo grava o arquivo `.vm` dentro da pasta de recursos de teste do repositório. Para experimentos e demonstrações, prefira copiar os fontes para uma pasta dentro de `target/`, como mostrado na seção de demonstração.

### Compilar um diretório — requisito obrigatório da atividade

Formato:

```bash
java -cp target/classes br.edu.ufma.jackcompiler.JackCompiler caminho/para/diretorio
```

Exemplo com o programa Square:

```bash
java -cp target/classes br.edu.ufma.jackcompiler.JackCompiler src/test/resources/nand2tetris/Project11/Square
```

O compilador encontra os arquivos `.jack` que estão **diretamente** dentro do diretório e gera um `.vm` para cada um, na mesma pasta:

```text
Square/
├── Main.jack
├── Main.vm
├── Square.jack
├── Square.vm
├── SquareGame.jack
└── SquareGame.vm
```

O comportamento da CLI é o seguinte:

1. Aceita exatamente um argumento: um arquivo `.jack` ou um diretório.
2. Ao receber um diretório, lista somente os arquivos regulares com extensão `.jack` que estão diretamente nele; não percorre subdiretórios.
3. Ordena os fontes pelo nome antes de compilá-los.
4. Gera um arquivo `.vm` com o mesmo nome-base de cada fonte, no próprio diretório de entrada.
5. Ignora arquivos de outras extensões e não copia arquivos do sistema operacional Jack para a pasta.
6. Compila os fontes encontrados antes de começar a gravar as saídas. Assim, um erro de compilação em qualquer fonte impede a gravação daquele lote e preserva arquivos VM preexistentes. Uma falha de I/O durante a gravação ainda pode deixar saídas parciais.
7. Arquivos `.vm` regulares existentes podem ser substituídos pela nova compilação.

Se a entrada for inválida, não for um arquivo `.jack` ou não contiver arquivos `.jack`, a CLI informa o erro e termina com código diferente de zero.

### Compilar os seis programas do Project 11 sem alterar os fontes versionados

Os fontes oficiais utilizados pelos testes estão em `src/test/resources/nand2tetris/Project11/`. Para criar cópias isoladas em `target/demo/`, execute:

```bash
mkdir -p target/demo/Square
cp src/test/resources/nand2tetris/Project11/Square/*.jack target/demo/Square/

# Confirme que a pasta contém somente os fontes Jack antes da compilação.
ls -la target/demo/Square

# Compile o diretório.
java -cp target/classes br.edu.ufma.jackcompiler.JackCompiler target/demo/Square

# Confira os arquivos gerados.
find target/demo/Square -maxdepth 1 -type f -printf '%f\n' | sort
```

Para gerar os arquivos VM dos seis programas em cópias separadas:

```bash
for programa in Seven ConvertToBin Square Average Pong ComplexArrays; do
    mkdir -p "target/demo/$programa"
    cp "src/test/resources/nand2tetris/Project11/$programa/"*.jack "target/demo/$programa/"
    java -cp target/classes br.edu.ufma.jackcompiler.JackCompiler "target/demo/$programa" || exit 1
done
```

A quantidade de arquivos `.vm` esperada por programa é:

| Programa | Fontes Jack | Arquivos VM gerados |
|---|---:|---:|
| Seven | 1 | `Main.vm` |
| ConvertToBin | 1 | `Main.vm` |
| Square | 3 | `Main.vm`, `Square.vm`, `SquareGame.vm` |
| Average | 1 | `Main.vm` |
| Pong | 4 | `Ball.vm`, `Bat.vm`, `Main.vm`, `PongGame.vm` |
| ComplexArrays | 1 | `Main.vm` |

## Executar os arquivos no VM Emulator

A geração dos arquivos `.vm` e a execução no emulador são verificações diferentes. O `JackCompiler` produz o código; o VM Emulator executa esse código.

1. Baixe o pacote de ferramentas do Nand2Tetris pela [página oficial de software](https://www.nand2tetris.org/software) ou utilize o pacote de testes disponibilizado pelo professor.
2. Extraia o pacote em uma pasta de sua escolha.
3. No Linux, abra um terminal na pasta `tools` do pacote e torne o script executável, se necessário:

   ```bash
   chmod +x VMEmulator.sh
   ./VMEmulator.sh
   ```

4. No VM Emulator, use a opção de carregar um programa e selecione a **pasta** que contém os arquivos `.vm` gerados. O emulador carrega os arquivos VM da pasta; não selecione somente um arquivo individual.
5. Inicie a execução com os controles do emulador. Quando o programa depender do teclado, habilite a entrada de teclado no próprio emulador.

Não copie os arquivos VM do sistema operacional Jack (`Math.vm`, `Memory.vm`, `Screen.vm`, `Output.vm`, `Keyboard.vm`, `String.vm`, `Array.vm` e `Sys.vm`) para a pasta do programa. O VM Emulator oficial possui a implementação do sistema operacional embutida.

### Comportamento esperado dos programas

| Programa | O que exercita | Resultado esperado no VM Emulator |
|---|---|---|
| Seven | Expressões aritméticas, `do` e chamada de função do sistema operacional | Imprime o número `7` na tela. |
| ConvertToBin | `while`, condicionais aninhadas, funções e operadores bit a bit | Antes de executar, defina um valor em `RAM[8000]`. Use `no animation`; ao terminar, confira `RAM[8001..8016]`: os valores devem representar os bits do número, usando `0` e `1`, sem `-1`. |
| Square | Construtores, métodos, campos e chamadas entre classes | Exibe um quadrado; as setas movimentam o quadrado e `z`/`x` alteram seu tamanho. |
| Average | Arrays, strings constantes e leitura do teclado | Solicita a quantidade de números, recebe os valores e exibe a média. |
| Pong | Programa orientado a objetos com várias classes | O jogo funciona com bola, raquete e placar. |
| ComplexArrays | Arrays de arrays, índices aninhados e expressões complexas | Para cada teste, o valor obtido deve coincidir com o valor esperado. |

Não há arquivos `.cmp` para esses seis programas do Project 11: a verificação descrita na atividade é executar os programas no VM Emulator e conferir o comportamento.

## Analisador léxico e sintático: geração de XML

Além da geração VM, o repositório mantém a CLI da etapa anterior, implementada em `JackAnalyzer`. Ela gera XML de tokens e XML da estrutura sintática.

Compilar um arquivo e definir a pasta de saída:

```bash
java -cp target/classes br.edu.ufma.jackcompiler.JackAnalyzer \
  src/test/resources/nand2tetris/Square/Main.jack \
  target/xml-main
```

Compilar todos os arquivos Jack diretamente presentes em um diretório:

```bash
java -cp target/classes br.edu.ufma.jackcompiler.JackAnalyzer \
  src/test/resources/nand2tetris/Square \
  target/xml-square
```

A saída contém arquivos `*T.xml` (tokens) e `*P.xml` (árvore sintática). Se o segundo argumento for omitido, a pasta padrão é `target/xml`.

Para executar somente o scanner e gerar o XML de tokens:

```bash
java -cp target/classes br.edu.ufma.jackcompiler.JackAnalyzer \
  --tokens src/test/resources/nand2tetris/Square/Main.jack \
  target/xml-tokens
```

Nesse modo, a pasta de saída padrão é `target/tokens`. A CLI de XML não sobrescreve saídas existentes; escolha outra pasta ou remova as saídas anteriores após conferi-las.

**Diferença entre as duas CLIs:** `JackAnalyzer` gera XML para análise e comparação com as referências da etapa sintática. `JackCompiler` gera instruções `.vm` para execução no VM Emulator. Para a demonstração de geração de código exigida nesta entrega, use `JackCompiler`.

## Organização interna e arquitetura

O código principal está no pacote `br.edu.ufma.jackcompiler`:

| Classe | Responsabilidade |
|---|---|
| `JackTokenizer` | Percorre o código-fonte e produz tokens com tipo, lexema, linha e coluna. |
| `JackParser` | Reconhece a gramática Jack por descida recursiva e gera a estrutura sintática utilizada pela saída XML. |
| `TokenXmlWriter` | Serializa os tokens no formato XML. |
| `SyntaxXmlWriter` | Serializa a árvore sintática no formato XML. |
| `JackAnalyzer` | Integra tokenizer, parser e escritores XML na CLI da etapa sintática. |
| `JackCompilationEngine` | Consome os tokens do `JackTokenizer`, reconhece as construções Jack e produz código VM durante a análise. |
| `SymbolTable` | Armazena nome, tipo, categoria e índice dos identificadores nos escopos de classe e subrotina. |
| `VMWriter` | Escreve e valida comandos da máquina virtual. |
| `JackCompiler` | Recebe arquivo ou diretório, coordena a compilação e grava um `.vm` por fonte. |

O tokenizer é compartilhado pelas duas linhas de processamento. Para a saída XML, o fluxo é `JackTokenizer → JackParser → SyntaxXmlWriter/TokenXmlWriter`. Para a saída VM, o fluxo é `JackTokenizer → JackCompilationEngine → SymbolTable + VMWriter → arquivo .vm`. O motor de compilação realiza a análise sintática necessária à tradução e emite VM durante esse processamento; ele não lê o XML produzido pelo `JackParser`.

### Tabela de símbolos

A tabela utiliza dois escopos:

- **Escopo da classe:** símbolos `static` e `field`.
- **Escopo da subrotina:** símbolos `arg` e `var`.

O mapeamento para os segmentos VM é:

| Categoria Jack | Segmento VM |
|---|---|
| `static` | `static` |
| `field` | `this` |
| parâmetro (`arg`) | `argument` |
| variável local (`var`) | `local` |

A resolução procura primeiro no escopo da subrotina e depois no escopo da classe, permitindo que uma variável local tenha precedência sobre um atributo de mesmo nome. Em métodos, o objeto corrente é registrado como argumento de índice zero; os parâmetros explícitos começam no índice um.

### Geração VM

A `JackCompilationEngine` e o `VMWriter` implementam, entre outros, os seguintes comportamentos:

- Números inteiros: `push constant n`.
- `false` e `null`: `push constant 0`.
- `true`: `push constant 1` seguido de `neg` (representação VM `-1`).
- Operações aritméticas e lógicas: `add`, `sub`, `neg`, `eq`, `gt`, `lt`, `and`, `or` e `not`.
- Multiplicação e divisão: chamadas `Math.multiply` e `Math.divide`.
- Comandos `let`, `if`/`else`, `while`, `do` e `return`, com rótulos de desvio exclusivos.
- Comandos `do`: o valor retornado é descartado com `pop temp 0`.
- Retornos `void`: empilham zero antes de `return`.
- Construtores: alocam o número de campos com `Memory.alloc` e inicializam `pointer 0`.
- Métodos: inicializam `pointer 0` com `argument 0`.
- Chamadas de métodos de objetos: empilham a instância como argumento implícito e consultam o tipo do objeto na tabela de símbolos para formar o nome da chamada.
- Arrays: calculam o endereço-base mais o índice e utilizam `pointer 1` e `that 0`.
- Strings literais: utilizam `String.new` e chamadas sucessivas a `String.appendChar`.

Os arquivos `.vm` são gravados com um comando por linha e utilizam os segmentos e comandos definidos pela máquina virtual do Nand2Tetris.

## Uso de Inteligência Artificial

O uso de IA generativa foi permitido como apoio pela atividade
