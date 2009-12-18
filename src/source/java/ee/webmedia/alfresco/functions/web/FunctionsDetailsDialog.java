package ee.webmedia.alfresco.functions.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.Utils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class FunctionsDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private static final String PARAM_FUNCTION_NODEREF = "nodeRef";
    private static final String ERROR_MESSAGE_SERIES_EXIST = "function_validation_series";
    
    private transient FunctionsService functionsService;
    private transient SeriesService seriesService;
    private Function function;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getFunctionsService().saveOrUpdate(function);
        resetData();
        return outcome;
    }

    @Override
    public String cancel() {
        resetData();
        return super.cancel();
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    /**
     * JSP event handler.
     * Called before displaying function value.
     * 
     * @param event
     */
    public void select(ActionEvent event) {
        function = getFunctionsService().getFunctionByNodeRef(ActionUtil.getParam(event, PARAM_FUNCTION_NODEREF));
    }

    /**
     * JSP event handler.
     * 
     * @param event
     */
    public void addNewFunction(ActionEvent event) {
        function = getFunctionsService().createFunction();
    }

    /**
     * JSP event handler for the close button.
     * 
     * @param event
     */
    public String close() {
        List<Series> allSeries = getSeriesService().getAllSeriesByFunction(function.getNodeRef());
        boolean noOpenSeries = true;
        for (Series series : allSeries) {
            if (!DocListUnitStatus.CLOSED.equals(series.getStatus())) {
                Utils.addErrorMessage(Application.getMessage(FacesContext.getCurrentInstance(), ERROR_MESSAGE_SERIES_EXIST));
                noOpenSeries = false;
                break;
            }
        }
        if (!isClosed() && noOpenSeries) {
            getCurrentNode().getProperties().put(FunctionsModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
            getFunctionsService().saveOrUpdate(function);
            return getDefaultFinishOutcome();
        }
        return null;
    }
    
    public boolean isClosed() {
        final String currentStatus = (String) getCurrentNode().getProperties().get(FunctionsModel.Props.STATUS.toString());
        final boolean closed = DocListUnitStatus.CLOSED.equals(currentStatus);
        return closed;
    }
    
    public Node getCurrentNode() {
        return function.getNode();
    }

    // START: private methods
    private void resetData() {
        function = null;
    }
    // END: private methods

    // START: getters / setters

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }
    
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
