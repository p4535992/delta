package ee.webmedia.alfresco.workflow.web;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;

import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * This custom component provides the "Mark task as finished" comment field modal popup for workflow tasks.
 * 
 * @author Erko Hansar
 */
public class TaskListCommentComponent extends UICommand {

    private final static int ACTION_CLEAR = 1;
    private final static int ACTION_ADD = 2;

    public static final String TASK_INDEX = "taskIndex";

    public TaskListCommentComponent() {
        setRendererType(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void decode(FacesContext context) {
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String actionValue = requestMap.get(getClientId(context));
        if (StringUtils.isNotBlank(actionValue)) {
            int action = Integer.parseInt(actionValue);
            if (action == ACTION_CLEAR) {
                // do nothing;
            } else if (action == ACTION_ADD) {
                String comment = requestMap.get(getClientId(context) + "_comment");
                String taskIndexValue = requestMap.get(TaskListGenerator.getActionId(context, this));
                int taskIndex = Integer.parseInt(taskIndexValue.substring((TASK_INDEX + ";").length()));

                CommentEvent event = new CommentEvent(this, taskIndex, comment);
                queueEvent(event);
            } else {
                throw new RuntimeException("Unknown action: " + actionValue);
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (isRendered() == false) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        // modal popup code
        out.write("<div id=\"" + TaskListGenerator.getDialogId(context, this) + "\" class=\"modalpopup modalwrap\">");
        out.write("<div class=\"modalpopup-header clear\"><h1>");
        out.write(MessageUtil.getMessage("task_finish_popup"));
        out.write("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, this, TaskListGenerator.getActionId(context, this), ""));
        out.write(Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_CLEAR)));
        out.write("\">");
        out.write(MessageUtil.getMessage("close_window"));
        out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner modalpopup-filter\">");

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td>" + MessageUtil.getMessage("task_finish_comment") + ":</td></tr>");
        out.write("<tr><td>");
        out.write("<textarea id=\"" + getClientId(context) + "_comment\" name=\"" + getClientId(context) + "_comment\" style=\"height:100px; width:100%;\" onkeyup=\"");
        out.write("document.getElementById('" + getClientId(context) + "_btn').disabled = (document.getElementById('" + getClientId(context)
                + "_comment').value == null || document.getElementById('" + getClientId(context) + "_comment').value.replace(/^\\s+|\\s+$/g, '').length == 0);");
        out.write("\" ></textarea>");
        out.write("</td></tr>");
        out.write("<tr><td>");
        out.write("<input id=\"" + getClientId(context) + "_btn\" type=\"submit\" value=\"" + MessageUtil.getMessage("task_finish") + "\" disabled=\"true\" onclick=\""
                + Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_ADD)) + "\" />");
        out.write("</td></tr>");
        out.write("</tbody></table>");

        // close modal popup
        out.write("</div></div></div>");
    }

    public static class CommentEvent extends ActionEvent {

        private static final long serialVersionUID = 1L;

        public int taskIndex;
        public String comment;

        public CommentEvent(UIComponent component, int taskIndex, String comment) {
            super(component);
            this.taskIndex = taskIndex;
            this.comment = comment;
        }

    }

}
