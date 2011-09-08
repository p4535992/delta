package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class UsersContactsGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.USER, FieldType.CONTACT, FieldType.USER_CONTACT, FieldType.USERS, FieldType.CONTACTS, FieldType.USERS_CONTACTS };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator("SearchGenerator");
        item.setStyleClass("expand19-200 medium");
        item.setEditable(true);
        switch (field.getFieldTypeEnum()) {
        case USER:
        case USERS:
            item.setPickerCallback("#{UserListDialog.searchUsersWithNameValue}");
            item.setDialogTitleId("users_search_title");
            break;
        case CONTACT:
        case CONTACTS:
            item.setPickerCallback("#{AddressbookDialog.searchContactsWithNameValue}");
            item.setDialogTitleId("contacts_search_title");
            break;
        case USER_CONTACT:
        case USERS_CONTACTS:
            item.setPickerCallback("#{DocumentDynamicDialog.searchUsersOrContacts}");
            item.setDialogTitleId("users_contacts_search_title");
            item.setShowFilter(true);
            item.setFilters("#{MetadataBlockBean.userOrContactSearchFilters}");
            break;
        default:
            throw new RuntimeException("Unsupported field: " + field);
        }
        // TODO in view mode, display as "value1, value2, ...", currently displayed as "[value1, value2, ...]" - use converter?
    }

}
