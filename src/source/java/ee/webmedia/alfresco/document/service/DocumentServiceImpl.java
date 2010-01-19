package ee.webmedia.alfresco.document.service;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.QueryParser;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.type.model.DocumentType;
import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class DocumentServiceImpl implements DocumentService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentServiceImpl.class);

    private static final BeanPropertyMapper<Document> documentBeanPropertyMapper = BeanPropertyMapper.newInstance(Document.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private DocumentTypeService documentTypeService;
    private UserService userService;
    private AuthenticationService authenticationService;
    private RegisterService registerService;
    private VolumeService volumeService;
    private SeriesService seriesService;
    private SearchService searchService;

    // doesn't need to be synchronized, because it is not modified during runtime
    private Map<QName/* nodeType/nodeAspect */, PropertiesModifierCallback> creationPropertiesModifierCallbacks = new LinkedHashMap<QName, PropertiesModifierCallback>();

    @Override
    public Node getDocument(NodeRef nodeRef) {
        Node document = generalService.fetchNode(nodeRef);
        setTransientProperties(document, getAncestorNodesByDocument(nodeRef));
        return document;
    }

    @Override
    public NodeRef getDrafts() {
        return generalService.getNodeRef(DocumentCommonModel.Repo.DRAFTS_SPACE);
    }

    @Override
    public Node createDocument(QName documentTypeId) {
        // XXX do we need to check if document type is used?
        if (!dictionaryService.isSubClass(documentTypeId, DocumentCommonModel.Types.DOCUMENT)) {
            throw new RuntimeException("DocumentTypeId '" + documentTypeId.toPrefixString(namespaceService) + "' must be a subclass of '"
                    + DocumentCommonModel.Types.DOCUMENT.toPrefixString(namespaceService) + "'");
        }
        NodeRef parent = getDrafts();

        Set<QName> aspects = new HashSet<QName>();
        RepoUtil.getMandatoryAspects(dictionaryService.getType(documentTypeId), aspects);

        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        // first iterate over callbacks to be able to predict in which order callbacks will be called (that is registration order).
        for (QName callbackAspect : creationPropertiesModifierCallbacks.keySet()) {
            for (QName docAspect : aspects) {
                if (dictionaryService.isSubClass(docAspect, callbackAspect)) {
                    PropertiesModifierCallback callback = creationPropertiesModifierCallbacks.get(docAspect);
                    callback.doWithProperties(properties);
                }
            }
        }

        NodeRef document = nodeService.createNode(parent, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT, documentTypeId, properties)
                .getChildRef();
        return getDocument(document);
    }

    @Override
    public Node updateDocument(final Node node) {
        final NodeRef docNodeRef = node.getNodeRef();

        // Write document properties to repository
        // If owner is changed to another user, then after this call we don't have permissions any more to write document properties
        generalService.setPropertiesIgnoringSystem(docNodeRef, node.getProperties());

        // If document is updated for the first time, add SEARCHABLE aspect to document and it's children files.
        if (!nodeService.hasAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            nodeService.addAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
            for (ChildAssociationRef childRef : nodeService.getChildAssocs(docNodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL)) {
                nodeService.addAspect(childRef.getChildRef(), DocumentCommonModel.Aspects.SEARCHABLE, null);
            }
        }
        
        final String volumeNodeRef = (String) node.getProperties().get(TransientProps.VOLUME_NODEREF);
        final String caseNodeRef = (String) node.getProperties().get(TransientProps.CASE_NODEREF);
        final NodeRef targetParentRef;
        Node existingParentNode = null;
        if (StringUtils.isNotBlank(caseNodeRef)) {
            targetParentRef = new NodeRef(caseNodeRef);
            existingParentNode = getCaseByDocument(docNodeRef);
        } else {
            targetParentRef = new NodeRef(volumeNodeRef);
            final Volume volume = volumeService.getVolumeByNodeRef(targetParentRef);
            if (volume.isContainsCases()) {
                throw new RuntimeException("Selected volume '" + volume.getTitle() + "' must contain cases, not directly documents. Invalid caseNodeRef: '"
                        + caseNodeRef + "'");
            }
            existingParentNode = getVolumeByDocument(docNodeRef);
        }
        if (existingParentNode == null || !targetParentRef.equals(existingParentNode.getNodeRef())) {
            // was not saved (under volume nor case) or saved, but parent (volume or case) must be changed
            try {
                // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
                final NodeRef newDocLocationRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                    @Override
                    public NodeRef doWork() throws Exception {
                        return nodeService.moveNode(docNodeRef, targetParentRef //
                                , DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT).getChildRef();
                    }
                }, AuthenticationUtil.getSystemUserName());
                return getDocument(newDocLocationRef);
            } catch (Exception e) {
                final String msg = "Failed to move document to volumes folder";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        return node;
    }

    private static Set<String> ignoredPropertiesWhenMakingCopy = new HashSet<String>();
    static {
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.REG_NUMBER.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.REG_DATE_TIME.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_EMAIL.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_ID.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_JOB_TITLE.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_NAME.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.toString());
        ignoredPropertiesWhenMakingCopy.add(DocumentCommonModel.Props.OWNER_PHONE.toString());
    }

    @Override
    public Node copyDocument(NodeRef nodeRef) {
        Node doc = getDocument(nodeRef);
        Node copiedDoc = createDocument(doc.getType());

        // PROPERTIES
        for (Map.Entry<String, Object> prop : doc.getProperties().entrySet()) {
            if (!ignoredPropertiesWhenMakingCopy.contains(prop.getKey())) {
                copiedDoc.getProperties().put(prop.getKey(), prop.getValue());
            }
        }

        // ANCESTORS
        setTransientProperties(copiedDoc, getAncestorNodesByDocument(doc.getNodeRef()));
        // DEFAULT VALUES
        copiedDoc.getProperties().put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.WORKING.getValueName());

        if (log.isDebugEnabled())
            log.debug("Copied document: " + copiedDoc.toString());
        return copiedDoc;
    }

    @Override
    public void setTransientProperties(Node document, DocumentParentNodesVO documentParentNodesVO) {
        Node functionNode = documentParentNodesVO.getFunctionNode();
        Node seriesNode = documentParentNodesVO.getSeriesNode();
        Node volumeNode = documentParentNodesVO.getVolumeNode();
        Node caseNode = documentParentNodesVO.getCaseNode();

        // put props with empty values if missing, otherwise use existing values
        final Map<String, Object> props = document.getProperties();
        props.put(TransientProps.FUNCTION_NODEREF, functionNode != null ? functionNode.getNodeRef().toString() : null);
        props.put(TransientProps.SERIES_NODEREF, seriesNode != null ? seriesNode.getNodeRef().toString() : null);
        props.put(TransientProps.VOLUME_NODEREF, volumeNode != null ? volumeNode.getNodeRef().toString() : null);
        props.put(TransientProps.CASE_NODEREF, caseNode != null ? caseNode.getNodeRef().toString() : null);

        // add labels
        String volumeLbl = volumeNode != null ? volumeNode.getProperties().get(VolumeModel.Props.MARK).toString() //
                + " " + volumeNode.getProperties().get(VolumeModel.Props.TITLE).toString() : " ";
        String seriesLbl = seriesNode != null ? seriesNode.getProperties().get(SeriesModel.Props.SERIES_IDENTIFIER).toString() //
                + " " + seriesNode.getProperties().get(SeriesModel.Props.TITLE).toString() : " ";
        String functionLbl = functionNode != null ? functionNode.getProperties().get(FunctionsModel.Props.MARK).toString() //
                + " " + functionNode.getProperties().get(FunctionsModel.Props.TITLE).toString() : " ";
        props.put(TransientProps.FUNCTION_LABEL, functionLbl);
        props.put(TransientProps.SERIES_LABEL, seriesLbl);
        props.put(TransientProps.VOLUME_LABEL, volumeLbl);
    }

    @Override
    public void deleteDocument(NodeRef nodeRef) {
        log.debug("Deleting document: " + nodeRef);
        nodeService.deleteNode(nodeRef);
    }

    @Override
    public boolean isMetadataEditAllowed(NodeRef nodeRef) {
        if (userService.isDocumentManager()) {
            return true;
        }
        String ownerId = (String) nodeService.getProperty(nodeRef, DocumentCommonModel.Props.OWNER_ID);
        return authenticationService.getCurrentUserName().equals(ownerId);
    }

    @Override
    public List<Document> getAllDocumentsByVolume(NodeRef volumeRef) {
        List<ChildAssociationRef> volumeAssocs = nodeService.getChildAssocs(volumeRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        List<Document> docsOfVolume = new ArrayList<Document>(volumeAssocs.size());
        for (ChildAssociationRef volume : volumeAssocs) {
            docsOfVolume.add(getDocumentByNodeRef(volume.getChildRef(), volumeRef));
        }
        return docsOfVolume;
    }

    @Override
    public List<Document> getAllDocumentsByCase(NodeRef caseRef) {
        List<ChildAssociationRef> caseAssocs = nodeService.getChildAssocs(caseRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        List<Document> docsOfCase = new ArrayList<Document>(caseAssocs.size());
        for (ChildAssociationRef caseAssocRef : caseAssocs) {
            final Node doc = getDocument(caseAssocRef.getChildRef());
            docsOfCase.add(getDocumentByNodeRef(doc.getNodeRef(), caseRef));
        }
        return docsOfCase;
    }

    private Document getDocumentByNodeRef(NodeRef docRef, NodeRef volRef) {
        Document doc = documentBeanPropertyMapper.toObject(nodeService.getProperties(docRef));
        if (volRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(docRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Volume is expected to have only one parent series, but got " + parentAssocs.size() + " matching the criteria.");
            }
            volRef = parentAssocs.get(0).getParentRef();
        }
        final Node documentNode = getDocument(docRef);
        doc.setVolumeNodeRef(volRef);
        doc.setNode(documentNode);
        doc.setDocumentType(documentTypeService.getDocumentType(documentNode.getType()));
        if (log.isDebugEnabled()) {
            log.debug("Document: " + doc);
        }
        return doc;
    }

    @Override
    public void addPropertiesModifierCallback(QName qName, PropertiesModifierCallback propertiesModifierCallback) {
        this.creationPropertiesModifierCallbacks.put(qName, propertiesModifierCallback);
    }

    @Override
    public Node getVolumeByDocument(NodeRef nodeRef) {
        return getVolumeByDocument(nodeRef, null);
    }

    @Override
    public Node getCaseByDocument(NodeRef nodeRef) {
        return generalService.getParentWithType(nodeRef, CaseModel.Types.CASE);
    }

    @Override
    public DocumentParentNodesVO getAncestorNodesByDocument(NodeRef docRef) {
        final Node caseRef = getCaseByDocument(docRef);
        Node volumeNode = getVolumeByDocument(docRef, caseRef);
        Node seriesNode = volumeNode != null ? getSeriesByVolume(volumeNode.getNodeRef()) : null;
        Node functionNode = seriesNode != null ? getFunctionBySeries(seriesNode.getNodeRef()) : null;
        return new DocumentParentNodesVO(functionNode, seriesNode, volumeNode, caseRef);
    }

    private Node getFunctionBySeries(NodeRef seriesRef) {
        return seriesRef == null ? null : generalService.getParentWithType(seriesRef, FunctionsModel.Types.FUNCTION);
    }

    private Node getSeriesByVolume(NodeRef volumeRef) {
        return volumeRef == null ? null : generalService.getParentWithType(volumeRef, SeriesModel.Types.SERIES);
    }

    private Node getVolumeByDocument(NodeRef docRef, Node caseNode) {
        final NodeRef parentRef;
        if (caseNode != null) {
            parentRef = caseNode.getNodeRef();
        } else {
            parentRef = docRef;
        }
        return generalService.getParentWithType(parentRef, VolumeModel.Types.VOLUME);
    }

    @Override
    public boolean isSaved(NodeRef nodeRef) {
        final Node parentVolume = getVolumeByDocument(nodeRef);
        return parentVolume != null ? true : null != generalService.getParentWithType(nodeRef, CaseModel.Types.CASE);
    }

    @Override
    public Node registerDocument(Node docNode) {
        DocumentParentNodesVO parentNodes = getAncestorNodesByDocument(docNode.getNodeRef());
        Node caseNode = parentNodes.getCaseNode();// TODO: dokumendi asja alla registreerimsiel...
        Node volumeNode = parentNodes.getVolumeNode();
        Node seriesNode = parentNodes.getSeriesNode();
        Node functionNode = parentNodes.getFunctionNode();
        // update counter for this register
        final String volumeMark = volumeService.getVolumeByNodeRef(volumeNode.getNodeRef()).getVolumeMark();
        final Series series = seriesService.getSeriesByNodeRef(seriesNode.getNodeRef());
        final Map<String, Object> serProps = series.getNode().getProperties();
        Integer registerId = (Integer) serProps.get(SeriesModel.Props.REGISTER.toString());
        boolean individualizingNumbers = (Boolean) serProps.get(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString());
        final int incrementedCount = registerService.increaseCount(registerId);
        Register register = registerService.getRegister(registerId);
        register.setCounter(incrementedCount);
        // compose regNumber
        String regNumber = volumeMark + "/" + register.getPrefix() + (register.getCounter()) + register.getSuffix();
        if (individualizingNumbers) {
            regNumber += "-1";
        }
        final Map<String, Object> props = docNode.getProperties();
        props.put(DocumentCommonModel.Props.REG_NUMBER.toString(), regNumber);
        props.put(DocumentCommonModel.Props.REG_DATE_TIME.toString(), new Date());
        // transient props
        props.put(TransientProps.FUNCTION_NODEREF, functionNode.getNodeRef().toString());
        props.put(TransientProps.SERIES_NODEREF, seriesNode.getNodeRef().toString());
        props.put(TransientProps.VOLUME_NODEREF, volumeNode.getNodeRef().toString());
        props.put(TransientProps.CASE_NODEREF, caseNode.getNodeRef().toString());

        final DocumentType documentType = documentTypeService.getDocumentType(docNode.getType());
        if (!documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
            props.put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
        }
        // update register and document
        registerService.updatePropertiesFromObject(register);
        return updateDocument(docNode);
    }

    @Override
    public List<Document> searchDocumentsQuick(String searchString) {
        long startTime = System.currentTimeMillis();
        List<Document> result = new ArrayList<Document>();

        // Escape symbols and use only 10 first unique words which contain at least 3 characters
        List<String> searchWords = new ArrayList<String>();
        if (StringUtils.isNotBlank(searchString)) {
            searchString = replaceCustom(searchString, " ");
            for (String searchWord : searchString.split("\\s")) {
                if (searchWord.length() >= 3 && searchWords.size() < 10) {
                    searchWord = stripCustom(searchWord);
                    if (searchWord.length() >= 3 && searchWords.size() < 10) {
                        searchWord = QueryParser.escape(searchWord);
                        boolean exists = false;
                        for (String tmpWord : searchWords) {
                            exists |= tmpWord.equalsIgnoreCase(searchWord);
                        }
                        if (!exists) {
                            searchWords.add(searchWord);
                        }
                    }
                }
            }
        }
        log.info("getDocumentsQuickSearch - words: " + searchWords.toString() + ", from string: " + searchString);
        if (searchWords.isEmpty()) {
            return result;
        }

        // Fetch a list of all the properties from document type and it's subtypes.
        List<QName> searchProperties = new ArrayList<QName>();
        List<QName> searchPropertiesDate = new ArrayList<QName>();
        Collection<QName> docProperties = dictionaryService.getProperties(DocumentCommonModel.MODEL);
        docProperties.addAll(dictionaryService.getProperties(DocumentSpecificModel.MODEL));
        for (QName property : docProperties) {
            PropertyDefinition propDef = dictionaryService.getProperty(property);
            QName type = propDef.getDataType().getName();
            if (type.equals(DataTypeDefinition.TEXT) || type.equals(DataTypeDefinition.INT) || type.equals(DataTypeDefinition.LONG) ||
                    type.equals(DataTypeDefinition.FLOAT) || type.equals(DataTypeDefinition.DOUBLE)) {
                searchProperties.add(property);
            } else if (type.equals(DataTypeDefinition.DATE) || type.equals(DataTypeDefinition.DATETIME)) {
                searchPropertiesDate.add(property);
            }
        }

        /*
         * Construct a query with following structure:
         * (TYPE:document AND (@prop1:*word1* OR @prop2:*word1*) AND (@prop1:*word2* OR @prop2:*word2*)) OR
         * (ASPECT:file AND (@name:*word1* OR @content:*word1*) AND (@name:*word2* OR @content:*word2*))
         */

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        format.setLenient(false);
        SimpleDateFormat luceneFormat = new SimpleDateFormat("yyyy-MM-dd");

        StringBuilder query = new StringBuilder();
        query.append("ASPECT:").append(Repository.escapeQName(DocumentCommonModel.Aspects.SEARCHABLE)).append(" AND (");
        query.append("(TYPE:").append(Repository.escapeQName(DocumentCommonModel.Types.DOCUMENT));
        for (String searchWord : searchWords) {
            if (StringUtils.isNotBlank(searchWord)) {
                query.append(" AND (");
                String separator = "";
                for (QName property : searchProperties) {
                    query.append(separator).append("@").append(Repository.escapeQName(property)).append(":\"*").append(searchWord).append("*\"");
                    separator = " OR ";
                }
                Date tmpDate = null;
                try {
                    tmpDate = format.parse(searchWord);
                } catch (ParseException e) {
                }
                // if it's a date match, then also add date properties
                if (tmpDate != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("getDocumentsQuickSearch - found date match: " + searchWord + " -> " + luceneFormat.format(tmpDate));
                    }
                    for (QName property : searchPropertiesDate) {
                        // format is like dateProp:"2010-01-08T00:00:00.000"
                        query.append(separator).append("@").append(Repository.escapeQName(property)).append(":\"").append(luceneFormat.format(tmpDate)).append(
                                "T00:00:00.000\"");
                        separator = " OR ";
                    }
                }
                query.append(")");
            }
        }
        query.append(") OR (ASPECT:").append(Repository.escapeQName(DocumentCommonModel.Aspects.FILE));
        for (String searchWord : searchWords) {
            if (StringUtils.isNotBlank(searchWord)) {
                query.append(" AND (@").append(Repository.escapeQName(ContentModel.PROP_NAME)).append(":\"*").append(searchWord).append("*\"");
                query.append(" OR @").append(Repository.escapeQName(ContentModel.PROP_CONTENT)).append(":\"*").append(searchWord).append("*\")");
            }
        }
        query.append(")");

        // if (log.isDebugEnabled()) {
        // log.debug("QUERY:\n" + query.toString() + "\n\n");
        // }

        // build up the search parameters
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(query.toString());
        sp.addStore(generalService.getStore());
        sp.setLimit(200); // only 100 unique documents used but limit set to 200 to allow room for duplicates
        sp.setLimitBy(LimitBy.FINAL_SIZE);
        long queryStart = System.currentTimeMillis();
        ResultSet resultSet = searchService.query(sp);
        try {
            if (log.isDebugEnabled()) {
                log.debug("getDocumentsQuickSearch - query time " + (System.currentTimeMillis() - queryStart) + "ms");
                log.debug("Quick search results: " + resultSet.length());
                queryStart = System.currentTimeMillis();
            }

            List<String> nodeIds = new ArrayList<String>();
            for (ResultSetRow row : resultSet) {
                NodeRef nodeRef = row.getNodeRef();
                NodeRef parRef = row.getChildAssocRef().getParentRef();

                // If node is document then add directly, otherwise it's a file and we can add it's parent
                if (dictionaryService.isSubClass(nodeService.getType(nodeRef), DocumentCommonModel.Types.DOCUMENT)) {
                    // Check for duplicate documents (when multiple files match in the same document
                    if (!nodeIds.contains(nodeRef.getId())) {
                        result.add(getDocumentByNodeRef(nodeRef, null));
                        nodeIds.add(nodeRef.getId());
                    }
                } else {
                    if (!nodeIds.contains(parRef.getId())) {
                        result.add(getDocumentByNodeRef(parRef, null));
                        nodeIds.add(parRef.getId());
                    }
                }

                // Only use first 100 distinct results
                if (result.size() >= 100) {
                    break;
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("getDocumentsQuickSearch - result time " + (System.currentTimeMillis() - queryStart) + "ms");
            }
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("getDocumentsQuickSearch - total time " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return result;
    }

    /**
     * Replace a custom set of characters that have special meaning in Lucene query, others need to be replaced outside of this method.
     * 
     * Compared to default set we don't replace +, -, &
     * 
     * @see QueryParser#escape(String) for default set
     */
    private static String replaceCustom(String s, String replacement) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{'
                    || c == '}' || c == '~' || c == '*' || c == '?' || c == '|') {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Not so beautiful workaround for the problem where special symbols like +, -, _, / etc cause a problem with 
     * Alfresco lucene query parser and break the query. 
     * 
     * For example *11-21/344* will find the correct results, *11-21* will also find the correct results 
     * but *11-21/* will be replaced with *11-21 (missing the end wildcard) and will not find the correct results.   
     */
    private static String stripCustom(String s) {
        int firstChar = 0;
        int lastChar = s.length();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetterOrDigit(s.charAt(i))) {
                firstChar = i;
                break;
            }
        }
        for (int i = s.length(); i > 0; i--) {
            if (Character.isLetterOrDigit(s.charAt(i - 1))) {
                lastChar = i;
                break;
            }
        }
        if (lastChar > firstChar) {
            return s.substring(firstChar, lastChar);
        }
        return "";
    }
    
    // START: getters / setters

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setDocumentTypeService(DocumentTypeService documentTypeService) {
        this.documentTypeService = documentTypeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
    // END: getters / setters

}
