package ee.webmedia.alfresco.document.search.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.lucene.search.BooleanQuery;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCreateOrRegistrateDateComparator;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class DocumentSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchResultsDialog.class);

    private static final List<String> EP_EXPORT_SEND_MODES = Arrays.asList(SendMode.MAIL.getValueName(), SendMode.REGISTERED_MAIL.getValueName());

    private transient SendOutService sendOutService;

    private Node searchFilter;
    private String dialogOutcome;

    public String setup(Node filter) {
        this.searchFilter = filter;
        restored();
        return dialogOutcome;
    }

    @Override
    public void restored() {
        try {
            documents = getDocumentSearchService().searchDocuments(searchFilter);
        } catch (BooleanQuery.TooManyClauses e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(searchFilter.getProperties()));
            filterProps.remove(DocumentSearchModel.Props.OUTPUT);
            log.error("Document search of failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, Repository
                            .getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService())); // stack trace is logged in the service
            documents = Collections.emptyList();
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_search_toomanyclauses");
        }
        String dialog = "documentSearchResultsDialog";
        if (DocumentSearchDialog.OUTPUT_EXTENDED.equals(searchFilter.getProperties().get(DocumentSearchModel.Props.OUTPUT))) {
            dialog = "documentSearchExtendedResultsDialog";
            documents = getDocumentService().processExtendedSearchResults(documents, searchFilter);
        }
        Collections.sort(documents, DocumentCreateOrRegistrateDateComparator.getComparator());        
        dialogOutcome = dialog;
        super.restored();
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_results");
    }

    /** @param event */
    public void exportAsCsv(ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("documentList");
        
        // Erko hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();        
    }

    /** @param event */
    public void exportEstonianPost(ActionEvent event) {
        CSVExporter exporter = new CSVExporter(new EstonianPostExportDataReader());
        exporter.setOrderInfo(0, false);
        exporter.export("documentList");
        
        // Erko hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();        
    }

    private class EstonianPostExportDataReader implements DataReader {
        @Override
        public List<String> getHeaderRow(UIRichList list, FacesContext fc) {
            return Arrays.asList(MessageUtil.getMessage("document_send_mode"),
                    MessageUtil.getMessage("document_regNumber"),
                    MessageUtil.getMessage("document_search_export_recipient"));
        }

        @Override
        public List<List<String>> getDataRows(UIRichList list, FacesContext fc) {
            List<List<String>> data = new ArrayList<List<String>>();
            while (list.isDataAvailable()) {
                Document document = (Document) list.nextRow();
                List<SendInfo> sendInfos = getSendOutService().getSendInfos(document.getNodeRef());
                for (SendInfo sendInfo : sendInfos) {
                    if (EP_EXPORT_SEND_MODES.contains(sendInfo.getSendMode().toString())) {
                        data.add(Arrays.asList(sendInfo.getSendMode().toString(), document.getRegNumber(), sendInfo.getRecipient().toString()));
                    }
                }
            }
            return data;
        }
    }

    protected SendOutService getSendOutService() {
        if (sendOutService == null) {
            sendOutService = (SendOutService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance()).getBean(SendOutService.BEAN_NAME);
        }
        return sendOutService;
    }

}
