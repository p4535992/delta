package ee.webmedia.alfresco.document.assocsdyn.web;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * Decides if add reply association button should be visible
 */
public class AddReplyAssocEvaluator extends BaseAddAssocEvaluator {
    private static final long serialVersionUID = 1L;

    public AddReplyAssocEvaluator() {
        super(DocTypeAssocType.REPLY);
    }

    @Override
    protected boolean isAddAssocToUnregistratedDocEnabled(DocumentType documentType) {
        return documentType.isAddReplyToUnregistratedDocEnabled();
    }

    @Override
    public boolean evaluate() {
        return false;
    }

}
