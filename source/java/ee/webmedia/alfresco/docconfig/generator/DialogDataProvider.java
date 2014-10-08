package ee.webmedia.alfresco.docconfig.generator;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

public interface DialogDataProvider {

    UIPropertySheet getPropertySheet();

    DocumentDynamic getDocument();

    CaseFile getCaseFile();

    Node getNode();

    boolean isInEditMode();

    void switchMode(boolean inEditMode);// TODO refactor this method out of this interface
}
