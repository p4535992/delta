package ee.webmedia.alfresco.menu.service;

import static org.alfresco.util.EqualsHelper.nullSafeEquals;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.EqualsHelper;
import org.springframework.util.Assert;

/**
 * @author Riina Tens
 */
public class ShortcutMenuItem implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * If true, shortcut contains action, outcome and nodeRef defining shortcut outcome,
     * if false, shortcut menuItemId refers to menu item
     */
    private final boolean outcomeShortcut;
    private String menuItemId;
    private ShortcutMenuItemOutcome outcome;
    private NodeRef actionNodeRef;
    private NodeRef menuItemNodeRef;

    public ShortcutMenuItem(String menuItemId) {
        this.menuItemId = menuItemId;
        outcomeShortcut = false;
    }

    public ShortcutMenuItem(NodeRef menuItemNodeRef, ShortcutMenuItemOutcome outcome, NodeRef actionNodeRef) {
        Assert.notNull(outcome);
        this.outcome = outcome;
        this.actionNodeRef = actionNodeRef;
        this.menuItemNodeRef = menuItemNodeRef;
        outcomeShortcut = true;
    }

    public boolean isOutcomeShortcut() {
        return outcomeShortcut;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

    public ShortcutMenuItemOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ShortcutMenuItemOutcome outcome) {
        this.outcome = outcome;
    }

    public NodeRef getActionNodeRef() {
        return actionNodeRef;
    }

    public void setActionNodeRef(NodeRef actionNodeRef) {
        this.actionNodeRef = actionNodeRef;
    }

    public boolean equals(ShortcutMenuItem otherShortcutmenuItem) {
        if (outcomeShortcut != otherShortcutmenuItem.isOutcomeShortcut()) {
            return false;
        }
        if (!outcomeShortcut) {
            return EqualsHelper.nullSafeEquals(menuItemId, otherShortcutmenuItem.getMenuItemId());
        }
        return outcome == otherShortcutmenuItem.getOutcome() && nullSafeEquals(actionNodeRef, otherShortcutmenuItem.getActionNodeRef());
    }

    public NodeRef getMenuItemNodeRef() {
        return menuItemNodeRef;
    }

    public void setMenuItemNodeRef(NodeRef menuItemNodeRef) {
        this.menuItemNodeRef = menuItemNodeRef;
    }

}
