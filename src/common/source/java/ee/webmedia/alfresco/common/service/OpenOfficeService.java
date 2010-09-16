package ee.webmedia.alfresco.common.service;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;

public interface OpenOfficeService {

    String REGEXP_PATTERN = "\\{[^\\}]+\\}";

    void replace(ContentReader reader, ContentWriter writer, ReplaceCallback callback) throws Exception;

    interface ReplaceCallback {
        String getReplace(String found);
    }

}
