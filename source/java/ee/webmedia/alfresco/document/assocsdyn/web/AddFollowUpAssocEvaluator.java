package ee.webmedia.alfresco.document.assocsdyn.web;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * Decides if add followUp association button should be visible
 */
public class AddFollowUpAssocEvaluator extends BaseAddAssocEvaluator {
    private static final long serialVersionUID = 1L;

    public AddFollowUpAssocEvaluator() {
        super(DocTypeAssocType.FOLLOWUP);
    }

    @Override
    protected boolean isAddAssocToUnregistratedDocEnabled(DocumentType documentType) {
        return documentType.isAddFollowUpToUnregistratedDocEnabled();
    }

}
