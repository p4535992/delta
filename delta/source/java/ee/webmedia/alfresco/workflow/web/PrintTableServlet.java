package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
import static ee.webmedia.alfresco.docadmin.service.MetadataItemCompareUtil.cast;
import static java.util.Arrays.asList;

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

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.repo.tag.PageTag;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.ComparableTransformer;
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
@SuppressWarnings("deprecation")
public class PrintTableServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_MODE = "tableMode";

    public static final ComparatorChain ROW_COMPARATOR = new ComparatorChain();
    static {
        ROW_COMPARATOR.addComparator(new TransformingComparator(new ComparableTransformer<Row>() {
            @Override
            public Comparable<Integer> tr(Row input) {
                return input.groupNr;
            }
        }));
        ROW_COMPARATOR.addComparator(new TransformingComparator(new ComparableTransformer<Row>() {
            @Override
            public Comparable<Integer> tr(Row input) {
                return input.childNodeNr;
            }
        }));
        ROW_COMPARATOR.addComparator(new TransformingComparator(new ComparableTransformer<Row>() {
            @Override
            public Comparable<Integer> tr(Row input) {
                return input.grandChildNodeNr;
            }
        }));
        ROW_COMPARATOR.addComparator(new TransformingComparator(new ComparableTransformer<Row>() {
            @Override
            public Comparable<Integer> tr(Row input) {
                return input.fieldNr;
            }
        }));
    }

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

            List<Row> rows = null;
            if (TableMode.REVIEW_NOTES == mode) {
                rows = getReviewNotesData();
            } else if (TableMode.DOCUMENT_FIELD_COMPARE == mode) {
                rows = getDocumentFieldsData(request);
            }

            renderRows(out, rows);

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

    private void renderRows(PrintWriter out, List<Row> rows) {
        int zebra = 0;
        StringBuilder sb = new StringBuilder();
        for (Row row : rows) {
            sb.append("<tr class='").append(zebra % 2 == 0 ? "recordSetRow" : "recordSetRowAlt").append(" ").append(row.styleClass).append("'>");
            for (String cell : row.getCells()) {
                sb.append("<td>").append(cell).append("</td>");
            }
            sb.append("</tr>");
            zebra++;
        }

        out.print(sb.toString());
    }

    private void renderTableEnd(PrintWriter out) {
        out.println("</tbody></table>");
    }

    private List<Row> getReviewNotesData() {
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

            List<Row> data = new ArrayList<Row>();
            { // Fetch data
                if (richList.isDataAvailable()) {
                    while (richList.isDataAvailable()) {
                        Task task = (Task) richList.nextRow();
                        Date completedDateTime = task.getCompletedDateTime();
                        String completedDTStr = completedDateTime != null ? Task.dateFormat.format(completedDateTime) : "";
                        data.add(new Row(asList(task.getOwnerName(), completedDTStr, task.getOutcome() + ": " + task.getComment())));
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

    private List<Row> getDocumentFieldsData(HttpServletRequest request) {
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

        List<Row> data = new ArrayList<Row>();
        Map<String, Row> result = new HashMap<String, Row>();
        getAdjacentProperties(docRef1, docRef2, result, getDocumentConfigService().getPropertyDefinitions(new Node(docRef1)), 0, -1, -1);
        getAdjacentProperties(docRef1, docRef2, result, getDocumentConfigService().getPropertyDefinitions(new Node(docRef2)), 0, -1, -1);

        for (Row row : result.values()) {
            data.add(row);
        }

        Collections.sort(data, cast(ROW_COMPARATOR, Row.class));
        return data;
    }

    private void getAdjacentProperties(NodeRef nodeRef1, NodeRef nodeRef2, Map<String, Row> result, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, int nodeLevel,
            int childIndex, int grandChildIndex) {
        Map<QName, Serializable> props1 = nodeRef1 != null ? getNodeService().getProperties(nodeRef1) : Collections.<QName, Serializable> emptyMap();
        Map<QName, Serializable> props2 = nodeRef2 != null ? getNodeService().getProperties(nodeRef2) : Collections.<QName, Serializable> emptyMap();

        for (Entry<String, Pair<DynamicPropertyDefinition, Field>> entry : propDefs.entrySet()) {
            String key = entry.getKey();
            if (result.get(key) != null) {
                continue;
            }
            QName[] level = entry.getValue().getFirst().getChildAssocTypeQNameHierarchy();
            Field field = entry.getValue().getSecond();
            if (field == null || level == null && nodeLevel != 0 || (level != null && level.length != nodeLevel)) {
                continue; // hidden field or wrong level
            }
            Row row = new Row();
            row.fieldNr = field.getOrder();
            FieldGroup fieldGroup = null;
            if (field.getParent() instanceof FieldGroup) {
                fieldGroup = (FieldGroup) field.getParent();
                row.groupNr = fieldGroup.getOrder();
            } else {
                row.groupNr = row.fieldNr;

            }

            QName qName = field.getQName();
            Serializable prop1 = props1.get(qName);
            Serializable prop2 = props2.get(qName);
            row.styleClass = !ObjectUtils.equals(prop1, prop2) ? "red" : "";

            row.childNodeNr = childIndex;
            row.grandChildNodeNr = grandChildIndex;
            row.setCells(asList(field.getName(), getFieldTypeSpecificValue(field, prop1), getFieldTypeSpecificValue(field, prop2)));
            result.put(key + childIndex + grandChildIndex, row);
        }

        if (nodeLevel < 3) {
            fetchChildNodeData(nodeRef1, nodeRef2, propDefs, result);
        }
    }

    private void fetchChildNodeData(NodeRef docRef1, NodeRef docRef2, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, Map<String, Row> result) {
        // Domestic applications
        getApplicationsData(docRef1, docRef2, propDefs, result, DocumentChildModel.Assocs.APPLICANT_DOMESTIC, DocumentChildModel.Assocs.ERRAND_DOMESTIC);
        // Abroad applications
        getApplicationsData(docRef1, docRef2, propDefs, result, DocumentChildModel.Assocs.APPLICANT_ABROAD, DocumentChildModel.Assocs.ERRAND_ABROAD);

        // Contract parties
        List<ChildAssociationRef> doc1Parties = getChildAssocs(docRef1, DocumentChildModel.Assocs.CONTRACT_PARTY);
        List<ChildAssociationRef> doc2Parties = getChildAssocs(docRef2, DocumentChildModel.Assocs.CONTRACT_PARTY);
        int partyCount = Math.max(doc1Parties.size(), doc2Parties.size());

        if (partyCount == 0) {
            return;
        }

        String party = MessageUtil.getMessage("subPropSheet_contractParty_plain");
        Map<String, Row> tempResult = new HashMap<String, Row>();
        for (int i = 0; i < partyCount; i++) {
            Row row = new Row(asList(party, "", ""));
            row.childNodeNr = i;
            row.grandChildNodeNr = -1;
            row.groupNr = 0;
            row.styleClass = "bold";
            result.put("applicant" + i, row);
            NodeRef subNodeRef1 = getChildRefOrNull(doc1Parties, i);
            NodeRef subNodeRef2 = getChildRefOrNull(doc2Parties, i);
            getAdjacentProperties(subNodeRef1, subNodeRef2, tempResult, propDefs, 1, i, -1);
            row.groupNr = getGroupNrAndAdd(tempResult, result);
        }

    }

    private void getApplicationsData(NodeRef docRef1, NodeRef docRef2, Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, Map<String, Row> result, QName childNodeAssoc,
            QName grandChildNodeAssoc) {
        String applicant = MessageUtil.getMessage("document_applicantInstitutionPerson");
        String application = MessageUtil.getMessage("document_errand_errandDates_title");
        List<ChildAssociationRef> applicant1 = getChildAssocs(docRef1, childNodeAssoc);
        List<ChildAssociationRef> applicant2 = getChildAssocs(docRef2, childNodeAssoc);
        int applicantCount = Math.max(applicant1.size(), applicant2.size());

        if (applicantCount == 0) {
            return;
        }

        Map<String, Row> tempResult = new HashMap<String, Row>();
        for (int i = 0; i < applicantCount; i++) {
            Row row = new Row(asList(applicant, "", ""));
            row.childNodeNr = i;
            row.grandChildNodeNr = -1;
            row.styleClass = "bold";
            result.put("applicant" + i, row);
            NodeRef subNodeRef1 = getChildRefOrNull(applicant1, i);
            NodeRef subNodeRef2 = getChildRefOrNull(applicant2, i);
            getAdjacentProperties(subNodeRef1, subNodeRef2, tempResult, propDefs, 1, i, -1);
            row.groupNr = getGroupNrAndAdd(tempResult, result);

            // applications
            List<ChildAssociationRef> application1 = getChildAssocs(subNodeRef1, grandChildNodeAssoc);
            List<ChildAssociationRef> application2 = getChildAssocs(subNodeRef2, grandChildNodeAssoc);
            int applicationCount = Math.max(application1.size(), application2.size());
            for (int j = 0; j < applicationCount; j++) {
                Row row2 = new Row(asList(application, "", ""));
                row2.childNodeNr = i;
                row2.grandChildNodeNr = j;
                row2.styleClass = "bold";
                result.put("application" + i + "-" + j, row2);
                NodeRef subNodeRef21 = getChildRefOrNull(application1, j);
                NodeRef subNodeRef22 = getChildRefOrNull(application2, j);
                getAdjacentProperties(subNodeRef21, subNodeRef22, tempResult, propDefs, 2, i, j);
                row2.groupNr = getGroupNrAndAdd(tempResult, result);
            }
        }
    }

    private int getGroupNrAndAdd(Map<String, Row> tempResult, Map<String, Row> result) {
        int groupNr = 0;
        for (Row row : tempResult.values()) {
            groupNr = row.groupNr;
            break;
        }

        // Add and clear
        result.putAll(tempResult);
        tempResult.clear();

        return groupNr;
    }

    private NodeRef getChildRefOrNull(List<ChildAssociationRef> childAssocRefs, int i) {
        return childAssocRefs.size() > i ? childAssocRefs.get(i).getChildRef() : null;
    }

    private List<ChildAssociationRef> getChildAssocs(NodeRef parentRef, QName assocQName) {
        return parentRef != null ? getNodeService().getChildAssocs(parentRef, assocQName, assocQName) : Collections.<ChildAssociationRef> emptyList();
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
            @SuppressWarnings("unchecked")
            List<String> orgStructs = (List<String>) prop;
            return UserUtil.getDisplayUnit(orgStructs);
        case DATE:
            if (prop instanceof List) {
                @SuppressWarnings("unchecked")
                List<Date> propDates = (List<Date>) prop;
                List<String> dates = new ArrayList<String>(propDates.size());
                for (Date date : propDates) {
                    dates.add(date == null ? "" : FastDateFormat.getInstance("dd.MM.yyyy").format(date));
                }
                return StringUtils.join((List<?>) prop, ", ");
            }
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

    private class Row {
        private int groupNr = 0;
        private int fieldNr = 0;
        private int childNodeNr = 0;
        private int grandChildNodeNr = 0;
        private String styleClass = "";
        private List<String> cells = new ArrayList<String>();

        public Row() {
            // Default constructor
        }

        public Row(List<String> list) {
            cells = list;
        }

        public List<String> getCells() {
            return cells;
        }

        public void setCells(List<String> cells) {
            this.cells = cells;
        }
    }
}
