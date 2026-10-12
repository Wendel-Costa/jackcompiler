# Origem dos arquivos de teste

Os seis arquivos em `Square/` foram extraídos sem alteração de
`nand2tetris/projects/10/Square/` do pacote oficial do nand2tetris.
São entradas e resultados esperados de teste; não são implementações do compilador.

- Página oficial: https://www.nand2tetris.org/software
- Documento de download vinculado pela página:
  https://drive.google.com/file/d/1IkIR8Pwq3PY49QgXpUJOkUUVht-TKIET/view
- Pacote ZIP indicado pelo documento:
  https://drive.google.com/file/d/1xZzcMIUETv3u3sdpM_oTJSTetpVee3KZ/view
- Obtido em: 12/10/2026.
- SHA-256 do ZIP: `22be29e048f1b90f58dc3c211bb870cc21ca4c680b6ecadff5d9db5bb213a8b4`.

Autoria dos materiais: Noam Nisan e Shimon Schocken, conforme os cabeçalhos
dos fontes e o livro *The Elements of Computing Systems* (MIT Press).
Os avisos originais foram preservados. Os materiais são usados exclusivamente
como referências de validação da atividade, conforme solicitado no enunciado.

| Arquivo | SHA-256 |
|---|---|
| Main.jack | 3037f6b23c08335e8e526e9dfad41a3352f45aa01c88425fe8a80288ad5b5e4b |
| Square.jack | 8b535757b4c622cc0ba2d3738fac9183db289774c77ec4681679c6e879a7bbc7 |
| SquareGame.jack | 4670145e02151af882f4ead680c6c3f0e3f2657d0b597c9f5a762c6665311086 |
| MainT.xml | 06d62d055622afd7c0742035e967387a735f060eebd8c22dec076cf4c00ebf8e |
| SquareT.xml | 1e793360c645d1a83f962571b9421babfec61aafe621278a7f13c542fa96617e |
| SquareGameT.xml | 561d4fa65b78cfb474173613fa410add4c1d6c16d2dc13075ae5aa668cb53228 |

As referências usam CRLF. O teste normaliza apenas os finais de linha para LF;
não remove espaços, não reordena tokens e não gera o esperado com nosso scanner.
