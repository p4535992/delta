package ee.webmedia.alfresco.utils;

import java.util.Arrays;
import java.util.Comparator;

import javax.faces.model.SelectItem;

public class WebUtil {

    public static void sort(SelectItem[] items) {
        Arrays.sort(items, new Comparator<SelectItem>() {
            @Override
            public int compare(SelectItem a, SelectItem b) {
                return a.getLabel().compareTo(b.getLabel());
            }
        });
    }

}
