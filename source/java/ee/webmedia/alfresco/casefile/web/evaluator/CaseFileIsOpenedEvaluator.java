package ee.webmedia.alfresco.casefile.web.evaluator;

import java.util.Map;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.evaluator.AbstarctCaseFileStatusEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;

public class CaseFileIsOpenedEvaluator extends AbstarctCaseFileStatusEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Node caseFileNode) {
        return evaluateInternal(caseFileNode);

    }

    @Override
    protected boolean isValidStatus(Map<String, Object> properties) {
        return DocListUnitStatus.OPEN.getValueName().equals(properties.get(DocumentDynamicModel.Props.STATUS.toString()));
    }

    @Override
    protected boolean isValidStatus(String status) {
        return DocListUnitStatus.OPEN.getValueName().equals(status);
    }
    
    @Override
    protected boolean evaluateInternal(Node caseFileNode) {
        if (!Boolean.valueOf(getParametersService().getStringParameter(Parameters.ALLOW_CLOSE_FOR_ARCIVED_VOLUMES)) && !StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(caseFileNode.getNodeRef().getStoreRef()) || BeanHelper.getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        Map<String, Object> properties = caseFileNode.getProperties();
        return isValidStatus(properties) && isUserAllowedToView(caseFileNode, properties);
    }

}