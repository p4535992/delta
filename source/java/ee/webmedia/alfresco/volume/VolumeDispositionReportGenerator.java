package ee.webmedia.alfresco.volume;

import com.csvreader.CsvReader;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.eventplan.model.RetaintionStart;
import ee.webmedia.alfresco.sharepoint.ImportUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import static ee.webmedia.alfresco.sharepoint.ImportUtil.getString;

public class VolumeDispositionReportGenerator {
  private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
  private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(VolumeDispositionReportGenerator.class);

  public static HSSFWorkbook generate(File file) {
    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet = workbook.createSheet("sheet1");
    createDispositionReportHeaders(sheet);
    createDispositionReportDataRows(sheet, file);
    resizeColumns(sheet);
    return workbook;
  }

  private static void createDispositionReportHeaders(HSSFSheet sheet) {
    HSSFRow headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("Järjekorra number");
    headerRow.createCell(1).setCellValue("Tähis");
    headerRow.createCell(2).setCellValue("Pealkiri");
    headerRow.createCell(3).setCellValue("Kehtiv alates");
    headerRow.createCell(4).setCellValue("Kehtiv kuni");
    headerRow.createCell(5).setCellValue("Säilitatakse alaliselt");
    headerRow.createCell(6).setCellValue("Säilitustähtaja arvestus");
    headerRow.createCell(7).setCellValue("Säilitusaeg aastates");
    headerRow.createCell(8).setCellValue("Säilitustähtaeg kuupäevana");
    headerRow.createCell(9).setCellValue("Viide õigusaktile");
  }

  private static void createDispositionReportDataRows(HSSFSheet sheet, File file) {
    CsvReader csvReader = null;
    try {
      csvReader = ImportUtil.createDataReader(file, ',');
      if (csvReader.readHeaders()) {
        HashSet<String> volumeRefs = new HashSet<>();
        int volumeCount = 1;
        while (csvReader.readRecord()) {
          String volumeRefString = getString(csvReader, 3);
          if (StringUtils.isNotBlank(volumeRefString) && !volumeRefs.contains(volumeRefString)) {
            volumeRefs.add(volumeRefString);
            NodeRef volumeRef = new NodeRef(volumeRefString);
            if (BeanHelper.getNodeService().exists(volumeRef)) {
              createDispositionReportDataRow(sheet, volumeRef, volumeCount);
              volumeCount++;
            } else {
              LOG.info("NodeRef (line " + volumeCount + ") " + volumeRefString + " does not exist!");
            }
          } else {
            LOG.info("Skipping NodeRef(" + volumeRefString + ") in line " + volumeCount);
          }
        }
      }
    } catch (IOException e) {
      throw new UnableToPerformException("Error reading input csv " + file.getName() + ", aborting updater", e);
    } finally {
      if (csvReader != null) {
        csvReader.close();
      }
    }
  }

  private static void createDispositionReportDataRow(HSSFSheet sheet, NodeRef nodeRef, int volumeCount) {
    Map<QName, Serializable> props = BeanHelper.getNodeService().getProperties(nodeRef);
    HSSFRow row = sheet.createRow(volumeCount);
    row.createCell(0).setCellValue(volumeCount);
    row.createCell(1).setCellValue(getStringProperty(props, VolumeModel.Props.MARK));
    row.createCell(2).setCellValue(getStringProperty(props, VolumeModel.Props.TITLE));
    row.createCell(3).setCellValue(getDateProperty(props, VolumeModel.Props.VALID_FROM));
    row.createCell(4).setCellValue(getDateProperty(props, VolumeModel.Props.VALID_TO));
    row.createCell(5).setCellValue(getBooleanProperty(props, EventPlanModel.Props.RETAIN_PERMANENT));
    row.createCell(6).setCellValue(getRetaintionStart(props));
    row.createCell(7).setCellValue(getStringProperty(props, EventPlanModel.Props.RETAINTION_PERIOD));
    row.createCell(8).setCellValue(getDateProperty(props, EventPlanModel.Props.RETAIN_UNTIL_DATE));
    row.createCell(9).setCellValue(getStringProperty(props, EventPlanModel.Props.ARCHIVING_NOTE));
  }

  private static String getBooleanProperty(Map<QName, Serializable> props, QName propertyName) {
    Boolean value = (Boolean) props.get(propertyName);
    return BooleanUtils.isTrue(value) ? "Jah" : "";
  }

  private static String getStringProperty(Map<QName, Serializable> props, QName propertyName) {
    Serializable value = props.get(propertyName);
    return value != null ? value.toString() : "";
  }

  private static String getDateProperty(Map<QName, Serializable> props, QName propertyName) {
    Date value = (Date) props.get(propertyName);
    return value != null ? dateFormat.format(value) : "";
  }

  private static String getRetaintionStart(Map<QName, Serializable> props) {
    String value = getStringProperty(props, EventPlanModel.Props.RETAINTION_START);
    return StringUtils.isNotBlank(value) ? MessageUtil.getMessage(RetaintionStart.valueOf(value)) : "";
  }

  private static void resizeColumns(HSSFSheet sheet) {
    for (int i = 0; i < 10; i++) {
      sheet.autoSizeColumn(i);
    }
  }
}
