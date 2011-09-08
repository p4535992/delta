package ee.webmedia.alfresco.docconfig.generator;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

/**
 * @author Alar Kvell
 */
public interface DialogDataProvider {

    UIPropertySheet getPropertySheet();

    Node getNode();

    boolean isInEditMode();

    <E extends PropertySheetStateHolder> E getStateHolder(String key, Class<E> clazz);

    void switchMode(boolean inEditMode);
}
