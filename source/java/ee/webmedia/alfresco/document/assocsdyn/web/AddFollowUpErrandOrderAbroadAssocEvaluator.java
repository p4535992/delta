package ee.webmedia.alfresco.document.assocsdyn.web;

import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;

public class AddFollowUpErrandOrderAbroadAssocEvaluator extends AddFollowUpDocumentTypeAssocEvaluator {
    private static final long serialVersionUID = 1L;

    public AddFollowUpErrandOrderAbroadAssocEvaluator() {
        super(SystematicDocumentType.ERRAND_ORDER_ABROAD.getId());
        skipFollowUpReportAndErrandOrderAbroad = false;
    }

}
