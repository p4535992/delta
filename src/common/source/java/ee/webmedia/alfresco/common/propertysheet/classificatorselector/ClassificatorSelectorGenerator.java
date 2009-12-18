package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Generator, that generates a DropDown selection with values given by classificator with name defined using "classificatorName" attribute in the show-property
 * element.
 * 
 * @author Ats Uiboupin
 */
public class ClassificatorSelectorGenerator extends BaseComponentGenerator {

    public static final String ATTR_CLASSIFICATOR_NAME = "classificatorName";

    private static final String CUST_ATTR_STYLE_CLASS = "styleClass";
    private ClassificatorService classificatorService;
    private GeneralService generalService;

    public UISelectOne generate(FacesContext context, String id) {
        HtmlSelectOneMenu selectComponent = getSelectionComponent(context, "classificatorSelector");
        String styleClass = getStyleClass();
        if (StringUtils.isNotBlank(styleClass)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = selectComponent.getAttributes();
            attributes.put(CUST_ATTR_STYLE_CLASS, styleClass);
        }
        return selectComponent;
    }
    
    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, UIComponent component,
            boolean realTimeChecking, String idSuffix) {
        super.setupMandatoryValidation(context, propertySheet, item, component, true, idSuffix);
        // add event handler to kick off real time checks
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("onchange", "processButtonState();");
    }

    private HtmlSelectOneMenu getSelectionComponent(FacesContext context, String id) {
        HtmlSelectOneMenu selectComponent = (HtmlSelectOneMenu) context.getApplication().createComponent(HtmlSelectOneMenu.COMPONENT_TYPE);
        selectComponent.setId(id);

        @SuppressWarnings("unchecked")
        List<UIComponent> selectOptions = selectComponent.getChildren();

        String classificatorName = getClassificatorName();
        if (StringUtils.isBlank(classificatorName)) {
            return selectComponent;
        }
        List<ClassificatorValue> classificators //
        = classificatorService.getActiveClassificatorValues(classificatorService.getClassificatorByName(classificatorName));

        Collections.sort(classificators);
        ClassificatorValue defaultOrExistingValue = null;
        String existingValue = getGeneralService().getExistingRepoValue4ComponentGenerator();
        for (ClassificatorValue classificator : classificators) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(classificator.getValueName());
            selectItem.setItemValue(classificator.getValueName()); // must not be null or emtpy (even if using only label)
            if ((existingValue != null && StringUtils.equals(existingValue, classificator.getValueName())) // prefer existing value..
                    || (existingValue == null && classificator.isByDefault())) { // .. to default value
                selectComponent.setValue(selectItem.getItemValue()); // make the selection
                defaultOrExistingValue = classificator;
            }
            selectOptions.add(selectItem);
        }
        if (null == defaultOrExistingValue) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel("");
            selectItem.setItemValue("");
            selectOptions.add(0, selectItem);
        }
        // selectComponent.setStyleClass(BINDING_MARKER_CLASS);
        return selectComponent;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    private String getClassificatorName() {
        return getCustomAttributes().get(ATTR_CLASSIFICATOR_NAME);
    }

    private String getStyleClass() {
        return getCustomAttributes().get(STYLE_CLASS);
    }

    // START: getters / setters
    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }
    // END: getters / setters

}
