package ee.webmedia.alfresco.document.file.model;

import ee.webmedia.alfresco.document.file.model.FileModel.Props;

/**
 * Used as a marker in Props#GENERATION_TYPE field for concrete type of generated file
 * 
 * @author Ats Uiboupin
 */
public enum GeneratedFileType {
    /** files with {@link Props#GENERATION_TYPE} value equal to this constant name() are files generated during signing */
    SIGNED_PDF,
    WORD_TEMPLATE
}
