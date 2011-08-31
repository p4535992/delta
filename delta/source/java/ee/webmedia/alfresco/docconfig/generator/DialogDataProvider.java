package ee.webmedia.alfresco.docconfig.generator;

import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.web.WmNode;

/**
 * @author Alar Kvell
 */
public interface DialogDataProvider {

    UIPropertySheet getPropertySheet();

    WmNode getNode();

    boolean isInEditMode();

}
