package ee.webmedia.alfresco.casefile.web;

import java.io.IOException;

import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringEscapeUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.web.DocumentLinkGeneratorModalComponent;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class CaseFileLinkGeneratorModalComponent extends DocumentLinkGeneratorModalComponent {

    public static final String CASE_FILE_LINK_MODAL_ID = "generateCaseFileLinkModal";

    @Override
    protected String generateUrl() {
        return StringEscapeUtils.escapeHtml(BeanHelper.getDocumentTemplateService().getCaseFileUrl(BeanHelper.getCaseFileDialog().getCaseFile().getNodeRef()));
    }

    @Override
    protected void createHeader(ResponseWriter out) throws IOException {
        ComponentUtil.writeModalHeader(out, CASE_FILE_LINK_MODAL_ID, MessageUtil.getMessage("caseFile_view_url"), null);
    }

}
