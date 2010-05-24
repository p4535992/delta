package ee.webmedia.alfresco.series.web;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Form backing bean for Series list
 * 
 * @author Ats Uiboupin
 */
public class SeriesListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient SeriesService seriesService;
    private transient FunctionsService functionsService;
    private Function function;
    private List<Series> series;
    private boolean disableActions = false;
    
    public static final String BEAN_NAME = "SeriesListDialog"; 

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        resetFields();
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    @Override
    public void restored() {
        loadSeries();
    }

    @Override
    public Object getActionsContext() {
        return function.getNode();
    }

    @Override
    public String getActionsConfigId() {
        if(disableActions) {
            return "";
        }
        return super.getActionsConfigId();
    }

    public void showAll(ActionEvent event) {
        showAll(new NodeRef(ActionUtil.getParam(event, "functionNodeRef")));
    }
    
    public void showAll(NodeRef nodeRef) {
        function = getFunctionsService().getFunctionByNodeRef(nodeRef);
        loadSeries();
        disableActions = false;
    }

    private void loadSeries() {
        series = getSeriesService().getAllSeriesByFunction(function.getNodeRef());
    }

    public void showAllForStructUnit(NodeRef nodeRef, Integer structUnitId) {
        function = getFunctionsService().getFunctionByNodeRef(nodeRef);
        series = getSeriesService().getAllSeriesByFunctionForStructUnit(nodeRef, structUnitId);
        disableActions = true;
    }

    public List<Series> getSeries() {
        return series;
    }

    public Function getFunction() {
        return function;
    }

    public String getListTitle() {
        if(disableActions)
            return "Minu dokumentide loetelu";

        return getFunction().getMark() + " " + getFunction().getTitle();
    }

    // END: jsf actions/accessors

    private void resetFields() {
        function = null;
        series = null;
    }

    public boolean getDisableActions() {
        return disableActions;
    }

    public void setDisableActions(boolean disableActions) {
        this.disableActions = disableActions;
    }

    // START: getters / setters
    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    protected SeriesService getSeriesService() {
        if (seriesService == null) {
            seriesService = (SeriesService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(SeriesService.BEAN_NAME);
        }
        return seriesService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }
    // END: getters / setters

}
