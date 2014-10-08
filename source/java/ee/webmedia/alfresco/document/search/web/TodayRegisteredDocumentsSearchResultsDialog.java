package ee.webmedia.alfresco.document.search.web;

import java.util.Map;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
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
        resetLimit(false);
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    /** @param event */
    public void setupSearch(ActionEvent event) {
        quickSearch = true;
        resetLimit(false);
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    @Override
    public void restored() {
        BeanHelper.getVisitedDocumentsBean().resetVisitedDocuments(documents);
    }

    private void doInitialSearch() {
        documents = setLimited(getDocumentSearchService().searchTodayRegisteredDocuments(quickSearch ? searchValue : null, getLimit()));
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
