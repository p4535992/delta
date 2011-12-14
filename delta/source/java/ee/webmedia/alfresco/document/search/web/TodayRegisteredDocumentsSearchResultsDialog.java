package ee.webmedia.alfresco.document.search.web;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class TodayRegisteredDocumentsSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    private transient UIPanel panel;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        documents = getDocumentSearchService().searchTodayRegisteredDocuments();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_registeredToday");
    }

    public void setPanel(UIPanel panel) {
        this.panel = panel;
    }

    public UIPanel getPanel() {
        if (panel == null) {
            panel = new UIPanel();
        }
        return panel;
    }

}
