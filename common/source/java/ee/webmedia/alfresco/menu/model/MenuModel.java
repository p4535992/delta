package ee.webmedia.alfresco.menu.model;

import org.alfresco.service.namespace.QName;

public interface MenuModel {
    String URI = "http://alfresco.webmedia.ee/model/menu/1.0";
    String PREFIX = "menu:";

    public interface Aspects {
        QName SHORTCUTS = QName.createQName(URI, "shortcuts");
    }

    public interface Props {
        QName SHORTCUTS = QName.createQName(URI, "shortcuts");
    }

}
