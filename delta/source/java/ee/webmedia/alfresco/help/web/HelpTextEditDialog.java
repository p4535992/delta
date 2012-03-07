package ee.webmedia.alfresco.help.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getHelpTextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;

import org.alfresco.config.Config;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.config.DialogsConfigElement;
import org.alfresco.web.config.DialogsConfigElement.DialogConfig;
import org.springframework.beans.factory.BeanNameAware;

import ee.webmedia.alfresco.common.web.BeanHelper;
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

    public static final String TYPE_DIALOG = "Dialog";

    public static final String TYPE_DOC_TYPE = "DocumentType";

    public static final String TYPE_FIELD = "Field";

    public static final String BEAN_NAME_SUFFIX = "HelpTextEditDialog";

    private String dialogMode;

    private Node helpText;

    @Override
    public void setBeanName(String beanName) {
        dialogMode = beanName.substring(0, beanName.length() - BEAN_NAME_SUFFIX.length());

        if (!TYPE_DIALOG.equals(dialogMode) && !TYPE_DOC_TYPE.equals(dialogMode) && !TYPE_FIELD.equals(dialogMode)) {
            throw new IllegalStateException("Invalid dialog mode: " + dialogMode);
        }
    }

    @SuppressWarnings("unused")
    public void init(ActionEvent event) {
        helpText = new TransientNode(HelpTextModel.Types.HELP_TEXT, HelpTextModel.Types.HELP_TEXT.getLocalName(), null);
    }

    public void init(NodeRef helpTextRef) {
        helpText = BeanHelper.getGeneralService().fetchNode(helpTextRef);
        helpText.getProperties().put(getCodeEditProp().toString(), helpText.getProperties().get(HelpTextModel.Props.CODE));
        WebUtil.navigateTo("dialog:" + dialogMode + HelpTextEditDialog.BEAN_NAME_SUFFIX);
    }

    @SuppressWarnings({ "unused", "unchecked" })
    public List<SelectItem> getCodes(FacesContext context, UIInput input) {
        List<SelectItem> result = new ArrayList<SelectItem>();

        if (TYPE_DIALOG.equals(dialogMode)) {
            Config appConfig = Application.getConfigService(context).getGlobalConfig();
            DialogsConfigElement dialogs = (DialogsConfigElement) appConfig.getConfigElement("dialogs");
            dialogs.getDialogs().values().iterator().next().getTitleId();
            for (DialogConfig dialogConf : dialogs.getDialogs().values()) {
                if (dialogConf.getPage() == null || !dialogConf.getPage().contains("ee/webmedia/")) {
                    continue;
                }

                String title = dialogConf.getTitleId() != null ? MessageUtil.getMessage(dialogConf.getTitleId()) : dialogConf.getTitle();
                if (title != null) {
                    String label = new StringBuilder(title).append(" (").append(dialogConf.getManagedBean()).append(')').toString();
                    result.add(new SelectItem(dialogConf.getName(), label));
                }
            }
        }

        Collections.sort(result, SelectItemComparator.INSTANCE);
        return result;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        String code = (String) helpText.getProperties().get(getCodeEditProp());

        if (helpText instanceof TransientNode) {
            String content = (String) helpText.getProperties().get(HelpTextModel.Props.CONTENT.toString());

            if (TYPE_DIALOG.equals(dialogMode)) {
                getHelpTextService().addDialogHelp(code, content);
            } else if (TYPE_DOC_TYPE.equals(dialogMode)) {
                getHelpTextService().addDocumentTypeHelp(code, content);
            } else if (TYPE_FIELD.equals(dialogMode)) {
                getHelpTextService().addFieldHelp(code, content);
            }
            MessageUtil.addInfoMessage("help_text_info_added");
        } else {
            helpText.getProperties().put(HelpTextModel.Props.CODE.toString(), code);
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

    private QName getCodeEditProp() {
        return QName.createQName(HelpTextModel.URI, HelpTextModel.Props.CODE.getLocalName() + dialogMode);
    }

    private static class SelectItemComparator implements Comparator<SelectItem> {

        private static final Comparator<SelectItem> INSTANCE = new SelectItemComparator();

        @Override
        public int compare(SelectItem o1, SelectItem o2) {
            return o1.getLabel().compareTo(o2.getLabel());
        }
    }
}
