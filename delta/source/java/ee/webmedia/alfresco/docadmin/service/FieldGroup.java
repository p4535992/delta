package ee.webmedia.alfresco.docadmin.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * container for {@link Field}. Stored under /fieldGroupDefinitions or {@link DocumentTypeVersion}
 * 
 * @author Ats Uiboupin
 */
public class FieldGroup extends FieldAndGroupBase implements MetadataContainer {
    private static final long serialVersionUID = 1L;

    public FieldGroup(BaseObject parent) {
        super(parent, DocumentAdminModel.Types.FIELD_GROUP);
    }

    /** Used by {@link BaseService#getObject(NodeRef, Class)} through reflection */
    public FieldGroup(BaseObject parent, WmNode node) {
        super(parent, node);
    }

    public ChildrenList<Field> getFields() {
        return getChildren(Field.class);
    }

    @Override
    public ChildrenList<Field> getMetadata() {
        return getFields();
    }

    @Override
    public String getAdditionalInfo() {
        List<String> fieldNames = new ArrayList<String>();
        for (Field field : getFields().getList()) {
            fieldNames.add(field.getName() + " (" + field.getFieldId() + ", " + field.getFieldType() + ")");
        }
        return StringUtils.join(fieldNames, ", ");
    }

    // START: properties
    public final boolean isMandatoryForVol() {
        return getPropBoolean(DocumentAdminModel.Props.MANDATORY_FOR_VOL);
    }

    public final void setMandatoryForVol(boolean mandatoryForVol) {
        setProp(DocumentAdminModel.Props.MANDATORY_FOR_VOL, mandatoryForVol);
    }

    public final String getReadonlyFieldsName() {
        return getProp(DocumentAdminModel.Props.READONLY_FIELDS_NAME);
    }

    public final void setReadonlyFieldsName(String readonlyFieldsName) {
        setProp(DocumentAdminModel.Props.READONLY_FIELDS_NAME, readonlyFieldsName);
    }

    public final String getReadonlyFieldsRule() {
        return getProp(DocumentAdminModel.Props.READONLY_FIELDS_RULE);
    }

    public final void setReadonlyFieldsRule(String readonlyFieldsRule) {
        setProp(DocumentAdminModel.Props.READONLY_FIELDS_RULE, readonlyFieldsRule);
    }

    public final void setShowInTwoColumns(boolean showInTwoColumns) {
        setProp(DocumentAdminModel.Props.SHOW_IN_TWO_COLUMNS, showInTwoColumns);
    }

    public final boolean isShowInTwoColumns() {
        return getPropBoolean(DocumentAdminModel.Props.SHOW_IN_TWO_COLUMNS);
    }

    public final String getThesaurus() {
        return getProp(DocumentAdminModel.Props.THESAURUS);
    }

    public final void setThesaurus(String thesaurus) {
        setProp(DocumentAdminModel.Props.THESAURUS, thesaurus);
    }

    // END: properties

    @Override
    public FieldGroup clone() {
        return (FieldGroup) super.clone(); // just return casted type
    }
}
