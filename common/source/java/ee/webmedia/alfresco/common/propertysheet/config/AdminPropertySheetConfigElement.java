package ee.webmedia.alfresco.common.propertysheet.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.config.ConfigElement;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.PropertySheetConfigElement;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

public class AdminPropertySheetConfigElement extends WMPropertySheetConfigElement {
    private static final long serialVersionUID = 1L;

    protected Map<String, ItemConfig> adminViewableItems = new LinkedHashMap<String, ItemConfig>(8, 10f);
    protected Map<String, ItemConfig> adminEditableItems = new LinkedHashMap<String, ItemConfig>(8, 10f);

    @Override
    protected void addItem(ItemConfig itemConfig) {
        addItemInternal(itemConfig);
    }
    
    @Override
    protected void addItem(ItemConfigVO itemConfig) {
        addItemInternal(itemConfig);
    }
    
    private void addItemInternal(ItemConfig itemConfig) {
        if (!(itemConfig instanceof CustomAttributes)) {
            super.addItem(itemConfig);
            if (itemConfig.isShownInViewMode()) {
                adminViewableItems.put(itemConfig.getName(), itemConfig);
            } else {
                if (adminViewableItems.containsKey(itemConfig.getName())) {
                    adminViewableItems.remove(itemConfig.getName());
                }
            }
            if (itemConfig.isShownInEditMode()) {
                adminEditableItems.put(itemConfig.getName(), itemConfig);
            } else {
                if (adminEditableItems.containsKey(itemConfig.getName())) {
                    adminEditableItems.remove(itemConfig.getName());
                }
            }
            return;
        }
        CustomAttributes customItemConfig = (CustomAttributes) itemConfig;
        boolean isViewAndEditByAdmin = Boolean.parseBoolean(customItemConfig.getCustomAttributes().get("view-and-edit-by-admin"));
        boolean isEditByAdmin = Boolean.parseBoolean(customItemConfig.getCustomAttributes().get("edit-by-admin"));

        items.put(itemConfig.getName(), itemConfig);

        if (itemConfig.isShownInViewMode()) {
            // add the item to the view list if it is editable
            if (!isViewAndEditByAdmin || isEditByAdmin) {
                viewableItems.put(itemConfig.getName(), itemConfig);
            } else {
                // if the item was added previously as admin viewable it should be removed
                if (viewableItems.containsKey(itemConfig.getName())) {
                    viewableItems.remove(itemConfig.getName());
                }
            }
            adminViewableItems.put(itemConfig.getName(), itemConfig);
        } else {
            // if the item was added previously as viewable it should be removed
            if (viewableItems.containsKey(itemConfig.getName())) {
                viewableItems.remove(itemConfig.getName());
                adminViewableItems.remove(itemConfig.getName());
            }
        }

        if (itemConfig.isShownInEditMode()) {
            // add the item to the edit list if it is editable
            if (isEditByAdmin) {
                // add to editableItems as read-only
                ItemConfig readOnlyProperty = ((ReadOnlyCopiableItemConfig) itemConfig).copyAsReadOnly();
                editableItems.put(itemConfig.getName(), readOnlyProperty);
            } else if (!isViewAndEditByAdmin) {
                editableItems.put(itemConfig.getName(), itemConfig);
            } else {
                // if the item was added previously as admin editable it should be removed
                if (editableItems.containsKey(itemConfig.getName())) {
                    editableItems.remove(itemConfig.getName());
                }
            }
            adminEditableItems.put(itemConfig.getName(), itemConfig);
        } else {
            // if the item was added previously as editable it should be removed
            if (editableItems.containsKey(itemConfig.getName())) {
                editableItems.remove(itemConfig.getName());
                adminEditableItems.remove(itemConfig.getName());
            }
        }
    }

    @Override
    public ConfigElement combine(ConfigElement configElement) {
        AdminPropertySheetConfigElement combinedElement = new AdminPropertySheetConfigElement();

        // add all the existing properties
        for (ItemConfig item : getItems().values()) {
            combinedElement.addItem(item);
        }

        // add all the properties from the given element
        for (ItemConfig item : ((PropertySheetConfigElement) configElement).getItems().values()) {
            combinedElement.addItem(item);
        }

        return combinedElement;
    }

    @Override
    public List<String> getItemNamesToShow() {
        Map<String, ItemConfig> map;
        if (hasAdminAuthority()) {
            map = adminViewableItems;
        } else {
            map = viewableItems;
        }

        List<String> propNames = new ArrayList<String>(map.size());

        for (String propName : map.keySet()) {
            propNames.add(propName);
        }

        return propNames;
    }

    @Override
    public Map<String, ItemConfig> getItemsToShow() {
        if (hasAdminAuthority()) {
            return adminViewableItems;
        }
        return viewableItems;
    }

    @Override
    public List<String> getEditableItemNamesToShow() {
        Map<String, ItemConfig> map;
        if (hasAdminAuthority()) {
            map = adminEditableItems;
        } else {
            map = editableItems;
        }

        List<String> propNames = new ArrayList<String>(map.size());

        for (String propName : map.keySet()) {
            propNames.add(propName);
        }

        return propNames;
    }

    @Override
    public Map<String, ItemConfig> getEditableItemsToShow() {
        if (hasAdminAuthority()) {
            return adminEditableItems;
        }
        return editableItems;
    }

    public static boolean hasAdminAuthority() {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getAuthorityService().hasAdminAuthority();
    }

}
