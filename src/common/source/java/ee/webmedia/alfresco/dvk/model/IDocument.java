package ee.webmedia.alfresco.dvk.model;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 */
@AlfrescoModelType(uri = "")
public interface IDocument extends LetterSender, AccessRights {
    // parent interface for documents
}
