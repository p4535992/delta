package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class UserContactGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.USER, FieldType.CONTACT, FieldType.USER_CONTACT, FieldType.USERS, FieldType.CONTACTS, FieldType.USERS_CONTACTS };
    }

    // TODO in some cases the filter index resets after search

    // TODO fix autocomplete!

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator("SearchGenerator");
        item.setStyleClass("expand19-200 medium");
        item.setEditable(true);
        item.setPickerCallback("#{UserContactGroupSearchBean.searchAllWithAdminsAndDocManagers}");
        item.setPreprocessCallback("#{UserContactGroupSearchBean.preprocessResultsToNames}");
        item.setSearchSuggestDisabled(true); // TODO temporary
        switch (field.getFieldTypeEnum()) {
        case USERS:
            item.setShowFilter(true);
            item.setFilters("#{UserContactGroupSearchBean.usersGroupsFilters}");
            item.setAddLabelId("add_user");
            //$FALL-THROUGH$
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
