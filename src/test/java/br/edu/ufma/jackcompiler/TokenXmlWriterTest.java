package br.edu.ufma.jackcompiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenXmlWriterTest {

    @Test
    void shouldSerializeAllTokenCategoriesWithInternalSpaces() {
        String actual = TokenXmlWriter.toXml(new JackTokenizer("let x = 7; \"Jack\""));
        assertEquals("<tokens>\n"
                + "<keyword> let </keyword>\n"
                + "<identifier> x </identifier>\n"
                + "<symbol> = </symbol>\n"
                + "<integerConstant> 7 </integerConstant>\n"
                + "<symbol> ; </symbol>\n"
                + "<stringConstant> Jack </stringConstant>\n"
                + "</tokens>\n", actual);
    }

    @Test
    void shouldEscapeXmlSymbolsAndStringContent() {
        String actual = TokenXmlWriter.toXml(new JackTokenizer("<>& \"a < b & c > d\""));
        assertEquals("<tokens>\n"
                + "<symbol> &lt; </symbol>\n"
                + "<symbol> &gt; </symbol>\n"
                + "<symbol> &amp; </symbol>\n"
                + "<stringConstant> a &lt; b &amp; c &gt; d </stringConstant>\n"
                + "</tokens>\n", actual);
        assertEquals("&quot;&amp;&lt;&gt;", TokenXmlWriter.escape("\"&<>"));
        assertEquals("&amp;lt;", TokenXmlWriter.escape("&lt;"));
    }

    @Test
    void shouldOmitEofAndCommentsFromEmptyInput() {
        assertEquals("<tokens>\n</tokens>\n",
                TokenXmlWriter.toXml(new JackTokenizer("/* vazio */")));
    }

    @Test
    void shouldPreserveSpacesInStringContent() {
        assertEquals("<tokens>\n<stringConstant>   Jack   </stringConstant>\n</tokens>\n",
                TokenXmlWriter.toXml(new JackTokenizer("\"  Jack  \"")));
    }
}
