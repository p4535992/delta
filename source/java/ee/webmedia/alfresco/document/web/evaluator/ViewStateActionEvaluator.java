package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;

/**
 * UI action evaluator for validating whether document screen is in view mode.
 */
public class ViewStateActionEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        FacesContext context = FacesContext.getCurrentInstance();
        MetadataBlockBean bean = (MetadataBlockBean) FacesHelper.getManagedBean(context, MetadataBlockBean.BEAN_NAME);

        return !bean.isInEditMode();
    }
}
