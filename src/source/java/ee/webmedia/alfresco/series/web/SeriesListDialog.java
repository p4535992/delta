package ee.webmedia.alfresco.series.web;

import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdown;
import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdownGenerator;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class SeriesListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient SeriesService seriesService;
    private transient FunctionsService functionsService;
    private Function function;

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

    public void showAll(ActionEvent event) {
        function = getFunctionsService().getFunctionByNodeRef(ActionUtil.getParam(event, "functionNodeRef"));
    }

    public List<Series> getSeries() {
        final List<Series> series = getSeriesService().getAllSeriesByFunction(function.getNodeRef());
        Collections.sort(series);
        return series;
    }

    public Function getFunction() {
        return function;
    }

    // END: jsf actions/accessors

    private void resetFields() {
        function = null;

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
