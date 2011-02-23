package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Generator, that generates a DropDown selection with values given by classificator with name defined using "classificatorName" attribute in the show-property
 * element.
 * 
 * @author Ats Uiboupin
 */
public class ClassificatorSelectorGenerator extends GeneralSelectorGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ClassificatorSelectorGenerator.class);

    public static final String ATTR_CLASSIFICATOR_NAME = "classificatorName";

    private transient ClassificatorService classificatorService;
    private transient GeneralService generalService;

    @Override
    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        final UIComponent selectComponent = super.generateSelectComponent(context, id, multiValued);
        if (!log.isDebugEnabled()) {
            return selectComponent;
        }
        // for debugging purpose in development
        return ComponentUtil.setTooltip(selectComponent, MessageUtil.getMessage("classificator_source", getClassificatorName()));
    }

    @Override
    protected List<UISelectItem> initializeSelectionItems(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIInput component, Object boundValue, boolean multiValued) {

        String classificatorName = getClassificatorName();
        if (StringUtils.isBlank(classificatorName)) {
            return null;
        }

        List<ClassificatorValue> classificators //
        = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(classificatorName));
        List<UISelectItem> results = new ArrayList<UISelectItem>(classificators.size() + 1);

        Collections.sort(classificators);
        ClassificatorValue defaultOrExistingValue = null;
        String existingValue = boundValue instanceof String ? (String) boundValue : null;
        if (!multiValued && existingValue == null) {
            existingValue = getGeneralService().getExistingRepoValue4ComponentGenerator();
        }
        boolean isSingleValued = isSingleValued(context, multiValued); // 
        for (ClassificatorValue classificator : classificators) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(classificator.getValueName());
            selectItem.setItemValue(classificator.getValueName()); // must not be null or emtpy (even if using only label)
            if (isSingleValued && ((existingValue != null && StringUtils.equals(existingValue, classificator.getValueName())) // prefer existing value..
                    || (existingValue == null && classificator.isByDefault()))) { // .. to default value
                component.setValue(selectItem.getItemValue()); // make the selection
                defaultOrExistingValue = classificator;
            }
            results.add(selectItem);
        }

        if (null == defaultOrExistingValue && isSingleValued) { // don't add default selection to multivalued component
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(MessageUtil.getMessage(context, "select_default_label"));
            selectItem.setItemValue("");
            results.add(0, selectItem);
        }
        return results;
    }

    protected String getClassificatorName() {
        return getCustomAttributes().get(ATTR_CLASSIFICATOR_NAME);
    }

    // START: getters / setters

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    protected ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }

    // END: getters / setters

}
