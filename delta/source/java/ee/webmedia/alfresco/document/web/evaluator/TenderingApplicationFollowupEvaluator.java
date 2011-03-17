package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.TENDERING_APPLICATION;

/**
 * @author Ats Uiboupin
 */
public class TenderingApplicationFollowupEvaluator extends AbstractFollowUpNodeTypeEvaluator {
    private static final long serialVersionUID = -6934987918148330482L;

    public TenderingApplicationFollowupEvaluator() {
        nodeTypes.add(TENDERING_APPLICATION);
    }

}
