package ee.webmedia.alfresco.common.propertysheet.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.config.ConfigException;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.config.PropertySheetElementReader;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorAndTextGenerator;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.EnumSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.generator.ActionLinkGenerator;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor;
import ee.webmedia.alfresco.common.propertysheet.parameter.ParameterInputAttributeGenerator;
import ee.webmedia.alfresco.common.propertysheet.patternoutput.PatternOutput;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.UserSearchGenerator;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.common.propertysheet.validator.MandatoryIfValidator;

/**
 * Custom PropertySheetConfigElement that also holds custom attributes read from "show-property" element.
 */
public class WMPropertySheetConfigElement extends PropertySheetConfigElement {
    private static final long serialVersionUID = 1L;

    private boolean showUnvalued;

    /**
     * Default constructor
     */
    public WMPropertySheetConfigElement() {
        this(CONFIG_ELEMENT_ID);
    }

    /**
     * Constructor
     * 
     * @param name Name of the element this config element represents
     */
    public WMPropertySheetConfigElement(String name) {
        super(name);
    }

    /**
     * Inner class to represent a configured property
     */
    public interface ReadOnlyCopiableItemConfig {
        ItemConfig copyAsReadOnly();
    }

    public static class ItemConfigVO extends ItemConfig implements CustomAttributes, ReadOnlyCopiableItemConfig {
        private static final long serialVersionUID = 1L;

        // additional fields not present in parent class
        protected Map<String, String> customAttributes = new HashMap<String, String>();
        private ConfigItemType configItemType;

        protected ItemConfigVO() {
            // For deserialization
        }

        public ItemConfigVO(String name) {
            super(name);
            setCustomAttribute(PropertySheetElementReader.ATTR_NAME, name);
        }

        public enum ConfigItemType {
            ASSOC, CHILD_ASSOC, PROPERTY, SEPARATOR, SUB_PROPERTY_SHEET;
        }

        @Override
        public String toString() {
            return new StringBuilder(super.toString()) //
                    .append(" configItemType=").append(configItemType).append(")") //
                    .append(" customAttributes=").append(customAttributes).append(")") //
                    .toString();
        }

        // TODO DLSeadist - maybe remove if not needed
        public void setName(String name) {
            if (name == null || name.length() == 0) {
                throw new ConfigException("You must specify a name for a proprty sheet item");
            }
            this.name = name;
            setCustomAttribute(PropertySheetElementReader.ATTR_NAME, name);
        }

        public void setDisplayLabel(String displayLabel) {
            this.displayLabel = displayLabel;
            setCustomAttributeAndIgnoreNullValue(PropertySheetElementReader.ATTR_DISPLAY_LABEL, displayLabel);
        }

        public void setDisplayLabelId(String displayLabelId) {
            this.displayLabelId = displayLabelId;
            setCustomAttributeAndIgnoreNullValue(PropertySheetElementReader.ATTR_DISPLAY_LABEL_ID, displayLabelId);
        }

        public void setConverter(String converter) {
            this.converter = converter;
            setCustomAttributeAndIgnoreNullValue(PropertySheetElementReader.ATTR_CONVERTER, converter);
        }

        public void setComponentGenerator(String componentGenerator) {
            this.componentGenerator = componentGenerator;
            setCustomAttributeAndIgnoreNullValue(PropertySheetElementReader.ATTR_COMPONENT_GENERATOR, componentGenerator);
        }
        
        public void setOutputTextPropertyValue(boolean showText) {
            setCustomAttribute(BaseComponentGenerator.OUTPUT_TEXT, Boolean.toString(showText));
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
            setCustomAttribute(PropertySheetElementReader.ATTR_READ_ONLY, Boolean.toString(readOnly));
        }

        public void setRendered(String rendered) {
            setCustomAttribute(BaseComponentGenerator.RENDERED, rendered);
        }

        public void setParameterName(String parameterName) {
            setCustomAttribute(ParameterInputAttributeGenerator.PARAMETER_NAME, parameterName);
        }

        public boolean isShowInViewMode() {
            return showInViewMode;
        }

        public void setShowInViewMode(boolean showInViewMode) {
            this.showInViewMode = showInViewMode;
            setCustomAttribute(PropertySheetElementReader.ATTR_SHOW_IN_VIEW_MODE, Boolean.toString(showInViewMode));
        }

        public boolean isShowInEditMode() {
            return showInEditMode;
        }

        public void setShowInEditMode(boolean showInEditMode) {
            this.showInEditMode = showInEditMode;
            setCustomAttribute(PropertySheetElementReader.ATTR_SHOW_IN_EDIT_MODE, Boolean.toString(showInEditMode));
        }

        private void setRendered(boolean rendered) {
            this.rendered = rendered;
            // XXX boolean rendered and customAttributes.get(BaseComponentGenerator.RENDERED) have different purposes, TODO fix it
            // setCustomAttribute(BaseComponentGenerator.RENDERED, Boolean.toString(rendered));
        }

        public boolean isIgnoreIfMissing() {
            return ignoreIfMissing;
        }

        public void setIgnoreIfMissing(boolean ignoreIfMissing) {
            this.ignoreIfMissing = ignoreIfMissing;
            setCustomAttribute(PropertySheetElementReader.ATTR_IGNORE_IF_MISSING, Boolean.toString(ignoreIfMissing));
        }

        public void setConfigItemType(ConfigItemType configItemType) {
            this.configItemType = configItemType;
        }

        public ConfigItemType getConfigItemType() {
            return configItemType;
        }

        @Override
        public ItemConfigVO copyAsReadOnly() {
            ItemConfigVO copy = new ItemConfigVO(name);
            copy.setCustomAttributes(new HashMap<String, String>(customAttributes));
            copy.setDisplayLabel(displayLabel);
            copy.setDisplayLabelId(displayLabelId);
            copy.setConverter(converter);
            copy.setComponentGenerator(componentGenerator);
            copy.setReadOnly(readOnly);
            copy.setShowInViewMode(showInViewMode);
            copy.setShowInEditMode(showInEditMode);
            copy.setRendered(rendered);
            copy.setIgnoreIfMissing(ignoreIfMissing);
            copy.setConfigItemType(configItemType);
            return copy;
        }

        @Override
        public Map<String, String> getCustomAttributes() {
            return customAttributes;
        }

        public String toPropString(String optionsSeparator) {
            StringBuilder s = new StringBuilder();
            s.append(getName());
            s.append(optionsSeparator).append(getComponentGenerator());
            for (Entry<String, String> entry : customAttributes.entrySet()) {
                String attrKey = entry.getKey();
                if (StringUtils.isBlank(attrKey)
                        || PropertySheetElementReader.ATTR_NAME.equals(attrKey)
                        || PropertySheetElementReader.ATTR_COMPONENT_GENERATOR.equals(attrKey)) {
                    continue;
                }
                if (StringUtils.contains(attrKey, '=')) {
                    throw new RuntimeException("Attribute key contains key and value separator: " + getName() + " " + attrKey + "=" + entry.getValue());
                }
                if (StringUtils.contains(attrKey, optionsSeparator)) {
                    throw new RuntimeException("Attribute key contains optionsSeparator: " + getName() + " " + attrKey + "=" + entry.getValue());
                }
                if (StringUtils.contains(entry.getValue(), optionsSeparator)) {
                    throw new RuntimeException("Attribute value contains optionsSeparator: " + getName() + " " + attrKey + "=" + entry.getValue());
                }
                s.append(optionsSeparator).append(attrKey).append('=').append(entry.getValue());
            }
            return s.toString();
        }

        @Override
        public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
            Assert.isTrue(!propertySheetItemAttributes.containsKey(null), "Attribute with null key not allowed");
            for (Entry<String, String> entry : propertySheetItemAttributes.entrySet()) {
                Assert.notNull(entry.getValue(), "Attribute key '" + entry.getKey() + "' has null value");
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_NAME)) {
                name = propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_NAME);
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_DISPLAY_LABEL)) {
                displayLabel = propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_DISPLAY_LABEL);
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_DISPLAY_LABEL_ID)) {
                displayLabelId = propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_DISPLAY_LABEL_ID);
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_CONVERTER)) {
                converter = propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_CONVERTER);
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_COMPONENT_GENERATOR)) {
                componentGenerator = propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_COMPONENT_GENERATOR);
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_READ_ONLY)) {
                readOnly = Boolean.parseBoolean(propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_READ_ONLY));
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_SHOW_IN_VIEW_MODE)) {
                showInViewMode = Boolean.parseBoolean(propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_SHOW_IN_VIEW_MODE));
            }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_SHOW_IN_EDIT_MODE)) {
                showInEditMode = Boolean.parseBoolean(propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_SHOW_IN_EDIT_MODE));
            }
            // XXX boolean rendered and customAttributes.get(BaseComponentGenerator.RENDERED) have different purposes, TODO fix it
            // if (propertySheetItemAttributes.containsKey(BaseComponentGenerator.RENDERED)) {
            // rendered = Boolean.parseBoolean(propertySheetItemAttributes.get(BaseComponentGenerator.RENDERED));
            // }
            if (propertySheetItemAttributes.containsKey(PropertySheetElementReader.ATTR_IGNORE_IF_MISSING)) {
                ignoreIfMissing = Boolean.parseBoolean(propertySheetItemAttributes.get(PropertySheetElementReader.ATTR_IGNORE_IF_MISSING));
            }
            customAttributes = propertySheetItemAttributes;
        }

        protected void setCustomAttributeAndIgnoreNullValue(String key, String value) {
            if (value == null) {
                return;
            }
            setCustomAttribute(key, value);
        }

        protected void setCustomAttribute(String key, String value) {
            Assert.notNull(key, "Attribute with null key not allowed");
            Assert.notNull(value, "Attribute key '" + key + "' has null value");
            customAttributes.put(key, value);
        }

        public void setForcedMandatory(Boolean forcedMandatory) {
            setCustomAttribute(BaseComponentGenerator.CustomAttributeNames.ATTR_FORCED_MANDATORY, forcedMandatory == null ? null : forcedMandatory.toString());
        }

        public void setReadOnlyIf(String valueBinding) {
            setCustomAttribute(BaseComponentGenerator.READONLY_IF, valueBinding);
        }

        public void setStyleClass(String styleClass) {
            setCustomAttribute(BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS, styleClass);
        }

        public String getStyleClass() {
            return getCustomAttributes().get(BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS);
        }

        public void setDontRenderIfDisabled(Boolean dontRenderIfDisabled) {
            setCustomAttribute(WMUIProperty.DONT_RENDER_IF_DISABLED_ATTR, dontRenderIfDisabled == null ? null : dontRenderIfDisabled.toString());
        }

        public void setRenderCheckboxAfterLabel(Boolean b) {
            setCustomAttribute(WMUIProperty.RENDER_CHECKBOX_AFTER_LABEL, b == null ? null : b.toString());
        }

        public void setSelectionItems(String valueBinding) {
            setCustomAttribute(GeneralSelectorGenerator.ATTR_SELECTION_ITEMS, valueBinding);
        }

        public void setValueChangeListener(String valueBinding) {
            setCustomAttribute(GeneralSelectorGenerator.ATTR_VALUE_CHANGE_LISTENER, valueBinding);
        }

        public void setSuggesterValues(String valueBinding) {
            setCustomAttribute(SuggesterGenerator.ComponentAttributeNames.SUGGESTER_VALUES, valueBinding);
        }

        public void setClassificatorName(String classificatorName) {
            setCustomAttribute(ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME, classificatorName);
        }

        public void setEnumClass(String enumClass) {
            setCustomAttribute(EnumSelectorGenerator.ATTR_ENUM_CLASS, enumClass);
        }

        public void setAllowCommaAsDecimalSeparator(Boolean allowCommaAsDecimalSeparator) {
            setCustomAttribute(WMUIProperty.ALLOW_COMMA_AS_DECIMAL_SEPARATOR_ATTR, allowCommaAsDecimalSeparator == null ? null : allowCommaAsDecimalSeparator.toString());
        }

        public void setPickerCallback(String pickerCallback) {
            setCustomAttribute(Search.PICKER_CALLBACK_KEY, pickerCallback);
        }

        public void setDialogTitleId(String dialogTitleId) {
            setCustomAttribute(Search.DIALOG_TITLE_ID_KEY, dialogTitleId);
        }

        public void setEditable(Boolean editable) {
            setCustomAttribute("editable", editable == null ? null : editable.toString());
        }

        public void setAjaxParentLevel(int ajaxParentLevel) {
            setCustomAttribute(Search.AJAX_PARENT_LEVEL_KEY, Integer.toString(ajaxParentLevel));
        }

        public void setSetterCallback(String setterCallback) {
            setCustomAttribute(Search.SETTER_CALLBACK, setterCallback);
        }

        public void setMandatoryIf(String mandatoryIf) {
            setCustomAttribute(MandatoryIfValidator.ATTR_MANDATORY_IF, mandatoryIf);
        }

        public void setShowFilter(Boolean showFilter) {
            setCustomAttribute(Search.SHOW_FILTER_KEY, showFilter == null ? null : showFilter.toString());
        }

        public void setFilters(String filtersBinding) {
            setCustomAttribute(Search.FILTERS_KEY, filtersBinding);
        }

        public void setFiltersAllowGroupSelect(Boolean allowGroupSelect) {
            setCustomAttribute(Search.FILTERS_ALLOW_GROUP_SELECT_KEY, allowGroupSelect == null ? null : allowGroupSelect.toString());
        }

        public void setUsernameProp(String usernameProp) {
            setCustomAttribute(UserSearchGenerator.USERNAME_PROP_ATTR, usernameProp);
        }

        public void setAddLabelId(String addLabelId) {
            setCustomAttribute(MultiValueEditor.ADD_LABEL_ID, addLabelId);
        }

        public void setInitialRows(int initialRows) {
            setCustomAttribute(MultiValueEditor.INITIAL_ROWS, Integer.toString(initialRows));
        }

        public void setPropsGeneration(String propsGeneration) {
            setCustomAttribute(CombinedPropReader.AttributeNames.PROPS_GENERATION, propsGeneration);
        }

        public void setProps(String props) {
            setCustomAttribute(CombinedPropReader.AttributeNames.PROPS, props);
        }

        public void setTextId(String textId) {
            setCustomAttribute(CombinedPropReader.AttributeNames.TEXT_ID, textId);
        }

        public void setFilterIndex(int filterIndex) {
            setCustomAttribute(Search.FILTER_INDEX, Integer.toString(filterIndex));
        }

        public void setNotEditable(Boolean notEditable) {
            setCustomAttribute(ClassificatorSelectorAndTextGenerator.CustomAttributeNames.NOT_EDITABLE, notEditable == null ? null : notEditable.toString());
        }

        public void setSearchSuggestDisabled(Boolean searchSuggestDisabled) {
            setCustomAttribute(Search.SEARCH_SUGGEST_DISABLED, searchSuggestDisabled == null ? null : searchSuggestDisabled.toString());
        }

        public void setPreprocessCallback(String preprocessCallback) {
            setCustomAttribute(MultiValueEditor.PREPROCESS_CALLBACK, preprocessCallback);
        }

        public void setShow(String show) {
            setCustomAttribute(WMUIPropertySheet.SHOW, show);
        }

        public void setAction(String action) {
            setCustomAttribute(ActionLinkGenerator.ACTION_KEY, action);
        }

        public void setActionListener(String actionListener) {
            setCustomAttribute(ActionLinkGenerator.ACTION_LISTENER_KEY, actionListener);
        }

        public void setActionListenerParams(String actionListenerParams) {
            setCustomAttribute(ActionLinkGenerator.ACTION_LISTENER_PARAMS_KEY, actionListenerParams);
        }

        public void setShowHeaders(Boolean showHeaders) {
            setCustomAttribute(MultiValueEditor.SHOW_HEADERS, showHeaders == null ? null : showHeaders.toString());
        }

        public void setNoAddLinkLabel(Boolean noAddLinkLabel) {
            setCustomAttribute(MultiValueEditor.NO_ADD_LINK_LABEL, noAddLinkLabel == null ? null : noAddLinkLabel.toString());
        }

        public void setIsAutomaticallyAddRows(Boolean isAutomaticallyAddRows) {
            setCustomAttribute(MultiValueEditor.IS_AUTOMATICALLY_ADD_ROWS, isAutomaticallyAddRows == null ? null : isAutomaticallyAddRows.toString());
        }

        public void setFilter(String filter) {
            setCustomAttribute(DimensionSelectorGenerator.ATTR_FILTER, filter);
        }

        public void setDisplayInline() {
            setCustomAttribute(WMUIPropertySheet.DISPLAY, WMUIPropertySheet.INLINE);
        }

        public void setSubPropertySheetId(String subPropertySheetId) {
            setCustomAttribute(SubPropertySheetItem.ATTR_SUB_PROPERTY_SHEET_ID, subPropertySheetId);
        }

        public void setBelongsToSubPropertySheetId(String belongsToSubPropertySheetId) {
            setCustomAttribute(SubPropertySheetItem.ATTR_BELONGS_TO_SUB_PROPERTY_SHEET_ID, belongsToSubPropertySheetId);
        }

        public void setAssocBrand(String assocBrand) {
            setCustomAttribute(SubPropertySheetItem.ATTR_ASSOC_BRAND, assocBrand);
        }

        public void setAssocName(String assocName) {
            setCustomAttribute(SubPropertySheetItem.ATTR_ASSOC_NAME, assocName);
        }

        public void setActionsGroupId(String actionsGroupId) {
            setCustomAttribute(SubPropertySheetItem.ATTR_ACTIONS_GROUP_ID, actionsGroupId);
        }

        public void setTitleLabelId(String titleLabelId) {
            setCustomAttribute(SubPropertySheetItem.ATTR_TITLE_LABEL_ID, titleLabelId);
        }

        public void setSetterCallbackTakesNode(Boolean setterCallbackTakesNode) {
            setCustomAttribute(Search.SETTER_CALLBACK_TAKES_NODE, setterCallbackTakesNode == null ? null : setterCallbackTakesNode.toString());
        }

        public void setOptionsSeparator(String optionsSeparator) {
            setCustomAttribute(CombinedPropReader.AttributeNames.OPTIONS_SEPARATOR, optionsSeparator);
        }

        public void setHiddenPropNames(String hiddenPropNames) {
            setCustomAttribute(MultiValueEditor.HIDDEN_PROP_NAMES, hiddenPropNames);
        }

        public void setSetterCallbackReturnsMap(Boolean setterCallbackReturnsMap) {
            setCustomAttribute(MultiValueEditor.SETTER_CALLBACK_RETURNS_MAP, setterCallbackReturnsMap == null ? null : setterCallbackReturnsMap.toString());
        }

        public void setDescriptionAsLabel(Boolean descriptionAsLabel) {
            setCustomAttribute(ClassificatorSelectorGenerator.ATTR_DESCRIPTION_AS_LABEL, descriptionAsLabel == null ? null : descriptionAsLabel.toString());
        }

        public void setPattern(String rule) {
            setCustomAttribute(PatternOutput.PATTERN_ATTR, rule);
        }

    }

    public void addItem(ItemConfigVO itemConfig) {
        super.addItem(itemConfig);
    }

    public void setShowUnvalued(boolean showUnvalued) {
        this.showUnvalued = showUnvalued;
    }

    public boolean isShowUnvalued() {
        return showUnvalued;
    }

}
