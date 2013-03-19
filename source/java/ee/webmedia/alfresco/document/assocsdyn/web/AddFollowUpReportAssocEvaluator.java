package ee.webmedia.alfresco.document.assocsdyn.web;

import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;

/**
 * @author Alar Kvell
 */
public class AddFollowUpReportAssocEvaluator extends AddFollowUpDocumentTypeAssocEvaluator {
    private static final long serialVersionUID = 1L;

    public AddFollowUpReportAssocEvaluator() {
        super(SystematicDocumentType.REPORT.getId());
        skipFollowUpReportAndErrandOrderAbroad = false;
    }

}
