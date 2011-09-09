package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFieldDetailsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFieldGroupDetailsDialog;
import static ee.webmedia.alfresco.utils.MessageUtil.getMessage;
import static ee.webmedia.alfresco.utils.TextUtil.collectionToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataContainer;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Shows list of {@link Field}, {@link FieldGroup} and {@link SeparatorLine} objects bound to the latest {@link DocumentTypeVersion} of viewable {@link DocumentType}
 * 
 * @author Ats Uiboupin
 */
public class FieldsListBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private MetadataContainer metadataContainer;

    void init(MetadataContainer parent) {
        metadataContainer = parent;
    }

    public List<? extends MetadataItem> getMetaFieldsList() {
        ChildrenList<? extends MetadataItem> metadata = metadataContainer.getMetadata();
        if (metadata == null) {
            return Collections.emptyList();
        }
        List<MetadataItem> modifiableMetadataList = new ArrayList<MetadataItem>(metadata);
        @SuppressWarnings("unchecked")
        Comparator<MetadataItem> comparator2 = new TransformingComparator(new ComparableTransformer<MetadataItem>() {
            @Override
            public Comparable<?> tr(MetadataItem input) {
                return input.getOrder();
            }
        }, new NullComparator());
        Collections.sort(modifiableMetadataList, comparator2);
        return modifiableMetadataList; // richList also needs modifiable list to sort elements by column
    }

    /** JSP */
    public void removeMetaField(ActionEvent event) {
        ChildrenList<? extends MetadataItem> metadata = metadataContainer.getMetadata();
        if (metadata == null) {
            throw new RuntimeException("unimplemented deleteMetaField from unsaved");
        }
        NodeRef metaFieldRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        MetadataItem removed = metadata.remove(metaFieldRef);
        MessageUtil.addWarningMessage("docType_metadataList_remove_postponed_" + removed.getType());
    }

    /** JSP */
    public void editField(ActionEvent event) {
        NodeRef fieldRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        Field field = (Field) metadataContainer.getMetadata().getChildByNodeRef(fieldRef);
        editField(field);
    }

    private void editField(Field field) {
        navigate("dialog:fieldDetailsDialog");
        getFieldDetailsDialog().editField(field, metadataContainer);
    }

    public void editFieldGroup(ActionEvent event) {
        NodeRef fieldRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        FieldGroup fieldGroup = (FieldGroup) metadataContainer.getMetadata().getChildByNodeRef(fieldRef);
        editFieldGroup(fieldGroup);
    }

    private void editFieldGroup(FieldGroup fieldGroup) {
        navigate("dialog:fieldGroupDetailsDialog");
        getFieldGroupDetailsDialog().editFieldGroup(fieldGroup, (DocumentTypeVersion) metadataContainer);
    }

    /** JSP */
    public void addMetadataItem(ActionEvent event) {
        String itemType = ActionUtil.getParam(event, "itemType");
        @SuppressWarnings("unchecked")
        ChildrenList<MetadataItem> metadata = (ChildrenList<MetadataItem>) metadataContainer.getMetadata();
        if ("field".equals(itemType)) {
            // XXX: tavaliselt listener, action(init dialog),
            // aga siin action(dialog.init())+addNewFieldToDocType(docType)
            navigate("dialog:fieldDetailsDialog");
            getFieldDetailsDialog().addNewFieldToDocType(metadataContainer);
        } else if ("group".equals(itemType)) {
            // XXX: tavaliselt listener, action(init dialog),
            // aga siin action(dialog.init())+addNewFieldToDocType(docType)
            navigate("dialog:fieldGroupDetailsDialog");
            getFieldGroupDetailsDialog().addNewFieldGroup((DocumentTypeVersion) metadataContainer);
        } else if ("separator".equals(itemType)) {
            SeparatorLine sep = metadata.add(SeparatorLine.class);
            sep.setOrder(metadata.size());
        } else {
            throw new RuntimeException("Unknown itemType='" + itemType + "'");
        }
        reorder(metadata);
    }

    private void navigate(String navigationOutcome) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getApplication().getNavigationHandler()
                .handleNavigation(context, null, navigationOutcome);
    }

    private void reorder(ChildrenList<MetadataItem> metadata) {
        // TODO DLSeadist reorder metadata items if needed

    }

    /**
     * Used by propertySheet.
     * Query callBack method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textBox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchFieldDefinitions(int filterIndex, String contains) {
        List<FieldDefinition> fieldDefinitions;
        if (StringUtils.isBlank(contains)) {
            fieldDefinitions = getDocumentAdminService().getFieldDefinitions();
        } else {
            fieldDefinitions = getDocumentAdminService().searchFieldDefinitions(contains);
        }
        List<SelectItem> results = new ArrayList<SelectItem>(fieldDefinitions.size());
        for (FieldDefinition fieldDef : fieldDefinitions) {
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
        }
        WebUtil.sort(results);
        return results.toArray(new SelectItem[results.size()]);
    }

    /** used from JSP when adding field based on existing fieldDefinition */
    public void addExistingField(String fieldDefId) {
        QName fieldDefIdQName = QName.createQName(fieldDefId);
        FieldDefinition fieldDefinition = getDocumentAdminService().getFieldDefinition(fieldDefIdQName);
        editField(fieldDefinition);
    }

    /**
     * Used by propertySheet.
     * Query callBack method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param filterIndex Index of the filter drop-down selection
     * @param contains Text from the contains textBox
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchFieldGroups(int filterIndex, String contains) {
        List<FieldGroup> fieldGrDefinitions;
        if (StringUtils.isBlank(contains)) {
            fieldGrDefinitions = getDocumentAdminService().getFieldGroupDefinitions();
        } else {
            fieldGrDefinitions = getDocumentAdminService().searchFieldGroupDefinitions(contains);
        }
        List<SelectItem> results = new ArrayList<SelectItem>(fieldGrDefinitions.size());
        for (FieldGroup fieldGrDef : fieldGrDefinitions) {
            SelectItem selectItem = new SelectItem(fieldGrDef.getNodeRef().toString(), fieldGrDef.getName());
            selectItem.setDescription(getMessage("fieldDefinitions_list") + ": " + fieldGrDef.getAdditionalInfo());
            results.add(selectItem);
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
        { // reset properties that should not be overwritten by fieldGroupDefinition when adding existing fieldGroup
            addableFieldGroup.setSystematic(false);
            addableFieldGroup.setMandatoryForDoc(false);
            addableFieldGroup.setRemovableFromSystematicDocType(true);
        }
        editFieldGroup(addableFieldGroup);
    }

    public void setDummy(@SuppressWarnings("unused") Object value) {
        // ignore we don't need to store the value
    }

    public Object getDummy() {
        return null; // we don't need to show the value in picker
    }

}
