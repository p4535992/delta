package ee.webmedia.alfresco.common.propertysheet.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.config.ConfigElement;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.PropertySheetConfigElement;

public class AdminPropertySheetConfigElement extends WMPropertySheetConfigElement {
    private static final long serialVersionUID = 1L;

    protected Map<String, ItemConfig> adminViewableItems = new LinkedHashMap<String, ItemConfig>(8, 10f);
    protected Map<String, ItemConfig> adminEditableItems = new LinkedHashMap<String, ItemConfig>(8, 10f);

    @Override
    protected void addItem(ItemConfig itemConfig) {
        if (!(itemConfig instanceof WMPropertyConfig)) {
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
        WMPropertyConfig propertyConfig = (WMPropertyConfig) itemConfig;
        boolean isViewAndEditByAdmin = Boolean.parseBoolean(propertyConfig.getCustomAttributes().get("view-and-edit-by-admin"));
        boolean isEditByAdmin = Boolean.parseBoolean(propertyConfig.getCustomAttributes().get("edit-by-admin"));

        items.put(propertyConfig.getName(), propertyConfig);

        if (propertyConfig.isShownInViewMode()) {
            // add the item to the view list if it is editable
            if (!isViewAndEditByAdmin || isEditByAdmin) {
                viewableItems.put(propertyConfig.getName(), propertyConfig);
            } else {
                // if the item was added previously as admin viewable it should be removed
                if (viewableItems.containsKey(propertyConfig.getName())) {
                    viewableItems.remove(propertyConfig.getName());
                }
            }
            adminViewableItems.put(propertyConfig.getName(), propertyConfig);
        } else {
            // if the item was added previously as viewable it should be removed
            if (viewableItems.containsKey(propertyConfig.getName())) {
                viewableItems.remove(propertyConfig.getName());
                adminViewableItems.remove(propertyConfig.getName());
            }
        }

        if (propertyConfig.isShownInEditMode()) {
            // add the item to the edit list if it is editable
            if (isEditByAdmin) {
                // add to editableItems as read-only
                WMPropertyConfig readOnlyProperty = new WMPropertyConfig(propertyConfig.getName(), propertyConfig.getDisplayLabel(), propertyConfig
                        .getDisplayLabelId(), true, propertyConfig.getConverter(), Boolean.toString(propertyConfig.isShownInViewMode()), Boolean
                        .toString(propertyConfig.isShownInEditMode()), propertyConfig.getComponentGenerator(), Boolean.toString(propertyConfig
                        .getIgnoreIfMissing()), propertyConfig.getCustomAttributes());
                editableItems.put(propertyConfig.getName(), readOnlyProperty);
            } else if (!isViewAndEditByAdmin) {
                editableItems.put(propertyConfig.getName(), propertyConfig);
            } else {
                // if the item was added previously as admin editable it should be removed
                if (editableItems.containsKey(propertyConfig.getName())) {
                    editableItems.remove(propertyConfig.getName());
                }
            }
            adminEditableItems.put(propertyConfig.getName(), propertyConfig);
        } else {
            // if the item was added previously as editable it should be removed
            if (editableItems.containsKey(propertyConfig.getName())) {
                editableItems.remove(propertyConfig.getName());
                adminEditableItems.remove(propertyConfig.getName());
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
