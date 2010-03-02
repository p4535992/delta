package ee.webmedia.alfresco.document.search.web;

import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.MessageUtil;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.springframework.web.jsf.FacesContextUtils;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Alar Kvell
 */
public class DocumentSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    private static final List<String> EP_EXPORT_SEND_MODES = Arrays.asList(SendMode.MAIL.getValueName(), SendMode.REGISTERED_MAIL.getValueName());

    private transient SendOutService sendOutService;

    public void setup(List<Document> documents) {
        this.documents = documents;
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_results");
    }

    public void exportAsCsv(ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("documentList");
    }

    public void exportEstonianPost(ActionEvent event) {
        CSVExporter exporter = new CSVExporter(new EstonianPostExportDataReader());
        exporter.setOrderInfo(0, false);
        exporter.export("documentList");
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
                List<SendInfo> sendInfos = getSendOutService().getSendInfos(document.getNode().getNodeRef());
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
