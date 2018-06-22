package ee.webmedia.alfresco.functions.model;

import static ee.webmedia.alfresco.app.AppConstants.getNewCollatorInstance;
import static ee.webmedia.alfresco.utils.RepoUtil.getProp;
import static ee.webmedia.alfresco.utils.RepoUtil.getPropBoolean;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.RepoUtil;

public class UnmodifiableFunction implements Serializable, Comparable<UnmodifiableFunction> {

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String mark;
    private final String title;
    private final String description;
    private final String status;
    private final int order;
    private final String functionLabel;
    private final boolean documentActivitiesAreLimited;
    private final NodeRef nodeRef;

    public UnmodifiableFunction(Node node) {
        type = RepoUtil.getProp(FunctionsModel.Props.TYPE, node);
        mark = RepoUtil.getProp(FunctionsModel.Props.MARK, node);
        title = RepoUtil.getProp(FunctionsModel.Props.TITLE, node);
        functionLabel = mark + " " + title;
        description = RepoUtil.getProp(FunctionsModel.Props.DESCRIPTION, node);
        status = RepoUtil.getProp(FunctionsModel.Props.STATUS, node);
        Integer orderPropValue = getProp(FunctionsModel.Props.ORDER, node);
        order = orderPropValue != null ? orderPropValue : 0;
        documentActivitiesAreLimited = getPropBoolean(FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED, node);
        nodeRef = node.getNodeRef();
    }

    public String getType() {
        return type;
    }

    public String getMark() {
        return mark;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public int getOrder() {
        return order;
    }

    public boolean isDocumentActivitiesAreLimited() {
        return documentActivitiesAreLimited;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getFunctionLabel() {
        return functionLabel;
    }

    public String getFunctionLabelForModal() {
        return functionLabel + " (" + status + ")";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nType = " + type + "\n");
        sb.append("mark = " + mark + "\n");
        sb.append("title = " + title + "\n");
        sb.append("nodeRef = " + getNodeRef() + "\n");
        return sb.toString();
    }

    @Override
    public int compareTo(UnmodifiableFunction fn2) {
        if (getOrder() == fn2.getOrder()) {
            int compareMark = getNewCollatorInstance().compare(getMark(), fn2.getMark());
            if (compareMark == 0) {
                if (title != null && fn2.title != null) {
                    return getNewCollatorInstance().compare(getTitle(), fn2.getTitle());
                } else if (title == null && fn2.title == null) {
                    return 0;
                }
                return title == null ? -1 : 1;
            }
            return compareMark;
        }
        return getOrder() - fn2.getOrder();
    }

}
