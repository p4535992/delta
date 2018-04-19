package ee.webmedia.alfresco.privilege.web;

import javax.faces.el.ValueBinding;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * If value is node/nodeRef, then it tries to find ancestor document and perform evaluation on document.
 * If value is docRef, or no parent document is found then original value is used for evaluation.
 * - that way dynamic privileges, that are defined only for document are also considered in addition to static privileges inherited from ancestors
 */
public class DocPermissionEvaluator extends org.alfresco.web.ui.repo.component.evaluator.PermissionEvaluator {

    @Override
    public Object getValue() {
        ValueBinding vb = getValueBinding("value");
        if (vb != null) {
            value = vb.getValue(getFacesContext());

        }
        if (value != null) {
            if (value instanceof Node) {
                value = ((Node) value).getNodeRef();
            }
            if (value instanceof NodeRef) {
                NodeRef nodeRef = (NodeRef) value;
                NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(nodeRef, DocumentCommonModel.Types.DOCUMENT, true, false);
                if (docRef != null) {
                    return docRef;
                }
            }
        }
        return value;
    }
}
