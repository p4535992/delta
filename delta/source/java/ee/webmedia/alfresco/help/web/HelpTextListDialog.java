package ee.webmedia.alfresco.help.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.help.model.HelpTextModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Dialog for viewing and editing help texts. There are three types of help texts: for dialogs, document types and fields.
 * <p>
 * Specification: <i>Kontekstitundlik abiinfo</i>.
 * 
 * @author Martti Tamm
 */
public class HelpTextListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public List<Node> getHelpTexts() {
        return BeanHelper.getHelpTextService().getHelpTexts();
    }

    public void edit(ActionEvent event) {
        NodeRef textRef = new NodeRef(ActionUtil.getParam(event, "textRef"));
        String type = StringUtils.capitalize((String) BeanHelper.getNodeService().getProperty(textRef, HelpTextModel.Props.TYPE));

        BeanHelper.getHelpTextEditDialog(type).init(textRef);
        WebUtil.navigateTo("dialog:" + StringUtils.uncapitalize(type) + HelpTextEditDialog.BEAN_NAME_SUFFIX);
    }

    public void delete(@SuppressWarnings("unused") ActionEvent event) {
        NodeRef textRef = new NodeRef(ActionUtil.getParam(event, "textRef"));
        BeanHelper.getHelpTextService().deleteHelp(textRef);
    }

}
