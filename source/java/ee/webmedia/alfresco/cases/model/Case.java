package ee.webmedia.alfresco.cases.model;

import java.io.Serializable;
import java.text.Collator;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * Java class representing case:case defined in caseModel.xml
 */
@AlfrescoModelType(uri = CaseModel.URI)
public class Case implements Serializable, Comparable<Case> {

    private static final long serialVersionUID = 1L;
    @AlfrescoModelProperty(isMappable = false)
    private final static Collator collator = AppConstants.getNewCollatorInstance();

    private String title;
    private String status;
    private int containingDocsCount;
    // non-mappable fields
    @AlfrescoModelProperty(isMappable = false)
    private NodeRef volumeNodeRef;

    @AlfrescoModelProperty(isMappable = false)
    private Node node;

    // START: methods that operate VO properties
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getContainingDocsCount() {
        return containingDocsCount;
    }

    public void setContainingDocsCount(int containingDocsCount) {
        this.containingDocsCount = containingDocsCount;
    }

    public NodeRef getVolumeNodeRef() {
        return volumeNodeRef;
    }

    public void setVolumeNodeRef(NodeRef volumeNodeRef) {
        this.volumeNodeRef = volumeNodeRef;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    // END: methods that operate VO properties

    // START: methods that operate on node, not on VO properties
    public boolean isClosed() {
        return RepoUtil.isExistingPropertyValueEqualTo(node, CaseModel.Props.STATUS, DocListUnitStatus.CLOSED.getValueName());
    }

    // END: methods that operate on node, not on VO properties

    @Override
    public int compareTo(Case other) {
        return collator.compare(getTitle(), other.getTitle());
    }

    @Override
    public String toString() {
        return new StringBuilder("Case:").append("\n\ttitle = " + title).append("\n\tstatus = " + status).toString();
    }

}
