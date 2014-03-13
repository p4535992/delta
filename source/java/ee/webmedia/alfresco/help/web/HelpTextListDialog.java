package ee.webmedia.alfresco.help.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getHelpTextEditDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getHelpTextService;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.help.model.HelpText;
import ee.webmedia.alfresco.help.model.HelpTextModel;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Dialog for viewing and editing help texts. There are three types of help texts: for dialogs, document types and fields.
 * <p>
 * Specification: <i>Kontekstitundlik abiinfo</i>.
 */
public class HelpTextListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public List<HelpText> getHelpTexts() {
        return getHelpTextService().getHelpTexts();
    }

    public void edit(ActionEvent event) {
        NodeRef textRef = new NodeRef(ActionUtil.getParam(event, "textRef"));
        String type = StringUtils.capitalize((String) getNodeService().getProperty(textRef, HelpTextModel.Props.TYPE));
        getHelpTextEditDialog(type).init(textRef);
    }

    public void deleteCallback() {
        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        servletContext.setAttribute("helpText", getHelpTextService().getHelpTextKeys());
    }

}
