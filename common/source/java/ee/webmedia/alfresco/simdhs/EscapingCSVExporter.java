package ee.webmedia.alfresco.simdhs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.springframework.util.Assert;

import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * CVS exporter, that uses CSVExporter to export JSF lists to CSV files, but this class uses more complex escaping.
 * 
 * @see CSVExporter
 */
public class EscapingCSVExporter extends CSVExporter {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(EscapingCSVExporter.class);

    public EscapingCSVExporter(DataReader dataReader) {
        super(dataReader);
    }

    @Override
    public void export(String tableName) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Starting to export CSV file from " + tableName);
                log.debug("Using " + dataReader.getClass());
            }
            FacesContext facesContext = FacesContext.getCurrentInstance();

            UIComponent table = ComponentUtil.findComponentById(facesContext, facesContext.getViewRoot(), tableName);
            Assert.notNull(table, "Component was not found: " + tableName);

            if (!(table instanceof UIRichList)) { // only UIRichList is supported for now
                throw new RuntimeException("Wrong component type for export.");
            }
            UIRichList richList = (UIRichList) table;
            richList.bind();

            // set message bundle (needed for header labels)
            ResourceBundle bundle = Application.getBundle(facesContext);
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
            requestMap.put("msg", new BundleMap(bundle));

            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            response.setCharacterEncoding(CHARSET);
            response.setContentType("text/csv; charset=" + CHARSET);
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setHeader("Content-disposition", "attachment;filename=export.csv");

            OutputStream outputStream = response.getOutputStream();
            // final CsvWriter writer = new CsvWriter(outputStream, ',', Charset.forName(CHARSET));
            final CsvWriter writer = new CsvWriter(outputStream, SEPARATOR, Charset.forName(CHARSET));

            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
            outputStream.write("\ufeff".getBytes(CHARSET));

            final List<String> headers = dataReader.getHeaderRow(richList, facesContext);
            for (String header : headers) {
                writer.write(header, true);
            }
            writer.endRecord();
            final List<List<String>> dataRows = dataReader.getDataRows(richList, facesContext);
            if (doOrdering) {
                orderRows(dataRows);
            }
            for (List<String> dataRow : dataRows) {
                writeRow(writer, dataRow);
                writer.endRecord();
            }

            writer.flush();
            writer.close();

            outputStream.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (IOException e) {
            throw new RuntimeException("CSV export failed", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("CSV export successfully completed");
        }
    }

    private void writeRow(CsvWriter writer, List<String> dataRow) {
        for (String column : dataRow) {
            try {
                writer.write(column);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write column:\n'" + column + "'", e);
            }
        }
    }
}
