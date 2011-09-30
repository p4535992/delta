package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getMetadataItemReorderHelper;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.reorderAndMarkBaseState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.collections.comparators.TransformingComparator;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for the list of field definitions
 * 
 * @author Ats Uiboupin
 */
public class FieldDefinitionListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private List<FieldDefinition> fieldDefinitions;

    //
    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        initFields();
    }

    private void initFields() {
        cancel();
        setFieldDefinitions(getDocumentAdminService().getFieldDefinitions());
    }

    @Override
    public void restored() {
        initFields();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        setFieldDefinitions(getDocumentAdminService().saveOrUpdateFieldDefinitions(fieldDefinitions));
        MessageUtil.addInfoMessage(context, "save_success");
        return null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String cancel() {
        fieldDefinitions = null;
        return super.cancel();
    }

    public void deleteField(ActionEvent event) {
        NodeRef fieldDefRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        getDocumentAdminService().deleteFieldDefinition(fieldDefRef);
        MessageUtil.addInfoMessage("fieldDefinitions_list_action_delete_success");
        initFields();
    }

    //
    // @Override
    // public Object getActionsContext() {
    // return null;
    // }
    // START: getters / setters
    /**
     * Used in JSP page to create table rows
     */
    public List<FieldDefinition> getFieldDefinitions() {
        return fieldDefinitions;
    }

    private void setFieldDefinitions(List<FieldDefinition> fd) {
        @SuppressWarnings("unchecked")
        Comparator<FieldDefinition> byNameComparator = new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
            @Override
            public Comparable<?> tr(FieldDefinition input) {
                return input.getName();
            }
        });
        Collections.sort(fd, byNameComparator);

        ArrayList<FieldDefinition> docSearchList = new ArrayList<FieldDefinition>();
        for (FieldDefinition fieldDefinition : fd) {
            if (fieldDefinition.isParameterInDocSearch()) {
                docSearchList.add(fieldDefinition);
            }
        }
        BaseObjectOrderModifier<FieldDefinition> modifier = getByDocSearchOrderModifier();
        modifier.markBaseState(docSearchList);
        reorderAndMarkBaseState(docSearchList, modifier);

        ArrayList<FieldDefinition> volSearchList = new ArrayList<FieldDefinition>();
        for (FieldDefinition fieldDefinition : fd) {
            if (fieldDefinition.isParameterInVolSearch()) {
                volSearchList.add(fieldDefinition);
            }
        }
        modifier = getByVolSearchOrderModifier();
        modifier.markBaseState(volSearchList);
        reorderAndMarkBaseState(volSearchList, modifier);

        fieldDefinitions = fd;
    }

    private BaseObjectOrderModifier<FieldDefinition> getByVolSearchOrderModifier() {
        return getMetadataItemReorderHelper(DocumentAdminModel.Props.PARAMETER_ORDER_IN_VOL_SEARCH);
    }

    private BaseObjectOrderModifier<FieldDefinition> getByDocSearchOrderModifier() {
        return getMetadataItemReorderHelper(DocumentAdminModel.Props.PARAMETER_ORDER_IN_DOC_SEARCH);
    }

    // END: getters / setters

}
