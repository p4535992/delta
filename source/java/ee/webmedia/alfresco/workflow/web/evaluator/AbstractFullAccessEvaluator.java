package ee.webmedia.alfresco.workflow.web.evaluator;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;

/**
 * Abstract base for compound workflow dialog evaluators to check if user has full or partial access rights.
 */
public abstract class AbstractFullAccessEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    protected boolean hasFullAccess() {
        FacesContext context = FacesContext.getCurrentInstance();
        ValueBinding vb = context.getApplication().createValueBinding("#{CompoundWorkflowDialog.fullAccess}");
        return (Boolean) vb.getValue(context);
    }

}
