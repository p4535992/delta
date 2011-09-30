package ee.webmedia.alfresco.docadmin.web;

import java.util.Arrays;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ee.webmedia.alfresco.docadmin.web.ListReorderHelper.OrderModifier;

/**
 * FIXME DLSeadist liigutada test kataloogi<br>
 * Test class for {@link ListReorderHelper#reorder(List, OrderModifier)}
 * 
 * @author Ats Uiboupin
 */
public class ListReorderHelperTest extends TestCase {

    static class Item {
        Integer originalOrder;
        Integer order;
        Integer expectedOrder;
        String customData;

        public Item(Integer previousOrder, Integer order, Integer expectedOrder, String customData) {
            originalOrder = previousOrder;
            this.order = order;
            this.expectedOrder = expectedOrder;
            this.customData = customData;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public Integer getOriginalOrder() {
            return originalOrder;
        }

        public void setOriginalOrder(Integer originalOrder) {
            this.originalOrder = originalOrder;
        }

        public String getCustomData() {
            return customData;
        }

        public void setCustomData(String customData) {
            this.customData = customData;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    private static final String SAME_ORDER_FIRST = "sameOrderFirst";
    private static final String SAME_ORDER_SECOND = "sameOrderSecond";
    private static List<Item> sortedList;

    private static final List<Item> INVALID_LIST = Arrays.asList(
            new Item(1, 1, 1, "bar")
            // order 2 not set to validate testValidateNoGapsInOrder()
            , new Item(5, 4, 2, SAME_ORDER_SECOND)
            , new Item(6, 4, 3, SAME_ORDER_FIRST)
            );

    public void testSort() {
        List<Item> unSortedList = Arrays.asList(
                  new Item(1, 1, 1, "1->1->1")
                , new Item(2, 7, 7, "2->7->7")
                , new Item(3, 3, 2, "3->3->2")
                , new Item(4, 4, 3, "4->4->3")
                , new Item(5, 7, 8, "5->7->8")
                , new Item(6, 7, 9, "6->7->9")
                , new Item(7, 7, 10, "7->7->10")
                );

        System.out.println("unSortedList:\n" + unSortedList);

        sortedList = ListReorderHelper.reorder(unSortedList, getOrderModifier());
        System.out.println("\nsortedList:\n" + sortedList);

    }

    public <O, F extends Comparable<F>> void testValidateIncrementalOrder() {
        validateIncrementalOrder(sortedList, getOrderModifier());
    }

    private OrderModifier<Item, Integer> getOrderModifier() {
        OrderModifier<Item, Integer> sortHelper = new OrderModifier<Item, Integer>() {
            @Override
            public Integer getOrder(Item object) {
                return object.order;
            }

            @Override
            public void setOrder(Item object, Integer previousMaxField) {
                object.order = (previousMaxField == null ? 1 : previousMaxField + 1);
            }

            @Override
            public Integer getOriginalOrder(Item object) {
                return object.originalOrder;
            }
        };
        return sortHelper;
    }

    /** validate stable sort */
    public void testValidateStableSort() {
        validateStableSort(sortedList);
        boolean testDetectedUnstableSort = false;
        try {
            validateStableSort(INVALID_LIST);
        } catch (AssertionFailedError e) {
            testDetectedUnstableSort = true;
        }
        if (!testDetectedUnstableSort) {
            fail("validateStableSort should have thrown error when given list of data that was not stable sorted");
        }
    }

    /**
     * validate that there are no gaps in the orders (e.g. 1, 2, 10, 11)
     */
    public void testValidateNoGapsInOrder() {
        validateNoGapsInOrder(sortedList);
        boolean testDetectedGapInOrder = false;
        try {
            validateNoGapsInOrder(INVALID_LIST);
        } catch (AssertionFailedError e) {
            testDetectedGapInOrder = true;
        }
        if (!testDetectedGapInOrder) {
            fail("validateNoGapsInOrder should have thrown error when given list contains gaps after reordering");
        }
    }

    /**
     * TODO: need to improve reordering - this test fails at the moment
     */
    public void testValidateExpectedOrder() {
        validateExpectedOrder(sortedList);
        // boolean testDetectedGapInOrder = false;
        // try {
        // validateExpectedOrder(INVALID_LIST);
        // } catch (AssertionFailedError e) {
        // testDetectedGapInOrder = true;
        // }
        // if (!testDetectedGapInOrder) {
        // fail("validateExpectedOrder should have thrown error when given list is not in expected order");
        // }
    }

    private void validateNoGapsInOrder(List<Item> sortedList) {
        int nextOrder = 1;
        for (Item object : sortedList) {
            Integer currentOrder = object.order;
            if (!currentOrder.equals(nextOrder)) {
                fail("expected to encounter object with order " + nextOrder + ", but found object with order " + currentOrder);
            }
            nextOrder++;
        }
    }

    private void validateExpectedOrder(List<Item> sortedList) {
        for (Item object : sortedList) {
            if (!object.order.equals(object.expectedOrder)) {
                fail("Expected order=" + object.expectedOrder + ", but actual order after sorting=" + object.order + ". object:\n" + object);
            }
        }
    }

    private void validateStableSort(List<Item> sortedList) {
        boolean foundFirst = false;
        for (Item object : sortedList) {
            String objectValue = object.customData;
            if (!foundFirst && objectValue.equals(SAME_ORDER_SECOND)) {
                fail("Sorting was not stableSort - if objects with equal orders are sorted then object that was befor must also be befor after sorting. sortedList=" + sortedList);
            }
            if (objectValue.equals(SAME_ORDER_FIRST)) {
                foundFirst = true;
            }
        }
    }

    private <O, F extends Comparable<F>> void validateIncrementalOrder(List<O> sortedList, OrderModifier<O, F> sortHelper) {
        Comparable<F> max = null;
        for (O object : sortedList) {
            F newOrder = sortHelper.getOrder(object);
            if (max != null && max.compareTo(newOrder) >= 0) {
                fail("After sorting found item with order '" + newOrder + "' after item with order '" + max + "'. sortedList=" + sortedList);
            }
            max = newOrder;
        }
    }
}
