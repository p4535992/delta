package ee.webmedia.alfresco.docadmin.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.ObjectUtils;
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
     * Reorder/reinitialize order of items in given <code>reorderableItems</code> collection. <br>
     * If in addition to reinitializing order properties the of items in collection you need items in collection to be reIndexed based on new order, then you could use
     * returned list, otherwise use collection that was given as a parameter.
     * 
     * @param <O> - type of objects to be sorted
     * @param <F> - type of field/property that contains order
     * @param reorderableItems
     * @param modifier - implementation that must be able to set order, return order and original order for each element in collection
     * @return new List that contains the same instances from the <code>reorderableItems</code>,
     *         but unlike <code>reorderableItems</code> element indexes are also set based on new order
     */
    public static <O, F extends Comparable<F>> List<O> reorder(Collection<O> reorderableItems, final OrderModifier<O, F> modifier) {
        SortedMap<F, O> sortedMap = new TreeMap<F, O>();
        List<O> sortedList = new ArrayList<O>(reorderableItems); // create new list so that element indexes in original list wouldn't change
        sortByOrder(sortedList, modifier); // sort list
        // assign new order to items in original list
        for (O object : sortedList) {
            F field = modifier.getOrder(object);
            modifier.setOrder(object, sortedMap.isEmpty() ? null : sortedMap.lastKey());
            field = modifier.getOrder(object);
            Assert.isTrue(field != null && !sortedMap.containsKey(field));
            sortedMap.put(field, object);
        }
        return new ArrayList<O>(sortedMap.values()); // return list that contains elements in the new order - sometimes it might be needed
    }

    static <O, F extends Comparable<F>> void sortByOrder(List<O> list, final OrderModifier<O, F> modifier) {
        ComparatorChain chain = new ComparatorChain(); // ordering items in list should be done based on...
        // 1) current order
        chain.addComparator(new TransformingComparator(new ComparableTransformer<O>() {
            @Override
            public Comparable<?> tr(O input) {
                return modifier.getOrder(input);
            }
        }, new NullComparator()));
        // 2) if current orders are equal, element with changed order should come before the element whose order hasn't changed
        chain.addComparator(new Comparator<O>() {
            @Override
            public int compare(O o1, O o2) {
                F originalOrder1 = modifier.getOriginalOrder(o1);
                F originalOrder2 = modifier.getOriginalOrder(o2);
                if (originalOrder1 == null) {
                    throw new IllegalArgumentException("Original order is not set for object " + o1);
                }
                if (originalOrder2 == null) {
                    throw new IllegalArgumentException("Original order is not set for object " + o2);
                }
                boolean o1OrderChanged = !ObjectUtils.equals(modifier.getOrder(o1), originalOrder1);
                boolean o2OrderChanged = !ObjectUtils.equals(modifier.getOrder(o2), originalOrder2);
                if (o1OrderChanged == o2OrderChanged) {
                    return 0;
                }
                if (o1OrderChanged && !o2OrderChanged) {
                    return -1;
                }
                return 1;
            }
        });
        // 3) if there are several elements with same order and they all didn't have this order before
        // then order elements by original order
        chain.addComparator(new TransformingComparator(new ComparableTransformer<O>() {
            @Override
            public Comparable<?> tr(O input) {
                return modifier.getOriginalOrder(input);
            }
        }, new NullComparator()));
        @SuppressWarnings("unchecked")
        Comparator<O> byOrderAndOriginalOrderComparator = chain;
        Collections.sort(list, byOrderAndOriginalOrderComparator);
    }
}
