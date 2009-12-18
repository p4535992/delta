package ee.webmedia.alfresco.volume.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = VolumeModel.URI)
public class Volume implements Serializable, Comparable<Volume> {

    private static final long serialVersionUID = 1L;

    private String volumeMark;
    private String title;
    private Date validFrom;
    private Date validTo;
    private String status;
    private Date dispositionDate;
    // non-mappable fields
    @AlfrescoModelProperty(isMappable = false)
    private NodeRef seriesNodeRef;

    @AlfrescoModelProperty(isMappable = false)
    private Node node;

    public void setVolumeMark(String volumeMark) {
        this.volumeMark = volumeMark;
    }

    public String getVolumeMark() {
        return volumeMark;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDispositionDate() {
        return dispositionDate;
    }

    public void setDispositionDate(Date dispositionDate) {
        this.dispositionDate = dispositionDate;
    }

    public NodeRef getSeriesNodeRef() {
        return seriesNodeRef;
    }

    public void setSeriesNodeRef(NodeRef seriesNodeRef) {
        this.seriesNodeRef = seriesNodeRef;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public int compareTo(Volume other) {
        if (getVolumeMark() == other.getVolumeMark()) {
            int cmpMark;
            if ((cmpMark = getTitle().compareTo(other.getTitle())) == 0) {
                return 0;
            }
            return cmpMark;
        }
        return getVolumeMark().compareTo(other.getVolumeMark());
    }

    @Override
    public String toString() {
        return new StringBuilder("Volume:")//
                .append("\n\tvolumeMark = " + volumeMark)
                .append("\n\ttitle = " + title)
                .append("\n\tvalidFrom = " + validFrom)
                .append("\n\tvalidTo = " + validTo)
                .append("\n\tstatus = " + status)
                .append("\n\tdispositionDate = " + dispositionDate).toString();
    }

}
