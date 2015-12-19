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
 */
public class TextPatternUtil {

    private static final String FORMULA_REGEX = "\\{(.*?)\\}";
    private static final String GROUP_REGEX = "/\\*(.*?)\\*/";
    private static final String LIST_GROUP_REGEX = "/¤(.*?)¤/";

    private static final Pattern FORMULA_PATTERN = Pattern.compile(FORMULA_REGEX);
    private static final Pattern GROUP_PATTERN = Pattern.compile(GROUP_REGEX);
    private static final Pattern LIST_GROUP_PATTERN = Pattern.compile(LIST_GROUP_REGEX);

    public static String getResult(String input, final Map<String, String> formulaValues) {
        return getResult(input, new Transformer<String, String>() {
            @Override
            public String tr(String formula) {
                return StringUtils.defaultString(formulaValues.get(formula));
            }
        });
    }

    public static String getResult(String input, Transformer<String, String> formulaValueLookup) {
        String groupsReplacedResult = replaceGroups(input, formulaValueLookup, LIST_GROUP_PATTERN, true);
        groupsReplacedResult = replaceGroups(groupsReplacedResult, formulaValueLookup, GROUP_PATTERN, false);
        return getReplacedFormulas(groupsReplacedResult, formulaValueLookup, false);
    }

    private static String replaceGroups(String input, Transformer<String, String> formulaValueLookup, Pattern groupPattern, boolean isListGroup) {
        StringBuffer groupsReplacedBuffer = new StringBuffer();
        Matcher groupMatcher = groupPattern.matcher(input);

        while (groupMatcher.find()) {
            String group = groupMatcher.group(1);
            String replacedFormulas = getReplacedFormulas(group, formulaValueLookup, false);
            String noMatch = group.replaceAll(FORMULA_REGEX, "");
            if (replacedFormulas.equals(noMatch)) {
                groupMatcher.appendReplacement(groupsReplacedBuffer, "");
            } else {
                if (isListGroup) {
                    replacedFormulas = getReplacedFormulas(group, formulaValueLookup, isListGroup);
                }
                groupMatcher.appendReplacement(groupsReplacedBuffer, replacedFormulas);
            }
        }
        String groupsReplacedResult = groupMatcher.appendTail(groupsReplacedBuffer).toString();
        return groupsReplacedResult;
    }

    private static String getReplacedFormulas(String input, Transformer<String, String> formulaValueLookup, boolean isListGroup) {
        StringBuffer result = new StringBuffer();
        Matcher formulaMatcher = FORMULA_PATTERN.matcher(input);
        boolean previousNotEmpty = false;
        while (formulaMatcher.find()) {
            String replacementValue = formulaValueLookup.tr(formulaMatcher.group(1));
            if (isListGroup && StringUtils.isNotBlank(replacementValue)) {
                if (previousNotEmpty) {
                    replacementValue = ", " + replacementValue;
                }
                previousNotEmpty = true;
            }
            if (StringUtils.isNotBlank(replacementValue)) { 
            	replacementValue = StringUtils.replace(replacementValue, "\\", "");
            	replacementValue = StringUtils.replace(replacementValue, "$", "\\$");
            }
            formulaMatcher.appendReplacement(result, replacementValue);
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
