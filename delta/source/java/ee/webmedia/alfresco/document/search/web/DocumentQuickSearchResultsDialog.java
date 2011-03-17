package ee.webmedia.alfresco.document.search.web;

import java.util.Collections;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;

import ee.webmedia.alfresco.document.model.DocumentCreateOrRegistrateDateComparator;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DocumentQuickSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentQuickSearchResultsDialog.class);

    private String searchValue;

    /** @param event */
    public void setup(ActionEvent event) {
        restored();
    }

    @Override
    public void restored() {
        try {
            documents = getDocumentSearchService().searchDocumentsQuick(searchValue);
            Collections.sort(documents, DocumentCreateOrRegistrateDateComparator.getComparator());
        } catch (BooleanQuery.TooManyClauses e) {
            log.error("Quick search of '" + searchValue + "' failed: " + e.getMessage()); // stack trace is logged in the service
            documents = Collections.emptyList();
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_search_toomanyclauses");
        } catch (Hits.TooLongQueryException e) {
            log.error("Quick search of '" + searchValue + "' failed: " + e.getMessage()); // stack trace is logged in the service
            documents = Collections.emptyList();
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_search_toolongquery");
        }

        // Quick search must "reset the current dialog stack" and put the document list dialog as the base dialog into the stack.
        // Also in case of quick search the cancel button is not displayed (the whole button container is not rendered through
        // container.jsp hack for DocumentListDialog). If there are more beans that need to sometimes display some buttons and sometimes
        // not. Then the hack should be refactored into new DialogManager isAnyButtonVisible method that asks this from then current
        // bean (BaseDialogBean always returns true).
        MenuBean.clearViewStack(String.valueOf(MenuBean.DOCUMENT_REGISTER_ID), null);

        MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME);
        menuBean.collapseMenuItems(null);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_quick_search_results");
    }
    
    @Override
    public String getInfoMessage() {
        if(getSearchValue().length() < 3) {
            return MessageUtil.getMessage("document_quick_search_query_3_char_min");
        }
        return super.getInfoMessage();
    }

    // START: getters / setters

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    // END: getters / setters
}