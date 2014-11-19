package ee.webmedia.alfresco.document.assocsdyn.web;

import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class AddFollowUpErrandOrderAbroadAssocEvaluator extends AddFollowUpDocumentTypeAssocEvaluator {
    private static final long serialVersionUID = 1L;

    public AddFollowUpErrandOrderAbroadAssocEvaluator() {
        super(SystematicDocumentType.ERRAND_ORDER_ABROAD.getId());
        skipFollowUpReportAndErrandOrderAbroad = false;
    }

}
