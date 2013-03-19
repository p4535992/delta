package ee.webmedia.alfresco.functions.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getMenuService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class FunctionsDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "FunctionsDetailsDialog";

    private static final String PARAM_FUNCTION_NODEREF = "nodeRef";
    private static final String ERROR_MESSAGE_SERIES_EXIST = "function_validation_series";

    private Function function;
    private boolean newFunction;

    private transient UIPropertySheet propertySheet;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        getFunctionsService().saveOrUpdate(function);
        resetData();
        getMenuService().menuUpdated(); // We need to refresh the left-hand sub-menu
        MessageUtil.addInfoMessage("save_success");
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

    // START: JSP event handlers
    /**
     * Called before displaying function value.
     */
    public void select(ActionEvent event) {
        function = getFunctionsService().getFunctionByNodeRef(ActionUtil.getParam(event, PARAM_FUNCTION_NODEREF));
    }

    public void addNewFunction(@SuppressWarnings("unused") ActionEvent event) {
        newFunction = true;
        function = getFunctionsService().createFunction();
    }

    // END: JSP event handlers

    /**
     * JSP event handler for the close button.
     * 
     * @param event
     */
    public String close() {
        if (function.getNode() instanceof TransientNode) {
            return null;
        }
        if (!isClosed()) {
            boolean wasClosed = getFunctionsService().closeFunction(function);
            if (!wasClosed) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERROR_MESSAGE_SERIES_EXIST);
                return null;
            }
            MessageUtil.addInfoMessage("function_close_success");
            propertySheet.getChildren().clear();
        }
        return null;
    }

    public String delete() {
        if (function.getNode() instanceof TransientNode) {
            return null;
        }
        if (isClosed()) {
            try {
                getFunctionsService().delete(function);
                MessageUtil.addInfoMessage("function_delete_success");
                return getDefaultCancelOutcome();
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(e);
            }
        }
        return null;
    }

    public String reopen() {
        if (isClosed()) {
            propertySheet.getChildren().clear();
            getFunctionsService().reopenFunction(function);
            MessageUtil.addInfoMessage("function_reopen_success");
        }
        return null;
    }

    public boolean isClosed() {
        final String currentStatus = (String) getCurrentNode().getProperties().get(FunctionsModel.Props.STATUS.toString());
        return DocListUnitStatus.CLOSED.equals(currentStatus);
    }

    public boolean isNew() {
        return newFunction;
    }

    public Node getCurrentNode() {
        return function.getNode();
    }

    // START: private methods
    private void resetData() {
        function = null;
        newFunction = false;
        propertySheet = null;
    }

    // END: private methods

    // START: getters / setters

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }
    // END: getters / setters
}
