package ee.webmedia.alfresco.search.lucene;

import java.io.IOException;
import java.io.Reader;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.impl.lucene.analysis.MLTokenDuplicator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

public class SimplePhraseAnalyzer extends Analyzer {

    MLAnalysisMode mode;

    public SimplePhraseAnalyzer(MLAnalysisMode mode) {
        this.mode = mode;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new MLTokenDuplicator(new SimplePhraseTokenizer(reader), I18NUtil.getLocale(), reader, mode);
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName,
            final Reader reader) throws IOException {
        Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
        if (tokenizer == null) {
            tokenizer = new MLTokenDuplicator(new SimplePhraseTokenizer(reader), I18NUtil.getLocale(), reader, mode);
            setPreviousTokenStream(tokenizer);
        } else {
            tokenizer.reset(reader);
        }
        return tokenizer;
    }
}