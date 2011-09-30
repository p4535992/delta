package ee.webmedia.alfresco.docadmin.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.ComparableTransformer;

/**
 * Helper class that (re)evaluates order property of items in list
 * 
 * @author Ats Uiboupin
 */
public class ListReorderHelper {

    /**
     * Accessor and mutator for order property/field of the object
     * 
     * @param <O> - type of objects to be sorted
     * @param <F> - type of field that is used to order
     */
    public interface OrderModifier<O, F extends Comparable<F>> {
        F getOrder(O object);

        void setOrder(O object, F previousMaxField);

        F getOriginalOrder(O object);
    }

    /**
     * TODO DLSeadist hetkel ei toimi päris korrektselt - test {@link ListReorderHelperTest#testValidateExpectedOrder()} on "punane"
     * 
     * @param <O> - type of objects to be sorted
     * @param <F> - type of field that contains order
     * @param list
     * @param modifier
     * @return
     */
    public static <O, F extends Comparable<F>> List<O> reorder(List<O> list, final OrderModifier<O, F> modifier) {
        SortedMap<F, O> sortedMap = new TreeMap<F, O>();
        ArrayList<O> sortedList = new ArrayList<O>(list);
        // TODO DLSeadist praegu ei arvestata sellega, et kui kahel objektil on sama order, siis võidab see, kelle orderit muudeti - kaotab see, kellel oli varem sama order
        sortByOrder(sortedList, modifier);
        for (O object : sortedList) {
            F field = modifier.getOrder(object);
            modifier.setOrder(object, sortedMap.isEmpty() ? null : sortedMap.lastKey());
            field = modifier.getOrder(object);
            Assert.isTrue(field != null && !sortedMap.containsKey(field));
            sortedMap.put(field, object);
        }
        return new ArrayList<O>(sortedMap.values());
    }

    private static <O, F extends Comparable<F>> void sortByOrder(List<O> list, final OrderModifier<O, F> helper) {
        @SuppressWarnings("unchecked")
        Comparator<O> byOrderComparator = new TransformingComparator(new ComparableTransformer<O>() {
            @Override
            public Comparable<?> tr(O input) {
                return helper.getOrder(input);
            }
        }, new NullComparator());
        Collections.sort(list, byOrderComparator);
    }
}
