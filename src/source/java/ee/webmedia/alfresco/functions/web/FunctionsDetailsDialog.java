package ee.webmedia.alfresco.functions.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class FunctionsDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private static final String PARAM_FUNCTION_NODEREF = "nodeRef";
    private static final String ERROR_MESSAGE_SERIES_EXIST = "function_validation_series";
    
    private transient FunctionsService functionsService;
    
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
        if(function.getNode() instanceof TransientNode) {
            return null;
        }
        if (!isClosed()) {
            boolean wasClosed = getFunctionsService().closeFunction(function);
            if(!wasClosed) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERROR_MESSAGE_SERIES_EXIST);
                return null;
		    }
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
    
	// END: getters / setters
}
