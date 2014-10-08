<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Riina Tens
 */
public class LinkedReviewTask extends Task {

    private static final long serialVersionUID = 1L;

    protected LinkedReviewTask(WmNode node, Workflow parent, Integer outcomes) {
        super(node, parent, outcomes);
    }

    @Override
    protected void preSave() {
        calculateOverdue();

        if (isUnsaved()) {
            getNode().getAspects().add(WorkflowSpecificModel.Aspects.SEARCHABLE);
        }
        if (isStatus(Status.DELETED)) {
            getNode().getAspects().remove(WorkflowSpecificModel.Aspects.SEARCHABLE);
        }
    }

}
=======
package ee.webmedia.alfresco.workflow.service;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

public class LinkedReviewTask extends Task {

    private static final long serialVersionUID = 1L;

    protected LinkedReviewTask(WmNode node, Workflow parent, Integer outcomes) {
        super(node, parent, outcomes);
    }

    @Override
    protected void preSave() {
        calculateOverdue();

        if (isUnsaved()) {
            getNode().getAspects().add(WorkflowSpecificModel.Aspects.SEARCHABLE);
        }
        if (isStatus(Status.DELETED)) {
            getNode().getAspects().remove(WorkflowSpecificModel.Aspects.SEARCHABLE);
        }
    }

}
>>>>>>> develop-5.1
