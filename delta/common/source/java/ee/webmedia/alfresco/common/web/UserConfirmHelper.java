package ee.webmedia.alfresco.common.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.application.Application;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Enables prompting users with a question and invoking methods based on user input.
 * 
 * @author Kaarel Jõgeva
 */
public class UserConfirmHelper implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "UserConfirmHelper";
    private static final String CONFIRM_ID = "userConfirmHelperConfirm";
    private static final String DENY_ID = "userConfirmHelperDeny";

    private transient UIPanel confirmContainer;

    public void setup(MessageData confirmMessage,
            String confirmAction, String confirmActionListener, Map<String, String> confirmParameters,
            String denyAction, String denyActionListener, Map<String, String> denyParameters) {
        reset();

        FacesContext context = FacesContext.getCurrentInstance();
        // Add buttons to the container
        addChildren(getConfirmContainer(), createButton(context, confirmAction, confirmActionListener, confirmParameters, CONFIRM_ID),
                createButton(context, denyAction, denyActionListener, denyParameters, DENY_ID), createScript(confirmMessage));
    }

    private HtmlCommandButton createButton(FacesContext context, String action, String actionListener, Map<String, String> params, String id) {
        HtmlCommandButton button = new HtmlCommandButton();
        button.setId(id);
        button.addActionListener(new ClearUserConfirmActionListener());
        button.setStyleClass("hidden");

        Application app = context.getApplication();
        if (StringUtils.isNotBlank(action)) {
            final MethodBinding mb;
            if (StringUtils.startsWith(action, "#{")) {
                mb = app.createMethodBinding(action, new Class[] {});
            } else {
                mb = new ConstantMethodBinding(action);
            }
            button.setAction(mb);
        }

        if (StringUtils.isNotBlank(actionListener)) {
            button.setActionListener(app.createMethodBinding(actionListener, new Class[] { javax.faces.event.ActionEvent.class }));
        }

        if (params != null) {
            for (Entry<String, String> entry : params.entrySet()) {
                addChildren(button, createUIParam(entry.getKey(), entry.getValue(), app));
            }
        }

        return button;
    }

    private HtmlOutputText createScript(MessageData confirmMessage) {
        HtmlOutputText script = new HtmlOutputText();
        script.setEscape(false);

        StringBuilder sb = new StringBuilder("<script type=\"text/javascript\">")
        .append("$jQ(document).ready(function() {")
        .append("var response = confirm(\"")
        .append(MessageUtil.getMessage(confirmMessage))
        .append("\");")
        .append("var btnId = response ? \"").append(CONFIRM_ID).append("\" : \"").append(DENY_ID).append("\";")
        .append("var btn = $jQ(escapeId4JQ(\"#dialog:\" + btnId));")
        .append("btn.click();")
        .append("});")
        .append("</script>");
        script.setValue(sb.toString());

        return script;
    }

    public void reset() {
        if (confirmContainer != null) {
            confirmContainer.getChildren().clear();
        }
    }

    // START: getters/setters

    public UIPanel getConfirmContainer() {
        if (confirmContainer == null) {
            confirmContainer = new UIPanel();
        }
        return confirmContainer;
    }

    public void setConfirmContainer(UIPanel confirmContainer) {
        this.confirmContainer = confirmContainer;
    }

    // END: getters/setters

    /**
     * ActionListener that clears the active confirm from the session
     * 
     * @author Kaarel Jõgeva
     */
    private class ClearUserConfirmActionListener implements ActionListener, Serializable {

        private static final long serialVersionUID = 1L;

        public ClearUserConfirmActionListener() {
            // Default constructor
        }

        @Override
        public void processAction(ActionEvent event) throws AbortProcessingException {
            BeanHelper.getUserConfirmHelper().reset();
        }
    }
}