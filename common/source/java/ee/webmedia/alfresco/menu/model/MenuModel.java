package ee.webmedia.alfresco.menu.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Alar Kvell
 */
public interface MenuModel {
    String URI = "http://alfresco.webmedia.ee/model/menu/1.0";
    String PREFIX = "menu:";

    public interface TYPES {
        QName OUTCOME_SHORTCUT = QName.createQName(URI, "outcomeShortcut");
        QName OUTCOME_SHORTCUTS_ROOT = QName.createQName(URI, "outcomeShortcutsRoot");
    }

    public interface Aspects {
        QName SHORTCUTS = QName.createQName(URI, "shortcuts");
        QName OUTCOME_SHORTCUTS_CONTAINER = QName.createQName(URI, "outcomeShortcutsContainer");
    }

    public interface Assocs {
        QName OUTCOME_SHORTCUTS_ROOT = QName.createQName(URI, "outcomeShortcutsRoot");
        QName OUTCOME_SHORTCUT = QName.createQName(URI, "outcomeShortcut");
    }

    public interface Props {
        QName SHORTCUTS = QName.createQName(URI, "shortcuts");
        QName OUTCOME = QName.createQName(URI, "outcome");
        QName ACTION_NODE_REF = QName.createQName(URI, "actionNodeRef");
    }

}
