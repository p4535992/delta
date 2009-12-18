package ee.webmedia.alfresco.series.web;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class SeriesDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private static final String PARAM_FUNCTION_NODEREF = "functionNodeRef";
    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";
    private transient SeriesService seriesService;
    private Series series;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getSeriesService().saveOrUpdate(series);
        resetFields();
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        String seriesNodeRef = ActionUtil.getParam(event, PARAM_SERIES_NODEREF);
        series = getSeriesService().getSeriesByNoderef(seriesNodeRef);
    }

    public void addNewSeries(ActionEvent event) {
        NodeRef funcNodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_FUNCTION_NODEREF));
        // create new node for series
        series = getSeriesService().createSeries(funcNodeRef);
    }

    public Node getCurrentNode() {
        return series.getNode();
    }

    public String close() {
        if (!isClosed()) {
            getCurrentNode().getProperties().put(SeriesModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
            getSeriesService().saveOrUpdate(series);
            return getDefaultFinishOutcome();
        }
        return null;
    }

    public boolean isClosed() {
        final String currentStatus = (String) getCurrentNode().getProperties().get(SeriesModel.Props.STATUS.toString());
        final boolean closed = DocListUnitStatus.CLOSED.equals(currentStatus);
        return closed;
    }

    // END: jsf actions/accessors

    private void resetFields() {
        series = null;
    }

    // START: getters / setters
    protected SeriesService getSeriesService() {
        if (seriesService == null) {
            seriesService = (SeriesService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(SeriesService.BEAN_NAME);
        }
        return seriesService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    // END: getters / setters
}
