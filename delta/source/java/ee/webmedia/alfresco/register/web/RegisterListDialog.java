package ee.webmedia.alfresco.register.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.utils.WebUtil;

public class RegisterListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient RegisterService registerService;
    private List<Register> registers;

    @Override
    public Object getActionsContext() {
        return null;
    }

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
    public List<SelectItem> findActiveRegisters(FacesContext context, UIInput selectComponent) {
        List<Register> allRegisters = getRegisterService().getRegisters();
        List<SelectItem> selectItems = new ArrayList<SelectItem>(allRegisters.size());
        // empty default selection
        selectItems.add(new SelectItem("", ""));
        for (Register register : allRegisters) {
            if (register.isActive()) {
                selectItems.add(new SelectItem(Integer.valueOf(register.getId()), register.getName()));
            }
        }
        WebUtil.sort(selectItems);
        return selectItems;
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