package ee.webmedia.alfresco.volume.model;

import static ee.webmedia.alfresco.app.AppConstants.DEFAULT_COLLATOR;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = VolumeModel.URI)
public class Volume implements Serializable, Comparable<Volume> {

    private static final long serialVersionUID = 1L;

    private String volumeType;
    private String volumeMark;
    private String title;
    private Date validFrom;
    private Date validTo;
    private String status;
    private Date dispositionDate;
    // FIXME: milleks see väli? On see üldse kuskil kasutusel?
    private Date seriesIdentifier;
    private boolean containsCases;
    private int containingDocsCount;
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

    public Date getSeriesIdentifier() {
        return seriesIdentifier;
    }

    public void setSeriesIdentifier(Date seriesIdentifier) {
        this.seriesIdentifier = seriesIdentifier;
    }

    public boolean isContainsCases() {
        return containsCases;
    }

    public void setContainsCases(boolean containsCases) {
        this.containsCases = containsCases;
    }

    public int getContainingDocsCount() {
        return Integer.valueOf(containingDocsCount);
    }

    public void setContainingDocsCount(int containingDocsCount) {
        this.containingDocsCount = containingDocsCount;
    }

    public NodeRef getSeriesNodeRef() {
        return seriesNodeRef;
    }

    public void setSeriesNodeRef(NodeRef seriesNodeRef) {
        this.seriesNodeRef = seriesNodeRef;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public boolean isDisposed() {
        return DocListUnitStatus.DESTROYED.getValueName().equals(status) ||
                (dispositionDate != null && dispositionDate.after(DateUtils.truncate(new Date(), Calendar.DATE)));
    }

    @Override
    public int compareTo(Volume other) {
        if (StringUtils.equalsIgnoreCase(getVolumeMark(), other.getVolumeMark())) {
            return AppConstants.DEFAULT_COLLATOR.compare(getTitle(), other.getTitle());
        }
        return DEFAULT_COLLATOR.compare(getVolumeMark(), other.getVolumeMark());
    }

    @Override
    public String toString() {
        return new StringBuilder("Volume:")//
                .append("\n\tvolumeMark = " + volumeMark)
                .append("\n\ttitle = " + title)
                .append("\n\tcontainsCases = " + containsCases)
                .append("\n\tvalidFrom = " + validFrom)
                .append("\n\tvalidTo = " + validTo)
                .append("\n\tstatus = " + status)
                .append("\n\tdispositionDate = " + dispositionDate).toString();
    }

}
