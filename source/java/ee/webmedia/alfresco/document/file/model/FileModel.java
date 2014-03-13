package ee.webmedia.alfresco.document.file.model;

import org.alfresco.service.namespace.QName;

public interface FileModel {
    String URI = "http://alfresco.webmedia.ee/model/file/1.0";

    /**
     * Properties described in alfresco model
     */
    public interface Props {
        QName GENERATED_FROM_TEMPLATE = QName.createQName(URI, "generatedFromTemplate");
        /**
         * property for distinguishing pdf files generated during signing from other generated files that at the moment use {@link #GENERATED} property <br>
         * known values are defined with: {@link GeneratedFileType}
         */
        QName GENERATION_TYPE = QName.createQName(URI, "generationType");
        QName ACTIVE = QName.createQName(URI, "active");
        QName DISPLAY_NAME = QName.createQName(URI, "displayName");
        QName NEW_VERSION_ON_NEXT_SAVE = QName.createQName(URI, "newVersionOnNextSave");
        QName UPDATE_METADATA_IN_FILES = QName.createQName(URI, "updateMetadataInFiles");
        QName PREVIOUS_FILE_PARENT = QName.createQName(URI, "previousFileParent");

    }
}