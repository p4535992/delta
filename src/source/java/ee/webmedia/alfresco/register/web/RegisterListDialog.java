package ee.webmedia.alfresco.register.web;

import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.service.RegisterService;

public class RegisterListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient RegisterService registerService;
    private List<Register> registers;

    @Override
    public void restored() {
        // Update list data
        registers = getRegisterService().getRegisters();
    }

    /**
     * Used in JSP pages.
     */
    public List<Register> getRegisters() {
        return registers;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        registers = getRegisterService().getRegisters();
    }

    @Override
    public String cancel() {
        registers = null;
        return super.cancel();
    }

    /*
     * This is a read-only dialog, so we have nothing to do here (Save/OK button isn't displayed)
     */
    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    /**
     * Query callback method executed by the component generated by GeneralSelectorGenerator.
     * This method is part of the contract to the GeneralSelectorGenerator, it is up to the backing bean
     * to execute whatever query is appropriate and populate <code>selectComponent</code> with selection items.<br>
     * 
     * @param context - FacesContext for creating selection items
     * @param selectComponent - selectComponent that will be rendered(use <code>selectComponent.getChildren()</code> to add selection items)
     * @return A collection of UISelectItem objects containing the selection items to show on form.
     */
    public void findActiveRegisters(FacesContext context, HtmlSelectOneMenu selectComponent) {
        List<Register> registers = getRegisterService().getRegisters();
        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();
        for (Register register : registers) {
            if (register.isActive()) {
                UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
                selectItem.setItemLabel(register.getName());
                selectItem.setItemValue(Integer.valueOf(register.getId()));
                selectOptions.add(selectItem);
            }
        }
        // empty default selection
        UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
        selectItem.setItemLabel("");
        selectItem.setItemValue("");
        selectOptions.add(0, selectItem);
    }

    // START: getters / setters
    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

    protected RegisterService getRegisterService() {
        if (registerService == null) {
            registerService = (RegisterService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(RegisterService.BEAN_NAME);
        }
        return registerService;
    }
    // END: getters / setters

}
