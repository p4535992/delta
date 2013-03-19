package ee.webmedia.alfresco.eventplan.web;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.eventplan.model.EventPlan;

/**
 * Specification: <em>Elukäigud.docx</em>
 * 
 * @author Martti Tamm
 */
public class EventPlanListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    @Override
    protected String finishImpl(FacesContext context, String outcome) {
        return null;
    }

    public List<EventPlan> getPlans() {
        return BeanHelper.getEventPlanService().getEventPlans();
    }
}
