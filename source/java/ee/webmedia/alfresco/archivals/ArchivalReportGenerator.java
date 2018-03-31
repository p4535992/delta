package ee.webmedia.alfresco.archivals;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.*;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class ArchivalReportGenerator {

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    private static final FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm");
    private static final int FIRST_DATA_ROW_INDEX = 5;
    private static final int FIRST_ORDER_NUMBER = 1;

    public static File generate(String fileName, List<NodeRef> volumeRefs) throws IOException {
        File tempFile = TempFileProvider.createTempFile(fileName, "");
        FileOutputStream out = new FileOutputStream(tempFile);
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("sheet1");
        String userId = AuthenticationUtil.getRunAsUser();
        String userName = BeanHelper.getUserService().getUserFullName(userId);
        createActivityUserName(sheet, userName);
        createActivityUserId(sheet, userId);
        createActivityDate(sheet);
        createTableHeaders(sheet);
        createDataRows(sheet, volumeRefs, FIRST_DATA_ROW_INDEX, FIRST_ORDER_NUMBER);
        resizeColumns(sheet);
        workbook.write(out);
        out.flush();
        out.close();
        return tempFile;
    }

    public static File update(File previousExcel, List<NodeRef> volumeRefs) throws IOException, InvalidFormatException {
        File tempFile = TempFileProvider.createTempFile(previousExcel.getName(), "");
        FileOutputStream out = new FileOutputStream(tempFile);
        FileInputStream inputStream = new FileInputStream(previousExcel);
        XSSFWorkbook workbook = (XSSFWorkbook) WorkbookFactory.create(inputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        int lastRowIndex = sheet.getLastRowNum();
        
        int lastOrderNumber = 0;
        if (lastRowIndex > 4) {
        	lastOrderNumber = (int) sheet.getRow(lastRowIndex).getCell(0).getNumericCellValue();
        }
        
        createDataRows(sheet, volumeRefs, lastRowIndex + 1, lastOrderNumber + 1);
        resizeColumns(sheet);
        workbook.write(out);
        out.flush();
        out.close();
        return tempFile;
    }

    private static void createActivityUserName(XSSFSheet sheet, String userName) {
        XSSFRow headerRow = sheet.createRow(0);
        createBoldCell(sheet, headerRow, "Akti koostaja eesnimi ja perenimi:", 0);
        headerRow.createCell(2).setCellValue(userName);
    }

    private static void createActivityUserId(XSSFSheet sheet, String userId) {
        XSSFRow headerRow = sheet.createRow(1);
        createBoldCell(sheet, headerRow, "Akti koostaja isikukood:", 0);
        headerRow.createCell(2).setCellValue(userId);
    }

    private static void createActivityDate(XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(2);
        createBoldCell(sheet, headerRow, "Arhiveerimistegevuse teostamise aeg:", 0);
        headerRow.createCell(2).setCellValue(formatDateTime(new Date()));
    }

    private static void createTableHeaders(XSSFSheet sheet) {
        XSSFRow headerRow = sheet.createRow(4);
        createBoldCell(sheet, headerRow, "Jrk nr", 0);
        createBoldCell(sheet, headerRow, "Tähis", 1);
        createBoldCell(sheet, headerRow, "Pealkiri", 2);
        createBoldCell(sheet, headerRow, "Kehtiv alates", 3);
        createBoldCell(sheet, headerRow, "Kehtiv kuni", 4);
        createBoldCell(sheet, headerRow, "Säilitustähtaeg", 5);
        createBoldCell(sheet, headerRow, "Hindamisotsus", 6);
    }

    private static void createDataRows(XSSFSheet sheet, List<NodeRef> volumeRefs, int rowNumber, int orderNumber) {
        for (NodeRef volumeRef : volumeRefs) {
            createDataRow(sheet, volumeRef, rowNumber, orderNumber);
            rowNumber++;
            orderNumber++;
        }
    }

    private static void createBoldCell(XSSFSheet sheet, XSSFRow row, String value, int index) {
        XSSFCell cell = row.createCell(index);
        cell.setCellValue(value);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        XSSFFont defaultFont = sheet.getWorkbook().createFont();
        defaultFont.setBold(true);
        style.setFont(defaultFont);
        cell.setCellStyle(style);
    }

    private static void createDataRow(XSSFSheet sheet, NodeRef nodeRef, int rowNumber, int orderNumber) {
        Map<QName, Serializable> props = BeanHelper.getNodeService().getProperties(nodeRef);
        XSSFRow row = sheet.createRow(rowNumber);
        row.createCell(0).setCellValue(orderNumber);
        row.createCell(1).setCellValue(getStringProperty(props, VolumeModel.Props.MARK));
        row.createCell(2).setCellValue(getStringProperty(props, VolumeModel.Props.TITLE));
        row.createCell(3).setCellValue(getDateAsStringProperty(props, VolumeModel.Props.VALID_FROM));
        row.createCell(4).setCellValue(getDateAsStringProperty(props, VolumeModel.Props.VALID_TO));
        row.createCell(5).setCellValue(getRetaintionDate(props));
        row.createCell(6).setCellValue(getStringProperty(props, EventPlanModel.Props.ASSESSMENT_DECISION_NOTE));
    }

    private static String getRetaintionDate(Map<QName, Serializable> props) {
        boolean retainPermanent = getBooleanProperty(props, EventPlanModel.Props.RETAIN_PERMANENT);
        if (retainPermanent) {
            return "Alaline";
        }
        return getDateAsStringProperty(props, EventPlanModel.Props.RETAIN_UNTIL_DATE);
    }

    private static String getStringProperty(Map<QName, Serializable> props, QName propertyName) {
        Serializable value = props.get(propertyName);
        return value != null ? value.toString() : "";
    }

    private static String getDateAsStringProperty(Map<QName, Serializable> props, QName propertyName) {
        Date value = (Date) props.get(propertyName);
        return value != null ? formatDate(value) : "";
    }

    private static String formatDate(Date date) {
        return date != null ? dateFormat.format(date) : "";
    }

    private static String formatDateTime(Date date) {
        return date != null ? dateTimeFormat.format(date) : "";
    }

    private static boolean getBooleanProperty(Map<QName, Serializable> props, QName propertyName) {
        Boolean value = (Boolean) props.get(propertyName);
        return BooleanUtils.isTrue(value);
    }

    private static void resizeColumns(XSSFSheet sheet) {
        for (int i = 0; i < 7; i++) {
            sheet.setColumnWidth(i, 4096);
        }
    }
}
