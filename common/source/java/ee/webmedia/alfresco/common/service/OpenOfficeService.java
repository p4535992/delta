package ee.webmedia.alfresco.common.service;

import java.util.Map;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;

public interface OpenOfficeService {

    String REGEXP_GROUP_PATTERN = "/\\*[^\\*/]+\\*/";
    String REGEXP_PATTERN = "\\{[^\\}]+\\}";
    String BEAN_NAME = "openOfficeService";

    void replace(ContentReader reader, ContentWriter writer, Map<String, String> formulas, boolean finalize) throws Exception;

    Map<String, String> modifiedFormulas(ContentReader fileContentReader, NodeRef documentNodeRef, NodeRef fileNodeRef) throws Exception;

    Map<String, String> getUsedFormulasAndValues(ContentReader reader) throws Exception;

    boolean isAvailable();

    class OpenOfficeReturnedNullInterfaceException extends Exception {
        private static final long serialVersionUID = 1L;

        private final Class<?> clazz;

        public OpenOfficeReturnedNullInterfaceException(Class<?> clazz) {
            super(clazz.getSimpleName() + " is null");
            this.clazz = clazz;
        }

        public Class<?> getClazz() {
            return clazz;
        }

    }
}