package ee.webmedia.alfresco.series.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Form backing bean for Series details
 * 
 * @author Ats Uiboupin
 */
public class SeriesDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private static final String PARAM_FUNCTION_NODEREF = "functionNodeRef";
    private static final String PARAM_SERIES_NODEREF = "seriesNodeRef";
    private transient SeriesService seriesService;
    private transient MenuService menuService;
    private Series series;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getSeriesService().saveOrUpdate(series);
        resetFields();
        getMenuService().menuUpdated(); // We need to refresh the left-hand sub-menu
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return series.getNode();
    }

    @Override
    public String getActionsConfigId() {
        if (!(series.getNode() instanceof TransientNode)) {
            return "browse_actions_series_details";
        }
        return null;
    }

    // START: jsf actions/accessors
    public void showDetails(ActionEvent event) {
        String seriesNodeRef = ActionUtil.getParam(event, PARAM_SERIES_NODEREF);
        series = getSeriesService().getSeriesByNodeRef(seriesNodeRef);
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
        if(series.getNode() instanceof TransientNode) {
            return null;
        }
        
        if (!isClosed()) {
            boolean wasClosed = getSeriesService().closeSeries(series);
            if(!wasClosed) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "series_validationMsg_closeNotPossible");
                return null;
            }
            return getDefaultFinishOutcome();
        }
        return null;
    }

    public boolean isClosed() {
        return seriesService.isClosed(getCurrentNode());
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
    
    protected MenuService getMenuService() {
        if (menuService == null) {
            menuService = (MenuService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(MenuService.BEAN_NAME);
        }
        return menuService;
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    // END: getters / setters
}
