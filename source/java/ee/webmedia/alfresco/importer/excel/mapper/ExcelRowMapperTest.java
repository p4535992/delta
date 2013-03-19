package ee.webmedia.alfresco.importer.excel.mapper;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.util.Assert;

public class ExcelRowMapperTest extends TestCase {

    public void testExtractDate() {
        ExcelRowMapper<Object> s = getInstance();
        final List<String> mustMatch = Arrays.asList("28.8.2008", "28.08.2008 ", "28.08.2008", " 28.08.2008", "28.08.2008 29.08.2008 ", "-28.08.2008-");
        Date extractionResult = null;
        for (String dateString : mustMatch) {
            final Date date = s.extractDate(dateString);
            if (extractionResult == null) {
                extractionResult = date;
            } else {
                assertEquals(extractionResult, date);
            }
            assertNotNull("result must NOT be null while parsing input '" + dateString + "'", date);
        }
        final List<String> mustNotMatch = Arrays.asList("028.08.2008 ", "28.08.20080", "028.08.20080", "32.08.2008", "28.13.2008", "28.08.08");
        for (String dateInput : mustNotMatch) {
            final Date date = s.extractDate(dateInput);
            assertNull("result must BE null while parsing input\n'" + dateInput + "'\nresult: " + date, date);
        }
    }

    static class TestExcelRowMapper extends ExcelRowMapper<Object> {
        @ExcelColumn('A')
        private final Integer OverRide = (int) 'Z';
        private final Integer FakeOverRide = (int) 'Z';
        @ExcelColumn('K')
        private final Integer SuperField = (int) 'Z';

        @Override
        public Object mapRow(Row row, long rowNr, File excelFile, String string) {
            return null;
        }

        public Integer getOverRide() {
            return OverRide;
        }

        public Integer getFakeOverRide() {
            return FakeOverRide;
        }

        public Integer getSuperField() {
            return SuperField;
        }
    }

    static class TestExcelRowMapper2 extends TestExcelRowMapper {
        @ExcelColumn('B')
        private final Integer OverRide = (int) 'X';
        @ExcelColumn('B')
        private final Integer FakeOverRide = (int) 'X';
        @ExcelColumn(colNr = 'C')
        private final Integer SubField = (int) 'X';
        @ExcelColumn()
        private final Integer NullField = (int) 'X';
    }

    private static ExcelRowMapper<Object> getInstance() {
        return new TestExcelRowMapper();
    }

    public void testSetExcelColumnsFromAnnotations() {
        final TestExcelRowMapper2 subclass = new TestExcelRowMapper2();
        Assert.isTrue(subclass.OverRide == 'X');
        Assert.isTrue(subclass.FakeOverRide == 'X');
        Assert.isTrue(subclass.SubField == 'X');
        Assert.isTrue(subclass.NullField == 'X');
        System.out.println("OverRide=" + subclass.OverRide);
        Integer superOverride = subclass.getOverRide();
        Integer superFakeOverride = subclass.getFakeOverRide();
        Assert.isTrue(superFakeOverride == 90/* 'Z' */);
        Assert.isTrue(subclass.getSuperField() == 'Z');
        Assert.isTrue(superOverride == 'Z');
        System.out.println("super OverRide=" + superOverride);
        System.out.println("SubField=" + subclass.SubField);

        // baby, do da magic!
        subclass.setExcelColumnsFromAnnotations();

        System.out.println("OverRide=" + subclass.OverRide);
        Assert.isTrue(subclass.OverRide == 1, "Override value is not eas expected '" + subclass.OverRide + "'");
        Assert.isTrue(subclass.FakeOverRide == 1, "FakeOverRide value is not eas expected '" + subclass.FakeOverRide + "'");
        superOverride = subclass.getOverRide();
        superFakeOverride = subclass.getFakeOverRide();
        System.out.println("super OverRide=" + superOverride);
        Assert.isTrue(superOverride == 1);
        Assert.isTrue(superFakeOverride == 90/* 'Z' */); // should still have the initial value(as field is not annotated)
        Assert.isTrue(subclass.getSuperField() == 10, "SuperField value is not as expected '" + subclass.getSuperField() + "'");
        Assert.isTrue(subclass.SubField == 'C');
        Assert.isTrue(subclass.NullField == null);

    }

}
