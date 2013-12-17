package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getBaseService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDialogManager;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFieldDetailsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFieldGroupDetailsDialog;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getMetadataItemReorderHelper;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.reorderAndMarkBaseState;
import static ee.webmedia.alfresco.utils.MessageUtil.getMessage;
import static ee.webmedia.alfresco.utils.TextUtil.collectionToString;
import static ee.webmedia.alfresco.utils.WebUtil.navigateTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataContainer;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.docdynamic.web.DialogBlockBean;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Shows list of {@link Field}, {@link FieldGroup} and {@link SeparatorLine} objects bound to the latest {@link DocumentTypeVersion} of viewable {@link DocumentType}
 * 
 * @author Ats Uiboupin
 */
public class FieldsListBean implements DialogBlockBean<Void> {
    private static final long serialVersionUID = 1L;
    private MetadataContainer metadataContainer;

    void init(MetadataContainer parent) {
        metadataContainer = parent;
    }

    @Override
    public void resetOrInit(Void nill) {
        metadataContainer = null;
    }

    public List<? extends MetadataItem> getMetaFieldsList() {
        BaseObject metaContainerObject = (BaseObject) metadataContainer;
        if (metaContainerObject.getPropBoolean(BaseService.CHILDREN_NOT_LOADED)) {
            // manually lazy loading children
            getBaseService().loadChildren(metaContainerObject, null);
        }
        return getMetaFieldsList(metadataContainer.getMetadata(), true);
    }

    private List<? extends MetadataItem> getMetaFieldsList(ChildrenList<? extends MetadataItem> metadata, boolean isInitialState) {
        if (metadata == null) {
            return Collections.emptyList();
        }
        List<MetadataItem> modifiableMetadataList = new ArrayList<MetadataItem>(metadata);
        BaseObjectOrderModifier<MetadataItem> reorderHelper = getMetadataItemReorderHelper(DocumentAdminModel.Props.ORDER);
        if (isInitialState) {
            reorderHelper.markBaseState(modifiableMetadataList);
        }
        return reorderAndMarkBaseState(modifiableMetadataList, reorderHelper);
    }

    /** JSP */
    public void removeMetaField(ActionEvent event) {
        ChildrenList<? extends MetadataItem> metadata = metadataContainer.getMetadata();
        if (metadata == null) {
            throw new RuntimeException("unimplemented deleteMetaField from unsaved");
        }
        NodeRef metaFieldRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        MetadataItem removed = metadata.remove(metaFieldRef);
        BaseObject dynType = removed.getParent();
        while (!(dynType instanceof DynamicType)) {
            dynType = dynType.getParent();
        }
        MessageUtil.addWarningMessage("dynType_metadataList_remove_postponed_" + dynType.getNode().getType().getLocalName() + "_" + removed.getType());
    }

    /** JSP */
    public void editField(ActionEvent event) {
        NodeRef fieldRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        Field field = (Field) metadataContainer.getMetadata().getChildByNodeRef(fieldRef);
        editField(field);
    }

    private void editField(Field field) {
        navigateTo("dialog:fieldDetailsDialog");
        getFieldDetailsDialog().editField(field, metadataContainer);
    }

    public void editFieldGroup(ActionEvent event) {
        NodeRef fieldRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        FieldGroup fieldGroup = (FieldGroup) metadataContainer.getMetadata().getChildByNodeRef(fieldRef);
        editFieldGroup(fieldGroup);
    }

    private void editFieldGroup(FieldGroup fieldGroup) {
        navigateTo("dialog:fieldGroupDetailsDialog");
        getFieldGroupDetailsDialog().editFieldGroup(fieldGroup, (DocumentTypeVersion) metadataContainer);
    }

    /** JSP */
    public void addMetadataItem(ActionEvent event) {
        String itemType = ActionUtil.getParam(event, "itemType");
        @SuppressWarnings("unchecked")
        ChildrenList<MetadataItem> metadata = (ChildrenList<MetadataItem>) metadataContainer.getMetadata();
        if ("field".equals(itemType)) {
            navigateTo("dialog:fieldDetailsDialog");
            getFieldDetailsDialog().addNewFieldToDocType(metadataContainer);
        } else if ("group".equals(itemType)) {
            navigateTo("dialog:fieldGroupDetailsDialog");
            getFieldGroupDetailsDialog().addNewFieldGroup((DocumentTypeVersion) metadataContainer);
        } else if ("separator".equals(itemType)) {
            metadata.add(SeparatorLine.class);
        } else {
            throw new RuntimeException("Unknown itemType='" + itemType + "'");
        }
        reorderAndMarkBaseState(metadata, getMetadataItemReorderHelper(DocumentAdminModel.Props.ORDER));
    }

    void doReorder() {
        @SuppressWarnings("unchecked")
        ChildrenList<MetadataItem> metadata = (ChildrenList<MetadataItem>) metadataContainer.getMetadata();
        reorderAndMarkBaseState(metadata, getMetadataItemReorderHelper(DocumentAdminModel.Props.ORDER));
    }

    /**
     * Used by propertySheet.
     * Query callBack method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchFieldDefinitions(PickerSearchParams params) {
        Pair<List<String>, Boolean> res = getMissingFieldsOfSystematicFieldGroup();
        List<String> missingFieldsOfFieldGroup = res.getFirst();
        boolean isDocTypeDetailsViewOrNonSystematicFielsGroup = res.getSecond();
        List<FieldDefinition> fieldDefinitions;
        if (StringUtils.isBlank(params.getSearchString())) {
            fieldDefinitions = getDocumentAdminService().getFieldDefinitions();
        } else {
            fieldDefinitions = getDocumentAdminService().searchFieldDefinitions(params.getSearchString());
        }
        List<SelectItem> results = new ArrayList<SelectItem>(params.getLimit());

        Class<? extends DynamicType> dynTypeClass = getDynTypeClass();
        for (FieldDefinition fieldDef : fieldDefinitions) {
            if (dynTypeClass != null && isDocTypeDetailsViewOrNonSystematicFielsGroup
                    && (fieldDef.isOnlyInGroup() || fieldDef.isMandatoryForDynType(dynTypeClass) || fieldDef.isInapplicableForDynType(dynTypeClass))) {
                continue;
            }

            if (missingFieldsOfFieldGroup != null && !missingFieldsOfFieldGroup.contains(fieldDef.getFieldId())) {
                continue; // searching fields for fieldGroup and fieldGroup already contains this field or systematic fieldGroup doesn't contain this field
            }
            SelectItem selectItem = new SelectItem(fieldDef.getFieldId().toString(), fieldDef.getFieldNameWithIdAndType());
            List<String> docTypes = fieldDef.getDocTypes();
            String docTypesString;
            if (docTypes.isEmpty()) {
                docTypesString = MessageUtil.getMessage("fieldDefinitions_list_noDocTypes");
            } else {
                docTypesString = collectionToString(docTypes);
            }
            selectItem.setDescription(getMessage("doc_types") + ": " + docTypesString);
            results.add(selectItem);
            if (results.size() == params.getLimit()) {
                break;
            }
        }
        WebUtil.sort(results);
        return results.toArray(new SelectItem[results.size()]);
    }

    private Class<? extends DynamicType> getDynTypeClass() {
        DocumentTypeVersion dynTypeVer = null;
        if (metadataContainer instanceof FieldGroup) {
            dynTypeVer = ((FieldGroup) metadataContainer).getParent();
        } else {
            dynTypeVer = (DocumentTypeVersion) metadataContainer;
        }
        return dynTypeVer != null && dynTypeVer.getParent() != null ? dynTypeVer.getParent().getClass() : null;
    }

    private Pair<List<String>, Boolean> getMissingFieldsOfSystematicFieldGroup() {
        FieldGroupDetailsDialog fieldGroupDetailsDialog = getFieldGroupDetailsDialog();
        List<String> missingFieldsOfSystematicFieldGroup = null;
        boolean isDocTypeDetailsViewOrNonSystematicFielsGroup = false;
        if (getDialogManager().getBean() == fieldGroupDetailsDialog) {
            FieldGroup fieldGroup = fieldGroupDetailsDialog.getFieldGroup();
            if (fieldGroup.isSystematic()) {
                FieldGroup fieldGroupDef = getDocumentAdminService().getFieldGroupDefinition(fieldGroup.getName());
                missingFieldsOfSystematicFieldGroup = fieldGroupDef.getFieldDefinitionIds();
                for (Field field : fieldGroup.getFields()) {
                    missingFieldsOfSystematicFieldGroup.remove(field.getOriginalFieldId());
                }
            } else {
                isDocTypeDetailsViewOrNonSystematicFielsGroup = true;
            }
        } else {
            isDocTypeDetailsViewOrNonSystematicFielsGroup = true;
        }
        return Pair.newInstance(missingFieldsOfSystematicFieldGroup, isDocTypeDetailsViewOrNonSystematicFielsGroup);
    }

    /** used from JSP when adding field based on existing fieldDefinition */
    public void addExistingField(String fieldDefId) {
        FieldDefinition fieldDefinition = getDocumentAdminService().getFieldDefinition(fieldDefId);
        editField(fieldDefinition);
    }

    /**
     * Used by propertySheet.
     * Query callBack method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchFieldGroups(PickerSearchParams params) {
        boolean isCaseFileType = ((DocumentTypeVersion) metadataContainer).getParent() instanceof CaseFileType;

        List<FieldGroup> fieldGrDefinitions;
        if (StringUtils.isBlank(params.getSearchString())) {
            fieldGrDefinitions = getDocumentAdminService().getFieldGroupDefinitions();
        } else {
            fieldGrDefinitions = getDocumentAdminService().searchFieldGroupDefinitions(params.getSearchString());
        }
        List<SelectItem> results = new ArrayList<SelectItem>();
        outer: for (FieldGroup fieldGrDef : fieldGrDefinitions) {
            // If a systematic group uses child-nodes, then disallow more than one of that same group to be added to document type
            if (getDocumentAdminService().isGroupLimitSingle(fieldGrDef)) {
                for (MetadataItem item : metadataContainer.getMetadata()) {
                    if ((item instanceof FieldGroup) && ((FieldGroup) item).isSystematic() && ((FieldGroup) item).getName().equals(fieldGrDef.getName())) {
                        continue outer;
                    }
                }
            }

            if (isCaseFileType) {
                if (fieldGrDef.isMandatoryForVol() || fieldGrDef.isInapplicableForVol()) {
                    continue; // mandatory fields should have been already added
                }
            } else {
                if (fieldGrDef.isMandatoryForDoc() || fieldGrDef.isInapplicableForDoc()) {
                    continue; // mandatory fields should have been already added
                }
            }
            SelectItem selectItem = new SelectItem(fieldGrDef.getNodeRef().toString(), fieldGrDef.getName());
            selectItem.setDescription(getMessage("fieldDefinitions_list") + ": " + fieldGrDef.getAdditionalInfo());
            results.add(selectItem);
            if (results.size() == params.getLimit()) {
                break;
            }
        }
        WebUtil.sort(results);
        return results.toArray(new SelectItem[results.size()]);
    }

    /** used from JSP when adding fieldGroup based on existing fieldGroup */
    public void addExistingFieldGroup(String fieldGroupNodeRef) {
        NodeRef fieldGroupRef = new NodeRef(fieldGroupNodeRef);
        FieldGroup fieldGroupDef = getDocumentAdminService().getFieldGroup(fieldGroupRef);
        FieldGroup addableFieldGroup = new FieldGroup((BaseObject) metadataContainer);
        getDocumentAdminService().addSystematicFields(fieldGroupDef, addableFieldGroup);
        editFieldGroup(addableFieldGroup);
    }

    public void setDummy(@SuppressWarnings("unused") Object value) {
        // ignore we don't need to store the value
    }

    public Object getDummy() {
        return null; // we don't need to show the value in picker
    }

}
