package ee.webmedia.alfresco.workflow.web.evaluator;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import ee.webmedia.alfresco.common.evaluator.CompoundWorkflowActionGroupSharedResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;

/**
 * Abstract base for compound workflow dialog evaluators to check if user has full or partial access rights.
 */
public abstract class AbstractFullAccessEvaluator extends SharedResourceEvaluator {

    private static final long serialVersionUID = 1L;

    public static boolean hasFullAccess() {
        FacesContext context = FacesContext.getCurrentInstance();
        ValueBinding vb = context.getApplication().createValueBinding("#{CompoundWorkflowDialog.fullAccess}");
        return (Boolean) vb.getValue(context);
    }

    protected boolean isOwnerOrDocManager() {
        FacesContext context = FacesContext.getCurrentInstance();
        ValueBinding vb = context.getApplication().createValueBinding("#{CompoundWorkflowDialog.ownerOrDocManager}");
        return (Boolean) vb.getValue(context);
    }

    protected boolean hasFullAccess(CompoundWorkflowActionGroupSharedResource resource) {
        Boolean fullAccess = resource.isFullAccess();
        if (resource.isFullAccess() != null) {
            return fullAccess;
        }
        fullAccess = hasFullAccess();
        resource.setFullAccess(fullAccess);
        return fullAccess;
    }

    protected boolean isOwnerOrDocManager(CompoundWorkflowActionGroupSharedResource resource) {
        Boolean isOwnerOrDocManager = resource.isOwnerOrDocManager();
        if (isOwnerOrDocManager != null) {
            return isOwnerOrDocManager;
        }
        isOwnerOrDocManager = isOwnerOrDocManager();
        resource.setOwnerOrDocManager(isOwnerOrDocManager);
        return isOwnerOrDocManager;
    }

}
