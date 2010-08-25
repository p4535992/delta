package ee.webmedia.alfresco.postipoiss;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.dom4j.Element;

/**
 * Some static utils for postipoiss import.
 */
class PostipoissUtil {

    static String findAnyValue(Element root, String xpath) {
        Element el = findAnyNode(root, xpath);
        return el == null ? null : el.getStringValue();
    }

    @SuppressWarnings("unchecked")
    static Element findAnyNode(Element root, String xpath) {
        List nodes = root.selectNodes(xpath);
        if (nodes != null && !nodes.isEmpty()) {
            return ((Element) nodes.get(0));
        }
        return null;
    }

    static int getYear(Date date) {
        return get(date, Calendar.YEAR);
    }

    static int get(Date date, int field) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(field);
    }

    static class Counter {
        int i;

        int next() {
            i++;
            return i;
        }
    }

}
