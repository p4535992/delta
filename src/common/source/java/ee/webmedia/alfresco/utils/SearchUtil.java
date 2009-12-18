package ee.webmedia.alfresco.utils;

import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.service.namespace.QName;
import org.apache.lucene.queryParser.QueryParser;

/**
 * @author Alar Kvell
 */
public class SearchUtil {

    /**
     * Strip characters that have special meaning in Lucene query.
     * 
     * @see QueryParser#escape(String)
     */
    public static String strip(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{'
                    || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&') {
                // ignore special characters
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Replace characters that have special meaning in Lucene query.
     * 
     * @see QueryParser#escape(String)
     */
    public static String replace(String s, String replacement) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{'
                    || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&') {
                // replace special characters
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Generate Lucene query for searching nodes by property values. Input string is tokenized by space and search query is constructed so that all tokens must
     * be present in any node property (AND search).
     * 
     * @param input search string, all special characters must be stripped or escaped previously
     * @param type node type, may be {@code null}
     * @param props node properties that are searched
     * @return Lucene search query
     */
    public static String generateQuery(String input, QName type, Set<QName> props) {
        StringBuilder query = new StringBuilder(128);
        if (type != null) {
            query.append("+(TYPE:\"").append(QueryParser.escape(type.toString())).append("\")");
        }
        for (StringTokenizer t = new StringTokenizer(input, " "); t.hasMoreTokens();) {
            String term = t.nextToken();
            query.append(" +(");
            for (QName prop : props) {
                query.append(" @").append(QueryParser.escape(prop.toString())).append(":\"*");
                query.append(term);
                query.append("*\"");
            }
            query.append(")");
        }
        return query.toString();
    }

}
