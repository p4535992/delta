package ee.webmedia.alfresco.workflow.web;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Keit Tehvan
 */
public class CompoundWorkflowLinkGeneratorModalComponent extends UICommand {

    public static final String COMPOUND_WORKFLOW_LINK_MODAL_ID = "generateCompoundWorkflowLinkModal";

    public CompoundWorkflowLinkGeneratorModalComponent() {
        setRendererType(null);
    }

    @Override
    public void decode(FacesContext context) {
        // do nothing
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        NodeRef nodeRef = BeanHelper.getCompoundWorkflowDialog().compoundWorkflow.getNodeRef();
        if (!isRendered() || RepoUtil.isUnsaved(nodeRef)) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        // modal popup code
        ComponentUtil.writeModalHeader(out, COMPOUND_WORKFLOW_LINK_MODAL_ID, MessageUtil.getMessage("compoundWorkflow_show_url"), null);

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td><textarea id='objectUrl' class='expand19-200' readonly='readonly'>" + BeanHelper.getDocumentTemplateService().getCompoundWorkflowUrl(nodeRef)
                + "</textarea></td></tr>");
        out.write("</tbody></table>");

        ComponentUtil.writeModalFooter(out);
    }

}
