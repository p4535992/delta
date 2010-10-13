package ee.webmedia.alfresco.common.service;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;

public interface OpenOfficeService {

    String REGEXP_PATTERN = "\\{[^\\}]+\\}";

    void replace(ContentReader reader, ContentWriter writer, ReplaceCallback callback) throws Exception;

    interface ReplaceCallback {
        String getReplace(String found);
    }

    class OpenOfficeReturnedNullInterfaceException extends Exception {
        private static final long serialVersionUID = 1L;

        private Class<?> clazz;

        public OpenOfficeReturnedNullInterfaceException(Class<?> clazz) {
            super(clazz.getSimpleName() + " is null");
            this.clazz = clazz;
        }
        
        public Class<?> getClazz() {
            return clazz;
        }

    }

}
