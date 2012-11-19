package ee.webmedia.alfresco.docdynamic.web;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringEscapeUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Keit Tehvan
 */
public class DocumentLinkGeneratorModalComponent extends UICommand {

    public static final String DOCUMENT_LINK_MODAL_ID = "generateDocumentLinkModal";

    public DocumentLinkGeneratorModalComponent() {
        setRendererType(null);
    }

    @Override
    public void decode(FacesContext context) {
        // do nothing
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (isRendered() == false) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        // modal popup code
        ComponentUtil.writeModalHeader(out, DOCUMENT_LINK_MODAL_ID, MessageUtil.getMessage("document_view_url"), null);

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td><textarea id='objectUrl' class='expand19-200' readonly='readonly'>"
                + StringEscapeUtils.escapeHtml(BeanHelper.getDocumentTemplateService().getDocumentUrl(BeanHelper.getDocumentDynamicDialog().getDocument().getNodeRef()))
                + "</textarea></td></tr>");
        out.write("</tbody></table>");
        ComponentUtil.writeModalFooter(out);
    }

}