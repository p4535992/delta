package ee.webmedia.alfresco.document.search.web;

import java.util.List;
import java.util.Map;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class TodayRegisteredDocumentsSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    private transient UIPanel panel;
    private String searchValue;
    private boolean quickSearch;

    @Override
    public void init(Map<String, String> params) {
        quickSearch = false;
        super.init(params);
    }

    /** @param event */
    public void setup(ActionEvent event) {
        searchValue = "";
        quickSearch = false;
        restored();
    }

    /** @param event */
    public void setupSearch(ActionEvent event) {
        quickSearch = true;
        restored();
    }

    @Override
    public void restored() {
        Pair<List<Document>, Boolean> searchTodayRegisteredDocuments = getDocumentSearchService().searchTodayRegisteredDocuments((quickSearch ? searchValue : null),
                !temporarilyDisableLimiting);
        documents = searchTodayRegisteredDocuments.getFirst();
        documentListLimited = searchTodayRegisteredDocuments.getSecond();
        temporarilyDisableLimiting = false;
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_registeredToday");
    }

    @Override
    public void setPanel(UIPanel panel) {
        this.panel = panel;
    }

    @Override
    public UIPanel getPanel() {
        if (panel == null) {
            panel = new UIPanel();
        }
        return panel;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    @Override
    public String getInfoMessage() {
        if (quickSearch && getSearchValue().length() < 3) {
            return MessageUtil.getMessage("document_quick_search_query_3_char_min");
        }
        return super.getInfoMessage();
    }

    public String getSearchValue() {
        return searchValue;
    }

}
