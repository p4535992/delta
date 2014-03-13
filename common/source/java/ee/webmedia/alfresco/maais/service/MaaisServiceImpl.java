package ee.webmedia.alfresco.maais.service;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.maais.MaaisDownloadContentServlet;
import ee.webmedia.alfresco.maais.generated.client.Case;
import ee.webmedia.alfresco.maais.generated.client.File;
import ee.webmedia.alfresco.maais.generated.client.ListCasesRequest;
import ee.webmedia.alfresco.maais.generated.client.ListCasesResponse;
import ee.webmedia.alfresco.maais.generated.client.MaaisAuthRequest;
import ee.webmedia.alfresco.maais.generated.client.MaaisAuthResponse;
import ee.webmedia.alfresco.maais.generated.client.NotifyAssociationRequest;
import ee.webmedia.alfresco.maais.generated.client.NotifyAssociationResponse;
import ee.webmedia.alfresco.maais.generated.client.ObjectFactory;
import ee.webmedia.alfresco.maais.generated.server.CatalogStructureElement;
import ee.webmedia.alfresco.maais.generated.server.DocumentFields;
import ee.webmedia.alfresco.maais.generated.server.RegisterDocumentRequest;
import ee.webmedia.alfresco.maais.generated.server.RegisterDocumentResponse;
import ee.webmedia.alfresco.maais.model.MaaisModel;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.XmlUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class MaaisServiceImpl implements MaaisService {
    private static final String METHOD_CALL_URL = "http://www.maais.ee/delta/:";

    private static final String NS_PREFIX = "tns";

    private static final String NS_URI = "http://www.maais.ee/dms/";

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MaaisServiceImpl.class);

    private static final ObjectFactory WS_CLIENT_FACTORY = new ObjectFactory();
    public static final ee.webmedia.alfresco.maais.generated.server.ObjectFactory WS_SERVER_FACTORY = new ee.webmedia.alfresco.maais.generated.server.ObjectFactory();

    private String organizationId;
    private String serviceUrl;
    private String maaisName;
    private WebServiceTemplate webServiceTemplate;
    private SimpleJdbcTemplate jdbcTemplate;
    private ParametersService parametersService;
    private GeneralService generalService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private DocumentService documentService;
    private DocumentTemplateService documentTemplateService;
    private VolumeService volumeService;
    private SeriesService seriesService;
    private CaseService caseService;
    private FunctionsService functionsService;
    private DocumentSearchService documentSearchService;
    private PersonService personService;
    private UserService userService;
    private OrganizationStructureService organizationStructureService;
    private FileService fileService;

    @Override
    public List<CatalogStructureElement> generateCatalogStructureFromTemplateName(String templateName) {
        DocumentTemplate template;
        try {
            template = documentTemplateService.getTemplateByName(templateName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Ei leidu sobivat dokumendi malli.");
        }
        List<NodeRef> seriesRefs = documentSearchService.searchOpenSeriesByDocType(template.getDocTypeId());
        List<CatalogStructureElement> resultToReturn = new ArrayList<CatalogStructureElement>();
        for (NodeRef seriesRef : seriesRefs) {
            Series series = seriesService.getSeriesByNodeRef(seriesRef);
            String seriesName = series.getSeriesIdentifier() + " " + series.getTitle();
            Function function = functionsService.getFunctionByNodeRef(series.getFunctionNodeRef());
            String functionName = function.getMark() + " " + function.getTitle();
            List<Volume> volumes = volumeService.getAllValidVolumesBySeries(seriesRef);
            for (Volume volume : volumes) {
                Node volumeNode = volume.getNode();
                String volumeName = volume.getVolumeMark() + " " + volume.getTitle();
                if (volumeService.isOpen(volumeNode)) {
                    NodeRef volumeRef = volumeNode.getNodeRef();
                    List<ee.webmedia.alfresco.cases.model.Case> cases = caseService.getAllCasesByVolume(volumeRef);
                    if (cases.isEmpty()) {
                        resultToReturn.add(createCatalogStructureElement(seriesName, functionName, volumeName, volumeRef, null));
                    } else {
                        for (ee.webmedia.alfresco.cases.model.Case caseInfo : cases) {
                            resultToReturn.add(createCatalogStructureElement(seriesName, functionName, volumeName, volumeRef, caseInfo));
                        }
                    }
                }
            }
        }
        return resultToReturn;
    }

    private CatalogStructureElement createCatalogStructureElement(String seriesName, String functionName, String volumeName, NodeRef volumeRef,
            ee.webmedia.alfresco.cases.model.Case caseInfo) {
        CatalogStructureElement element = WS_SERVER_FACTORY.createCatalogStructureElement();
        element.setFunction(functionName);
        element.setSeries(seriesName);
        element.setVolume(volumeName);
        element.setVolumeNodeRef(volumeRef.toString());
        if (caseInfo != null) {
            element.setCase(caseInfo.getTitle());
            element.setCaseNodeRef(caseInfo.getNode().getNodeRefAsString());
        }
        return element;
    }

    private List<Case> requestCases() {
        ListCasesRequest req = WS_CLIENT_FACTORY.createListCasesRequest();
        try {
            ListCasesResponse response = sendRequest(req, "ListCasesRequest", "listCasesIn");
            return response.getToimikud();
        } catch (Exception e) {
            // catch all web service invocation exceptions
            log.error("ListCasesRequest error, request=" + toString(req), e);
        }
        return null;
    }

    @Override
    public int updateMaaisCases() {
        List<Case> cases = requestCases(); // test server returned ~80k cases
        if (cases == null) {
            return 0;
        }
        NodeRef ref = generalService.getNodeRef(MaaisModel.Repo.MAAIS_CASES_XPATH);
        List<ChildAssociationRef> children = nodeService.getChildAssocs(ref, new HashSet<QName>(Arrays.asList(MaaisModel.Types.MAAIS_CASE)));
        Map<String, NodeRef> existingCases = new HashMap<String, NodeRef>();
        for (ChildAssociationRef childAssociationRef : children) {
            NodeRef childRef = childAssociationRef.getChildRef();
            existingCases.put((String) nodeService.getProperty(childRef, MaaisModel.Props.CASE_NUMBER), childRef);
        }
        log.info("found " + existingCases + " existing cases");
        for (Case case1 : cases) {
            createOrUpdateMaaisCase(case1, existingCases.get(case1.getToimik()));
            existingCases.remove(case1.getToimik());
        }
        log.info("deleting " + existingCases.size() + " cases");
        for (Entry<String, NodeRef> toBeRemoved : existingCases.entrySet()) {
            nodeService.deleteNode(toBeRemoved.getValue());
        }
        return cases.size();
    }

    private void createOrUpdateMaaisCase(Case case1, NodeRef nodeRef) {
        if (nodeRef == null) {
            nodeRef = nodeService.createNode(
                            generalService.getNodeRef(MaaisModel.Repo.MAAIS_CASES_XPATH),
                            ContentModel.ASSOC_CONTAINS,
                            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, case1.getToimik()),
                            MaaisModel.Types.MAAIS_CASE
                         ).getChildRef();
            log.info("Created case " + case1.getToimik());
        }
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(MaaisModel.Props.CASE_NUMBER, case1.getToimik());
        properties.put(MaaisModel.Props.CASE_RELATED_PERSON, case1.getIsik());
        properties.put(MaaisModel.Props.LAND_NAME, case1.getMaaüksuseNimi());
        properties.put(MaaisModel.Props.LAND_NUMBER, case1.getMaaüksuseNumber());
        nodeService.setProperties(nodeRef, properties);
    }

    @Override
    public boolean isServiceAvailable() {
        return StringUtils.isNotBlank(serviceUrl);
    }

    @Override
    public Date updateAuth(String userId) {
        if (StringUtils.isBlank(userId)) {
            log.warn("userId was null");
            return null;
        }
        Calendar instance = Calendar.getInstance();
        Date exp = getUserSessionExpiry(userId);
        if (exp != null) {
            Long period = parametersService.getLongParameter(Parameters.MAAIS_RENEW_SESSIONS_PERIOD);
            if ((exp.getTime() - new Date().getTime()) > (period.intValue() * 60000)) {
                instance.setTime(exp);
                return instance.getTime();
            }
        }
        MaaisAuthResponse response;
        Calendar cal = instance;
        MaaisAuthRequest req = WS_CLIENT_FACTORY.createMaaisAuthRequest();
        req.setIsikukood(userId);
        req.setNimi(userService.getUserFullName(userId));
        req.setAsutus(organizationId);
        try {
            response = sendRequest(req, "MaaisAuthRequest", "maaisAuthIn");
        } catch (Exception e) {
            // catch all web service invocation exceptions
            removeFromTable(userId);
            log.error("MaaisAuthRequest error, request=" + toString(req), e);
            return null;
        }
        BigInteger expTime = response.getURLExpirationTime();
        cal.add(Calendar.MINUTE, expTime.intValue());
        saveSession(userId, response.getURL(), cal);
        return cal.getTime();
    }

    @Override
    public String getUserUrl(String userId) {
        Map<String, Object> data = queryForSessionByUsername(userId);
        return (String) data.get("url");
    }

    @Override
    public Date getUserSessionExpiry(String userId) {
        Map<String, Object> data = queryForSessionByUsername(userId);
        return (Date) data.get("expiration_date_time");
    }

    @Override
    public void addMaaisChangedAspectIfNecessary(final NodeRef docRef, boolean checkForMaaisAssocs) {
        if (!isServiceAvailable() || docRef == null) {
            return;
        }
        if (!nodeService.exists(docRef) || nodeService.hasAspect(docRef, MaaisModel.Aspects.MAAIS_NOTIFY_ASSOC)) {
            return;
        }
        if (checkForMaaisAssocs) {
            DocAssocInfo maaisAssoc = getMaaisAssoc(docRef);
            if (maaisAssoc == null) {
                return;
            }
        }
        nodeService.addAspect(docRef, MaaisModel.Aspects.MAAIS_NOTIFY_ASSOC, null);
        RunAsWork<Void> work = new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                notifyAssoc(docRef);
                return null;
            }
        };
        generalService.runOnBackground(work, "maaisNotifyAssocImmediately");
    }

    @Override
    public int notifyFailedAssocs() {
        List<Document> documents = documentSearchService.searchDocumentsByAspect(MaaisModel.Aspects.MAAIS_NOTIFY_ASSOC);
        if (documents.isEmpty()) {
            return 0;
        }
        log.info("found " + documents.size() + " peviously failed documents to send");
        for (Document document : documents) {
            notifyAssoc(document.getNodeRef());
        }
        log.info("finished sending previously failed documents");
        return documents.size();
    }

    @Override
    public void notifyAssoc(NodeRef documentRef) {
        DocAssocInfo maaisAssoc = getMaaisAssoc(documentRef);
        NotifyAssociationRequest req = WS_CLIENT_FACTORY.createNotifyAssociationRequest();
        Map<QName, Serializable> docProps = nodeService.getProperties(documentRef);
        req.setTitle((String) docProps.get(DocumentCommonModel.Props.DOC_NAME));
        req.setTemplateName((String) docProps.get(DocumentSpecificModel.Props.TEMPLATE_NAME));
        req.setSupervisor((String) docProps.get(DocumentCommonModel.Props.OWNER_NAME));
        req.setDocumentType(nodeService.getType(documentRef).getLocalName());
        req.setDocumentReference(documentTemplateService.getDocumentUrl(documentRef));
        req.setDocumentNumber((String) docProps.get(DocumentCommonModel.Props.REG_NUMBER));
        req.setCaseNumber((maaisAssoc == null ? "" : maaisAssoc.getRegNumber()));
        req.setRegistrationDate(XmlUtil.getXmlGregorianCalendar((Date) docProps.get(DocumentCommonModel.Props.REG_DATE_TIME)));
        List<ee.webmedia.alfresco.document.file.model.File> docFiles = fileService.getAllActiveFiles(documentRef);
        for (ee.webmedia.alfresco.document.file.model.File file : docFiles) {
            File fail = WS_CLIENT_FACTORY.createFile();
            fail.setFilename(file.getName());
            fail.setLink(documentTemplateService.getServerUrl() + MaaisDownloadContentServlet.generateDownloadURL(file.getNodeRef(), file.getName()));
            req.getFiles().add(fail);
        }
        try {
            @SuppressWarnings("unused")
            NotifyAssociationResponse response = sendRequest(req, "NotifyAssociationRequest", "notifyAssociationIn");
        } catch (Exception e) {
            // catch all web service invocation exceptions
            log.error("NotifyAssociationRequest error, request=" + toString(req), e);
            return;
        }
        nodeService.removeAspect(documentRef, MaaisModel.Aspects.MAAIS_NOTIFY_ASSOC); // in case the service invocation fails a job could find nodes by this aspect and retry
    }

    private DocAssocInfo getMaaisAssoc(NodeRef documentRef) {
        List<DocAssocInfo> assocs = documentService.getAssocInfos(new MapNode(documentRef));
        DocAssocInfo maaisAssoc = null;
        for (DocAssocInfo docAssocInfo : assocs) {
            if (docAssocInfo.isMaaisCase()) {
                maaisAssoc = docAssocInfo;
                break;
            }
        }
        return maaisAssoc;
    }

    @Override
    public RegisterDocumentResponse registerMaaisDocument(RegisterDocumentRequest request) {
        final String templateString = request.getDocumentTemplate();
        DocumentTemplate template;
        try {
            template = documentTemplateService.getTemplateByName(templateString);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Ei leidu sobivat malli.", e);
        }
        if (template.getDocTypeId() == null) {
            throw new RuntimeException("Ei leidu sobivat malli.");
        }
        String nodeRefString = request.getVolumeCaseNodeRef();
        NodeRef nodeRef = new NodeRef(nodeRefString);
        if (!nodeService.exists(nodeRef)) {
            throw new RuntimeException("Ei leidu sobivat toimikut või asja.");
        }
        QName caseOrVolumeType = nodeService.getType(nodeRef);
        boolean isCase = caseOrVolumeType.equals(CaseModel.Types.CASE);
        if (!isCase && !caseOrVolumeType.equals(VolumeModel.Types.VOLUME)) {
            throw new RuntimeException("Ei leidu sobivat toimikut või asja.");
        }
        RegisterDocumentResponse response = WS_SERVER_FACTORY.createRegisterDocumentResponse();
        NodeRef documentRef = createMaaisDocument(template, nodeRef, request.getMaaisCase(), request.getDocumentFields(), isCase);
        response.setUrl(documentTemplateService.getDocumentUrl(documentRef));
        List<ee.webmedia.alfresco.document.file.model.File> files = fileService.getAllActiveFiles(documentRef);
        for (ee.webmedia.alfresco.document.file.model.File file : files) {
            ee.webmedia.alfresco.maais.generated.server.File fail = new ee.webmedia.alfresco.maais.generated.server.File();
            fail.setFilename(file.getName());
            fail.setUrl(documentTemplateService.getServerUrl() + MaaisDownloadContentServlet.generateDownloadURL(file.getNodeRef(), file.getName()));
            response.getFiles().add(fail);
        }
        return response;
    }

    // TODO Spec didn't say anything what to do with maaisCase
    private NodeRef createMaaisDocument(final DocumentTemplate template, final NodeRef parentRef, String maaisCase, DocumentFields documentFields, boolean parentIsCase) {
        NodeRef nodeRef = documentService.createDocument(template.getDocTypeId(), parentRef, null).getNodeRef();
        final Map<QName, Serializable> props = extractMaaisProps(documentFields);
        props.put(DocumentCommonModel.Props.DOC_NAME, documentFields.getDocName());
        fillLocationProps(props, parentRef, parentIsCase);
        fillAdditionalProps(props, template);
        props.putAll(getOwnerProps(documentFields.getOwnerId(), documentFields.getOwnerName()));
        props.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
        nodeService.setProperties(nodeRef, props);
        NodeRef maaisCaseRef = documentSearchService.getMaaisCase(maaisCase);
        if (maaisCaseRef == null) {
            // if not found, then we create it, additional data will be added during the usual import
            Case case1 = WS_CLIENT_FACTORY.createCase();
            case1.setToimik(maaisCase);
            createOrUpdateMaaisCase(case1, null);
            maaisCaseRef = documentSearchService.getMaaisCase(maaisCase);
        }
        documentService.createAssoc(maaisCaseRef, nodeRef, MaaisModel.Associations.MAAIS_CASE_DOCUMENT);
        try {
            documentTemplateService.populateTemplate(nodeRef);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return nodeRef;
    }

    private void fillAdditionalProps(Map<QName, Serializable> props, DocumentTemplate template) {
        // accessRestriction
        Series series = seriesService.getSeriesByNodeRef((NodeRef) props.get(DocumentCommonModel.Props.SERIES));
        HashMap<String, Object> docProps = new HashMap<String, Object>();
        MetadataBlockBean.setAccessRestrictionPropsFromSeries(docProps, series);
        props.putAll(RepoUtil.toQNameProperties(docProps));

        // storage type
        props.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());

        // template name
        props.put(DocumentSpecificModel.Props.TEMPLATE_NAME, template.getName());
    }

    private void fillLocationProps(Map<QName, Serializable> props, NodeRef parentRef, boolean parentIsCase) {
        NodeRef currentRef = parentRef;
        if (parentIsCase) {
            props.put(DocumentCommonModel.Props.CASE, currentRef);
            currentRef = nodeService.getPrimaryParent(currentRef).getParentRef();
        }
        props.put(DocumentCommonModel.Props.VOLUME, currentRef);
        currentRef = nodeService.getPrimaryParent(currentRef).getParentRef();
        props.put(DocumentCommonModel.Props.SERIES, currentRef);
        currentRef = nodeService.getPrimaryParent(currentRef).getParentRef();
        props.put(DocumentCommonModel.Props.FUNCTION, currentRef);
    }

    private HashMap<QName, Serializable> getOwnerProps(String ownerId, String ownerName) {
        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        if (!personService.personExists(ownerId)) {
            props.put(DocumentCommonModel.Props.OWNER_ID, ownerId);
            props.put(DocumentCommonModel.Props.OWNER_NAME, ownerName);
        } else {
            Map<QName, Serializable> personProps = nodeService.getProperties(personService.getPerson(ownerId));
            props.put(DocumentCommonModel.Props.OWNER_ID, personProps.get(ContentModel.PROP_USERNAME));
            props.put(DocumentCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(personProps));
            props.put(DocumentCommonModel.Props.OWNER_JOB_TITLE, personProps.get(ContentModel.PROP_JOBTITLE));
            String orgstructName = organizationStructureService.getOrganizationStructure((String) personProps.get(ContentModel.PROP_ORGID));
            props.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, orgstructName);
            props.put(DocumentCommonModel.Props.OWNER_EMAIL, personProps.get(ContentModel.PROP_EMAIL));
            props.put(DocumentCommonModel.Props.OWNER_PHONE, personProps.get(ContentModel.PROP_TELEPHONE));
        }
        return props;
    }

    private Map<QName, Serializable> extractMaaisProps(DocumentFields documentFields) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        Method[] methods = DocumentFields.class.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("getMaais")) {
                String propName = methodName.replaceFirst("getMaais", "maais");
                String value;
                try {
                    value = (String) method.invoke(documentFields);
                } catch (IllegalArgumentException e) {
                    continue;
                } catch (IllegalAccessException e) {
                    continue;
                } catch (InvocationTargetException e) {
                    continue;
                }
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                QName propQName = QName.createQName(DocumentSpecificModel.URI, propName);
                PropertyDefinition propDef = dictionaryService.getProperty(propQName);
                if (propDef == null) {
                    throw new RuntimeException("no definition found for " + propQName.toString());
                }
                props.put(propQName, value);
            }
        }
        return props;
    }

    @SuppressWarnings("unchecked")
    protected <T, R> T sendRequest(R req, String localName, final String methodName) {
        javax.xml.namespace.QName qName = new javax.xml.namespace.QName(NS_URI, localName, NS_PREFIX);
        JAXBElement<R> requestPayload = new JAXBElement<R>(qName, (Class<R>) req.getClass(), req);
        WebServiceMessageCallback callback = new WebServiceMessageCallback() {
            @Override
            public void doWithMessage(WebServiceMessage message) {
                ((SoapMessage) message).setSoapAction(METHOD_CALL_URL + methodName);
            }
        };
        Object marshalSendAndReceive = webServiceTemplate.marshalSendAndReceive(requestPayload, callback);
        if (marshalSendAndReceive instanceof JAXBElement<?>) {
            return ((JAXBElement<T>) marshalSendAndReceive).getValue();
        }
        return (T) marshalSendAndReceive;
    }

    private Map<String, Object> queryForSessionByUsername(String username) {
        try {
            String sql = "SELECT * FROM delta_maais_session WHERE username=? LIMIT 1";
            return jdbcTemplate.queryForMap(sql, username);
        } catch (EmptyResultDataAccessException e) {
            return new HashMap<String, Object>();
        }
    }

    public static String toString(Object object) {
        return ToStringBuilder.reflectionToString(object, ToStringStyle.MULTI_LINE_STYLE);
    }

    private void removeFromTable(String userId) {
        jdbcTemplate.update("DELETE FROM delta_maais_session WHERE username=?", userId);
    }

    private void saveSession(String username, String url, Calendar expiration) {
        int updatedCount = jdbcTemplate.update("UPDATE delta_maais_session SET url=?, expiration_date_time=?, created_date_time=CURRENT_TIMESTAMP WHERE username=?", url,
                expiration,
                username);
        if (updatedCount == 0) {
            jdbcTemplate.update("INSERT INTO delta_maais_session (username, url, expiration_date_time) VALUES (?,?,?)", username, url, expiration);
        }
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setWebServiceTemplate(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public void setMaaisName(String maaisName) {
        this.maaisName = maaisName;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setOrganizationStructureService(OrganizationStructureService organizationStructureService) {
        this.organizationStructureService = organizationStructureService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String getMaaisName() {
        return maaisName;
    }

}
