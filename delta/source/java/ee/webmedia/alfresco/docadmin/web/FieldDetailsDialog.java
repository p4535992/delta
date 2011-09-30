package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocTypeDetailsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.CLASSIFICATOR;
import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.CLASSIFICATOR_DEFAULT_VALUE;
import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.DEFAULT_DATE_SYSDATE;
import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.DEFAULT_SELECTED;
import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.DEFAULT_USER_LOGGED_IN;
import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.DEFAULT_VALUE;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.commitToMetadataContainer;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDuplicateFieldIds;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.isSavedInPreviousDocTypeVersionOrFieldDefinitions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter_ET_EN;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataContainer;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * Details dialog for creating/editing objects of type field or fieldDefinition
 * 
 * @author Ats Uiboupin
 */
public class FieldDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "FieldDetailsDialog";

    /** All properties of field type that are conditionally shown based on selected {@link FieldType} */
    private static final Set<QName> ALL_FIELD_TYPE_CUSTOM_PROPS = Collections.<QName> unmodifiableSet(new HashSet<QName>(
            Arrays.asList(DEFAULT_VALUE, CLASSIFICATOR, CLASSIFICATOR_DEFAULT_VALUE, DEFAULT_DATE_SYSDATE, DEFAULT_USER_LOGGED_IN, DEFAULT_SELECTED)));

    private transient UIPropertySheet propertySheet;

    // START: fields that should be reset
    private Field field;
    /**
     * Not initialized when editing fieldDefinition.<br>
     * Instance of {@link DocumentTypeVersion} (when adding/editing field to the latest {@link DocumentTypeVersion}).<br>
     * Instance of {@link FieldGroup} when adding/editing field in fieldGroup
     */
    private MetadataContainer fieldParent;

    // END: fields that should be reset

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            resetHidenProps();
            if (isFieldDefinition()) {
                field = getDocumentAdminService().saveOrUpdateField(field);
                MessageUtil.addInfoMessage(context, "save_success");
            } else {
                commitToMetadataContainer(field, fieldParent);
            }
        } else {
            isFinished = false;
            return null;
        }
        return outcome;
    }

    @Override
    public String getFinishButtonLabel() {
        if (isFieldDefinition()) {
            return super.getFinishButtonLabel();
        }
        return MessageUtil.getMessage("fieldOrFieldGroup_details_affirm_changes");
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isProperySheetEditable();
    }

    private boolean validate() {
        boolean valid = true;
        String fieldIdLocalName = field.getFieldId();
        if (DocumentDynamicModel.FORBIDDEN_FIELD_IDS.contains(fieldIdLocalName)) {
            valid = false;
            MessageUtil.addErrorMessage("field_details_error_fieldId_reserved", TextUtil.collectionToString(DocumentDynamicModel.FORBIDDEN_FIELD_IDS));
        }
        String defaultValue = field.getDefaultValue();
        if (StringUtils.isNotBlank(defaultValue)) {
            FieldType fieldTypeEnum = field.getFieldTypeEnum();
            try {
                if (FieldType.DOUBLE.equals(fieldTypeEnum)) {
                    field.setDefaultValue(Double.valueOf(DoubleCurrencyConverter_ET_EN.prepareDoubleString(defaultValue)).toString());
                } else if (FieldType.LONG.equals(fieldTypeEnum)) {
                    field.setDefaultValue(Long.valueOf(defaultValue).toString());
                }
            } catch (NumberFormatException e) {
                valid = false;
                MessageUtil.addErrorMessage("field_details_error_defaultValue_" + fieldTypeEnum.name());
            }
        }
        if (isFieldDefinition()) {
            FieldDefinition fd = (FieldDefinition) field;
            if (!fd.isParameterInDocSearch() && fd.getParameterOrderInDocSearch() != null) {
                valid = false;
                MessageUtil.addErrorMessage("field_details_error_parameterInDocSearch");
            }
            if (!fd.isParameterInVolSearch() && fd.getParameterOrderInVolSearch() != null) {
                valid = false;
                MessageUtil.addErrorMessage("field_details_error_parameterInVolSearch");
            }
        } else {
            if (!field.isCopyFromPreviousDocTypeVersion() && !field.isCopyOfFieldDefinition() && getDocumentAdminService().isFieldDefinitionExisting(fieldIdLocalName)) {
                MessageUtil.addErrorMessage("field_details_error_docField_sameIdFieldDef");
                valid = false;
            } else {
                // check that there is no field with same id added to ancestor DocumentTypeVersion
                Set<String> duplicateFieldIds = getDuplicateFieldIds(Arrays.asList(field), fieldParent);
                if (!duplicateFieldIds.isEmpty()) {
                    if (duplicateFieldIds.size() > 1 || !duplicateFieldIds.contains(fieldIdLocalName)) { // shouldn't happen
                        throw new IllegalStateException("Expected at most one element (fieldId of editable field). duplicateFieldIds=" + duplicateFieldIds);
                    }
                    valid = false;
                    MessageUtil.addErrorMessage("field_details_error_docField_sameIdFieldInDocType", fieldIdLocalName);
                }
            }
        }
        return valid;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    private void resetFields() {
        field = null;
        fieldParent = null;
        clearPropertySheet();
    }

    // START: protected methods for FieldsListBean
    void editField(Field f, MetadataContainer parentOfField) {
        Field clone;
        if (f instanceof FieldDefinition) {
            // create new Field based on FieldDefinition
            clone = new Field((BaseObject) parentOfField);
            BeanHelper.getDocumentAdminService().copyFieldProps(((FieldDefinition) f), clone);
            clone.setRemovableFromSystematicDocType(true);
            clone.setRemovableFromSystematicFieldGroup(true);
            clone.setOnlyInGroup(false);
        } else {
            clone = f.clone();
        }
        editFieldInner(clone, parentOfField);
    }

    void addNewFieldToDocType(MetadataContainer parentOfField) {
        resetFields();
        editFieldInner(new Field((BaseObject) parentOfField), parentOfField);
    }

    // END: protected methods for FieldsListBean

    // START: jsf actions/accessors

    /** used by jsp */
    public void editFieldDefinition(ActionEvent event) {
        NodeRef fieldRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        editFieldInner(getDocumentAdminService().getField(fieldRef), null);
    }

    private void editFieldInner(Field fieldOrFieldDef, MetadataContainer parentOfField) {
        resetFields();
        field = fieldOrFieldDef;
        fieldParent = parentOfField;
        propertySheet.setMode(isProperySheetEditable() ? "edit" : "view");
    }

    /** used by jsp */
    public boolean isShowSystematicComment() {
        return StringUtils.isNotBlank(field.getSystematicComment());
    }

    /** used by property sheet */
    public boolean isFieldDefinition() {
        return field instanceof FieldDefinition;
    }

    /** used by property sheet */
    public boolean isFieldTypeReadOnly() {
        if (field instanceof FieldDefinition) {
            FieldDefinition fd = (FieldDefinition) field;
            return field.isSystematic() || (fd.getDocTypes() != null && !fd.getDocTypes().isEmpty()) || (fd.getVolTypes() != null && !fd.getVolTypes().isEmpty());
        }
        return isFieldIdReadOnly() || field.isCopyOfFieldDefinition();
    }

    /** used by property sheet */
    public boolean isClassificatorDefaultValueReadOnly() {
        if (field instanceof FieldDefinition || !field.isSystematic()) {
            return false;
        }
        for (FieldDefinition fd : getDocumentAdminService().getFieldDefinitions()) {
            if (fd.getFieldId().equals(field.getFieldId())) {
                if (StringUtils.isNotBlank(fd.getClassificatorDefaultValue())) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /** used by property sheet */
    public boolean isDefaultValueMandatory() {
        return FieldType.INFORMATION_TEXT.equals(field.getFieldTypeEnum());
    }

    /** used by property sheet */
    public boolean isShowAdditionalFieldsSeparator() {
        FieldType fieldType = field.getFieldTypeEnum();
        boolean fieldTypeHasAdditionalFields = fieldType == null ? false : !fieldType.getFieldsUsed(field.isComboboxNotRelatedToClassificator()).isEmpty();
        return fieldTypeHasAdditionalFields && BeanHelper.getDocTypeDetailsDialog().isShowingLatestVersion();
    }

    /** used by property sheet */
    public boolean isProperyHidden(PropertySheetItem propSheetItem) {
        return !isShowPropery(getPropQName(propSheetItem));
    }

    public boolean isProperySheetEditable() {
        return field instanceof FieldDefinition || getDocTypeDetailsDialog().isShowingLatestVersion();
    }

    /** used by property sheet */
    public boolean isFieldIdReadOnly() {
        return isSavedInPreviousDocTypeVersionOrFieldDefinitions(field);
    }

    /** used by property sheet */
    public void fieldTypeChanged(ValueChangeEvent e) {
        FieldType newFieldType = DefaultTypeConverter.INSTANCE.convert(FieldType.class, e.getNewValue());
        field.setFieldTypeEnum(newFieldType); // property is needs to be manually updated to be used in #updateDefaultValueVisibility(boolean)
        // might need to show/hide separators or update values of CLASSIFICATOR_DEFAULT_VALUE
        ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, getPropertySheet(), new Closure() {
            @Override
            public void execute(Object input) {
                clearPropertySheet();
            }
        });
        updatePropSheetComponents();
    }

    /** used by jsp propertySheetGrid */
    public Field getField() {
        return field;
    }

    /** used by classificator search component */
    public void setClassificator(String classificatorName) {
        field.setClassificator(classificatorName);
        updatePropSheetComponents();
    }

    /** used by jsp propertySheetGrid classificator Search component to show tooltip */
    public String getClassificatorDescription(Object searchComponentRowValue) {
        String classificatorName = (String) searchComponentRowValue;
        if (StringUtils.isBlank(classificatorName)) {
            return null;
        }
        Classificator classificatorByName = getClassificatorService().getClassificatorByName(classificatorName);
        return classificatorByName.getDescription();
    }

    /**
     * used by property sheet
     * 
     * @param context
     * @param selectComponent
     * @return classificator values to be shown
     */
    public List<SelectItem> getClassificatorSelectItems(FacesContext context, UIInput selectComponent) {
        if (StringUtils.isBlank(field.getClassificator())) {
            return Collections.<SelectItem> emptyList();
        }
        List<ClassificatorValue> classificatorValues //
        = getClassificatorService().getActiveClassificatorValues(getClassificatorService().getClassificatorByName(field.getClassificator()));
        Collections.sort(classificatorValues);
        List<SelectItem> results = new ArrayList<SelectItem>(classificatorValues.size() + 1);
        String existingValue = field.getClassificatorDefaultValue();
        for (ClassificatorValue classificator : classificatorValues) {
            SelectItem selectItem = new SelectItem(classificator.getValueName(), classificator.getValueName());
            if ((existingValue != null && StringUtils.equals(existingValue, classificator.getValueName())) // prefer existing value..
                    || (existingValue == null && classificator.isByDefault())) { // .. to default value
                selectComponent.setValue(selectItem.getValue());
            }
            results.add(selectItem);
        }
        // add default value
        ComponentUtil.addDefault(results, context);
        return results;
    }

    /** used by jsp propertySheetGrid */
    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    /** used by jsp propertySheetGrid */
    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public void clearPropertySheet() {
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
            propertySheet.setMode(null);
        }
    }

    // END: jsf actions/accessors

    private QName getPropQName(PropertySheetItem propSheetItem) {
        return QName.createQName(propSheetItem.getName(), getNamespaceService());
    }

    private boolean isShowPropery(QName propQName) {
        FieldType fieldType = field.getFieldTypeEnum();
        return fieldType == null ? false : fieldType.getFieldsUsed(field.isComboboxNotRelatedToClassificator()).contains(propQName);
    }

    private void updatePropSheetComponents() {
        if (getPropertySheet() != null) {
            List<UIComponent> children = ComponentUtil.getChildren(getPropertySheet());
            for (UIComponent uiProperty : children) {
                if (uiProperty instanceof PropertySheetItem) {
                    PropertySheetItem psItem = (PropertySheetItem) uiProperty;
                    boolean isClassificatorDefaultValueUiProp = uiProperty.getId().endsWith("_classificatorDefaultValue");
                    if (isClassificatorDefaultValueUiProp
                            || uiProperty.getId().endsWith("_classificator")
                            || uiProperty.getId().endsWith("_defaultValue")
                            || uiProperty.getId().endsWith("_defaultDateSysdate")
                            || uiProperty.getId().endsWith("_defaultUserLoggedIn")
                            || uiProperty.getId().endsWith("_defaultSelected")) {
                        ComponentUtil.setReadonlyAttributeRecursively(uiProperty, isProperyHidden(psItem));
                        if (isClassificatorDefaultValueUiProp) {
                            HtmlSelectOneMenu clDefaultValues = (HtmlSelectOneMenu) uiProperty.getChildren().get(1);
                            List<SelectItem> clValueItems = getClassificatorSelectItems(FacesContext.getCurrentInstance(), clDefaultValues);
                            ComponentUtil.setSelectItems(FacesContext.getCurrentInstance(), clDefaultValues, clValueItems);
                        }
                    }
                }
            }
        }
    }

    private void resetHidenProps() {
        FieldType newFieldType = field.getFieldTypeEnum();
        Set<QName> propsUnusedByFieldType = new HashSet<QName>(ALL_FIELD_TYPE_CUSTOM_PROPS);
        if (newFieldType != null) {
            propsUnusedByFieldType.removeAll(newFieldType.getFieldsUsed(false));
        }
        for (QName prop : propsUnusedByFieldType) {
            field.setProp(prop, null);
        }
    }

}
