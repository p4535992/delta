package ee.webmedia.alfresco.docadmin.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Field that is stored under /fieldDefinitions folder, but not under {@link DocumentTypeVersion} or {@link FieldGroup}
 * 
 * @author Ats Uiboupin
 */
public class FieldDefinition extends Field {
    private static final long serialVersionUID = 1L;

    public FieldDefinition(BaseObject parent) {
        super(parent, DocumentAdminModel.Types.FIELD_DEFINITION);
    }

    /** Used by {@link BaseService#getObject(NodeRef, Class)} through reflection */
    public FieldDefinition(BaseObject parent, WmNode fieldDefNode) {
        super(parent, fieldDefNode);
    }

    public FieldDefinition(NodeRef parentRef) {
        super(parentRef, DocumentAdminModel.Types.FIELD_DEFINITION);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public FieldDefinition(NodeRef parentRef, WmNode fieldDefNode) {
        super(parentRef, fieldDefNode);
    }

    // START: properties

    public List<String> getDocTypes() {
        return getPropList(DocumentAdminModel.Props.DOC_TYPES);
    }

    public void setDocTypes(List<String> docTypes) {
        setPropList(DocumentAdminModel.Props.DOC_TYPES, docTypes);
    }

    public boolean isParameterInDocSearch() {
        return getPropBoolean(DocumentAdminModel.Props.IS_PARAMETER_IN_DOC_SEARCH);
    }

    public void setParameterInDocSearch(boolean isParameterInDocSearch) {
        setProp(DocumentAdminModel.Props.IS_PARAMETER_IN_DOC_SEARCH, isParameterInDocSearch);
    }

    public boolean isParameterInVolSearch() {
        return getPropBoolean(DocumentAdminModel.Props.IS_PARAMETER_IN_VOL_SEARCH);
    }

    public void setParameterInVolSearch(boolean isParameterInVolSearch) {
        setProp(DocumentAdminModel.Props.IS_PARAMETER_IN_VOL_SEARCH, isParameterInVolSearch);
    }

    public Integer getParameterOrderInDocSearch() {
        return getProp(DocumentAdminModel.Props.PARAMETER_ORDER_IN_DOC_SEARCH);
    }

    public void setParameterOrderInDocSearch(Integer parameterOrderInDocSearch) {
        setProp(DocumentAdminModel.Props.PARAMETER_ORDER_IN_DOC_SEARCH, parameterOrderInDocSearch);
    }

    public Integer getParameterOrderInVolSearch() {
        return getProp(DocumentAdminModel.Props.PARAMETER_ORDER_IN_VOL_SEARCH);
    }

    public void setParameterOrderInVolSearch(Integer parameterOrderInVolSearch) {
        setProp(DocumentAdminModel.Props.PARAMETER_ORDER_IN_VOL_SEARCH, parameterOrderInVolSearch);
    }

    public final List<String> getVolTypes() {
        return getPropList(DocumentAdminModel.Props.VOL_TYPES);
    }

    public final void setVolTypes(List<String> volTypes) {
        setPropList(DocumentAdminModel.Props.VOL_TYPES, volTypes);
    }

    // END: properties
    // START: properties that should not be changed
    public boolean isFixedParameterInDocSearch() {
        return getPropBoolean(DocumentAdminModel.Props.IS_FIXED_PARAMETER_IN_DOC_SEARCH);
    }

    public boolean isFixedParameterInVolSearch() {
        return getPropBoolean(DocumentAdminModel.Props.IS_FIXED_PARAMETER_IN_VOL_SEARCH);
    }

    // END: properties that should not be changed

    @Override
    public FieldDefinition clone() {
        return (FieldDefinition) super.clone(); // just return casted type
    }

}
