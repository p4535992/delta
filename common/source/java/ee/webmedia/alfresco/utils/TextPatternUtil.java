package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Operations dealing with patterns. Patterns may contain any text and formulas, formulas are replaced with values.
 * Formulas must be in the form of <code>{formula}</code>.
 * Patterns may also contain cancellation groups, which must be in the form of <code>&#47;*text {formula}*&#47;</code> are also supported.
 * 
 * @author Alar Kvell
 */
public class TextPatternUtil {

    private static final String FORMULA_REGEX = "\\{(.*?)\\}";
    private static final String GROUP_REGEX = "/\\*(.*?)\\*/";

    private static final Pattern FORMULA_PATTERN = Pattern.compile(FORMULA_REGEX);
    private static final Pattern GROUP_PATTERN = Pattern.compile(GROUP_REGEX);

    public static String getResult(String input, final Map<String, String> formulaValues) {
        return getResult(input, new Transformer<String, String>() {
            @Override
            public String tr(String formula) {
                return StringUtils.defaultString(formulaValues.get(formula));
            }
        });
    }

    public static String getResult(String input, Transformer<String, String> formulaValueLookup) {
        StringBuffer groupsReplacedBuffer = new StringBuffer();
        Matcher groupMatcher = GROUP_PATTERN.matcher(input);
        while (groupMatcher.find()) {
            String group = groupMatcher.group(1);
            String replacedFormulas = getReplacedFormulas(group, formulaValueLookup);
            String noMatch = group.replaceAll(FORMULA_REGEX, "");
            if (replacedFormulas.equals(noMatch)) {
                groupMatcher.appendReplacement(groupsReplacedBuffer, "");
            } else {
                groupMatcher.appendReplacement(groupsReplacedBuffer, replacedFormulas);
            }
        }
        String groupsReplacedResult = groupMatcher.appendTail(groupsReplacedBuffer).toString();
        return getReplacedFormulas(groupsReplacedResult, formulaValueLookup);
    }

    private static String getReplacedFormulas(String input, Transformer<String, String> formulaValueLookup) {
        StringBuffer result = new StringBuffer();
        Matcher formulaMatcher = FORMULA_PATTERN.matcher(input);
        while (formulaMatcher.find()) {
            formulaMatcher.appendReplacement(result, formulaValueLookup.tr(formulaMatcher.group(1)));
        }
        formulaMatcher.appendTail(result);
        return result.toString();
    }

    public static List<String> getFormulas(String input) {
        List<String> formulas = new ArrayList<String>();
        Matcher formulaMatcher = FORMULA_PATTERN.matcher(input);
        while (formulaMatcher.find()) {
            formulas.add(formulaMatcher.group(1));
        }
        return formulas;
    }

}
