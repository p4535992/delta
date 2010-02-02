package ee.webmedia.alfresco.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

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
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Alar Kvell
 */
public class DocumentServiceImpl implements DocumentService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentServiceImpl.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private DocumentTypeService documentTypeService;
    private RegisterService registerService;
    private VolumeService volumeService;
    private SeriesService seriesService;
    private SearchService searchService;
    private FileFolderService fileFolderService;
    private ContentService contentService;
    private String fromDvkXPath;

    private String defaultStore;
    private String incomingEmailPath;

    // doesn't need to be synchronized, because it is not modified during runtime
    private Map<QName/* nodeType/nodeAspect */, PropertiesModifierCallback> creationPropertiesModifierCallbacks = new LinkedHashMap<QName, PropertiesModifierCallback>();

    private static final String REGISTRATION_INDIVIDUALIZING_NUM_SUFFIX = "-1";

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
        return createDocument(documentTypeId, null, null);
    }

    @Override
    public Node createDocument(QName documentTypeId, NodeRef parentRef, Map<QName, Serializable> properties) {

        // XXX do we need to check if document type is used?
        if (!dictionaryService.isSubClass(documentTypeId, DocumentCommonModel.Types.DOCUMENT)) {
            throw new RuntimeException("DocumentTypeId '" + documentTypeId.toPrefixString(namespaceService) + "' must be a subclass of '"
                    + DocumentCommonModel.Types.DOCUMENT.toPrefixString(namespaceService) + "'");
        }
        if (parentRef == null) {
            parentRef = getDrafts();
        }
        if (properties == null) {
            properties = new HashMap<QName, Serializable>();
        }

        Set<QName> aspects = new HashSet<QName>();
        RepoUtil.getMandatoryAspects(dictionaryService.getType(documentTypeId), aspects);

        // first iterate over callbacks to be able to predict in which order callbacks will be called (that is registration order).
        for (QName callbackAspect : creationPropertiesModifierCallbacks.keySet()) {
            for (QName docAspect : aspects) {
                if (dictionaryService.isSubClass(docAspect, callbackAspect)) {
                    PropertiesModifierCallback callback = creationPropertiesModifierCallbacks.get(docAspect);
                    callback.doWithProperties(properties);
                }
            }
        }

        NodeRef document = nodeService.createNode(parentRef, DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT //
                , documentTypeId, properties).getChildRef();
        return getDocument(document);
    }

    @Override
    public Node updateDocument(final Node docNode) {
        final NodeRef docNodeRef = docNode.getNodeRef();
        final Map<String, Object> docProps = docNode.getProperties();
        // Prepare existingParentNode and targetParentRef properties
        final String volumeNodeRef = (String) docProps.get(TransientProps.VOLUME_NODEREF);
        final String caseNodeRef = (String) docProps.get(TransientProps.CASE_NODEREF);
        final NodeRef targetParentRef;
        Node existingParentNode = null;
        if (StringUtils.isNotBlank(caseNodeRef)) {
            targetParentRef = new NodeRef(caseNodeRef);
            existingParentNode = getCaseByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from volume to case?
                existingParentNode = getVolumeByDocument(docNodeRef);
            }
        } else {
            targetParentRef = new NodeRef(volumeNodeRef);
            final Volume volume = volumeService.getVolumeByNodeRef(targetParentRef);
            if (volume.isContainsCases()) {
                throw new RuntimeException("Selected volume '" + volume.getTitle() + "' must contain cases, not directly documents. Invalid caseNodeRef: '"
                        + caseNodeRef + "'");
            }
            existingParentNode = getVolumeByDocument(docNodeRef);
            if (existingParentNode == null) { // moving from case to volume?
                existingParentNode = getCaseByDocument(docNodeRef);
            }
        }

        // Prepare series and function properties
        NodeRef series = nodeService.getPrimaryParent(new NodeRef(volumeNodeRef)).getParentRef();
        if (series == null) {
            throw new RuntimeException("Volume parent is null: " + volumeNodeRef);
        }
        QName seriesType = nodeService.getType(series);
        if (!seriesType.equals(SeriesModel.Types.SERIES)) {
            throw new RuntimeException("Volume parent is not series, but " + seriesType + " - " + series);
        }
        NodeRef function = nodeService.getPrimaryParent(series).getParentRef();
        if (function == null) {
            throw new RuntimeException("Series parent is null: " + series);
        }
        QName functionType = nodeService.getType(function);
        if (!functionType.equals(FunctionsModel.Types.FUNCTION)) {
            throw new RuntimeException("Series parent is not function, but " + functionType + " - " + function);
        }
        docProps.put(DocumentCommonModel.Props.FUNCTION.toString(), function);
        docProps.put(DocumentCommonModel.Props.SERIES.toString(), series);

        // If document is updated for the first time, add SEARCHABLE aspect to document and it's children files.
        if (!nodeService.hasAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
            nodeService.addAspect(docNodeRef, DocumentCommonModel.Aspects.SEARCHABLE, null);
            docProps.put(DocumentCommonModel.Props.FILE_NAMES.toString(), getSearchableFileNames(docNodeRef));
            docProps.put(DocumentCommonModel.Props.FILE_CONTENTS.toString(), getSearchableFileContents(docNodeRef));
        }

        // Write document properties to repository
        // If owner is changed to another user, then after this call we don't have permissions any more to write document properties
        generalService.setPropertiesIgnoringSystem(docNodeRef, docProps);

        if (existingParentNode == null || !targetParentRef.equals(existingParentNode.getNodeRef())) {
            // was not saved (under volume nor case) or saved, but parent (volume or case) must be changed
            
            try {
                // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
                AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
                    @Override
                    public NodeRef doWork() throws Exception {
                        NodeRef newDocNodeRef = nodeService.moveNode(docNodeRef, targetParentRef //
                                , DocumentCommonModel.Assocs.DOCUMENT, DocumentCommonModel.Assocs.DOCUMENT).getChildRef();
                        if (!newDocNodeRef.equals(docNodeRef)) {
                            throw new RuntimeException("NodeRef changed while moving");
                        }
                        return null;
                    }
                }, AuthenticationUtil.getSystemUserName());
                if(existingParentNode!=null && !targetParentRef.equals(existingParentNode.getNodeRef())) {
                    final Map<String, Object> props = docProps;
                    final String existingRegNr = (String) props.get(DocumentCommonModel.Props.REG_NUMBER.toString());
                    if (StringUtils.isNotBlank(existingRegNr)) {
                        registerDocument(docNode, true);
                    }
                }
            } catch (Exception e) {
                final String msg = "Failed to move document to volumes folder";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        return getDocument(docNodeRef);
    }

    @Override
    public void updateSearchableFiles(NodeRef document) {
        if (nodeService.hasAspect(document, DocumentCommonModel.Aspects.SEARCHABLE)) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.FILE_NAMES, (Serializable) getSearchableFileNames(document));
            props.put(DocumentCommonModel.Props.FILE_CONTENTS, getSearchableFileContents(document));
            nodeService.addProperties(document, props);
        }
    }

    private List<String> getSearchableFileNames(NodeRef document) {
        List<FileInfo> files = fileFolderService.listFiles(document);
        List<String> fileNames = new ArrayList<String>(files.size());
        for (FileInfo file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    private ContentData getSearchableFileContents(NodeRef document) {
        List<FileInfo> files = fileFolderService.listFiles(document);
        if (files.size() == 0) {
            return null;
        }
        ContentWriter allWriter = contentService.getWriter(document, DocumentCommonModel.Props.FILE_CONTENTS, false);
        allWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        allWriter.setEncoding("UTF-8");
        OutputStream allOutput = allWriter.getContentOutputStream();

        for (FileInfo file : files) {
            ContentReader reader = fileFolderService.getReader(file.getNodeRef());
            if (reader != null && reader.exists()) {
                boolean readerReady = true;
                if (!EqualsHelper.nullSafeEquals(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN)
                        || !EqualsHelper.nullSafeEquals(reader.getEncoding(), "UTF-8")) {
                    ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    if (transformer == null) {
                        log.debug("No transformer found for " + reader.getMimetype());
                        continue;
                    }
                    ContentWriter writer = contentService.getTempWriter();
                    writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    writer.setEncoding("UTF-8");
                    try {
                        transformer.transform(reader, writer);
                        reader = writer.getReader();
                        if (!reader.exists()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Transformation did not write any content, fileName '" + file.getName() + "', " + file.getNodeRef());
                            }
                            readerReady = false;
                        }
                    } catch (ContentIOException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Transformation failed, fileName '" + file.getName() + "', " + file.getNodeRef(), e);
                        }
                        readerReady = false;
                    }
                }
                if (readerReady) {
                    InputStream input = reader.getContentInputStream();
                    try {
                        IOUtils.copy(input, allOutput);
                        input.close();
                        allOutput.write('\n');
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        try {
            allOutput.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return allWriter.getContentData();
    }

    @Override
    public Node createFollowUp(QName docType, NodeRef nodeRef) {
        return createFollowUpDocumentFromExisting(docType, nodeRef);
    }
    
    private Node createFollowUpDocumentFromExisting(QName docType, NodeRef nodeRef) {
        Node followUpDoc = createDocument(docType);
        Node doc = getDocument(nodeRef);
        
        Map<String, Object> props = followUpDoc.getProperties();
        Map<String, Object> docProps = doc.getProperties();
        /** All types share common properties */
        Set<String> copiedProps = DocumentPropertySets.commonProperties;
        
        /** Substitute and choose properties */
        if (DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT.equals(docType) && DocumentSubtypeModel.Types.CONTRACT_SIM.equals(doc.getType())) {
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_NUMBER.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_DATE.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_DATE));
        }
        
        /** Copy Properties */
        for (Map.Entry<String, Object> prop : docProps.entrySet()) {
            if (copiedProps.contains(prop.getKey())) {
                props.put(prop.getKey(), prop.getValue());
            }
        }
        
        /** Copy Ancestors (function, series, volume, case) */
        setTransientProperties(followUpDoc, getAncestorNodesByDocument(doc.getNodeRef()));

        /** Add association from new to original doc */
        nodeService.createAssociation(followUpDoc.getNodeRef(), doc.getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);

        if (log.isDebugEnabled()) {
            log.debug("Created followUp: " + docType.getLocalName() + " from " + doc.getType().getLocalName());
        }
        
        return followUpDoc;
    }

    @Override
    public Node createReply(QName docType, NodeRef nodeRef) {
        return createReplyDocumentFromExisting(docType, nodeRef);
    }
    
    private Node createReplyDocumentFromExisting(QName docType, NodeRef nodeRef) {
        Node replyDoc = createDocument(docType);
        Node doc = getDocument(nodeRef);
        
        Map<String, Object> props = replyDoc.getProperties();
        Map<String, Object> docProps = doc.getProperties();
        Set<String> copiedProps = null;

        /** Substitute and choose properties */
        if (DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(docType)) {
            @SuppressWarnings("unchecked")
            List<String> recipientNames = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_NAME);
            if (recipientNames.size() > 0) {
                recipientNames.remove(0);
            }
            recipientNames.add((String) docProps.get(DocumentSpecificModel.Props.SENDER_DETAILS_NAME.toString()));
            @SuppressWarnings("unchecked")
            List<String> recipientEmails = (List<String>) props.get(DocumentCommonModel.Props.RECIPIENT_EMAIL);
            if (recipientEmails.size() > 0) {
                recipientEmails.remove(0);
            }
            recipientEmails.add((String) docProps.get(DocumentSpecificModel.Props.SENDER_DETAILS_EMAIL.toString()));
            copiedProps = DocumentPropertySets.incomingAndOutgoingLetterProperties;

        } else if (DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT.equals(docType)) {

            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_NUMBER.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
            props.put(DocumentSpecificModel.Props.SECOND_PARTY_REG_DATE.toString(), docProps.get(
                    DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_DATE));
            copiedProps = DocumentPropertySets.commonProperties;
        } else {
            throw new RuntimeException("Unexpected docType: "+docType);
        }
        
        /** Copy Properties */
        for (Map.Entry<String, Object> prop : docProps.entrySet()) {
            if (copiedProps.contains(prop.getKey())) {
                props.put(prop.getKey(), prop.getValue());
            }
        }

        /** Copy Ancestors (function, series, volume, case) */
        setTransientProperties(replyDoc, getAncestorNodesByDocument(doc.getNodeRef()));

        /** Add association from new to original doc */
        nodeService.createAssociation(replyDoc.getNodeRef(), doc.getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_REPLY);

        if (log.isDebugEnabled()) {
            log.debug("Created reply: " + docType.getLocalName() + " from " + doc.getType().getLocalName());
        }
        return replyDoc;
    }


    @Override
    public Node copyDocument(NodeRef nodeRef) {
        Node doc = getDocument(nodeRef);
        Node copiedDoc = createDocument(doc.getType());

        // PROPERTIES
        for (Map.Entry<String, Object> prop : doc.getProperties().entrySet()) {
            if (!DocumentPropertySets.ignoredPropertiesWhenMakingCopy.contains(prop.getKey())) {
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
    public List<Document> getAllDocumentsByVolume(NodeRef volumeRef) {
        return getAllDocumentsByParentNodeRef(volumeRef);
    }

    @Override
    public List<Document> getAllDocumentsByCase(NodeRef caseRef) {
        return getAllDocumentsByParentNodeRef(caseRef);
    }
    
    public List<Document> getAllDocumentFromDvk() {
        List<Document> documents = getAllDocumentsByParentNodeRef(generalService.getNodeRef(fromDvkXPath));
        Collections.sort(documents);
        return documents;
    }

    @Override
    public List<Document> getIncomingEmails() {
        StoreRef storeRef = new StoreRef(defaultStore);
        NodeRef storeRootNodeRef = nodeService.getRootNode(storeRef);
        List<NodeRef> nodes = searchService.selectNodes(storeRootNodeRef, incomingEmailPath, null, namespaceService, false);
        if (nodes.size() != 1) {
            throw new RuntimeException("Wrong count of incoming email folders returned: " + nodes.size());
        }
        NodeRef incomingNodeRef = nodes.get(0);
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(incomingNodeRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        List<Document> docs = new ArrayList();
        for (ChildAssociationRef assocRef : childAssocs) {
            final Node doc = getDocument(assocRef.getChildRef());
            docs.add(getDocumentByNodeRef(doc.getNodeRef()));

        }
        return docs;
    }
    
    @Override
    public Document getDocumentByNodeRef(NodeRef docRef) {
        final Node documentNode = getDocument(docRef);
        DocumentType documentType = documentTypeService.getDocumentType(documentNode.getType());
        Document doc = new Document(documentNode, documentType);
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
        final NodeRef docOrCaseRef;
        if (caseNode != null) {
            docOrCaseRef = caseNode.getNodeRef();
        } else {
            docOrCaseRef = docRef;
        }
        return generalService.getParentWithType(docOrCaseRef, VolumeModel.Types.VOLUME);
    }

    @Override
    public boolean isSaved(NodeRef nodeRef) {
        final Node parentVolume = getVolumeByDocument(nodeRef);
        return parentVolume != null ? true : null != generalService.getParentWithType(nodeRef, CaseModel.Types.CASE);
    }

    @Override
    public Node registerDocument(Node docNode) {
        return registerDocument(docNode, false);
    }
    
    public Node registerDocument(Node docNode, boolean isRelocating) {
        final Map<String, Object> props = docNode.getProperties();
        final String existingRegNr = (String) props.get(DocumentCommonModel.Props.REG_NUMBER.toString());
        if (StringUtils.isNotBlank(existingRegNr) && !isRelocating) {
            throw new RuntimeException("Document already registered! docNode=" + docNode);
        }
        // only register when no existingRegNr or when relocating
        final String volumeNodeRef = (String) props.get(TransientProps.VOLUME_NODEREF);
        final String seriesNodeRef = (String) props.get(TransientProps.SERIES_NODEREF);
        final String caseNodeRef = (String) props.get(TransientProps.CASE_NODEREF);
        final NodeRef docRef = docNode.getNodeRef();
        final String volumeMark = volumeService.getVolumeByNodeRef(volumeNodeRef).getVolumeMark();
        final DocumentType documentType = documentTypeService.getDocumentType(docNode.getType());
        final List<AssociationRef> replyAssocs = nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        final boolean isReplyOrFollowupDoc = replyAssocs.size() > 0
                || nodeService.getTargetAssocs(docRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP).size() > 0; //
        String regNumber = null;
        if (!isReplyOrFollowupDoc) {
            log.debug("Starting to register initialDocument, docRef="+docRef);
            // registration of initial document ("Algatusdokument")
            final Series series = seriesService.getSeriesByNodeRef(seriesNodeRef);
            final Map<String, Object> serProps = series.getNode().getProperties();
            Integer registerId = (Integer) serProps.get(SeriesModel.Props.REGISTER.toString());
            boolean individualizingNumbers = (Boolean) serProps.get(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString());

            registerService.increaseCount(registerId); // increase before geting the register
            Register register = registerService.getRegister(registerId);
            // compose regNumber
            regNumber = volumeMark + "/" + register.getPrefix() + (register.getCounter()) + register.getSuffix();
            if (individualizingNumbers) {
                regNumber += REGISTRATION_INDIVIDUALIZING_NUM_SUFFIX;
            }
        } else { // registration of reply/followUp("Järg- või vastusdokument")
            log.debug("Starting to register "+(replyAssocs.size() > 0 ? "reply" : "followUp")+" document, docRef="+docRef);
            final Node initialDoc = getDocument(getInitialDocument(docRef));
            final Map<String, Object> initDocProps = initialDoc.getProperties();
            final String initDocRegNr = (String) initDocProps.get(DocumentCommonModel.Props.REG_NUMBER.toString());
            if (StringUtils.isNotBlank(initDocRegNr)) {
                final String initDocSeriesNodeRef = (String) initDocProps.get(TransientProps.SERIES_NODEREF.toString());

                final Series series = seriesService.getSeriesByNodeRef(initDocSeriesNodeRef);
                final Map<String, Object> serProps = series.getNode().getProperties();
                boolean individualizingNumbers = (Boolean) serProps.get(SeriesModel.Props.INDIVIDUALIZING_NUMBERS.toString());
                if (!individualizingNumbers) {
                    regNumber = initDocRegNr;
                } else { // add also individualizing number to regNr
                    final RegNrHolder initDocRegNrHolder = new RegNrHolder(initDocRegNr);
                    if (initDocRegNrHolder.getIndividualizingNr() != null) {
                        final NodeRef initDocParentRef = new NodeRef(caseNodeRef != null ? caseNodeRef : volumeNodeRef);
                        int maxIndivNr = initDocRegNrHolder.getIndividualizingNr();
                        for (Document anotherDoc : getAllDocumentsByParentNodeRef(initDocParentRef)) {// FIXME: getAllDocumentsByParentNodeRef ei toimi???
                            if (!docRef.equals(anotherDoc.getNode().getNodeRef())) {
                                final RegNrHolder anotherDocRegNrHolder = new RegNrHolder(anotherDoc.getRegNumber());
                                if (StringUtils.equals(initDocRegNrHolder.getRegNrWithoutIndividualizingNr() //
                                        , anotherDocRegNrHolder.getRegNrWithoutIndividualizingNr())) {
                                    final Integer anotherDocIndivNr = anotherDocRegNrHolder.getIndividualizingNr();
                                    if (anotherDocIndivNr != null) {
                                        maxIndivNr = Math.max(maxIndivNr, anotherDocIndivNr);
                                    }
                                }
                            }
                        }
                        regNumber = initDocRegNrHolder.getRegNrWithoutIndividualizingNr() + (maxIndivNr + 1);
                    } else {
                        // with correct data and *current* expected user behaviors this code should not be reached,
                        // however Maiga insisted that this behavior would be applied if smth. goes wrong
                        regNumber = initDocRegNr;
                    }
                }
                if (StringUtils.isNotBlank(regNumber) && documentType.getId().equals(DocumentSubtypeModel.Types.INSTRUMENT_OF_DELIVERY_AND_RECEIPT)) {
                    if (replyAssocs.size() > 0) {
                        final NodeRef contractDocRef = replyAssocs.get(0).getTargetRef();
                        Date finalTermOfDeliveryAndReceiptDate = (Date) nodeService.getProperty(contractDocRef,
                                DocumentSpecificModel.Props.FINAL_TERM_OF_DELIVERY_AND_RECEIPT);
                        if (finalTermOfDeliveryAndReceiptDate == null) {
                            nodeService.setProperty(contractDocRef, DocumentSpecificModel.Props.FINAL_TERM_OF_DELIVERY_AND_RECEIPT, new Date());
                        }
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(regNumber)) {
            props.put(DocumentCommonModel.Props.REG_NUMBER.toString(), regNumber);
            if (!isRelocating) {
                props.put(DocumentCommonModel.Props.REG_DATE_TIME.toString(), new Date());
            }
            if (!documentType.getId().equals(DocumentSubtypeModel.Types.INCOMING_LETTER)) {
                props.put(DocumentCommonModel.Props.DOC_STATUS.toString(), DocumentStatus.FINISHED.getValueName());
            }
            return updateDocument(docNode);
        }
        throw new UnableToPerformException("Document can't be registered because of initial document is not registered");
    }

    private List<Document> getAllDocumentsByParentNodeRef(NodeRef parentRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentRef, RegexQNamePattern.MATCH_ALL, RegexQNamePattern.MATCH_ALL);
        List<Document> docsOfParent = new ArrayList<Document>(childAssocs.size());
        for (ChildAssociationRef childAssocRef : childAssocs) {
            docsOfParent.add(getDocumentByNodeRef(childAssocRef.getChildRef()));
        }
        return docsOfParent;
    }

    private NodeRef getInitialDocument(NodeRef nodeRef) {
        NodeRef sourceRef = getFirstTargetAssocRef(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
        if (sourceRef == null) {
            return getFirstTargetAssocRef(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
        }
        return sourceRef;
    }

    private NodeRef getFirstTargetAssocRef(NodeRef sourceRef, QName assocQNamePattern) {
        List<AssociationRef> targetAssocs = nodeService.getTargetAssocs(sourceRef, assocQNamePattern);
        NodeRef targetRef = null;
        if(targetAssocs.size() > 0) {
            targetRef = targetAssocs.get(0).getTargetRef();
            if(targetAssocs.size() > 1) {
                log.warn("document with noderef '"+targetRef+"' has more than one '"+assocQNamePattern+"' relations!");
            }
        }
        return targetRef;
    }

    /**
     * Class that divides whole regNumber, that might contain individualizing number into individualizing number and rest of it
     * 
     * @author Ats Uiboupin
     */
    private static class RegNrHolder {
        /**
         * regNrWithoutIndividualizingNr - arbitary char-sequence ending with dash and followed by individualizingNr at the end<br>
         * individualizingNr - integer number at the end of regNr
         */
        private static final Pattern individualizingNrPattern = Pattern.compile("(.*-)(\\d{1,})\\z");
        private final String regNrWithoutIndividualizingNr;
        private final Integer individualizingNr;

        public RegNrHolder(String wholeRegNr) {
            if (StringUtils.isNotBlank(wholeRegNr)) {
                Matcher matcher = individualizingNrPattern.matcher(wholeRegNr.trim());
                if (matcher.find()) {
                    System.out.println(matcher.group(1));
                    System.out.println(matcher.group(2));
                    regNrWithoutIndividualizingNr = matcher.group(1);
                    individualizingNr = Integer.valueOf(matcher.group(2));
                } else {
                    regNrWithoutIndividualizingNr = wholeRegNr;
                    individualizingNr = null;
                }
            } else {
                regNrWithoutIndividualizingNr = wholeRegNr;
                individualizingNr = null;
            }
        }

        public String getRegNrWithoutIndividualizingNr() {
            return regNrWithoutIndividualizingNr;
        }

        public Integer getIndividualizingNr() {
            return individualizingNr;
        }
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
    
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setFromDvkXPath(String fromDvkXPath) {
        this.fromDvkXPath = fromDvkXPath;
    }

    public void setDefaultStore(String defaultStore) {
        this.defaultStore = defaultStore;
    }

    public void setIncomingEmailPath(String incomingEmailPath) {
        this.incomingEmailPath = incomingEmailPath;
    }
    // END: getters / setters
}
