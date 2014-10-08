package ee.webmedia.alfresco.utils;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.encoding.AbstractCharactersetFinder;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class HtmlCharsetFinder extends AbstractCharactersetFinder {

    // http://www.lemoda.net/regex/meta-charset/index.html
    // (<\s*meta\s+http-equiv\s*=\s*["']?content-type["']?\s*content=["']?\w+/\w+["']?;\s*charset=)([^"'>]+)(["']?\s*/?>)

    private final Pattern[] patterns = new Pattern[] {
            Pattern.compile(
                    "(<\\s*meta\\s+http-equiv\\s*=\\s*[\"']?content-type[\"']?\\s*content=[\"']?\\w+/\\w+[\"']?;\\s*charset=)([^\"'>]+)([\"']?\\s*/?>)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "(<\\s*meta\\s+content=[\"']?\\w+/\\w+[\"']?;\\s*charset=)([^\"'>]+)([\"']?\\s*http-equiv\\s*=\\s*[\"']?content-type[\"']?\\s*/?>)",
                    Pattern.CASE_INSENSITIVE)
    };

    @Override
    protected Charset detectCharsetImpl(byte[] buffer) throws Exception {
        String text = new String(buffer, "US-ASCII");
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String charset = matcher.group(2);
                try {
                    return Charset.forName(charset);
                } catch (UnsupportedCharsetException e) {
                    return null;
                }
            }
        }
        return null;
    }

}
