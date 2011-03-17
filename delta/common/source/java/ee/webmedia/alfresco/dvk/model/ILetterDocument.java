package ee.webmedia.alfresco.dvk.model;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 */
@AlfrescoModelType(uri = "")
public interface ILetterDocument extends LetterSender, AccessRights, IDocument {
    
}
