package ee.webmedia.alfresco.mso.service;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;

/**
 * Interface to MSO (Microsoft Office) webservice.
 * 
 * @author Alar Kvell
 */
public interface MsoService {

    boolean isMsoAvailable();

    boolean isTransformableToPdf(String sourceMimetype);

    void transformToPdf(ContentReader reader, ContentWriter writer) throws Exception;

}
