package ee.webmedia.alfresco.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.model.SelectItem;

public class WebUtil {

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

}
