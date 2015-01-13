package ee.webmedia.alfresco.mso.service;

import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;

/**
 * Interface to MSO (Microsoft Office) webservice.
 */
public interface MsoService {

    String BEAN_NAME = "msoService";

    String MIMETYPE_DOC = MimetypeMap.MIMETYPE_WORD;
    String MIMETYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    String MIMETYPE_DOT = "application/dot";
    String MIMETYPE_DOTX = "application/vnd.openxmlformats-officedocument.wordprocessingml.template";

    /**
     * @return if this service is enabled in configuration
     */
    boolean isAvailable();

    boolean isTransformableToPdf(String sourceMimetype);

    boolean isFormulasReplaceable(String sourceMimetype);

    void transformToPdf(ContentReader documentReader, ContentWriter pdfWriter) throws Exception;

    void replaceFormulas(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter) throws Exception;

    void replaceFormulasAndTransformToPdf(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter, ContentWriter pdfWriter) throws Exception;

    Map<String, String> modifiedFormulas(ContentReader documentReader) throws Exception;

}
