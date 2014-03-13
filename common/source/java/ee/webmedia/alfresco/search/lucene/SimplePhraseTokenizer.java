package ee.webmedia.alfresco.search.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

import org.alfresco.i18n.I18NUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.Token;

/**
 * Emits the input as a single token excluding locale and preceding spaces
 */
public class SimplePhraseTokenizer extends KeywordTokenizer {
    private static final String SPACE = "\u0000";
    private boolean done;

    public SimplePhraseTokenizer(Reader input) {
        super(input);
    }

    @Override
    public Token next(Token reusableToken) throws IOException {
        if (done) {
            return null;
        }
        reusableToken = super.next(reusableToken);
        if (reusableToken != null) {
            done = true;
            boolean termChanged = false;
            Locale locale = I18NUtil.getLocale();
            String localeStr = locale.toString();
            String language = locale.getLanguage();

            String term = reusableToken.term();
            if (StringUtils.startsWith(term, SPACE)) {
                term = StringUtils.trim(term);
                termChanged = true;
            }
            if (StringUtils.startsWith(term, language + SPACE) || StringUtils.startsWith(term, localeStr + SPACE)) {
                int firstSpace = term.indexOf(SPACE);
                term = StringUtils.substring(term, firstSpace + 1);
                termChanged = true;
            }
            if (termChanged) {
                reusableToken.setTermBuffer(term, 0, term.length());
            }
        }
        return reusableToken;
    }
}