package ee.webmedia.alfresco.functions.model;

import static ee.webmedia.alfresco.app.AppConstants.DEFAULT_COLLATOR;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = FunctionsModel.URI)
public class Function implements Serializable, Comparable<Function> {

    private static final long serialVersionUID = 1L;

    private String type;
    private String mark;
    private String title;
    private String description;
    private String status;
    private int order;
<<<<<<< HEAD
    private boolean documentActivitiesAreLimited;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    @AlfrescoModelProperty(isMappable = false)
    private Node node;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

<<<<<<< HEAD
    public boolean isDocumentActivitiesAreLimited() {
        return documentActivitiesAreLimited;
    }

    public void setDocumentActivitiesAreLimited(boolean documentActivitiesAreLimited) {
        this.documentActivitiesAreLimited = documentActivitiesAreLimited;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public NodeRef getNodeRef() {
        return node.getNodeRef();
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nType = " + type + "\n");
        sb.append("mark = " + mark + "\n");
        sb.append("title = " + title + "\n");
        sb.append("description = " + description + "\n");
        sb.append("status = " + status + "\n");
        sb.append("order = " + order + "\n");
        sb.append("nodeRef = " + getNodeRef() + "\n");
        return sb.toString();
    }

    @Override
    public int compareTo(Function fn2) {
        if (getOrder() == fn2.getOrder()) {
            int cmpMark;
            if ((cmpMark = DEFAULT_COLLATOR.compare(getMark(), fn2.getMark())) == 0) {
<<<<<<< HEAD
                if (title != null && fn2.title != null) {
                    return DEFAULT_COLLATOR.compare(getTitle(), fn2.getTitle());
                } else if (title == null && fn2.title == null) {
                    return 0;
                }
                return title == null ? -1 : 1;
=======
                return DEFAULT_COLLATOR.compare(getTitle(), fn2.getTitle());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            }
            return cmpMark;
        }
        return getOrder() - fn2.getOrder();
    }
<<<<<<< HEAD

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
