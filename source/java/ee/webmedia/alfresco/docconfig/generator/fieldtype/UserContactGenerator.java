package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactRelatedGroupGenerator;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserContactTableGenerator;

import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.systematic.UserContactRelatedGroupGenerator.UserContactRelatedGroupState;
import ee.webmedia.alfresco.docconfig.service.UserContactMappingCode;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * @author Alar Kvell
 */
public class UserContactGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.USER, FieldType.CONTACT, FieldType.USER_CONTACT, FieldType.USERS, FieldType.CONTACTS, FieldType.USERS_CONTACTS };
    }

    // TODO in some cases the filter index resets after search

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator("SearchGenerator");
        item.setStyleClass("expand19-200 medium");
        item.setEditable(true);
        item.setPickerCallback("#{UserContactGroupSearchBean.searchAllWithAdminsAndDocManagers}");
        item.setPreprocessCallback("#{UserContactGroupSearchBean.preprocessResultsToNames}");
        String stateHolderKey = field.getFieldId();
        boolean handledById = field.getOriginalFieldId() != null && (
                getUserContactRelatedGroupGenerator().handlesOriginalFieldId(field.getOriginalFieldId())
                || getUserContactTableGenerator().handlesOriginalFieldId(field.getOriginalFieldId()));
        boolean inSystematicGroup = (field.getParent() instanceof FieldGroup) && ((FieldGroup) field.getParent()).isSystematic();
        boolean itemUnprocessed = !item.getCustomAttributes().containsKey(Search.SETTER_CALLBACK) && !generatorResults.hasStateHolder(stateHolderKey);
        FieldType fieldType = field.getFieldTypeEnum();
        if ((!handledById || handledById && !inSystematicGroup) && itemUnprocessed) {
            item.setSetterCallback(BaseSystematicFieldGenerator.getBindingName("setData", stateHolderKey));
            boolean multivalued = fieldType == FieldType.USERS || fieldType == FieldType.CONTACTS || fieldType == FieldType.USERS_CONTACTS;
            // Setter that take nodes, need the result as NodeRef when not in Search dialog
            if (!field.isForSearch() && !multivalued) {
                item.setPreprocessCallback("#{UserContactGroupSearchBean.preprocessResultsToNodeRefs}");
            }
            item.setSetterCallbackTakesNode(true);
            item.setAjaxParentLevel(1);
            Map<QName, UserContactMappingCode> mapping = BeanHelper.getUserContactMappingService().getFieldIdsMappingOrNull(field);
            Assert.notNull(mapping, "Couldn't find mapping for " + field.getFieldId());
            generatorResults.addStateHolder(stateHolderKey, new UserContactRelatedGroupState(mapping));
        }
        ComponentUtil.addRecipientGrouping(field, item, BeanHelper.getNamespaceService());
        switch (fieldType) {
        case USERS:
            item.setShowFilter(true);
            item.setFilters("#{UserContactGroupSearchBean.usersGroupsFilters}");
            item.setAddLabelId("add_user");
            item.setFiltersAllowGroupSelect(true);
            item.setDialogTitleId("users_search_title");
            break;
        case USER:
            item.setDialogTitleId("users_search_title");
            break;
        case CONTACTS:
            item.setShowFilter(true);
            item.setFilters("#{UserContactGroupSearchBean.contactsGroupsFilters}");
            item.setAddLabelId("add_contact");
            //$FALL-THROUGH$
        case CONTACT:
            item.setDialogTitleId("contacts_search_title");
            item.setFilterIndex(UserContactGroupSearchBean.CONTACTS_FILTER);
            break;
        case USERS_CONTACTS:
            item.setShowFilter(true);
            item.setFilters("#{UserContactGroupSearchBean.usersGroupsContactsGroupsFilters}");
            item.setDialogTitleId("users_contacts_search_title");
            item.setAddLabelId("workflow_compound_add_user");
            item.setFiltersAllowGroupSelect(true);
            break;
        case USER_CONTACT:
            item.setShowFilter(true);
            item.setFilters("#{UserContactGroupSearchBean.usersContactsFilters}");
            item.setDialogTitleId("users_contacts_search_title");
            break;
        default:
            throw new RuntimeException("Unsupported field: " + field);
        }
    }
}
