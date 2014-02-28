package ee.webmedia.alfresco.series.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = SeriesModel.URI)
public class Series implements Serializable, Comparable<Series> {

    private static final long serialVersionUID = 1L;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef functionNodeRef;
    // mappable fields
    private String type;
    private String seriesIdentifier;
    private String title;
    private String description;
    private String status;
    private List<String> docType;
    private int containingDocsCount;
    private boolean newNumberForEveryDoc;
    private boolean individualizingNumbers;
    private String docNumberPattern;
    private int register;

    @AlfrescoModelProperty(isMappable = false)
    private Node node;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        node.getProperties().put(SeriesModel.Props.TYPE.toString(), type);
        this.type = type;
    }

    public void setValidFromDate(Date validFrom) {
        node.getProperties().put(SeriesModel.Props.VALID_FROM_DATE.toString(), validFrom);
    }

    public String getSeriesIdentifier() {
        return seriesIdentifier;
    }

    public void setSeriesIdentifier(String seriesIdentifier) {
        node.getProperties().put(SeriesModel.Props.SERIES_IDENTIFIER.toString(), seriesIdentifier);
        this.seriesIdentifier = seriesIdentifier;
    }

    public void setInitialSeriesIdentifier(String initialSeriesIdentifier) {
        node.getProperties().put("{temp}seriesIdentifier", initialSeriesIdentifier);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        node.getProperties().put(SeriesModel.Props.TITLE.toString(), title);
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFunctionNodeRef(NodeRef function) {
        functionNodeRef = function;
    }

    public NodeRef getFunctionNodeRef() {
        return functionNodeRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrder() {
        Integer order = (Integer) node.getProperties().get(SeriesModel.Props.ORDER.toString());
        if (order == null) {
            return 0;
        }
        return order;
    }

    public void setOrder(int order) {
        node.getProperties().put(SeriesModel.Props.ORDER.toString(), order);
    }

    public Integer getRetentionPeriod() {
        return (Integer) node.getProperties().get(SeriesModel.Props.RETENTION_PERIOD);
    }

    public void setRetentionPeriod(Integer retentionPeriod) {
        node.getProperties().put(SeriesModel.Props.RETENTION_PERIOD.toString(), retentionPeriod);
    }

    public List<String> getDocType() {
        return docType;
    }

    public void setDocType(List<String> docType) {
        node.getProperties().put(SeriesModel.Props.DOC_TYPE.toString(), docType);
        this.docType = docType;
    }

    public int getContainingDocsCount() {
        return containingDocsCount;
    }

    public void setContainingDocsCount(int containingDocsCount) {
        this.containingDocsCount = containingDocsCount;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public boolean isNewNumberForEveryDoc() {
        return newNumberForEveryDoc;
    }

    public void setNewNumberForEveryDoc(boolean newNumberForEveryDoc) {
        this.newNumberForEveryDoc = newNumberForEveryDoc;
    }

    public boolean isIndividualizingNumbers() {
        return individualizingNumbers;
    }

    public void setIndividualizingNumbers(boolean individualizingNumbers) {
        this.individualizingNumbers = individualizingNumbers;
    }

    public String getDocNumberPattern() {
        return docNumberPattern;
    }

    public void setDocNumberPattern(String docNumberPattern) {
        this.docNumberPattern = docNumberPattern;
    }

    public int getRegister() {
        return register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nType = " + type + "\n");
        sb.append("seriesIdentifier = " + seriesIdentifier + "\n");
        sb.append("title = " + title + "\n");
        sb.append("description = " + description + "\n");
        sb.append("status = " + status + "\n");
        sb.append("order = " + getOrder() + "\n");
        return sb.toString();
    }

    @Override
    public int compareTo(Series other) {
        if (getOrder() == other.getOrder()) {
            return AppConstants.DEFAULT_COLLATOR.compare(getSeriesIdentifier(), other.getSeriesIdentifier());
        }
        return getOrder() - other.getOrder();
    }

}
