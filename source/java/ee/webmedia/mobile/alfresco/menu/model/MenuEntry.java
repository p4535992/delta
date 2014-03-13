package ee.webmedia.mobile.alfresco.menu.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple VO that represents an item in sidebar menu
 */
public class MenuEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String details;
    private String target;
    private List<MenuEntry> subItems;
    private boolean topLevel;

    public MenuEntry() {
        // Default Constructor
    }

    public MenuEntry addSubItem(String title, String details, String target) {
        MenuEntry entry = new MenuEntry();
        entry.setTitle(title);
        entry.setDetails(details);
        entry.setTarget(target);

        getSubItems().add(entry);
        return entry;
    }

    public static MenuEntry parent(String title) {
        MenuEntry entry = new MenuEntry();
        entry.setTopLevel(true);
        entry.setTitle(title);

        return entry;
    }

    @Override
    public String toString() {
        return "MenuEntry [title=" + title + ", details=" + details + ", target=" + target + ", subItems=" + subItems + ", topLevel=" + topLevel + "]";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<MenuEntry> getSubItems() {
        if (subItems == null) {
            subItems = new ArrayList<MenuEntry>();
        }
        return subItems;
    }

    public void setSubItems(List<MenuEntry> subItems) {
        this.subItems = subItems;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

}
