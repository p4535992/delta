package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocLockService;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * This updater combines document updates from DocumentChangedTypePropertiesUpdater, DocumentAccessRestrictionUpdater and LogAndDeleteObjectsWithMissingType.
 * It is assumed that the updater is executed from nodeBrower as background job, so check for lock
 * and resolving possibly changed nodeRefs is added.
 * 
 * @author Riina Tens
 */
public class DocumentUpdater2 extends AbstractNodeUpdater {

    private Set<NodeRef> documentChangedTypePropertiesUpdaterRefs;
    private Set<NodeRef> documentAccessRestrictionUpdaterRefs;
    private Set<NodeRef> logAndDeleteObjectsWithMissingTypeRefs;
    private DocumentChangedTypePropertiesUpdater documentChangedTypePropertiesUpdater;
    private DocumentAccessRestrictionUpdater documentAccessRestrictionUpdater;
    private LogAndDeleteObjectsWithMissingType logAndDeleteObjectsWithMissingType;
    private DocLockService docLockService;

    @Override
    public boolean isContinueWithNextBatchAfterError() {
        return true;
    }

    @Override
    protected boolean processOnlyExistingNodeRefs() {
        // as this updater is executed on background possibly repeatedly, original nodeRefs may be moved
        // so it is needed to try to resolve not existing nodeRefs by id
        return false;
    }

    @Override
    protected void executeUpdater() throws Exception {
        // if getNodeLoadingResultSet has not been called, we need to load reference nodeRefs from updaters
        if (documentChangedTypePropertiesUpdaterRefs == null) {
            documentChangedTypePropertiesUpdaterRefs = loadNodesFromFile(new File(inputFolder, getDocumentChangedTypeFileName()), false);
        }
        if (documentAccessRestrictionUpdaterRefs == null) {
            documentAccessRestrictionUpdaterRefs = loadNodesFromFile(new File(inputFolder, getDocumentAccessRestrictionFileName()), false);
        }
        if (logAndDeleteObjectsWithMissingTypeRefs == null) {
            logAndDeleteObjectsWithMissingTypeRefs = loadNodesFromFile(new File(inputFolder, getObjectsWithMissingTypeFileName()), false);
        }
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<ResultSet> documentChangedTypePropertiesUpdaterResults = documentChangedTypePropertiesUpdater.getNodeLoadingResultSet();
        if (documentChangedTypePropertiesUpdaterResults == null) {
            documentChangedTypePropertiesUpdaterResults = new ArrayList<ResultSet>();
        }
        List<ResultSet> documentAccessRestrictionUpdaterResults = documentAccessRestrictionUpdater.getNodeLoadingResultSet();
        if (documentAccessRestrictionUpdaterResults == null) {
            documentAccessRestrictionUpdaterResults = new ArrayList<ResultSet>();
        }
        List<ResultSet> logAndDeleteObjectsWithMissingTypeResults = logAndDeleteObjectsWithMissingType.getNodeLoadingResultSet();
        if (logAndDeleteObjectsWithMissingTypeResults == null) {
            logAndDeleteObjectsWithMissingTypeResults = new ArrayList<ResultSet>();
        }

        documentChangedTypePropertiesUpdaterRefs = getNodeRefs(documentChangedTypePropertiesUpdaterResults);
        documentAccessRestrictionUpdaterRefs = getNodeRefs(documentAccessRestrictionUpdaterResults);
        logAndDeleteObjectsWithMissingTypeRefs = getNodeRefs(logAndDeleteObjectsWithMissingTypeResults);

        writeNodesToFile(new File(inputFolder, getDocumentChangedTypeFileName()), documentChangedTypePropertiesUpdaterRefs, log);
        writeNodesToFile(new File(inputFolder, getDocumentAccessRestrictionFileName()), documentAccessRestrictionUpdaterRefs, log);
        writeNodesToFile(new File(inputFolder, getObjectsWithMissingTypeFileName()), logAndDeleteObjectsWithMissingTypeRefs, log);
        List<ResultSet> allResults = new ArrayList<ResultSet>();

        allResults.addAll(documentChangedTypePropertiesUpdaterResults);
        allResults.addAll(documentAccessRestrictionUpdaterResults);
        allResults.addAll(logAndDeleteObjectsWithMissingTypeResults);

        return allResults;
    }

    private String getDocumentChangedTypeFileName() {
        return getBaseFileName() + "DocumentChangedType.csv";
    }

    private String getDocumentAccessRestrictionFileName() {
        return getBaseFileName() + "DocumentAccessRestriction.csv";
    }

    private String getObjectsWithMissingTypeFileName() {
        return getBaseFileName() + "ObjectsWithMissingType.csv";
    }

    private Set<NodeRef> getNodeRefs(List<ResultSet> resultSets) {
        Set<NodeRef> nodeRefs = new HashSet<NodeRef>();
        for (ResultSet resultSet : resultSets) {
            nodeRefs.addAll(resultSet.getNodeRefs());
        }
        return nodeRefs;
    }

    private NodeRef resolveNodeRef(NodeRef originalNodeRef) {
        return generalService.getExistingNodeRefAllStores(originalNodeRef.getId());
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        if (!(nodeService.exists(nodeRef))) {
            nodeRef = resolveNodeRef(nodeRef);
            if (nodeRef == null) {
                return new String[] { "Not existing node" };
            }
        }
        QName type = nodeService.getType(nodeRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "Type is not document, but " + type + ", skipping node" };
        }
        if (docLockService.isLockByOther(nodeRef)) {
            // throw exception to skip locked node and retry on next execution
            throw new NodeLockedException(nodeRef);
        }
        List<String> results = new ArrayList<String>();
        results.add("Updating");
        DocumentDynamic document = null;
        String accessRestriction = null;
        String documentTypeId = null;

        boolean updateDocumentChangedTypeProps = documentChangedTypePropertiesUpdaterRefs.contains(nodeRef);
        boolean isSeriesChild = false;
        if (updateDocumentChangedTypeProps) {
            isSeriesChild = generalService.getAncestorNodeRefWithType(nodeRef, SeriesModel.Types.SERIES) == null;
        }
        if (updateDocumentChangedTypeProps && isSeriesChild) {
            // need to load whole document
            document = BeanHelper.getDocumentDynamicService().getDocument(nodeRef);
            accessRestriction = document.getAccessRestriction();
            documentTypeId = document.getDocumentTypeId();
        } else {
            // need only properties
            Map<QName, Serializable> documentProps = nodeService.getProperties(nodeRef);
            accessRestriction = (String) documentProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
            documentTypeId = (String) documentProps.get(Props.OBJECT_TYPE_ID);
        }

        if (updateDocumentChangedTypeProps) {
            if (isSeriesChild) {
                String[] updateNodeResult = documentChangedTypePropertiesUpdater.updateNode(nodeRef, document);
                addResult(results, updateNodeResult);
            } else {
                results.add(documentChangedTypePropertiesUpdater.getNotSeriesChildMsg(nodeRef));
            }
        } else {
            results.add("Not present in documentChangedTypePropertiesUpdaterRefs");
        }
        if (documentAccessRestrictionUpdaterRefs.contains(nodeRef)) {
            String[] updateNodeResult = documentAccessRestrictionUpdater.updateNode(nodeRef, accessRestriction);
            addResult(results, updateNodeResult);
        } else {
            results.add("Not present in documentAccessRestrictionUpdaterRefs");
        }
        if (logAndDeleteObjectsWithMissingTypeRefs.contains(nodeRef)) {
            String[] updateNodeResult = logAndDeleteObjectsWithMissingType.updateNode(nodeRef, type, documentTypeId);
            addResult(results, updateNodeResult);
        } else {
            results.add("Not present in logAndDeleteObjectsWithMissingTypeRefs");
        }
        return results.toArray(new String[results.size()]);
    }

    private void addResult(List<String> results, String[] updateNodeResult) {
        if (updateNodeResult != null) {
            results.addAll(Arrays.asList(updateNodeResult));
        } else {
            results.add("");
        }
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "action", "documentChangedTypePropertiesUpdater result", "documentAccessRestrictionUpdater result",
                "logAndDeleteObjectsWithMissingType result" };
    }

    public void setDocumentChangedTypePropertiesUpdater(DocumentChangedTypePropertiesUpdater documentChangedTypePropertiesUpdater) {
        this.documentChangedTypePropertiesUpdater = documentChangedTypePropertiesUpdater;
    }

    public void setDocumentAccessRestrictionUpdater(DocumentAccessRestrictionUpdater documentAccessRestrictionUpdater) {
        this.documentAccessRestrictionUpdater = documentAccessRestrictionUpdater;
    }

    public void setLogAndDeleteObjectsWithMissingType(LogAndDeleteObjectsWithMissingType logAndDeleteObjectsWithMissingType) {
        this.logAndDeleteObjectsWithMissingType = logAndDeleteObjectsWithMissingType;
    }

    public void setDocLockService(DocLockService docLockService) {
        this.docLockService = docLockService;
    }

}
