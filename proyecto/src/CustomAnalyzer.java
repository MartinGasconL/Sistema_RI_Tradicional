//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.es.SpanishLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.IOUtils;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * Personalized spanish analizer.
 */
public final class CustomAnalyzer extends StopwordAnalyzerBase {

    public static CharArraySet getDefaultStopSet() throws IOException {
        return WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class, "spanish_stop.txt", StandardCharsets.UTF_8));
    }

    public CustomAnalyzer() throws IOException {
        super(getDefaultStopSet());
    }

    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        result = new StopFilter(result, this.stopwords);
        result = new SpanishLightStemFilter(result);
        return new TokenStreamComponents(source, result);
    }

    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }
}
