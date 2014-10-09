package ee.webmedia.alfresco.series.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Form backing bean for Series list
 */
public class SeriesListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient SeriesService seriesService;
    private transient FunctionsService functionsService;
    private transient UserService userService;
    private UnmodifiableFunction function;
    private List<UnmodifiableSeries> series;
    private String activeStructUnit;

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
    public String getActionsConfigId() {
        return super.getActionsConfigId();
    }

    public void showAll(ActionEvent event) {
        showAll(new NodeRef(ActionUtil.getParam(event, "functionNodeRef")));
    }

    public void showAll(NodeRef nodeRef) {
        function = getFunctionsService().getUnmodifiableFunction(nodeRef, null);
        loadSeries();
        getLogService().addLogEntry(LogEntry.create(LogObject.FUNCTION, getUserService(), nodeRef, "applog_space_open", function.getMark(), function.getTitle()));
    }

    private void loadSeries() {
        series = getSeriesService().getAllSeriesByFunction(getFunction().getNodeRef());
    }

    public void showMyStructUnit(ActionEvent event) {
        Map<QName, Serializable> userProperties = userService.getUserProperties(AuthenticationUtil.getRunAsUser());
        String userStructUnit = (String) userProperties.get(ContentModel.PROP_ORGID);
        showAllForStructUnit(new NodeRef(ActionUtil.getParam(event, "functionNodeRef")), userStructUnit);
    }

    public void showAllForStructUnit(NodeRef functionRef, String structUnitId) {
        function = getFunctionsService().getUnmodifiableFunction(functionRef, null);
        series = getSeriesService().getAllSeriesByFunctionForStructUnit(functionRef, structUnitId);
        activeStructUnit = structUnitId;
    }

    public List<UnmodifiableSeries> getSeries() {
        if (series == null) { // can happen when user navigates to series list via "my documents" menu item
            showAllForStructUnit(getFunctionRef(), getUserService().getCurrentUsersStructUnitId());
        }
        return series;
    }

    public NodeRef getFunctionRef() {
        return getFunction().getNodeRef();
    }

    private UnmodifiableFunction getFunction() {
        if (function == null) {
            NodeRef nodeRef = BeanHelper.getMenuBean().getLinkNodeRef();
            ChildAssociationRef caRef = BeanHelper.getNodeService().getPrimaryParent(nodeRef);
            NodeRef functionRef = caRef.getChildRef();
            function = getFunctionsService().getUnmodifiableFunction(functionRef, null);
        }
        return function;
    }

    public String getListTitle() {
        return getFunction().getFunctionLabel();
    }

    // END: jsf actions/accessors

    private void resetFields() {
        function = null;
        series = null;
        activeStructUnit = null;
    }

    @Override
    public void clean() {
        resetFields();
    }

    public String getActiveStructUnit() {
        return activeStructUnit;
    }

    public void setActiveStructUnit(String activeStructUnit) {
        this.activeStructUnit = activeStructUnit;
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

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    // END: getters / setters

}
