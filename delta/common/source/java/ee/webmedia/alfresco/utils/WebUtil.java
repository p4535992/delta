package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.HtmlUtils;

public class WebUtil {
    /**
     * http://www.mkyong.com/regular-expressions/how-to-extract-html-links-with-regular-expression/
     */
    private static final String HTML_A_REGEX = "(?i)<a([^>]+)>(.+?)</a>";
    private static final Pattern HTML_A_PATTERN = Pattern.compile(HTML_A_REGEX);

    public static Comparator<SelectItem> selectItemLabelComparator = new Comparator<SelectItem>() {
        @Override
        public int compare(SelectItem a, SelectItem b) {
            return a.getLabel().toLowerCase().compareTo(b.getLabel().toLowerCase());
        }
    };

    public static void sort(SelectItem[] items) {
        Arrays.sort(items, selectItemLabelComparator);
    }

    public static void sort(List<SelectItem> items) {
        Collections.sort(items, selectItemLabelComparator);
    }

    /**
     * Replaces textual URLs with HTML hyperlinks
     * Regex from: http://daringfireball.net/2010/07/improved_regex_for_matching_urls
     * 
     * @param text
     * @return
     */
    public static String processLinks(final String text) {
        if (StringUtils.isBlank(text)
                || (!StringUtils.containsIgnoreCase(text, "http://") && !StringUtils.containsIgnoreCase(text, "https://") && !StringUtils.containsIgnoreCase(
                        text, "www."))) {
            return text;
        }

        String replacedText = text
                .replaceAll(
                        "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))",
                        "<a target=\"_blank\" href=\"$1\">$1</a>");
        return StringUtils.replace(replacedText, "href=\"www.", "href=\"http://www."); // Prefix www with protocol
    }

    /**
     * Escapes all HTML except <a/> links
     * Regex from: http://www.mkyong.com/regular-expressions/how-to-extract-html-links-with-regular-expression/
     * 
     * @param text
     * @return
     */
    public static String escapeHtmlExceptLinks(final String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        // Find all Links
        List<String> links = new ArrayList<String>();
        Matcher matcher = HTML_A_PATTERN.matcher(text);
        while (matcher.find()) {
            links.add(matcher.group());
        }

        // Escape text surrounding links and weave text back together
        String[] parts = text.split(HTML_A_REGEX);
        StringBuffer b = new StringBuffer();
        final int linkCount = links.size();
        for (int i = 0; i < parts.length; i++) {
            b.append(HtmlUtils.htmlEscape(parts[i]));
            if (linkCount > i) {
                b.append(links.get(i));
            }
        }

        return b.toString();
    }

    public static String getValuesAsJsArrayString(Collection<String> suggesterValues) {
        final StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (String value : suggesterValues) {
            final String escapedValue = StringEscapeUtils.escapeJavaScript(value);
            sb.append("\"" + escapedValue + "\"");
            if (i != suggesterValues.size() - 1) {
                sb.append(", ");
            }
            i++;
        }
        return sb.append("]").toString();
    }

}
