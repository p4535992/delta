package ee.webmedia.alfresco.common.propertysheet.relateddropdown;

import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;

/**
 * Generator that generates selection (dropdown) components that can depend on other components of this type.<br>
 * 
 * @see {@link RelatedDropdown}
 * @author Ats Uiboupin
 */
public class RelatedDropdownGenerator extends GeneralSelectorGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(RelatedDropdownGenerator.class);
    private static final String ATTR_GROUP = "group";
    private static final String ATTR_ORDER = "order";
    private static final String ATTR_INITIAL_SELECTION_ITEMS = "initialSelectionItems";
    private static final String ATTR_INITIAL_CRITERIA_SOURCE_PROP = "initialSearchCriteriaSourceProp";
    public static final String ATTR_AFTER_SELECT = "afterSelect";

    @Override
    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        RelatedDropdown selectComponent = new RelatedDropdown();
        setId(context, selectComponent);
        setFillingInformation(selectComponent);
        return selectComponent;
    }

    @Override
    protected boolean isMultiValued(PropertyDefinition propertyDef) {
        return false;
    }

    @Override
    protected List<UIComponent> initializeSelectionItems(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIInput selectComponent, Object boundValue, boolean multiValued) {

        String initMethodBindingName = getInitialSelectionItems();
        ((RelatedDropdown) selectComponent).clearValues();
        final String initialSearchCriteriaSourceProp = StringUtils.trimToNull(getInitialSearchCriteriaSourceProp());
        if (initialSearchCriteriaSourceProp != null) { // if initialSearchCriteriaSourceProp is defined prefer it over ..
            initMethodBindingName = getSelectionItems(); // .. use regular selectionItems method binding with initialSearchCriteriaSourceProp as search criteria
        }
        if (StringUtils.isNotBlank(initMethodBindingName)) {
            // initialSelectionItems methodBinding or initialSearchCriteriaSourceProp property name is given to initialize values
            if (selectComponent instanceof HtmlSelectOneMenu) {
                ((HtmlSelectOneMenu) selectComponent).setDisabled(false);
            } else if (selectComponent instanceof HtmlSelectManyListbox) {
                ((HtmlSelectManyListbox) selectComponent).setDisabled(false);
            }
            MethodBinding mb = context.getApplication().createMethodBinding(initMethodBindingName,
                    new Class[] { FacesContext.class, HtmlSelectOneMenu.class, Object.class });
            Object initialSearchCriteria = null;
            if (item instanceof CustomAttributes && initialSearchCriteriaSourceProp != null) {
                final Map<String, Object> properties = propertySheet.getNode().getProperties();
                initialSearchCriteria = properties.get(initialSearchCriteriaSourceProp);
            }
            if (log.isDebugEnabled()) {
                String msg = "Setting values for dropdown from '" + initMethodBindingName + "', giving ";
                if (initialSearchCriteriaSourceProp != null) {
                    msg += "value of '" + initialSearchCriteriaSourceProp + "'=";
                }
                msg += "'" + initialSearchCriteria + "' as a search cirteria";
                log.debug(msg);
            }
            mb.invoke(context, new Object[] { context, selectComponent, initialSearchCriteria });
            if (boundValue != null) { // if boundValue is not present use default selection that methodBinding might have assigned, otherwise...
                selectComponent.setSubmittedValue(boundValue);// ..set selected value based on method binding
            }
        }
        return null;
    }

    private void setFillingInformation(RelatedDropdown selectComponent) {
        final String groupName = getGroupName();
        final Integer order = getOrder();
        final String selectionItems = getSelectionItems();
        if (StringUtils.isBlank(groupName) || order == null || selectionItems == null) {
            throw new RuntimeException(getName() + ": '" + ATTR_GROUP + "', '" + ATTR_ORDER + "' and '" + ATTR_SELECTION_ITEMS
                    + "' must be defined for '" + this.getClass().getCanonicalName() + "' component generator");
        }
        selectComponent.setGroup(groupName);
        selectComponent.setOrder(order);
        selectComponent.setSelectionItems(selectionItems);
        selectComponent.setAfterSelect(getAfterSelect());
    }

    private void setId(FacesContext context, RelatedDropdown selectComponent) {
        String name = getName();
        final int localStart = name.lastIndexOf("}") + 1;
        if (localStart != -1) {
            name = name.substring(localStart);
        }
        FacesHelper.setupComponentId(context, selectComponent, getIdPrefix() + name + getIdSuffix());
    }

    @Override
    protected String getIdPrefix() {
        return "relatedDropdown_";
    }

    private String getName() {
        return getCustomAttributes().get("name");
    }

    private String getInitialSelectionItems() {
        return getCustomAttributes().get(ATTR_INITIAL_SELECTION_ITEMS);
    }

    private String getInitialSearchCriteriaSourceProp() {
        return getCustomAttributes().get(ATTR_INITIAL_CRITERIA_SOURCE_PROP);
    }

    private String getAfterSelect() {
        return getCustomAttributes().get(ATTR_AFTER_SELECT);
    }

    private String getGroupName() {
        return getCustomAttributes().get(ATTR_GROUP);
    }

    private Integer getOrder() {
        return DefaultTypeConverter.INSTANCE.convert(Integer.class, getCustomAttributes().get(ATTR_ORDER));
    }

}
