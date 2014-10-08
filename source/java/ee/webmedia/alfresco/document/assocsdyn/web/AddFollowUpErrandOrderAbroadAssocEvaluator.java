package ee.webmedia.alfresco.document.assocsdyn.web;

import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class AddFollowUpErrandOrderAbroadAssocEvaluator extends AddFollowUpDocumentTypeAssocEvaluator {
    private static final long serialVersionUID = 1L;

    public AddFollowUpErrandOrderAbroadAssocEvaluator() {
        super(SystematicDocumentType.ERRAND_ORDER_ABROAD.getId());
        skipFollowUpReportAndErrandOrderAbroad = false;
    }

}
