<<<<<<< HEAD
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
    WORD_TEMPLATE,
    TRANSFORMED_PDF,
    OPENOFFICE_TEMPLATE
}
=======
package ee.webmedia.alfresco.document.file.model;

import ee.webmedia.alfresco.document.file.model.FileModel.Props;

/**
 * Used as a marker in Props#GENERATION_TYPE field for concrete type of generated file
 */
public enum GeneratedFileType {
    /** files with {@link Props#GENERATION_TYPE} value equal to this constant name() are files generated during signing */
    SIGNED_PDF,
    WORD_TEMPLATE
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
