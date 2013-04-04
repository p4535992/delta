package ee.webmedia.alfresco.document.file.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 */
public interface FileModel {
    String URI = "http://alfresco.webmedia.ee/model/file/1.0";

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        /**
         * XXX: this field could be refactored out in favour of field {@link #GENERATION_TYPE}, but not with boolean property, but with some kind of
         * distinguishing value (defined by {@link GeneratedFileType})
         */
        QName GENERATED = QName.createQName(URI, "generated");
        /**
         * property for distinguishing pdf files generated during signing from other generated files that at the moment use {@link #GENERATED} property <br>
         * known values are defined with: {@link GeneratedFileType}
         */
        QName GENERATION_TYPE = QName.createQName(URI, "generationType");
        QName ACTIVE = QName.createQName(URI, "active");
        QName DISPLAY_NAME = QName.createQName(URI, "displayName");

    }
}