package ee.webmedia.alfresco.simdhs;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.web.ui.common.component.data.UIRichList;

/**
 * Interface used for defining data reader for {@link ee.webmedia.alfresco.simdhs.CSVExporter}.
<<<<<<< HEAD
 * 
 * @author Romet Aidla
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public interface DataReader {

    /**
     * Gets header row for CSV export.
     * 
     * @param list List that will be exported
     * @param fc Faces context
     * @return header row
     */
    List<String> getHeaderRow(UIRichList list, FacesContext fc);

    /**
     * Gets data rows for CSV export.
     * 
     * @param list List that will be exported
     * @param fc Faces context
     * @return list where every item is one data row in CSV export.
     */
    List<List<String>> getDataRows(UIRichList list, FacesContext fc);
}
