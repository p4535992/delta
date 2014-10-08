<<<<<<< HEAD
package ee.webmedia.alfresco.register.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getRegisterService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class RegisterDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private Node register;
    private transient UIPropertySheet propertySheet;

    /**
     * Action listener for JSP pages
     * 
     * @param event ActionLink
     */
    public void setupRegister(ActionEvent event) {
        setupRegister(Integer.parseInt(ActionUtil.getParam(event, "id")));
    }

    /**
     * Resets current registers counter
     */
    public void resetCounter() {
        getRegisterService().resetCounter(getRegister());
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
            propertySheet.setMode(null);
            propertySheet.setNode(null);
        }
    }

    /**
     * Action listener for JSP pages
     * 
     * @param id id of the register that should be loaded
     */
    public void setupRegister(int id) {
        setRegister(getRegisterService().getRegisterNode(id));
    }

    public void setupNewRegister(@SuppressWarnings("unused") ActionEvent event) {
        setRegister(getRegisterService().createRegister());
    }

    @Override
    public String cancel() {
        register = null;
        propertySheet = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // set updated values
        getRegisterService().updateProperties(register);
        register = null;
        propertySheet = null;
        MessageUtil.addInfoMessage("save_success");
        return outcome;
    }

    public Node getRegister() {
        return register;
    }

    public void setRegister(Node register) {
        this.register = register;
    }

    public boolean isCounterReadOnly() {
        int counter = ((Integer) register.getProperties().get(RegisterModel.Prop.COUNTER)).intValue();
        return !(counter == 0 || getRegisterService().isValueEditable());
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }
}
=======
package ee.webmedia.alfresco.register.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getRegisterService;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class RegisterDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private Node register;
    private transient UIPropertySheet propertySheet;

    /**
     * Action listener for JSP pages
     * 
     * @param event ActionLink
     */
    public void setupRegister(ActionEvent event) {
        setupRegister(Integer.parseInt(ActionUtil.getParam(event, "id")));
    }

    /**
     * Resets current registers counter
     */
    public void resetCounter() {
        getRegisterService().resetCounter(getRegister());
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
            propertySheet.setMode(null);
            propertySheet.setNode(null);
        }
    }

    /**
     * Action listener for JSP pages
     * 
     * @param id id of the register that should be loaded
     */
    public void setupRegister(int id) {
        setRegister(getRegisterService().getRegisterNode(id));
    }

    public void setupNewRegister(@SuppressWarnings("unused") ActionEvent event) {
        setRegister(getRegisterService().createRegister());
    }

    @Override
    public String cancel() {
        register = null;
        propertySheet = null;
        return super.cancel();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // set updated values
        getRegisterService().updateProperties(register);
        register = null;
        propertySheet = null;
        MessageUtil.addInfoMessage("save_success");
        return outcome;
    }

    public Node getRegister() {
        return register;
    }

    public void setRegister(Node register) {
        this.register = register;
    }

    public boolean isCounterReadOnly() {
        int counter = ((Integer) register.getProperties().get(RegisterModel.Prop.COUNTER)).intValue();
        return !(counter == 0 || getRegisterService().isValueEditable());
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }
}
>>>>>>> develop-5.1
