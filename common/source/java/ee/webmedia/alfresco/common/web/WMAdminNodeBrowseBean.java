package ee.webmedia.alfresco.common.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
import static ee.webmedia.alfresco.common.web.BeanHelper.getExporterService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSearchService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.repo.importer.ACPImportPackageHandler;
import org.alfresco.repo.importer.ImportTimerProgress;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.search.impl.lucene.LuceneResultSetRow;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.view.ExportPackageHandler;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ImportPackageHandler;
import org.alfresco.service.cmr.view.ImporterBinding;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.cmr.view.ReferenceType;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ISO9075;
import org.alfresco.web.bean.admin.AdminNodeBrowseBean;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Fieldable;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.service.CustomReindexComponent;
import ee.webmedia.alfresco.utils.FilenameUtil;

public class WMAdminNodeBrowseBean extends AdminNodeBrowseBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WMAdminNodeBrowseBean.class);

    private static final long serialVersionUID = -3757857288967828948L;
    private String targetRef;
    private String importFileName;
    private String assocTypeQName;
    private List<SelectItem> assocTypeQNames;
    transient private DataModel sourceAssocs;

    public String getLuceneIndexInfo() {
        if (getNodeRef() == null) {
            return null;
        }
        return BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<String>() {
            @Override
            public String execute() throws Throwable {
                StringBuilder s = new StringBuilder();
                logLuceneIndexDocuments(s, getNodeRef());
                return s.toString();
            }
        }, true);
    }

    private void logLuceneIndexDocuments(StringBuilder s, NodeRef nodeRef) {
        String query = "ID:\"" + nodeRef.toString() + "\"";
        logLuceneIndexDocuments(s, nodeRef.getStoreRef(), "", query, true);
    }

    private void logLuceneIndexDocuments(StringBuilder s, StoreRef storeRef, String logPrefix, String query, boolean logTxns) {
        ResultSet resultSet = getSearchService().query(storeRef, SearchService.LANGUAGE_LUCENE, query);
        try {
            s.append("Found ").append(resultSet.length()).append(" documents from Lucene index for ").append(query).append("\n");
            for (int i = 0; i < resultSet.length(); i++) {
                String changeTxnId = null;
                s.append("\nLUCENE_DOCUMENT[").append(logPrefix).append(i + 1).append("]");
                @SuppressWarnings("unchecked")
                List<Fieldable> fields = ((LuceneResultSetRow) resultSet.getRow(i)).getDocument().getFields();
                for (Fieldable field : fields) {
                    s.append("\n  ").append(field.name()).append("=").append(field.stringValue());
                    if ("TX".equals(field.name())) {
                        changeTxnId = field.stringValue();
                    }
                }
                s.append("\n");
                if (logTxns && changeTxnId != null) {
                    logTxn(s, resultSet.getNodeRef(i).getStoreRef(), logPrefix + (i + 1) + ".", changeTxnId);
                }
            }

        } finally {
            resultSet.close();
        }
    }

    private void logTxn(StringBuilder s, StoreRef storeRef, String logPrefix, String changeTxnId) {
        DataSource dataSource = BeanHelper.getSpringBean(DataSource.class, "dataSource");
        SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
        NodeDaoService nodeDaoService = BeanHelper.getSpringBean(NodeDaoService.class, "nodeDaoService");

        List<Map<String, Object>> txns = jdbcTemplate.queryForList("SELECT * FROM alf_transaction WHERE change_txn_id = ?", changeTxnId);
        s.append("\nFound ").append(txns.size()).append(" alf_transaction rows from database for change_txn_id=").append(changeTxnId).append("\n");
        int i = 1;
        for (Map<String, Object> txn : txns) {
            Long txnId = (Long) txn.get("id");
            Transaction dbTxn = nodeDaoService.getTxnById(txnId);
            s.append("\nDB_ALF_TRANSACTION[").append(logPrefix).append(i++).append("]\n  ").append(CustomReindexComponent.getTransactionInfo(dbTxn)).append(" ")
                    .append(dbTxn.getChangeTxnId());
            List<NodeRef> txnChanges = nodeDaoService.getTxnChanges(txnId);
            int txnDeleteCount = nodeDaoService.getTxnDeleteCount(txnId);
            int txnUpdateCount = nodeDaoService.getTxnUpdateCount(txnId);
            s.append("\n  updateCount=").append(txnUpdateCount).append(" deleteCount=").append(txnDeleteCount).append("\n  changes[").append(txnChanges.size()).append("]");
            for (NodeRef nodeRef : txnChanges) {
                s.append("\n    ").append(nodeRef.toString());
            }
            s.append("\n");
        }
        s.append("\n");

        String query = "TX:\"" + changeTxnId + "\"";
        logLuceneIndexDocuments(s, storeRef, logPrefix + "a.", query, false);
    }

    public List<SelectItem> getAssocTypeQNames() {
        final Collection<QName> allAssociations = getDictionaryService().getAllAssociations();
        assocTypeQNames = new ArrayList<SelectItem>(allAssociations.size() + 1);
        for (QName assocQName : allAssociations) {

            final AssociationDefinition association = getDictionaryService().getAssociation(assocQName);
            final QName sourceName = association.getSourceClass().getName();
            final QName targetName = association.getTargetClass().getName();
            final SelectItem item = new SelectItem(assocQName.toString(), assocQName.getPrefixString() + " (" + sourceName.toPrefixString() + " -> "
                    + targetName.toPrefixString() + ")");
            if (!getDictionaryService().isSubClass(getNodeType(), sourceName)) {
                item.setDisabled(true);
            }
            if (getDictionaryService().isSubClass(getNodeType(), targetName)) {
                item.setLabel("(target) " + item.getLabel());
            }
            assocTypeQNames.add(item);
        }
        assocTypeQNames.add(0, new SelectItem("[defaultSelection]", ""));
        return assocTypeQNames;
    }

    public void setTargetRef(String targetRef) {
        this.targetRef = targetRef;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setAssocTypeQName(String assocTypeQName) {
        this.assocTypeQName = assocTypeQName;
    }

    public String getAssocTypeQName() {
        return assocTypeQName;
    }

    public void submitCreateAssoc() {
        if (StringUtils.isBlank(assocTypeQName)) {
            addErrorMessage("nodeBrowser_custom_noSuchAssocTypeQName");
            return;
        }
        final QName newAssocTypeQname = QName.createQName(assocTypeQName);
        final Collection<QName> allAssociations = getDictionaryService().getAllAssociations();
        if (allAssociations.contains(newAssocTypeQname)) {
            getNodeService().createAssociation(getNodeRef(), new NodeRef(targetRef), newAssocTypeQname);
            assocs = null; // reset assocs, so they would be recreated
        } else {
            addErrorMessage("nodeBrowser_custom_noSuchAssocTypeQName");
        }
    }

    private void addErrorMessage(String messageText) {
        // FIXME: kunagi hiljem võiks teha MessageUtil'i peale MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "nodeBrowser_custom_noSuchAssocTypeQName",
        // assocTypeQName);
        FacesContext context = FacesContext.getCurrentInstance();
        FacesMessage message = new FacesMessage();
        message.setSeverity(FacesMessage.SEVERITY_ERROR);
        message.setDetail(messageText);
        context.addMessage("searchForm:query", message);
    }

    /**
     * Action to select association From node
     * 
     * @return next action
     */
    public String selectFromNode() {
        AssociationRef assocRef = (AssociationRef) getFromAssocs().getRowData();
        setNodeRef(assocRef.getSourceRef());
        return "success";
    }

    public DataModel getFromAssocs() {
        List<AssociationRef> assocRefs = getNodeService().getSourceAssocs(getNodeRef(), RegexQNamePattern.MATCH_ALL);
        sourceAssocs = new ListDataModel(assocRefs);
        return sourceAssocs;
    }

    public String getPrimaryPathShort() {
        final String primaryPath = super.getPrimaryPath();
        int startURI = primaryPath.indexOf("{");
        int nextStartURI = primaryPath.indexOf("{");
        String primaryPathShort;
        if (startURI != -1) {
            primaryPathShort = primaryPath.substring(0, startURI);
            while (nextStartURI != -1) {
                final int endIndex = primaryPath.indexOf("}", nextStartURI);
                int oldNextStartURI = nextStartURI;
                nextStartURI = primaryPath.indexOf("{", endIndex);
                final String shortPathPart;
                if (nextStartURI != -1) {
                    shortPathPart = primaryPath.substring(oldNextStartURI, nextStartURI);
                } else {
                    shortPathPart = primaryPath.substring(oldNextStartURI);
                }
                primaryPathShort += QName.createQName(shortPathPart).toPrefixString(BeanHelper.getNamespaceService());
            }
        } else {
            primaryPathShort = primaryPath;
        }
        return ISO9075.decode(primaryPathShort.toString());
    }

    public void importACP(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("Node import started: " + getNodeRef());
        File dataFile = new File(getImportFileName());

        // setup an ACP Package Handler to export to an ACP file format
        ImportPackageHandler handler;
        ImporterService importerService = BeanHelper.getImporterService();

        Location location = new Location(getNodeRef());
        location.setChildAssocType(ContentModel.ASSOC_CHILDREN);
        ImporterBinding binding = null;
        ImportTimerProgress progress = new ImportTimerProgress(LOG);
        String fileName = dataFile.getName();
        if (fileName.endsWith(".acp")) {
            handler = new ACPImportPackageHandler(dataFile, CHARSET);
            // now export (note: we're not interested in progress in the example)
            importerService.importView(handler, location, binding, progress);
        } else if (fileName.endsWith(".xml")) {
            Reader fileReader = null;
            try {
                fileReader = new InputStreamReader(new FileInputStream(dataFile), AppConstants.CHARSET);
                importerService.importView(fileReader, location, binding, progress);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(fileReader);
            }
        } else {
            throw new RuntimeException("Extension of file is unknown. File: " + fileName);
        }
    }

    public void export(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("Node export started: " + getNodeRef());
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        try {
            String packageName = "export";
            File dataFile = new File(packageName);
            File contentDir = new File(packageName);

            outputStream = getExportOutStream(response, "export-" + FilenameUtil.buildFileName(getPrimaryPathShort(), "acp"));
            // setup an ACP Package Handler to export to an ACP file format
            ExportPackageHandler handler = new ACPExportPackageHandler(outputStream, dataFile, contentDir, BeanHelper.getMimetypeService());

            // now export (note: we're not interested in progress in the example)
            BeanHelper.getExporterService().exportView(handler, getExportParameters(), null);

            outputStream.flush();
        } catch (IOException e) {
            String msg = "Failed to export node: " + getNodeRef();
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            context.responseComplete();

            // hack for incorrect view id in the next request
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();

            LOG.info("Node export completed: " + getNodeRef());
        }
    }

    public void exportXml(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("Node export started: " + getNodeRef());
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        try {
            outputStream = getExportOutStream(response, "export-" + FilenameUtil.buildFileName(getPrimaryPathShort(), "xml"));
            // now export (note: we're not interested in progress in the example)
            getExporterService().exportView(outputStream, getExportParameters(), null);
            outputStream.flush();
        } catch (IOException e) {
            String msg = "Failed to export node: " + getNodeRef();
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            context.responseComplete();
            // hack for incorrect view id in the next request
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();
            LOG.info("Node export completed: " + getNodeRef());
        }
    }

    public static OutputStream getExportOutStream(HttpServletResponse response, String fileName) throws IOException {
        String ext = fileName.substring(FilenameUtils.indexOfExtension(fileName) + 1);
        String contentType = MimetypeMap.MIMETYPE_BINARY;
        if (!"acp".equalsIgnoreCase(ext)) {
            String type = BeanHelper.getMimetypeService().getMimetypesByExtension().get(ext);
            if (StringUtils.isNotBlank(type)) {
                contentType = type;
            }
        }
        response.setContentType(contentType);
        response.setHeader("Expires", "0");
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "public");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        return response.getOutputStream();
    }

    private ExporterCrawlerParameters getExportParameters() {
        ExporterCrawlerParameters parameters = new ExporterCrawlerParameters();
        parameters.setReferenceType(ReferenceType.NODEREF);
        parameters.setExportFrom(new Location(getNodeRef()));
        parameters.setCrawlSelf(true);
        return parameters;
    }

    public void delete(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("Node delete started: " + getNodeRef());
        getNodeService().deleteNode(getNodeRef());
        setNodeRef(getPrimaryParent());
    }

    public void setImportFileName(String importFileName) {
        this.importFileName = importFileName;
    }

    public String getImportFileName() {
        return importFileName;
    }

}
