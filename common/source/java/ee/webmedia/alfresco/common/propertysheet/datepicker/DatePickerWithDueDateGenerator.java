package ee.webmedia.alfresco.common.propertysheet.datepicker;

import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator.ATTR_DESCRIPTION_AS_LABEL;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_SPECIFIERS;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_SPECIFIER_LABELS;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.ATTR_FILTER_NUMERIC;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.CLASSIFICATOR_NAME_SEPARATOR;
import static ee.webmedia.alfresco.utils.ComponentUtil.addAttributes;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.joda.time.LocalDate;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.LabelAndValueSelectorRenderer;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.web.DueDateDaysConverter;

public class DatePickerWithDueDateGenerator extends DatePickerGenerator {

    private final MultiClassificatorSelectorGenerator classificatorSelectorGenerator = new MultiClassificatorSelectorGenerator();

    @Override
    public UIComponent generate(FacesContext context, String id) {
        HtmlPanelGroup group = (HtmlPanelGroup) context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
        List<UIComponent> children = group.getChildren();
        children.add(super.generate(context, id));
        children.add(createDueDateDaysSelector(context, FacesHelper.makeLegalId(id), classificatorSelectorGenerator, true, null));

        return group;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef, UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, getDatePickerComponent(component));
    }

    @Override
    protected void setupConverter(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupConverter(context, propertySheet, property, propertyDef, getDatePickerComponent(component));
    }

    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, UIComponent component, boolean realTimeChecking,
            String idSuffix) {
        super.setupMandatoryValidation(context, propertySheet, item, getDatePickerComponent(component), realTimeChecking, idSuffix);
    }

    @Override
    protected void setupMandatoryPropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, PropertyDefinition propertyDef,
            UIComponent component) {
        Map<String, String> customAttributes = classificatorSelectorGenerator.getCustomAttributes();
        customAttributes.put(GeneralSelectorGenerator.ATTR_VALUE_CHANGE_LISTENER, "#{DocumentDialogHelperBean.setDocumentDueDate}");
        GeneralSelectorGenerator.setupValueChangeListener(context, getDropdownComponent(component), customAttributes);
        super.setupMandatoryPropertyIfNecessary(context, propertySheet, property, propertyDef, getDatePickerComponent(component));
    }

    @Override
    protected void setupConverter(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem association, AssociationDefinition associationDef, UIComponent component) {
        super.setupConverter(context, propertySheet, association, associationDef, getDatePickerComponent(component));
    }

    @Override
    protected void setupValidDateConstraint(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, UIComponent component) {
        super.setupValidDateConstraint(context, propertySheet, property, getDatePickerComponent(component));
    }

    @Override
    protected void createAndSetConverter(FacesContext context, String converterId, UIComponent component) {
        super.createAndSetConverter(context, converterId, getDatePickerComponent(component));
    }

    public static UIComponent createDueDateDaysSelector(FacesContext context, String id, boolean isEditable, ValueBinding vb) {
        return createDueDateDaysSelector(context, id, new MultiClassificatorSelectorGenerator(), isEditable, vb);
    }

    public static UIComponent createDueDateDaysSelector(FacesContext context, String id, MultiClassificatorSelectorGenerator classificatorSelectorGenerator, boolean isEditable,
            ValueBinding vb) {
        Map<String, String> selectorGeneratorAttributes = classificatorSelectorGenerator.getCustomAttributes();
        selectorGeneratorAttributes.put(ATTR_FILTER_NUMERIC, "true");
        selectorGeneratorAttributes.put(ATTR_DESCRIPTION_AS_LABEL, "true");
        selectorGeneratorAttributes.put(ATTR_CLASSIFICATOR_NAME, "dueDateCalendarDays" + CLASSIFICATOR_NAME_SEPARATOR + "dueDateWorkDays");
        selectorGeneratorAttributes.put(ATTR_CLASSIFICATOR_SPECIFIER_LABELS,
                MessageUtil.getMessage("calendarDays") + CLASSIFICATOR_NAME_SEPARATOR + MessageUtil.getMessage("workingDays"));
        selectorGeneratorAttributes.put(ATTR_CLASSIFICATOR_SPECIFIERS, "false" + CLASSIFICATOR_NAME_SEPARATOR + "true");

        Map<String, Object> classificatorSelectorComponentAttributes = new HashMap<String, Object>();
        classificatorSelectorComponentAttributes.put(CustomAttributeNames.STYLE_CLASS, "width120 task-due-date-days");
        classificatorSelectorComponentAttributes.put("displayMandatoryMark", true);
        classificatorSelectorComponentAttributes.put("styleClass", "task-due-date-days margin-left-4 width130");
        classificatorSelectorComponentAttributes.put(ClassificatorSelectorGenerator.ATTR_IGNORE_REPO_VALUE, Boolean.TRUE);

        UIComponent classificatorSelector = classificatorSelectorGenerator.generateSelectComponent(context, null, false);
        classificatorSelector.setId("task-dueDateDays-" + id);
        ComponentUtil.createAndSetConverter(context, DueDateDaysConverter.CONVERTER_ID, classificatorSelector);
        addAttributes(classificatorSelector, classificatorSelectorComponentAttributes);

        if (vb != null) {
            classificatorSelector.setValueBinding("value", vb);
        }
        classificatorSelectorGenerator.setupSelectComponent(context, null, null, null, classificatorSelector, false);
        classificatorSelector.setRendererType(LabelAndValueSelectorRenderer.LABEL_AND_VALUE_SELECTOR_RENDERER_TYPE);

        if (!isEditable) {
            putAttribute(classificatorSelector, "readonly", true);
        }
        return classificatorSelector;
    }

    public static LocalDate calculateDueDate(Boolean isWorkingDays, Integer dueDateDays) {
        LocalDate newDueDate = new LocalDate();
        if (Boolean.TRUE.equals(isWorkingDays)) {
            newDueDate = CalendarUtil.addWorkingDaysToDate(newDueDate, dueDateDays, BeanHelper.getClassificatorService());
        } else {
            newDueDate = newDueDate.plusDays(dueDateDays);
        }
        return newDueDate;
    }

    private UIComponent getDatePickerComponent(UIComponent component) {
        if (component instanceof HtmlPanelGroup && component.getChildCount() == 2) {
            return ComponentUtil.getChildren(component).get(0);
        }
        return component;
    }

    private UIComponent getDropdownComponent(UIComponent component) {
        if (component instanceof HtmlPanelGroup && component.getChildCount() == 2) {
            return ComponentUtil.getChildren(component).get(1);
        }
        return component;
    }

}
