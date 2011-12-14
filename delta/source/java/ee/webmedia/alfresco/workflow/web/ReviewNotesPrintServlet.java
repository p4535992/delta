package ee.webmedia.alfresco.workflow.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.repo.tag.PageTag;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * Servlet that render review notes block form document dialog - could be used to view all review notes and/or print them without rest of the document dialog
 * 
 * @author Ats Uiboupin
 */
public class ReviewNotesPrintServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            PageTag pageTag = new PageTag();
            pageTag.setTitle(MessageUtil.getMessage("workflow_task_review_notes"));
            pageTag.doStartTag(request, out, request.getSession());
            renderTableStart(out);
            UIRichList richList = BeanHelper.getWorkflowBlockBean().getReviewNotesRichList();
            if (richList != null) {
                int origPageSize;
                ValueBinding pageSizeVB;
                { // need to show all elements of the richlist - before changing pageSize remember original value
                    pageSizeVB = richList.getValueBinding("pageSize");
                    if (pageSizeVB != null) {
                        origPageSize = (Integer) pageSizeVB.getValue(FacesContext.getCurrentInstance());
                    } else {
                        origPageSize = richList.getPageSize();
                    }
                }
                { // show all rows on single page
                    if (pageSizeVB != null) {
                        richList.setValueBinding("pageSize", null);
                    }
                    richList.setPageSize(Integer.MAX_VALUE);
                }
                richList.bind();// prepare the component current row against the current page settings
                renderRows(out, richList);
                { // restore original pageSize
                    if (pageSizeVB != null) {
                        richList.setValueBinding("pageSize", pageSizeVB);
                    }
                    richList.setPageSize(origPageSize);
                }
            }
            renderTableEnd(out);
            pageTag.doEndTag(out);
        } catch (JspException e) {
            throw new RuntimeException("Failed", e);
        }
    }

    private void renderTableStart(PrintWriter out) {
        out.println("<table width='100%' cellspacing='0' cellpadding='0'><thead><tr>"
                + "<th style='width: 20%;'>" + MessageUtil.getMessage("workflow_task_reviewer_name") + "</th>"
                + "<th style='width: 10%;'>" + MessageUtil.getMessage("workflow_date") + "</th>"
                + "<th style='width: 70%;'>" + MessageUtil.getMessage("workflow_task_review_note") + "</th>"
                + "</tr></thead><tbody>");
    }

    private void renderRows(PrintWriter out, UIRichList richList) {
        if (richList.isDataAvailable()) {
            while (richList.isDataAvailable()) {
                Task task = (Task) richList.nextRow();
                Date completedDateTime = task.getCompletedDateTime();
                String completedDTStr = completedDateTime != null ? Task.dateFormat.format(completedDateTime) : "";
                out.println(getTr(task.getOwnerName(), completedDTStr, task.getOutcome() + ": " + task.getComment()));
            }
        }
    }

    private String getTr(String name, String date, String notes) {
        return "<tr class='recordSetRow'>"
                + "<td style='width: 20%;'><span>" + name + "</span></td>"
                + "<td style='width: 10%;'><span>" + date + "</span></td>"
                + "<td style='width: 70%;'><span>" + notes + "</span></td>"
                + "</tr>";
    }

    private void renderTableEnd(PrintWriter out) {
        out.println("</tbody></table>");
    }

}
