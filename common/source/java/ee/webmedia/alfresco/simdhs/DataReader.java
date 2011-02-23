package ee.webmedia.alfresco.simdhs;

import org.alfresco.web.ui.common.component.data.UIRichList;

import javax.faces.context.FacesContext;
import java.util.List;

/**
 * Interface used for defining data reader for {@link ee.webmedia.alfresco.simdhs.CSVExporter}.
 *
 * @author Romet Aidla
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
