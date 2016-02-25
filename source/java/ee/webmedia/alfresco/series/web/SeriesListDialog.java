package ee.webmedia.alfresco.series.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;

import java.io.Serializable;
import java.util.ArrayList;
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
import ee.webmedia.alfresco.utils.MessageUtil;

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
    private boolean disableActions = false;
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
        if (disableActions) {
            return "";
        }
        return super.getActionsConfigId();
    }

    public void showAll(ActionEvent event) {
        showAll(new NodeRef(ActionUtil.getParam(event, "functionNodeRef")));
    }

    public void showAll(NodeRef nodeRef) {
        function = getFunctionsService().getUnmodifiableFunction(nodeRef, null);
        loadSeries();
        getLogService().addLogEntry(LogEntry.create(LogObject.FUNCTION, getUserService(), nodeRef, "applog_space_open", function.getMark(), function.getTitle()));
        disableActions = false;
    }

    private void loadSeries() {
        if (disableActions) {
        	if (activeStructUnit != null) {
        		series = getSeriesService().getAllSeriesByFunctionForStructUnit(getFunctionRef(), activeStructUnit);
        		// also add all series allowed for relatedUsersGroups
    	        List<UnmodifiableSeries> seriesForUsersGroups = getSeriesService().getAllSeriesByFunctionForRelatedUsersGroups(getFunctionRef(), AuthenticationUtil.getRunAsUser());
    	        	
    	        if (seriesForUsersGroups != null && !seriesForUsersGroups.isEmpty()) {
    	        	List<UnmodifiableSeries> missingSeries = new ArrayList<>();
    	        	missingSeries.addAll(seriesForUsersGroups);
    	        	for (UnmodifiableSeries serie: series) {
    	        		for (UnmodifiableSeries serieForUsersGroups: seriesForUsersGroups) {
    	            		if (serie.getNodeRef().equals(serieForUsersGroups.getNodeRef())) {
    	            			missingSeries.remove(serieForUsersGroups);
    	            			break;
    	            		}
    	            	}
    	        	}
    	        	series.addAll(missingSeries);
    	        }
        	} else {
        		series = getSeriesService().getAllSeriesByFunctionForRelatedUsersGroups(getFunctionRef(), AuthenticationUtil.getRunAsUser());
        	}
        } else {
            series = getSeriesService().getAllSeriesByFunction(getFunction().getNodeRef());
        }
    }

    public void showMyStructUnit(ActionEvent event) {
        Map<QName, Serializable> userProperties = userService.getUserProperties(AuthenticationUtil.getRunAsUser());
        String userStructUnit = (String) userProperties.get(ContentModel.PROP_ORGID);
        showAllForStructUnit(new NodeRef(ActionUtil.getParam(event, "functionNodeRef")), userStructUnit);
    }
    
    public void showAllForStructUnit(NodeRef functionRef, String structUnitId) {
        function = getFunctionsService().getUnmodifiableFunction(functionRef, null);
        
        if (structUnitId != null) {
        	series = getSeriesService().getAllSeriesByFunctionForStructUnit(functionRef, structUnitId);
    	
	        // also add all series allowed for relatedUsersGroups
	        List<UnmodifiableSeries> seriesForUsersGroups = getSeriesService().getAllSeriesByFunctionForRelatedUsersGroups(functionRef, AuthenticationUtil.getRunAsUser());
	        	
	        if (seriesForUsersGroups != null && !seriesForUsersGroups.isEmpty()) {
	        	List<UnmodifiableSeries> missingSeries = new ArrayList<>();
	        	missingSeries.addAll(seriesForUsersGroups);
	        	for (UnmodifiableSeries serie: series) {
	        		for (UnmodifiableSeries serieForUsersGroups: seriesForUsersGroups) {
	            		if (serie.getNodeRef().equals(serieForUsersGroups.getNodeRef())) {
	            			missingSeries.remove(serieForUsersGroups);
	            			break;
	            		}
	            	}
	        	}
	        	series.addAll(missingSeries);
	        }
        } else {
    		series = getSeriesService().getAllSeriesByFunctionForRelatedUsersGroups(functionRef, AuthenticationUtil.getRunAsUser());
    	}
        activeStructUnit = structUnitId;
        disableActions = true;
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

    public UnmodifiableFunction getFunction() {
        if (function == null) {
            NodeRef nodeRef = BeanHelper.getMenuBean().getLinkNodeRef();
            ChildAssociationRef caRef = BeanHelper.getNodeService().getPrimaryParent(nodeRef);
            NodeRef functionRef = caRef.getChildRef();
            function = getFunctionsService().getUnmodifiableFunction(functionRef, null);
        }
        return function;
    }

    public String getListTitle() {
        if (disableActions) {
            return MessageUtil.getMessage("series_my_documents_list");
        }
        return getFunction().getFunctionLabel();
    }

    // END: jsf actions/accessors

    private void resetFields() {
        function = null;
        series = null;
        disableActions = false;
        activeStructUnit = null;
    }

    @Override
    public void clean() {
        resetFields();
    }

    public boolean getDisableActions() {
        return disableActions;
    }

    public void setDisableActions(boolean disableActions) {
        this.disableActions = disableActions;
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
