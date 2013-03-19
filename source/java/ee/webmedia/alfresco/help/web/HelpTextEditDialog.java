package ee.webmedia.alfresco.help.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getHelpTextService;
import static ee.webmedia.alfresco.help.web.HelpTextUtil.TYPE_DIALOG;
import static ee.webmedia.alfresco.help.web.HelpTextUtil.TYPE_DOCUMENT_TYPE;
import static ee.webmedia.alfresco.help.web.HelpTextUtil.TYPE_FIELD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;

import org.alfresco.config.Config;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.config.DialogsConfigElement;
import org.alfresco.web.config.DialogsConfigElement.DialogConfig;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.BeanNameAware;

import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.help.model.HelpTextModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Dialog for adding and editing an help text. There are three types of help texts: for dialogs, document types and fields.
 * <p>
 * Specification: <i>Kontekstitundlik abiinfo</i>.
 * 
 * @author Martti Tamm
 */
public class HelpTextEditDialog extends BaseDialogBean implements BeanNameAware {

    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME_SUFFIX = "HelpTextEditDialog";

    private static final String PROP_CODE = HelpTextModel.Props.CODE.toString();

    private static final String PROP_NAME = HelpTextModel.Props.NAME.toString();

    private String dialogMode;

    private Node helpText;

    @Override
    public void setBeanName(String beanName) {
        dialogMode = StringUtils.uncapitalize(beanName.substring(0, beanName.length() - BEAN_NAME_SUFFIX.length()));

        if (!TYPE_DIALOG.equals(dialogMode) && !TYPE_DOCUMENT_TYPE.equals(dialogMode) && !TYPE_FIELD.equals(dialogMode)) {
            throw new IllegalStateException("Invalid dialog mode: " + dialogMode);
        }
    }

    @SuppressWarnings("unused")
    public void init(ActionEvent event) {
        helpText = new TransientNode(HelpTextModel.Types.HELP_TEXT, HelpTextModel.Types.HELP_TEXT.getLocalName(), null);
    }

    public void init(NodeRef helpTextRef) {
        helpText = getGeneralService().fetchNode(helpTextRef);

        if (TYPE_DIALOG.equals(dialogMode)) {
            helpText.getProperties().put(getCodeEditProp(), helpText.getProperties().get(PROP_CODE) + ";" + helpText.getProperties().get(PROP_NAME));
        } else {
            helpText.getProperties().put(getCodeEditProp(), helpText.getProperties().get(PROP_NAME));
        }

        WebUtil.navigateTo("dialog:" + dialogMode + BEAN_NAME_SUFFIX);
    }

    @SuppressWarnings({ "unused", "unchecked" })
    public List<SelectItem> getCodes(FacesContext context, UIInput input) {
        List<SelectItem> result = new ArrayList<SelectItem>();

        if (TYPE_DIALOG.equals(dialogMode)) {
            Config appConfig = Application.getConfigService(context).getGlobalConfig();
            DialogsConfigElement dialogs = (DialogsConfigElement) appConfig.getConfigElement("dialogs");
            for (DialogConfig dialogConf : dialogs.getDialogs().values()) {
                if (dialogConf.getPage() == null || !dialogConf.getPage().contains("ee/webmedia/")) {
                    continue;
                }

                String title = dialogConf.getTitleId() != null ? MessageUtil.getMessage(dialogConf.getTitleId()) : dialogConf.getTitle();
                if (title != null) {
                    String label = title + " (" + dialogConf.getManagedBean() + ')';
                    result.add(new SelectItem(dialogConf.getName() + ";" + title, label));
                }
            }
        }

        Collections.sort(result, WebUtil.selectItemLabelComparator);
        return result;
    }

    public void processFieldSearchResults(String fieldCode) {
        FieldDefinition fieldDefinition = getDocumentAdminService().getFieldDefinition(fieldCode);
        String fieldName = fieldDefinition != null ? fieldDefinition.getName() : fieldCode;
        helpText.getProperties().put(PROP_CODE, fieldCode);
        helpText.getProperties().put(PROP_NAME, fieldName);
        helpText.getProperties().put(getCodeEditProp(), fieldName);
    }

    public void processDocumentTypeSearchResults(String docTypeCode) {
        String docTypeName = getDocumentAdminService().getDocumentTypeName(docTypeCode);
        if (docTypeName == null) {
            docTypeName = docTypeCode;
        }
        helpText.getProperties().put(PROP_CODE, docTypeCode);
        helpText.getProperties().put(PROP_NAME, docTypeName);
        helpText.getProperties().put(getCodeEditProp(), docTypeName);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        String code = (String) helpText.getProperties().get(PROP_CODE.toString());
        String name = (String) helpText.getProperties().get(PROP_NAME.toString());

        if (TYPE_DIALOG.equals(dialogMode)) {
            code = (String) helpText.getProperties().get(getCodeEditProp());
            name = StringUtils.substringAfter(code, ";");
            code = StringUtils.substringBefore(code, ";");
            helpText.getProperties().put(PROP_CODE, code);
            helpText.getProperties().put(PROP_NAME, name);
        }

        if (helpText instanceof TransientNode) {
            String content = (String) helpText.getProperties().get(HelpTextModel.Props.CONTENT.toString());
            NodeRef helpRef = null;

            if (TYPE_DIALOG.equals(dialogMode)) {
                helpRef = getHelpTextService().addDialogHelp(code, content).getNodeRef();
            } else if (TYPE_DOCUMENT_TYPE.equals(dialogMode)) {
                helpRef = getHelpTextService().addDocumentTypeHelp(code, content).getNodeRef();
            } else if (TYPE_FIELD.equals(dialogMode)) {
                helpRef = getHelpTextService().addFieldHelp(code, content).getNodeRef();
            }

            getNodeService().setProperty(helpRef, HelpTextModel.Props.NAME, name);
            MessageUtil.addInfoMessage("help_text_info_added");
        } else {
            getHelpTextService().editHelp(helpText);
            MessageUtil.addInfoMessage("help_text_info_saved");
        }

        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        servletContext.setAttribute("helpText", getHelpTextService().getHelpTextKeys());

        return outcome;
    }

    public String getType() {
        return dialogMode;
    }

    public Node getNode() {
        return helpText;
    }

    private String getCodeEditProp() {
        return "hlt:code" + StringUtils.capitalize(dialogMode);
    }
}
