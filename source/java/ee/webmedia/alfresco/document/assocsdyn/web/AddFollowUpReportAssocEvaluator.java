package ee.webmedia.alfresco.document.assocsdyn.web;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;

public class AddFollowUpReportAssocEvaluator extends AddFollowUpDocumentTypeAssocEvaluator {
    private static final long serialVersionUID = 1L;

    public AddFollowUpReportAssocEvaluator() {
        super(SystematicDocumentType.REPORT.getId());
        skipFollowUpReportAndErrandOrderAbroad = false;
    }
    
    @Override
    public boolean evaluate(Node docNode) {
    	if (BeanHelper.getUserService().isGuest()) {
        	return false;
        }
        return super.evaluate(docNode);
    }

    @Override
    public boolean evaluate() {
    	if (BeanHelper.getUserService().isGuest()) {
        	return false;
        }
        return super.evaluate();
    }

}
