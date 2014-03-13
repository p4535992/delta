package ee.webmedia.alfresco.docadmin.service;

import java.util.Map;

public interface DocumentTypeValidator {

    boolean validate(DocumentTypeVersion type, Map<String, String> errorMessages);

}
