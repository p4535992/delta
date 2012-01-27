package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.repo.tag.PageTag;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * Servlet that render review notes block form document dialog - could be used to view all review notes and/or print them without rest of the document dialog
 * Also supports comparing metadata of two documents
 * 
 * @author Ats Uiboupin
 * @author Kaarel Jõgeva
 */
public class PrintTableServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_MODE = "tableMode";

    public enum TableMode {
        REVIEW_NOTES,
        DOCUMENT_FIELD_COMPARE
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String modeAttribute = request.getParameter(TABLE_MODE);
            Assert.isTrue(StringUtils.isNotBlank(modeAttribute), "Table printing mode attribute must be present in request!");
            TableMode mode = TableMode.valueOf(modeAttribute);

            resp.setContentType("text/html");
            resp.setCharacterEncoding("UTF-8");
            PrintWriter out = resp.getWriter();
            PageTag pageTag = new PageTag();
            pageTag.setTitle(getPageTitle(mode));
            pageTag.doStartTag(request, out, request.getSession());
            renderTableStart(out, mode);

            List<List<Cell>> data = null;
            if (TableMode.REVIEW_NOTES == mode) {
                data = getReviewNotesData();
            } else if (TableMode.DOCUMENT_FIELD_COMPARE == mode) {
                data = getDocumentFieldsData(request);
            }

            renderRows(out, data);

            renderTableEnd(out);
            pageTag.doEndTag(out);
        } catch (JspException e) {
            throw new RuntimeException("Failed", e);
        }
    }

    private String getPageTitle(TableMode mode) {
        switch (mode) {
        case REVIEW_NOTES:
            return MessageUtil.getMessage("workflow_task_review_notes");
        case DOCUMENT_FIELD_COMPARE:
            return MessageUtil.getMessage("document_assocsBlockBean_compare");
        }
        return "";
    }

    private void renderTableStart(PrintWriter out, TableMode mode) {
        StringBuilder sb = new StringBuilder(
                "<div class='panel view-mode' style='padding: 10px;' id='metadata-panel'>"
                        +
                        "<div class='panel-wrapper'>"
                        +
                        "<h3>"
                        + getPageTitle(mode)
                        + "&nbsp;&nbsp;</h3><div class='panel-border' id='metadata-panel-panel-border'><div id='dialog:dialog-body:doc-metatada_container'><table width='100%' cellspacing='0' cellpadding='0'><thead><tr>\n");

        for (String columnName : getColumnNames(mode)) {
            sb.append("\t<th>").append(MessageUtil.getMessage(columnName)).append("</th>\n");
        }
        sb.append("</tr></thead><tbody>\n");

        out.println(sb.toString());
    }

    private List<String> getColumnNames(TableMode mode) {
        if (TableMode.REVIEW_NOTES == mode) {
            return Arrays.asList("workflow_task_reviewer_name", "workflow_date", "workflow_task_review_note");
        } else if (TableMode.DOCUMENT_FIELD_COMPARE == mode) {
            return Arrays.asList("document_assocsBlockBean_compare_field", "document_assocsBlockBean_compare_doc1", "document_assocsBlockBean_compare_doc2");
        }

        return Collections.<String> emptyList();
    }

    private void renderRows(PrintWriter out, List<List<Cell>> data) {
        int zebra = 0;
        StringBuilder sb = new StringBuilder();
        for (List<Cell> row : data) {
            sb.append("<tr class='").append(zebra % 2 == 0 ? "recordSetRow" : "recordSetRowAlt").append("'>");
            for (Cell cell : row) {
                sb.append(cell.toString());
            }
            sb.append("</tr>");
            zebra++;
        }

        out.print(sb.toString());
    }

    private void renderTableEnd(PrintWriter out) {
        out.println("</tbody></table>");
    }

    private List<List<Cell>> getReviewNotesData() {
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

            List<List<Cell>> data = new ArrayList<List<Cell>>();
            { // Fetch data
                if (richList.isDataAvailable()) {
                    while (richList.isDataAvailable()) {
                        Task task = (Task) richList.nextRow();
                        Date completedDateTime = task.getCompletedDateTime();
                        String completedDTStr = completedDateTime != null ? Task.dateFormat.format(completedDateTime) : "";
                        data.add(Arrays.asList(new Cell(task.getOwnerName()), new Cell(completedDTStr), new Cell(task.getOutcome() + ": " + task.getComment())));
                    }
                }
            }

            { // restore original pageSize
                if (pageSizeVB != null) {
                    richList.setValueBinding("pageSize", pageSizeVB);
                }
                richList.setPageSize(origPageSize);
            }

            return data;
        }

        return Collections.emptyList();
    }

    private List<List<Cell>> getDocumentFieldsData(HttpServletRequest request) {
        PermissionService permissionService = getPermissionService();
        String docRef1Str = request.getParameter("doc1");
        Assert.notNull(docRef1Str, "Document 1 NodeRef must be supplied!");
        NodeRef docRef1 = new NodeRef(docRef1Str);
        Assert.isTrue(permissionService.hasPermission(docRef1, DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA) == AccessStatus.ALLOWED, "Missing "
                + DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA + " privilege for " + docRef1 + "!");

        String docRef2Str = request.getParameter("doc2");
        Assert.notNull(docRef2Str, "Document 2 NodeRef must be supplied!");
        NodeRef docRef2 = new NodeRef(docRef2Str);
        Assert.isTrue(permissionService.hasPermission(docRef2, DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA) == AccessStatus.ALLOWED, "Missing "
                + DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA + " privilege for " + docRef1 + "!");

        DocumentConfigService documentConfigService = BeanHelper.getDocumentConfigService();
        NodeService nodeService = BeanHelper.getNodeService();
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs1 = documentConfigService.getPropertyDefinitions(new Node(docRef1));
        Map<QName, Serializable> props1 = nodeService.getProperties(docRef1);
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs2 = documentConfigService.getPropertyDefinitions(new Node(docRef2));
        Map<QName, Serializable> props2 = nodeService.getProperties(docRef2);

        List<List<Cell>> data = new ArrayList<List<Cell>>();
        Map<String, List<Cell>> result = new HashMap<String, List<Cell>>();
        getAdjacentProperties(propDefs1, props1, props2, result);
        getAdjacentProperties(propDefs2, props1, props2, result);

        for (List<Cell> list : result.values()) {
            data.add(list);
        }

        return data;
    }

    private void getAdjacentProperties(Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions, Map<QName, Serializable> props1, Map<QName, Serializable> props2,
            Map<String, List<Cell>> result) {
        for (Entry<String, Pair<DynamicPropertyDefinition, Field>> entry : propertyDefinitions.entrySet()) {
            String key = entry.getKey();
            if (result.get(key) != null) {
                continue;
            }
            Field field = entry.getValue().getSecond();
            if (field == null) {
                continue; // hidden field
            }
            QName qName = field.getQName();

            Serializable prop1 = props1.get(qName);
            Serializable prop2 = props2.get(qName);
            boolean highlight = !ObjectUtils.equals(prop1, prop2);
            result.put(key, Arrays.asList(new Cell(field.getName(), highlight), new Cell(getFieldTypeSpecificValue(field, prop1), highlight),
                    new Cell(getFieldTypeSpecificValue(field, prop2), highlight)));
        }
    }

    private String getFieldTypeSpecificValue(Field field, Serializable prop) {
        if (prop == null) {
            return "";
        }

        // Handle location properties separately
        if (prop instanceof NodeRef) {
            return DocumentLocationGenerator.getDocumentListUnitLabel((NodeRef) prop);
        }
        switch (field.getFieldTypeEnum()) {
        case CHECKBOX:
            return MessageUtil.getMessage(Boolean.TRUE.equals(prop) ? "yes" : "no");
        case STRUCT_UNIT:
            return UserUtil.getDisplayUnit((List<String>) prop);
        case DATE:
            return FastDateFormat.getInstance("dd.MM.yyyy").format(prop);
        case USERS:
        case CONTACTS:
        case USERS_CONTACTS:
        case LISTBOX:
        default:
            if (prop instanceof List) {
                return StringUtils.join((List<?>) prop, ", ");
            }
            return prop.toString();
        }
    }

    private class Cell {
        private final String value;
        private boolean highlight;

        public Cell(String value, boolean highlight) {
            this.value = value;
            this.highlight = highlight;
        }

        public Cell(String value) {
            this.value = value;
        }

        public String getValue() {
            return StringUtils.defaultIfEmpty(value, "");
        }

        public String getStyleClass() {
            if (highlight) {
                return " class='red'";
            }
            return "";
        }

        @Override
        public String toString() {
            return new StringBuilder("<td").append(getStyleClass()).append(">").append(getValue()).append("</td>").toString();
        }
    }

}
