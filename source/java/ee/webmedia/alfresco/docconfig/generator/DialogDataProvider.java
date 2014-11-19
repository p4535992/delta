package ee.webmedia.alfresco.docconfig.generator;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

<<<<<<< HEAD
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

/**
 * @author Alar Kvell
 */
=======
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface DialogDataProvider {

    UIPropertySheet getPropertySheet();

    DocumentDynamic getDocument();

<<<<<<< HEAD
    CaseFile getCaseFile();

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    Node getNode();

    boolean isInEditMode();

<<<<<<< HEAD
    void switchMode(boolean inEditMode);// TODO refactor this method out of this interface
=======
    void switchMode(boolean inEditMode);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
