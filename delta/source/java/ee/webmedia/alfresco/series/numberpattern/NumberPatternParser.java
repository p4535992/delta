package ee.webmedia.alfresco.series.numberpattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * @author Keit Tehvan
 */
public class NumberPatternParser {

    /**
     * @author Kaarel Jõgeva
     */
    public enum RegisterNumberPatternParams {
        S, T, TA, TN(true), DA, DN(true);

        private boolean digitAllowed;

        private RegisterNumberPatternParams() {
            digitAllowed = false;
        }

        private RegisterNumberPatternParams(boolean digitAllowed) {
            this.digitAllowed = digitAllowed;
        }

        public Boolean isDigitAllowed() {
            return digitAllowed;
        }

        public void setDigitAllowed(boolean digitAllowed) {
            this.digitAllowed = digitAllowed;
        }

        public static RegisterNumberPatternParams getValidParam(String param) {
            for (RegisterNumberPatternParams value : values()) {
                String name = value.name();
                if (name.equals(param)) {
                    return value;
                }
            }
            return getValidDigitParam(param);
        }

        public static RegisterNumberPatternParams getValidDigitParam(String param) {
            for (RegisterNumberPatternParams value : values()) {
                if (value.isDigitAllowed() && matchesParamWithDigit(param, value.name())) {
                    return value;
                }
            }
            return null;
        }

        public static boolean matchesParamWithDigit(String paramToMatch, String allowed) {
            return paramToMatch.matches("^[0-9]" + allowed + "$");
        }

        public static boolean matchesAnyParamWithDigit(String paramToMatch) {
            List<RegisterNumberPatternParams> digitParams = new ArrayList<RegisterNumberPatternParams>();
            for (RegisterNumberPatternParams param : values()) {
                if (param.isDigitAllowed()) {
                    digitParams.add(param);
                }
            }
            return paramToMatch.matches("^[0-9]?" + StringUtils.join(digitParams, '|') + "$");
        }
    }

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");

    private Set<String> allParams;
    private Set<String> invalidParams;
    private final String initialInput;

    public NumberPatternParser(String input) {
        initialInput = input;
        if (isBlank()) {
            return;
        }

        Matcher matcher = PARAM_PATTERN.matcher(initialInput);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (RegisterNumberPatternParams.getValidParam(match) == null) {
                getInvalidParams().add(match);
            }
            getAllParams().add(match);
        }
    }

    public boolean isBlank() {
        return StringUtils.isBlank(initialInput);
    }

    public boolean containsParam(String param) {
        if (RegisterNumberPatternParams.matchesAnyParamWithDigit(param)) {
            for (String existing : getAllParams()) {
                if (RegisterNumberPatternParams.matchesParamWithDigit(existing, param)) {
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
