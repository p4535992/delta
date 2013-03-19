package ee.webmedia.alfresco.common.propertysheet.datepicker;

import static ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerGenerator.containsCamelCaseWord;
import junit.framework.TestCase;

/**
 * test DatePickerGeneratorTest.testContainsCamelCaseWord
 * @author Ats Uiboupin
 */
public class DatePickerGeneratorTest extends TestCase {

    public void testContainsCamelCaseWord() {
        assertTrue(containsCamelCaseWord("begin", "begin"));
        assertTrue(containsCamelCaseWord("end", "end"));
        assertTrue(containsCamelCaseWord("Begin", "Begin"));
        assertTrue(containsCamelCaseWord("End", "End"));

        assertTrue(containsCamelCaseWord("_end", "end"));
        assertTrue(containsCamelCaseWord("end_", "end"));
        assertFalse(containsCamelCaseWord("endx", "end"));
        assertTrue(containsCamelCaseWord("endX", "end"));
        assertTrue(containsCamelCaseWord("Xend", "end"));
        assertTrue(containsCamelCaseWord("endxXend", "end"));
        assertFalse(containsCamelCaseWord("endxXEnd", "end"));
        assertTrue(containsCamelCaseWord("endxXEnd", "End")); // this is arguable

        assertFalse(containsCamelCaseWord("senderRegDateBegin", "end"));
        assertTrue(containsCamelCaseWord("senderRegDateBegin", "Begin"));
        assertFalse(containsCamelCaseWord("senderRegDateBegin", "begin"));
        assertFalse(containsCamelCaseWord("senderRegDateBegin", "End"));
        //
        assertFalse(containsCamelCaseWord("beginningEnding", "begin"));
        assertFalse(containsCamelCaseWord("beginningEnding", "end"));
    }

}
