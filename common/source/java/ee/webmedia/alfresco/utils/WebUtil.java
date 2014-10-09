package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.common.service.ApplicationConstantsBean.SORT_ALLOWED_LIMIT;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.springframework.web.util.HtmlUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.workflow.model.RelatedUrl;

public class WebUtil {
    /**
     * http://www.mkyong.com/regular-expressions/how-to-extract-html-links-with-regular-expression/
     */
    private static final String HTML_A_REGEX = "(?i)<a([^>]+)>(.+?)</a>";
    private static final Pattern HTML_A_PATTERN = Pattern.compile(HTML_A_REGEX);

    public static Comparator<SelectItem> selectItemLabelComparator = new Comparator<SelectItem>() {
        @Override
        public int compare(SelectItem a, SelectItem b) {
            return AppConstants.getNewCollatorInstance().compare(a.getLabel(), b.getLabel());
        }
    };

    public static void sort(SelectItem[] items) {
        Arrays.sort(items, selectItemLabelComparator);
    }

    public static void sort(List<SelectItem> items) {
        QuickSort quickSort = new QuickSort(items, "label", true, IDataContainer.SORT_CASEINSENSITIVE);
        quickSort.sort();
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

    public static boolean isNotValidUrl(String website) {
        if (StringUtils.isBlank(website)) {
            return true;
        }
        website = website.toLowerCase();
        return !(website.startsWith("http://") || website.startsWith("https://") || website.startsWith("ftp://"));
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
        if (parts.length == 0) {
            return text;
        }
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

    public static String removeHtmlComments(String input) {
        // For example, MS Word HTML files contain comments like <!--[if gte mso 9]><xml>...
        // And this kind of HTML cannot be rendered to browser inside our page HTML,
        // because it breaks Internet Explorer (but not Firefox) - IE stops page rendering entirely at the point of this comment
        return StringUtils.defaultString(input).replaceAll("(?s)<!--.*?-->", "");
    }

    public static String removeHtmlTags(String input) {
        return StringUtils.defaultString(input).replaceAll("\\<.*?>", "");
    }

    public static void navigateTo(String navigationOutcome) {
        navigateTo(navigationOutcome, null);
    }

    public static void navigateWithCancel() {
        navigateTo(BeanHelper.getDialogManager().cancel());
    }

    public static void navigateTo(String navigationOutcome, FacesContext context) {
        if (context == null) {
            context = FacesContext.getCurrentInstance();
        }
        context.getApplication().getNavigationHandler().handleNavigation(context, null, navigationOutcome);
    }

    public static NodeRef getNodeRefFromNodeId(String currentNodeId) {
        return StringUtils.isNotBlank(currentNodeId) ? getGeneralService().getExistingNodeRefAllStores(currentNodeId) : null;
    }

    public static void toggleSystemUrlTarget(String currentDeltaUrlPrefix, Collection<RelatedUrl> urls) {
        if (StringUtils.isNotBlank(currentDeltaUrlPrefix)) {
            for (RelatedUrl relatedUrl : urls) {
                String url = relatedUrl.getUrl();
                if (StringUtils.isNotBlank(url) && url.startsWith(currentDeltaUrlPrefix)) {
                    relatedUrl.setTarget(RelatedUrl.TARGET_SELF);
                }
            }
        }
    }

    public static boolean exceedsLimit(List<NodeRef> nodeRefs, Log log) {
        int size = nodeRefs != null ? nodeRefs.size() : 0;
        if (size > SORT_ALLOWED_LIMIT) {
            if (log != null) {
                log.warn("List size " + size + " exceeds " + SORT_ALLOWED_LIMIT + ", sorting is not performed!");
            }
            return true;
        }
        return false;
    }

}
