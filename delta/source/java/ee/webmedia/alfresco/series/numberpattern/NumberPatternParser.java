package ee.webmedia.alfresco.series.numberpattern;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * @author Keit Tehvan
 */
public class NumberPatternParser {
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");
    private static final Pattern TENTATIVE_PATTERN = Pattern.compile("/\\*(.*?)\\*/"); // Dokumentide loetelu - Sarjad.docx punkt 5.1.6.9

    private Set<String> allParams;
    private Set<String> invalidParams;
    private final String initialInput;
    private static final String[] allAllowedParams = new String[] { "S", "T", "TA", "TN", "DA", "DN" };
    private static final String[] allAllowedParamsWithDigit = new String[] { "TN", "DN" };

    public NumberPatternParser(String input) {
        initialInput = input;
        if (isBlank()) {
            return;
        }
        // Matcher matcher = TENTATIVE_PATTERN.matcher("{S}/*/{TA}{3TN}*/{TR}/*/{TA}/*/{TA}{TN}*/{3TN}*//{DN}"); // this one is really problematic

        Matcher matcher = PARAM_PATTERN.matcher(initialInput);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (!Arrays.asList(allAllowedParams).contains(match) || isParamWithDigit(match)) {
                getInvalidParams().add(match);
            }
            getAllParams().add(match);
        }
    }

    public boolean isBlank() {
        return StringUtils.isBlank(initialInput);
    }

    private boolean isParamWithDigit(String match) {
        for (String allowed : allAllowedParamsWithDigit) {
            if (matchesParamWithDigit(match, allowed)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesParamWithDigit(String paramToMatch, String allowed) {
        return paramToMatch.matches("^[0-9]" + allowed + "$");
    }

    public boolean containsParam(String param) {
        if (Arrays.asList(allAllowedParamsWithDigit).contains(param)) {
            for (String existing : getAllParams()) {
                if (matchesParamWithDigit(existing, param)) {
                    return true;
                }
            }
        }
        return getAllParams().contains(param);
    }

    public Set<String> getAllParams() {
        if (allParams == null) {
            allParams = new HashSet<String>();
        }
        return allParams;
    }

    public Set<String> getInvalidParams() {
        if (invalidParams == null) {
            invalidParams = new HashSet<String>();
        }
        return invalidParams;
    }

    public boolean isValid() {
        return getInvalidParams().isEmpty();
    }

    public String getInitialInput() {
        return initialInput;
    }
}
