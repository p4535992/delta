package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorAndTextGenerator.CustomAttributeConstants.LAYOUT_SIDE_BY_SIDE;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorAndTextGenerator.CustomAttributeNames.LAYOUT;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorAndTextGenerator.CustomAttributeNames.RENDERER_TYPE;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Generator, that generates a DropDown selection with values given by classificator with name defined using "classificatorName" attribute in the show-property
 * element.<br>
 * Binding on the client side should be done using JQuery when document is completely loaded <br>
 * (based on class name {@link ClassificatorSelectorAndTextGenerator#BINDING_MARKER_CLASS}).
 * show-property element can have following attributes:
 * <ul>
 * <li>{@link #STYLE_CLASS} - style class that will be added</li>
 * <li>{@link #RENDERER_TYPE} - renderer that will be used(to generate textArea(by default) or for example plain input(ComponentConstants#JAVAX_FACES_PANELGROUP)</li>
 * <li>{@link #LAYOUT} - {@value #LAYOUT_SIDE_BY_SIDE} will give side-by-side layout opposed to default row-by-row</li>
 * <ul>
 */
public class ClassificatorSelectorAndTextGenerator extends TextAreaGenerator {
    private static final String SELECT_SOURCE = "select_source"; // id attribute suffix of HTML select element
    private static final String SELECT_TARGET = "select_target"; // id attribute suffix of HTML input/textArea element

    private ClassificatorService classificatorService;
    private GeneralService generalService;
    private UIInput textTargetComponent;

    public static final String BINDING_MARKER_CLASS = "selectBoundWithText";

    interface CustomAttributeNames {
        String RENDERER_TYPE = "rendererType";
        String LAYOUT = "layoutStyle";
    }

    interface CustomAttributeConstants {
        String LAYOUT_SIDE_BY_SIDE = "sideBySide";
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        String layout = getLayout();
        UIComponent containerComponent;
        if (StringUtils.isNotBlank(layout) && layout.equals(LAYOUT_SIDE_BY_SIDE)) { // for example ComponentConstants.JAVAX_FACES_TEXT
            containerComponent = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
        } else { // default to javax.faces.Grid (using tableRows)
            containerComponent = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_GRID);
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> panelGroupChildren = containerComponent.getChildren();

        HtmlSelectOneMenu selectComponent = getSelectionComponent(context, SELECT_SOURCE);

        final UIInput textTargetComponent = (UIInput) super.generate(context, SELECT_TARGET);
        String rendererType = getRendererType();
        if (StringUtils.isNotBlank(rendererType)) { // for example ComponentConstants.JAVAX_FACES_TEXT
            textTargetComponent.setRendererType(rendererType);
        }

        // Must add addMandatoryIfValidator manually, because returnable component(containerComponent) is not instance of UIInput
        addMandatoryIfValidator(context, textTargetComponent);

        String styleClass = getStyleClass();
        if (StringUtils.isNotBlank(styleClass)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = textTargetComponent.getAttributes();
            attributes.put(STYLE_CLASS, styleClass);
        }
        this.textTargetComponent = textTargetComponent;
        panelGroupChildren.add(selectComponent);

        String existingValue = getGeneralService().getExistingRepoValue4ComponentGenerator();
        if (existingValue != null) {
            textTargetComponent.setValue(existingValue);
        }
        panelGroupChildren.add(textTargetComponent);

        return containerComponent;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        if (textTargetComponent != null) {
            textTargetComponent.setValueBinding("value", component.getValueBinding("value"));
        }
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
        ClassificatorValue defaultValue = null;
        for (ClassificatorValue classificator : classificators) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(classificator.getValueName());
            selectItem.setItemValue(classificator.getValueName()); // must not be null or emtpy (even if using only label)
            if (classificator.isByDefault()) {
                selectComponent.setValue(selectItem.getItemValue()); // make the selection
                defaultValue = classificator;
            }
            selectOptions.add(selectItem);
        }
        if (null == defaultValue) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel("");
            selectItem.setItemValue("");
            selectOptions.add(0, selectItem);
        }
        selectComponent.setStyleClass(BINDING_MARKER_CLASS);
        return selectComponent;
    }

    private String getClassificatorName() {
        return getCustomAttributes().get(ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME);
    }

    private String getRendererType() {
        return getCustomAttributes().get(RENDERER_TYPE);
    }

    private String getStyleClass() {
        return getCustomAttributes().get(STYLE_CLASS);
    }

    private String getLayout() {
        return getCustomAttributes().get(LAYOUT);
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    // START: getters / setters
    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }
    // END: getters / setters

}
