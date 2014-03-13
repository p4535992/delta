package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CHANCELLORS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CONTRACT_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CONTRACT_SIM;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.CONTRACT_SMIT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.DECREE;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_APPLICATION_DOMESTIC;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ERRAND_ORDER_ABROAD_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.INTERNAL_APPLICATION_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LEAVING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.LICENCE;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MANAGEMENTS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MEMO;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MINISTERS_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.MINUTES;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.ORDER_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.OUTGOING_LETTER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PERSONELLE_ORDER_SIM;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PERSONELLE_ORDER_SMIT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PROJECT_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REGULATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REPORT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.RESOLUTION_MV;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.SUPERVISION_REPORT;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TRAINING_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.VACATION_APPLICATION;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.VACATION_ORDER;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.VACATION_ORDER_SMIT;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.DocumentSavedActionEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;
import ee.webmedia.alfresco.workflow.service.HasNoStoppedOrInprogressWorkflowsEvaluator;

/**
 * Evaluator, that evaluates to true if user is admin or document manager or document owner.
 */
public class SendOutActionEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 2958297435415449179L;
    private static final List<QName> registeredTypes = Arrays.asList(
            OUTGOING_LETTER, CHANCELLORS_ORDER, MINISTERS_ORDER, PERSONELLE_ORDER_SIM, PERSONELLE_ORDER_SMIT, REGULATION
            , VACATION_ORDER, VACATION_ORDER_SMIT, DECREE, CONTRACT_SIM, CONTRACT_SMIT
            , SUPERVISION_REPORT, MANAGEMENTS_ORDER, INTERNAL_APPLICATION, INSTRUMENT_OF_DELIVERY_AND_RECEIPT
            , REPORT, LICENCE, MEMO, MINUTES, TRAINING_APPLICATION, LEAVING_LETTER, ERRAND_ORDER_ABROAD, ERRAND_APPLICATION_DOMESTIC
            , ORDER_MV, CONTRACT_MV, INTERNAL_APPLICATION_MV, VACATION_APPLICATION, ERRAND_ORDER_ABROAD_MV, RESOLUTION_MV, PROJECT_APPLICATION
            );

    @Override
    public boolean evaluate(Node node) {
        boolean result = new ViewStateActionEvaluator().evaluate(node) && new DocumentSavedActionEvaluator().evaluate(node)
                && node.hasPermission(DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA);
        if (result) {
            final Map<String, Object> props = node.getProperties();
            final String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
            if (regNumber == null) {
                result = new HasNoStoppedOrInprogressWorkflowsEvaluator().evaluate(node);
            }
            if (result && registeredTypes.contains(node.getType())) {
                result = regNumber != null;
            }
        }
        return result;
    }
}
