package ee.webmedia.alfresco.adr.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class AdrDocNameUpdater extends AbstractNodeUpdater {

    private DocumentAdminService documentAdminService;
    private BulkLoadNodeService bulkLoadNodeService;

    private String endDateStr;
    private Map<String, Boolean> docTypesCache;
    private Map<String, String> docTypeIdToName;
    private Map<NodeRef, Node> batchNodes;

    private static final Set<QName> DOC_TYPE_PROPS = new HashSet<>(Arrays.asList(DocumentAdminModel.Props.OBJECT_TYPE_ID, DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR));

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Date endDate = null;
        try {
            endDateStr = StringUtils.trimToEmpty(endDateStr);
            endDate = new SimpleDateFormat("dd.MM.yyyy").parse(endDateStr);
        } catch (ParseException e) {
            log.error("Cannot start updater because expected end date in format 'dd.MM.yyyy' but got '" + endDateStr + "'");
            return null;
        }

        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                generatePropertyNullQuery(DocumentDynamicModel.Props.DOC_NAME_ADR),
                joinQueryPartsOr(
                        generatePropertyExactQuery(DocumentDynamicModel.Props.PUBLISH_TO_ADR, PublishToAdr.TO_ADR.getValueName()),
                        generatePropertyExactQuery(DocumentDynamicModel.Props.PUBLISH_TO_ADR, PublishToAdr.REQUEST_FOR_INFORMATION.getValueName())),
                generateDatePropertyRangeQuery(null, endDate, org.alfresco.model.ContentModel.PROP_CREATED)
                );

        List<ResultSet> result = new ArrayList<>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected void initializeBeforeUpdating() throws Exception {
        super.initializeBeforeUpdating();
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        Set<QName> propsToLoad = new HashSet<>();
        propsToLoad.add(DocumentAdminModel.Props.NAME);
        propsToLoad.add(DocumentAdminModel.Props.ID);
        NodeRef docTypesRoot = generalService.getNodeRef(DocumentAdminModel.Repo.DOCUMENT_TYPES_SPACE);

        final Map<String, String> props = new HashMap<>();
        bulkLoadNodeService.loadChildNodes(docTypesRoot, propsToLoad, DocumentAdminModel.Types.DOCUMENT_TYPE, null,
                new CreateObjectCallback<Void>() {
                    @Override
                    public Void create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                        props.put((String) properties.get(DocumentAdminModel.Props.ID), (String) properties.get(DocumentAdminModel.Props.NAME));
                        return null;
                    }
                });

        docTypeIdToName = props;
        docTypesCache = new HashMap<>();
    }

    @Override
    protected void doBeforeBatchUpdate(List<NodeRef> batchList) {
        if (CollectionUtils.isEmpty(batchList)) {
            return;
        }
        batchNodes = bulkLoadNodeService.loadNodes(batchList, DOC_TYPE_PROPS);
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        Node docNode = batchNodes.get(docRef);
        if (docNode == null) {
            log.warn("Did not find node from database: " + docRef);
            return null;
        }
        Map<String, Object> props = docNode.getProperties();
        String typeId = (String) props.get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        Integer versionNr = (Integer) props.get(DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR);
        if (typeId == null || versionNr == null) {
            log.warn("Unable to update document " + docRef);
            return null;
        }
        Boolean hasDocNameAdrField = docTypesCache.get(typeId + "-" + versionNr);
        if (hasDocNameAdrField == null) {
            Pair<DocumentType, DocumentTypeVersion> typeAndVersion = documentAdminService.getDocumentTypeAndVersion(typeId, versionNr, false);
            DocumentTypeVersion version = typeAndVersion.getSecond();
            Collection<Field> fields = version.getFieldsById(Collections.singleton(DocumentDynamicModel.Props.DOC_NAME_ADR.getLocalName()));
            hasDocNameAdrField = CollectionUtils.isNotEmpty(fields) ? true : false;
            docTypesCache.put(typeId + "-" + versionNr, hasDocNameAdrField);
        }
        if (!hasDocNameAdrField) {
            log.info("Document " + docRef + " (type: " + typeId + ", ver: " + versionNr + ") does not have docNameAdr field, skipping");
            return null;
        }
        String docTypeName = docTypeIdToName.get(typeId);
        nodeService.setProperty(docRef, DocumentDynamicModel.Props.DOC_NAME_ADR, docTypeName);

        return new String[] { docTypeName };
    }

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        resetFields();
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

    private void resetFields() {
        endDateStr = null;
        docTypesCache = null;
        docTypeIdToName = null;
        batchNodes = null;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public String getEndDateStr() {
        return endDateStr;
    }

    public void setEndDateStr(String endDateStr) {
        this.endDateStr = endDateStr;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
