package ee.webmedia.alfresco.common.propertysheet.datepicker;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet.ClientValidation;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generates text field for a date. UIInput has "date" class for jQuery date picker plug-in.
 * Also adds JavaScript validation.
 */
public class DatePickerGenerator extends BaseComponentGenerator {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DatePickerGenerator.class);
    /** predefined property names, that should be considered as beginDates despite how they are named */
    private List<String> addBeginDateClassByPropName = new ArrayList<String>();
    /** predefined property names, that should be considered as endDates despite how they are named */
    private List<String> addEndDateClassByPropName = new ArrayList<String>();
    public static final String DATE_FIELD_LABEL = "dateFieldLabel";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
        FacesHelper.setupComponentId(context, component, id);
        return component;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);

        if (!ComponentUtil.isComponentDisabledOrReadOnly(component)) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> attributes = component.getAttributes();
            String styleClass = getCustomAttributes().get(STYLE_CLASS);
            if (StringUtils.isBlank(styleClass)) {
                styleClass = "date";
            }
            String additionalStyleClass = getAdditionalStyleClass(propertyDef);
            if (additionalStyleClass != null) {
                styleClass += " " + additionalStyleClass;
            }
            attributes.put("styleClass", styleClass);
        }
    }

    private String getAdditionalStyleClass(PropertyDefinition propertyDef) {
        String propName = propertyDef.getName().getLocalName();
        if (propName.endsWith(DateGenerator.END_PREFIX)) {
            return "endDate";
        }
        boolean isBegin = isBeginDate(propName);
        boolean isEnd = isEndDate(propName);
        if (!isBegin && !isEnd) {
            return null;
        }
        if (isBegin && isEnd) {
            String msg = "Unable to decide if property " + propertyDef.getName() + " should be beginDate or endDate - validation might not work correctly";
            LOG.warn(msg);
            if (BeanHelper.getApplicationService().isTest()) {
                throw new IllegalStateException(msg + " (this exception is thrown only when project.test is set)");
            }
            return null;
        }
        return isBegin ? "beginDate" : "endDate";
    }

    private boolean isEndDate(String propName) {
        if (addBeginDateClassByPropName.contains(propName)) {
            return false;
        }
        return addEndDateClassByPropName.contains(propName) || containsCamelCaseWord(propName, "end") || containsCamelCaseWord(propName, "End");
    }

    private boolean isBeginDate(String propName) {
        if (addEndDateClassByPropName.contains(propName)) {
            return false;
        }
        return (addBeginDateClassByPropName.contains(propName)) || containsCamelCaseWord(propName, "begin") || containsCamelCaseWord(propName, "Begin");
    }

    public static boolean containsCamelCaseWord(String wholeText, String word) {
        int startPos = 0;
        int index = -1;
        while (0 <= (index = StringUtils.indexOf(wholeText, word, startPos))) {
            int constantEndIndex = index + word.length();
            boolean startOk = false;
            if (index > 0) {
                boolean wordStartsWithLowerCase = Character.isLowerCase(word.charAt(0));
                if (wordStartsWithLowerCase) {
                    if (!Character.isLowerCase(wholeText.charAt(index - 1))) {
                        startOk = true;
                    }
                } else {
                    // startOk = !Character.isUpperCase(wholeText.charAt(index - 1));// alternative: when "End" shouldn't be considered as a word in "XEnd"
                    startOk = true;
                }
            } else {
                startOk = true;
            }
            if (startOk) {
                if (wholeText.length() == constantEndIndex) {
                    return true; // end ok
                }
                if (!Character.isLowerCase(wholeText.charAt(constantEndIndex))) {
                    return true;
                }
            }
            startPos = constantEndIndex;
        }
        return false;
    }

    @Override
    protected void setupConverter(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, PropertyDefinition propertyDef,
            UIComponent component) {
        // Check for a valid date, when user can actually change it.
        if (propertySheet.inEditMode() && !ComponentUtil.isComponentDisabledOrReadOnly(component)) {
            setupValidDateConstraint(context, propertySheet, property, component);
        }
        ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, component);
    }

    protected void setupValidDateConstraint(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, UIComponent component) {
        if (isCreateOutputText(context)) {
            return;
        }
        addClientValidation(context, propertySheet, property, component, component.getClientId(context), "validateDate");
    }

    protected void addClientValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, UIComponent component, String elementClientId,
            String jsFuntctionName) {
        String value = "document.getElementById('" + elementClientId + "')";
        List<String> params = new ArrayList<String>(2);
        params.add(value);

        // add the validation failed messages
        String matchMsg = Application.getMessage(context, "validation_date_failed");
        addStringConstraintParam(params, MessageFormat.format(matchMsg, getDateFieldTitle(property)));

        // add the validation case to the property sheet
        propertySheet.addClientValidation(new ClientValidation(jsFuntctionName, params, true));

        // add event handler to kick off real time checks
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        String onchange = StringUtils.trimToEmpty((String) attributes.get("onchange"));
        onchange += "processButtonState();";
        attributes.put("onchange", onchange);
    }

    protected String getDateFieldTitle(PropertySheetItem property) {
        if (property != null) {
            return property.getResolvedDisplayLabel();
        }
        return getCustomAttributes().get(DATE_FIELD_LABEL);
    }

    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, UIComponent component,
            boolean realTimeChecking, String idSuffix) {
        super.setupMandatoryValidation(context, propertySheet, item, component, true, idSuffix);
    }

    @Override
    protected void setupMandatoryPropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupMandatoryPropertyIfNecessary(context, propertySheet, property, propertyDef, component);

        // Must do this after component has beed added to tree
        GeneralSelectorGenerator.setupValueChangeListener(context, component, getCustomAttributes());
    }

    public void setAddBeginDateClassByPropName(List<String> addBeginDateClassByPropName) {
        this.addBeginDateClassByPropName = addBeginDateClassByPropName;
    }

    public void setAddEndDateClassByPropName(List<String> addEndDateClassByPropName) {
        this.addEndDateClassByPropName = addEndDateClassByPropName;
    }

}