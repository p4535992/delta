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

    void switchMode(boolean inEditMode);
}
