package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
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
    public static final String ATTR_CLASSIFICATOR_PROP = "classificatorProp";
    public static final String ATTR_DESCRIPTION_AS_LABEL = "descriptionAsLabel";

    private transient ClassificatorService classificatorService;
    private transient GeneralService generalService;

    @Override
    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        final UIComponent selectComponent = super.generateSelectComponent(context, id, multiValued);
        if (!log.isDebugEnabled()) {
            return selectComponent;
        }
        // for debugging purpose in development
        return ComponentUtil.setTooltip(selectComponent, MessageUtil.getMessage("classificator_source", getValueProviderName(null)));
    }

    @Override
    protected List<UISelectItem> initializeSelectionItems(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIInput component, Object boundValue, boolean multiValued) {

        // PropertySheet might be null if component has not been added to a PropertySheet yet
        Node node = propertySheet == null ? null : propertySheet.getNode();
        String valueProviderName = getValueProviderName(node);
        if (StringUtils.isBlank(valueProviderName)) {
            return null;
        }

        List<ClassificatorSelectorValueProvider> valueProviders = getSelectorValueProviders(valueProviderName, component, context);
        List<UISelectItem> results = new ArrayList<UISelectItem>(valueProviders.size() + 1);

        ClassificatorSelectorValueProvider defaultOrExistingValue = null;
        String existingValue = boundValue instanceof String ? (String) boundValue : null;
        if (!multiValued && existingValue == null) {
            existingValue = getGeneralService().getExistingRepoValue4ComponentGenerator();
        }
        boolean isSingleValued = isSingleValued(context, multiValued); //
        for (ClassificatorSelectorValueProvider classificator : valueProviders) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            if (isDescriptionAsLabel()) {
                selectItem.setItemLabel(classificator.getClassificatorDescription());
            } else {
                selectItem.setItemLabel(classificator.getSelectorValueName());
            }
            // Convert value so validation doesn't fail
            selectItem.setItemValue(RendererUtils.getConvertedUIOutputValue(context, component, classificator.getSelectorValueName())); // must not be null or empty
            if (isSingleValued
                    && ((existingValue != null && StringUtils.equals(existingValue, classificator.getSelectorValueName())) // prefer existing value..
                    || (existingValue == null && classificator.isByDefault()))) { // .. to default value
                component.setValue(selectItem.getItemValue()); // make the selection
                defaultOrExistingValue = classificator;
            }
            results.add(selectItem);
        }

        if (null == defaultOrExistingValue && isSingleValued) { // don't add default selection to multivalued component
            addDefault(context, results);
        }
        return results;
    }

    protected List<ClassificatorSelectorValueProvider> getSelectorValueProviders(String classificatorName, UIComponent component, FacesContext context) {
        List<ClassificatorValue> classificatorValues //
        = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(classificatorName));
        Collections.sort(classificatorValues);
        List<ClassificatorSelectorValueProvider> valueProviders = new ArrayList<ClassificatorSelectorValueProvider>(classificatorValues);
        return valueProviders;
    }

    /**
     * @param node property sheet node (null when creating tooltip)
     * @return classificator name that is used to generate select values (translated text pointing to field used as a source of classificator name)
     */
    protected String getValueProviderName(Node node) {
        String classificatorProviderProp = getCustomAttributes().get(ATTR_CLASSIFICATOR_PROP);
        if (StringUtils.isNotBlank(classificatorProviderProp)) {
            QName propQName = QName.createQName(classificatorProviderProp, getNamespaceService());
            if (node == null) {
                return MessageUtil.getMessage("classificator_source_classificatorNameContainer", classificatorProviderProp);
            }
            return (String) node.getProperties().get(propQName);
        }
        return getCustomAttributes().get(ATTR_CLASSIFICATOR_NAME);
    }

    protected boolean isDescriptionAsLabel() {
        return Boolean.valueOf(getCustomAttributes().get(ATTR_DESCRIPTION_AS_LABEL));
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
