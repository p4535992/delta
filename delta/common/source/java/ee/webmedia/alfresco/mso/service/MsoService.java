package ee.webmedia.alfresco.mso.service;

import java.util.Map;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;

/**
 * Interface to MSO (Microsoft Office) webservice.
 * 
 * @author Alar Kvell
 */
public interface MsoService {

    /**
     * @return if this service is enabled in configuration
     */
    boolean isAvailable();

    boolean isTransformableToPdf(String sourceMimetype);

    boolean isFormulasReplaceable(String sourceMimetype);

    void transformToPdf(ContentReader documentReader, ContentWriter pdfWriter) throws Exception;

    void replaceFormulas(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter) throws Exception;

    void replaceFormulasAndTransformToPdf(Map<String, String> formulas, ContentReader documentReader, ContentWriter documentWriter, ContentWriter pdfWriter) throws Exception;

}
