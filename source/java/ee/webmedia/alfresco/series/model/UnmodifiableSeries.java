package ee.webmedia.alfresco.series.model;

import static ee.webmedia.alfresco.utils.RepoUtil.getProp;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.app.AppConstants;

public class UnmodifiableSeries implements Serializable, Comparable<UnmodifiableSeries> {

    private static final long serialVersionUID = 1L;
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private final int order;
    private final String seriesIdentifier;
    private final String title;
    private final String seriesLabel;
    private final String status;
    private final Date validFrom;
    private final Date validTo;
    private final List<String> volType;
    private final List<String> docTypes;
    private final List<String> structUnits;
    private final List<String> relatedUsersGroups;
    private final String type;
    private final int containingDocsCount;
    private final NodeRef seriesRef;
    private final NodeRef functionRef;
    private final boolean restrictedSeries;

    public UnmodifiableSeries(Node node, NodeRef functionRef) {
        Integer orderPropvalue = getProp(SeriesModel.Props.ORDER, node);
        order = orderPropvalue != null ? orderPropvalue : 0;
        seriesIdentifier = getProp(SeriesModel.Props.SERIES_IDENTIFIER, node);
        title = getProp(SeriesModel.Props.TITLE, node);
        status = getProp(SeriesModel.Props.STATUS, node);
        validFrom = getProp(SeriesModel.Props.VALID_FROM_DATE, node);
        validTo = getProp(SeriesModel.Props.VALID_TO_DATE, node);
        volType = getProp(SeriesModel.Props.VOL_TYPE, node);
        docTypes = getProp(SeriesModel.Props.DOC_TYPE, node);
        structUnits = getProp(SeriesModel.Props.STRUCT_UNIT, node);
        relatedUsersGroups = getProp(SeriesModel.Props.RELATED_USERS_GROUPS, node);
        type = getProp(SeriesModel.Props.TYPE, node);
        Integer containingDocsCountPropValue = getProp(SeriesModel.Props.CONTAINING_DOCS_COUNT, node);
        containingDocsCount = containingDocsCountPropValue != null ? containingDocsCountPropValue : 0;
        seriesLabel = seriesIdentifier + " " + title;
        this.functionRef = functionRef;
        seriesRef = node.getNodeRef();
        restrictedSeries = Boolean.FALSE.equals(node.getProperties().get(SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS));
    }

    public int getOrder() {
        return order;
    }

    public String getSeriesIdentifier() {
        return seriesIdentifier;
    }

    public String getSeriesLabel() {
        return seriesLabel;
    }

    public String getTitle() {
        return title;
    }

    public NodeRef getSeriesRef() {
        return seriesRef;
    }

    public NodeRef getNodeRef() {
        return seriesRef;
    }

    public NodeRef getFunctionRef() {
        return functionRef;
    }

    public String getStatus() {
        return status;
    }
    
    public Date getValidFrom() {
        return validFrom;
    }

    public List<String> getVolType() {
        return volType;
    }

    public List<String> getDocTypes() {
        return docTypes;
    }

    public List<String> getStructUnits() {
        return structUnits;
    }
    
    public List<String> getRelatedUsersGroups() {
        return relatedUsersGroups;
    }

    public String getType() {
        return type;
    }

    public int getContainingDocsCount() {
        return containingDocsCount;
    }

    @Override
    public int compareTo(UnmodifiableSeries other) {
        if (order == other.getOrder()) {
            return AppConstants.getNewCollatorInstance().compare(seriesIdentifier, other.getSeriesIdentifier());
        }
        return order - other.getOrder();
    }

    public String getSeriesLabelForModal() {
        StringBuilder sb = new StringBuilder(seriesLabel);
        sb.append(" (");
        if (getValidFrom() != null) {
            sb.append(dateFormat.format(getValidFrom()));
        }
        sb.append(" - ");
        if (getValidTo() != null) {
            sb.append(dateFormat.format(getValidTo()));
        }
        sb.append(")");
        return sb.toString();
    }

    public boolean isRestrictedSeries() {
        return restrictedSeries;
    }

    public Date getValidTo() {
        return validTo;
    }
}
