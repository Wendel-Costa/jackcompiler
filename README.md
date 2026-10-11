# JackCompiler

Projeto acadêmico de implementação de um compilador para a
linguagem Jack, baseado na abordagem do nand2tetris.


## Integrantes

- Nome: Wendel Davi Reis Costa

- Nome: Yasmin Mendes Ferreira

## Tecnologias

- Java 17 ou superior
- Maven
- JUnit 5 para testes automatizados
- Git para controle de versão

## Referência de implementação

O desenvolvimento do analisador léxico terá como base o manual
de implementação disponibilizado pelo professor e os materiais
do projeto nand2tetris.

A implementação será construída gradualmente, contemplando
as cinco categorias de tokens de Jack:

- keyword
- symbol
- integerConstant
- stringConstant
- identifier

## Estratégia de testes

Os testes serão desenvolvidos junto com as funcionalidades
do analisador léxico.

A validação final da primeira etapa será feita comparando a
saída XML produzida pelo programa com os arquivos de referência
oficiais do nand2tetris para Main, Square e SquareGame.

## Estrutura prevista

```text
jackcompiler/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── br/edu/ufma/jackcompiler/
│   └── test/
│       └── java/
│           └── br/edu/ufma/jackcompiler/
├── scripts/
├── .gitignore
├── pom.xml
└── README.md