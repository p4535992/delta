package ee.webmedia.xtee.client.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.util.Base64;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.mime.Attachment;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ee.sk.digidoc.SignedDoc;
import ee.webmedia.xtee.client.exception.XTeeServiceConsumptionException;
import ee.webmedia.xtee.client.service.DhlXTeeService;
import ee.webmedia.xtee.client.service.XTeeDatabaseService;
import ee.webmedia.xtee.client.service.extractor.CustomExtractor;
import ee.webmedia.xtee.model.XTeeAttachment;
import ee.webmedia.xtee.model.XTeeMessage;
import ee.webmedia.xtee.model.XmlBeansXTeeMessage;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.AadressType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DhlDokumentType;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.DokumentDocument;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.MetainfoDocument.Metainfo;
import ee.webmedia.xtee.types.ee.riik.schemas.dhl.TransportDocument.Transport;
import ee.webmedia.xtee.types.ee.riik.schemas.dhlMetaAutomatic.DhlDokIDType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.DocumentRefsArrayType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendStatusRequestType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendStatusResponseTypeUnencoded;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendingOptionsV2RequestType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.InstitutionArrayType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.InstitutionRefsArrayType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.InstitutionType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.MarkDocumentsReceivedRequestType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.MarkDocumentsReceivedRequestTypeUnencoded;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.MarkDocumentsReceivedResponseType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.OccupationArrayType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.OccupationType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.ReceiveDocumentsRequestType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.ReceiveDocumentsResponseTypeUnencoded;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.SendDocumentsResponseType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.SendDocumentsV2RequestType;
import ee.webmedia.xtee.types.ee.riik.xtee.dhl.producers.producer.dhl.GetSendStatusResponseTypeUnencoded.Item;
import ee.webmedia.xtee.types.ee.sk.digiDoc.v13.DataFileType;
import ee.webmedia.xtee.types.ee.sk.digiDoc.v13.SignedDocType;
import ee.webmedia.xtee.util.AttachmentUtil;

/**
 * @author Ats Uiboupin
 */
public class DhlXTeeServiceImpl extends XTeeDatabaseService implements DhlXTeeService {
    private static Log log = LogFactory.getLog(DhlXTeeServiceImpl.class);

    // START: XTEE DVK service names and versions
    private static final String SEND_DOCUMENTS = "sendDocuments";
    private static final String SEND_DOCUMENTS_VERSION = "v2";

    private static final String GET_SENDING_OPTIONS = "getSendingOptions";
    private static final String GET_SENDING_OPTIONS_VERSION = "v2";

    private static final String GET_OCCUPATION_LIST = "getOccupationList";
    private static final String GET_OCCUPATION_LIST_VERSION = "v1";

    private static final String XTEE_METHOD_GET_SEND_STATUS = "getSendStatus";
    private static final String GET_SEND_STATUS_VERSION = "v1";

    private static final String RECEIVE_DOCUMENTS = "receiveDocuments";
    private static final String RECEIVE_DOCUMENTS_VERSION = "v2";// v3 existed, but removed from wsdl, as it didn't work

    private static final String MARK_DOCUMENTS_RECEIVED = "markDocumentsReceived";
    private static final String MARK_DOCUMENTS_RECEIVED_VERSION = "v1";

    private static final String RUN_SYSTEM_CHECK = "runSystemCheck";
    private static final String RUN_SYSTEM_CHECK_VERSION = "v1";
    // START: XTEE DVK service names and versions

    private GetDvkOrganizationsHelper dvkOrganizationsHelper;

    private final SendDocumentsHelper sendDocumentsHelper;

    public DhlXTeeServiceImpl() {
        this(null);
    }

    public DhlXTeeServiceImpl(DvkOrganizationsUpdateStrategy updateStrategy) {
        this.dvkOrganizationsHelper = new GetDvkOrganizationsHelperImpl();
        if (updateStrategy != null) {
            dvkOrganizationsHelper.setUpdateStrategy(updateStrategy);
        }
        this.sendDocumentsHelper = this.new SendDocumentsHelper();
    }

    /* removed from some version of dhl
    @Override
    public void runSystemCheck() {
        String queryMethod = getDatabase() + "." + RUN_SYSTEM_CHECK + "." + RUN_SYSTEM_CHECK_VERSION;
        RunSystemCheckRequestType request = RunSystemCheckRequestType.Factory.newInstance();
        log.debug("executing " + queryMethod);
        try {
            XTeeMessage<RunSystemCheckResponseType> response //
            = send(new XmlBeansXTeeMessage<RunSystemCheckRequestType>(request), RUN_SYSTEM_CHECK,
                    RUN_SYSTEM_CHECK_VERSION);
            if (!response.getContent().getStringValue().equalsIgnoreCase("OK")) {
                throw new RuntimeException("Service didn't respond with 'OK': " + response.getContent().getStringValue());
            }
        } catch (XTeeServiceConsumptionException e) {
            throw new RuntimeException(resolveMessage(queryMethod, e), e);
        }
    }
     */

    @Override
    public void markDocumentsReceived(Collection<String> receivedDocumentIds) {
        String queryMethod = getDatabase() + "." + MARK_DOCUMENTS_RECEIVED + "." + MARK_DOCUMENTS_RECEIVED_VERSION;
        MarkDocumentsReceivedRequestType request = MarkDocumentsReceivedRequestType.Factory.newInstance();
        byte[] attachmentBody = MarkDocumentsReceived.createMarkDocumentsReceivedAttachmentBody(receivedDocumentIds);
        request.setDokumendid(attachmentBody);
        String cid = AttachmentUtil.getUniqueCid();
        XmlCursor cursor = request.newCursor();
        cursor.toNextToken();
        Element node = (Element) cursor.getDomNode();
        node.setAttribute("href", "cid:" + cid);
        cursor.dispose();

        XTeeAttachment attachment = new XTeeAttachment(cid, "{http://www.w3.org/2001/XMLSchema}base64Binary", attachmentBody);
        log.debug("executing " + queryMethod);
        try {
            XTeeMessage<MarkDocumentsReceivedResponseType> response //
            = send(new XmlBeansXTeeMessage<MarkDocumentsReceivedRequestType>(request, Arrays.asList(attachment)), MARK_DOCUMENTS_RECEIVED,
                    MARK_DOCUMENTS_RECEIVED_VERSION);
            if (!response.getContent().getStringValue().equalsIgnoreCase("OK")) {
                throw new RuntimeException("Service didn't respond with 'OK': " + response.getContent().getStringValue());
            }
        } catch (XTeeServiceConsumptionException e) {
            throw new RuntimeException(resolveMessage(queryMethod, e), e);
        }
    }

    @Override
    public ReceivedDocumentsWrapper receiveDocuments(int maxNrOfDocuments) {
        String queryMethod = getDatabase() + "." + RECEIVE_DOCUMENTS + "." + RECEIVE_DOCUMENTS_VERSION;
        ReceiveDocumentsRequestType request = ReceiveDocumentsRequestType.Factory.newInstance();
        request.setArv(BigInteger.valueOf(maxNrOfDocuments));
        log.debug("executing " + queryMethod);
        try {
            XTeeMessage<ReceiveDocumentsResponseTypeUnencoded> response //
            = send(new XmlBeansXTeeMessage<ReceiveDocumentsRequestType>(request), RECEIVE_DOCUMENTS, RECEIVE_DOCUMENTS_VERSION);
            return new ReceivedDocumentsWrapperImpl(response);
        } catch (XTeeServiceConsumptionException e) {
            throw new RuntimeException(resolveMessage(queryMethod, e), e);
        }
    }

    public static class ReceivedDocumentsWrapperImpl extends AbstractMap<String, ReceivedDocumentsWrapper.ReceivedDocument> implements ReceivedDocumentsWrapper {
        private final List<DhlDokumentType> receivedDocuments;
        private final Map<String /* dhlId */, ReceivedDocument> dhlDocumentsMap;

        public ReceivedDocumentsWrapperImpl(XTeeMessage<ReceiveDocumentsResponseTypeUnencoded> response) {
            List<DokumentDocument> dokumentDocuments;
            try {
                dokumentDocuments = getTypeFromGzippedAndEncodedSoapArray(response.getAttachments().get(0).getInputStream(),
                        DokumentDocument.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get input of attachment ", e);
            }
            this.receivedDocuments = new ArrayList<DhlDokumentType>();
            for (DokumentDocument dokumentDocument : dokumentDocuments) {
                receivedDocuments.add(dokumentDocument.getDokument());
            }
            this.dhlDocumentsMap = new HashMap<String, ReceivedDocument>();
            for (DhlDokumentType dhlDokument : receivedDocuments) {
                if (log.isTraceEnabled()) {
                    log.trace("received dhlDokument : " + dhlDokument + "'");
                }
                Metainfo metainfo = dhlDokument.getMetainfo();
                MetainfoHelper metaInfoHelper = new MetainfoHelper(metainfo);
                String dhlId = metaInfoHelper.getDhlId();
                dhlDocumentsMap.put(dhlId, new ReceivedDocumentImpl(dhlDokument, dhlId, metaInfoHelper));
            }
        }

        @Override
        public Iterator<String> iterator() {
            return dhlDocumentsMap.keySet().iterator();
        }

        @Override
        public Set<Entry<String, ReceivedDocument>> entrySet() {
            return dhlDocumentsMap.entrySet();
        }

        // START: getters/setters
        public Map<String, ReceivedDocument> getDhlDocumentsMap() {
            return dhlDocumentsMap;
        }

        public List<DhlDokumentType> getReceivedDocuments() {
            return receivedDocuments;
        }

        // END: getters/setters

        public static class ReceivedDocumentImpl implements ReceivedDocument {
            private final String dhlId;
            private final MetainfoHelper metaInfoHelper;
            private final DhlDokumentType dhlDocument;

            public ReceivedDocumentImpl(DhlDokumentType dhlDocument, String dhlId, MetainfoHelper metaInfoHelper) {
                this.dhlDocument = dhlDocument;
                this.dhlId = dhlId;
                this.metaInfoHelper = metaInfoHelper;
            }

            public DhlDokumentType getDhlDocument() {
                return dhlDocument;
            }

            public SignedDocType getSignedDoc() {
                return dhlDocument.getSignedDoc();
            }

            // START: getters/setters
            public String getDhlId() {
                return dhlId;
            }

            public MetainfoHelper getMetaInfoHelper() {
                return metaInfoHelper;
            }
            // END: getters/setters

        }

    }

    @Override
    public Set<String> sendDocuments(Collection<ContentToSend> contentsToSend, AadressType[] recipients, AadressType sender) {
        return sendDocuments(contentsToSend, recipients, sender, null, null);
    }

    @Override
    public Set<String> sendDocuments(Collection<ContentToSend> contentsToSend, AadressType[] recipients, AadressType sender,
            SendDocumentsDokumentCallback dokumentCallback, SendDocumentsRequestCallback requestCallback) {

        String queryMethod = getDatabase() + "." + SEND_DOCUMENTS + "." + SEND_DOCUMENTS_VERSION;

        DokumentDocument dokumentDocument = sendDocumentsHelper.constructDokumentDocument(contentsToSend, sender, recipients);

        if (dokumentCallback != null) {
            dokumentCallback.doWithDocument(dokumentDocument);
        }

        byte[] dokumentBytes = sendDocumentsHelper.getBytes(dokumentDocument);
        if (log.isTraceEnabled()) {
            log.trace("Constructed dokument document is: \n" + new String(dokumentBytes));
        }

        byte[] base64Doc = gzipAndEncodeDocument(dokumentBytes);

        final String cid = AttachmentUtil.getUniqueCid();
        final XTeeAttachment attachment = new XTeeAttachment(cid, "{http://www.w3.org/2001/XMLSchema}base64Binary", base64Doc);

        SendDocumentsV2RequestType request = SendDocumentsV2RequestType.Factory.newInstance();

        request.setDokumendid(null);
        XmlCursor cursor = request.newCursor();
        cursor.toNextToken();
        Element node = (Element) cursor.getDomNode();
        node.setAttribute("href", "cid:" + cid);
        cursor.dispose();

        if (requestCallback != null) {
            requestCallback.doWithRequest(request);
        }

        log.debug("executing " + queryMethod);
        try {
            XTeeMessage<SendDocumentsResponseType> response = send(new XmlBeansXTeeMessage<SendDocumentsV2RequestType>(request, Collections
                    .singletonList(attachment)), SEND_DOCUMENTS, SEND_DOCUMENTS_VERSION);

            List<DhlDokIDType> dokumentDocuments //
            = getTypeFromGzippedAndEncodedSoapArray(response.getAttachments().get(0).getInputStream(), DhlDokIDType.class);
            Set<String> sentDocumentsDhlIds = new HashSet<String>();
            for (DhlDokIDType dokIDType : dokumentDocuments) {
                sentDocumentsDhlIds.add(dokIDType.getStringValue());
            }
            return sentDocumentsDhlIds;
        } catch (XTeeServiceConsumptionException e) {
            throw new RuntimeException(resolveMessage(queryMethod, e), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract response " + queryMethod, e);
        }
    }

    @Override
    public List<OccupationType> getOccupationList(List<String> institutionRegNrs) {
        String queryMethod = getDatabase() + "." + GET_OCCUPATION_LIST + "." + GET_OCCUPATION_LIST_VERSION;
        InstitutionRefsArrayType request = InstitutionRefsArrayType.Factory.newInstance();
        for (String regNr : institutionRegNrs) {
            request.addAsutus(regNr);
        }
        log.debug("executing " + queryMethod);
        try {
            XTeeMessage<OccupationArrayType> response = send(new XmlBeansXTeeMessage<InstitutionRefsArrayType>(request)//
                    , GET_OCCUPATION_LIST, GET_OCCUPATION_LIST_VERSION);
            return response.getContent().getAmetikohtList();
        } catch (XTeeServiceConsumptionException e) {
            throw new RuntimeException(resolveMessage(queryMethod, e), e);
        }
    }

    public Map<String/* regNr */, String/* name */> getSendingOptions() {
        String queryMethod = getDatabase() + "." + GET_SENDING_OPTIONS + "." + GET_SENDING_OPTIONS_VERSION;
        log.debug("executing " + queryMethod);
        GetSendingOptionsV2RequestType request = GetSendingOptionsV2RequestType.Factory.newInstance();
        // keha.setVastuvotmataDokumenteOotel(false);
        try {
            final XTeeMessage<InstitutionArrayType> response = send(new XmlBeansXTeeMessage<GetSendingOptionsV2RequestType>(request)//
                    , GET_SENDING_OPTIONS, GET_SENDING_OPTIONS_VERSION);
            if (log.isTraceEnabled()) {
                log.trace("excecution result#1 response:\t" + ToStringBuilder.reflectionToString(response) + "'");
            }
            InstitutionArrayType responseContent = response.getContent();
            List<InstitutionType> orgList = responseContent.getAsutusList();
            log.debug(orgList.size() + " organisations can use DVK");
            HashMap<String, String> dvkCapableOrganizations = new HashMap<String, String>(orgList.size());
            for (InstitutionType institutionType : orgList) {
                dvkCapableOrganizations.put(institutionType.getRegnr(), institutionType.getNimi());
                log.debug("\t" + institutionType.getRegnr() + ":\t" + institutionType.getNimi());
            }
            return dvkCapableOrganizations;
        } catch (XTeeServiceConsumptionException e) {
            throw new RuntimeException(resolveMessage(queryMethod, e), e);
        }
    }

    @Override
    public List<Item> getSendStatuses(Set<String> ids) {
        String queryMethod = getDatabase() + "." + XTEE_METHOD_GET_SEND_STATUS + "." + GET_SEND_STATUS_VERSION;
        try {
            DocumentRefsArrayType documentRefsArray = DocumentRefsArrayType.Factory.newInstance();
            for (String id : ids) {
                documentRefsArray.addDhlId(id);
            }
            String attachmentBodyUnencoded = documentRefsArray.toString();
            log.debug("getSendStatus plain attachment body: '" + attachmentBodyUnencoded + "'");

            GetSendStatusRequestType request = GetSendStatusRequestType.Factory.newInstance();
            XmlCursor cursor = request.newCursor();
            String cid = AttachmentUtil.getUniqueCid();
            cursor.toNextToken();
            cursor.insertAttributeWithValue("href", "cid:" + cid);
            cursor.dispose();
            byte[] base64 = gzipAndEncodeDocument(attachmentBodyUnencoded);
            XTeeAttachment attachment = new XTeeAttachment(cid, "{http://www.w3.org/2001/XMLSchema}base64Binary", base64);
            XTeeMessage<GetSendStatusRequestType> reqMessage = new XmlBeansXTeeMessage<GetSendStatusRequestType>(request, Arrays.asList(attachment));

            log.debug("executing " + queryMethod);
            XTeeMessage<GetSendStatusResponseTypeUnencoded> response = send(reqMessage, XTEE_METHOD_GET_SEND_STATUS, GET_SEND_STATUS_VERSION, null,
                    new GetSendStatusExtractor());
            final GetSendStatusResponseTypeUnencoded unencoded = response.getContent();
            return unencoded.getItemList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to excecute xtee query " + queryMethod, e);
        }
    }

    private static class GetSendStatusExtractor extends CustomExtractor {

        @Override
        public XTeeMessage<GetSendStatusResponseTypeUnencoded> extractData(WebServiceMessage message) throws IOException, TransformerException {
            Attachment attachment = (Attachment) ((SaajSoapMessage) message).getAttachments().next();
            String xml = new String(unzipAndDecode(attachment.getInputStream()), "UTF-8");
            final GetSendStatusResponseTypeUnencoded content = getTypeFromXml(addCorrectNamespaces(xml), GetSendStatusResponseTypeUnencoded.class);
            return new XmlBeansXTeeMessage<GetSendStatusResponseTypeUnencoded>(content);
        }

        private String addCorrectNamespaces(String xml) throws IOException {
            Node root;
            try {
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));

                Document targetDoc = docBuilder.newDocument();
                root = targetDoc.appendChild(targetDoc.createElementNS("", "xml-fragment"));

                Node keha = doc.getFirstChild();
                NodeList items = keha.getChildNodes();
                for (int i = 0; i < items.getLength(); i++) {
                    Node item = items.item(i);
                    final String name1 = item.getNodeName();
                    log.debug("name=" + name1);
                    if ("item".equalsIgnoreCase(name1)) {
                        log.debug("parsing item");
                        Node itemTarget = root.appendChild(targetDoc.createElementNS("", "item"));

                        NodeList sublist = item.getChildNodes();
                        for (int j = 0; j < sublist.getLength(); j++) {
                            Node subItem = sublist.item(j);
                            final String localName = subItem.getNodeName();
                            if ("dhl_id".equalsIgnoreCase(localName)) {
                                final String NS_DHL_META_AUTOMATIC = "http://www.riik.ee/schemas/dhl-meta-automatic";
                                addNameSpace(NS_DHL_META_AUTOMATIC, subItem, itemTarget);
                            } else if ("edastus".equalsIgnoreCase(localName)) {
                                final String NS_SCHEMAS_DHL = "http://www.riik.ee/schemas/dhl";
                                addNameSpace(NS_SCHEMAS_DHL, subItem, itemTarget);
                            } else if ("olek".equalsIgnoreCase(localName)) {
                                addNameSpace("", subItem, itemTarget);
                                // } else {
                                // log.error("Unexpected localName: '"+localName+"', text:\n'"+subItem.getTextContent()+"'");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("failed to add correct namespaces to given xml: " + xml, e);
            }
            String resultXml = xmlNodeToString(root);
            if (log.isDebugEnabled()) {
                log.debug("Added namespaces for the xml.\n START: xml source:\n" + xml + "\n\nEND: xml source\nSTART: xml result\n" + resultXml
                        + "\n\nEND: xml result");
            }
            return resultXml;
        }

        private static String xmlNodeToString(Node node) {
            try {
                Source source = new DOMSource(node);
                StringWriter stringWriter = new StringWriter();
                Result result = new StreamResult(stringWriter);
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer();
                transformer.transform(source, result);
                return stringWriter.getBuffer().toString();
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void addNameSpace(String ns, Node source, Node target) {
            Node newElement;
            if (source.getNodeType() == Node.TEXT_NODE) {
                final String textContent = source.getTextContent();
                log.debug("Creating textnode with content:\n'" + textContent + "'");
                newElement = target.getOwnerDocument().createTextNode(textContent);
            } else {
                newElement = target.getOwnerDocument().createElementNS(ns, source.getNodeName());
            }
            NodeList childNodes = source.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childItem = childNodes.item(i);
                addNameSpace(ns, childItem, newElement);
            }
            target.appendChild(newElement);
        }

    }

    /**
     * @author ats.uiboupin
     *         Since the list of DVK capable organisations grows/changes very rarely (maybe one in a month or even more rarely)
     *         it might be feasible to keep the list cached in memory and for example <br>
     *         updated onece a day by some job, <br>
     *         or use strategy pattern ant set strategy when creating instance of the service class in the top-level class.
     */
    private class GetDvkOrganizationsHelperImpl implements GetDvkOrganizationsHelper {
        private Map<String/* regNr */, String/* name */> dvkCapableOrganizations;

        private DvkOrganizationsUpdateStrategy updateStrategy;

        @Override
        public Map<String/* regNr */, String/* name */> getDvkOrganizationsCache() {
            if (getUpdateStrategy().update4getDvkOrganizationsCache(dvkCapableOrganizations)) {
                updateDvkCapableOrganisationsCache();
            }
            return dvkCapableOrganizations;
        }

        @Override
        public String getOrganizationName(String regnr) {
            String orgName = getDvkOrganizationsCache().get(regnr);
            if (getUpdateStrategy().update4getOrganizationName(orgName)) {
                updateDvkCapableOrganisationsCache();
                orgName = getDvkOrganizationsCache().get(regnr);
            }
            return orgName;
        }

        @Override
        public void updateDvkCapableOrganisationsCache() {
            setDvkOrganizationsCache(getSendingOptions());
            log.debug("--updateDvkCapableOrganisationsCache--");
        }

        @Override
        public void setDvkOrganizationsCache(Map<String, String> cache) {
            this.dvkCapableOrganizations = cache;
            getUpdateStrategy().setLastUpdated(Calendar.getInstance());
        }

        // START: getters/setters
        @Override
        public DvkOrganizationsUpdateStrategy getUpdateStrategy() {
            if (updateStrategy == null) {
                log.debug("-- using default cache update strategy--");
                updateStrategy = new DhlXTeeService.DvkOrganizationsCacheingUpdateStrategy().setMaxUpdateInterval(24 * 60);// 24h
            }
            return updateStrategy;
        }

        @Override
        public void setUpdateStrategy(DvkOrganizationsUpdateStrategy updateStrategy) {
            this.updateStrategy = updateStrategy;
        }
        // END: getters/setters

    }

    /**
     * @author ats.uiboupin
     *         Contains methods used by sendDocuments service. <br>
     *         Current implementation uses {@link GetDvkOrganizationsHelper} to fill receivers names based on registration codes.
     */
    private class SendDocumentsHelper {

        /**
         * Creates (Dhl)&lt;Dokument/&gt; element containing info about sender, recipients
         * and given files in &lt;SignedDoc/&gt; element(aka digiDoc)
         * 
         * @param contentsToSend
         * @param sender
         * @param recipients
         * @return (Dhl)&lt;Dokument/&gt; XmlObject
         */
        private DokumentDocument constructDokumentDocument(Collection<ContentToSend> contentsToSend, AadressType sender, AadressType[] recipients) {
            DokumentDocument dokumentDocument = DokumentDocument.Factory.newInstance();
            DhlDokumentType dokumentContainer = dokumentDocument.addNewDokument();

            // Add mandatory empty elements
            dokumentContainer.addNewMetaxml();
            dokumentContainer.addNewAjalugu();

            if (StringUtils.isBlank(sender.getAsutuseNimi())) {
                String senderName = getDvkOrganizationsHelper().getOrganizationName(sender.getRegnr());
                sender.setAsutuseNimi(senderName);
            }

            Transport transport = dokumentContainer.addNewTransport();
            transport.setSaatja(sender);

            for (AadressType recipient : recipients) {
                String recipientName = getDvkOrganizationsHelper().getOrganizationName(recipient.getRegnr());
                if (StringUtils.isBlank(recipientName)) {
                    throw new IllegalArgumentException("Cannot send documents to recipient with reg.Nr "
                            + recipient.getRegnr() + " using DVK because recipient" + recipient.getRegnr() + " is not DVK capable.");
                }
                recipient.setAsutuseNimi(recipientName);
            }
            transport.setSaajaArray(recipients);
            final DataFileType[] dataFiles = addSignedDocToDokument(contentsToSend, dokumentContainer);

            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("Constructed dokument from: ")//
                        .append(sender.getRegnr()).append("-").append(sender.getAsutuseNimi()).append(" to [");//
                for (AadressType recipient : recipients) {
                    sb.append(recipient.getRegnr()).append("-").append(recipient.getAsutuseNimi()).append(" | ");
                }
                sb.append("] with files: ");
                for (DataFileType dataFile : dataFiles) {
                    sb.append(dataFile.getFilename()).append("(").append(dataFile.getSize()).append(") | ");
                }
                log.debug(sb.toString());
            }
            return dokumentDocument;
        }

        /**
         * Adds &lt;SignedDoc/&gt; element(aka digiDoc) containing given files as &lt;DataFile/&gt; to
         * (Dhl)&lt;Dokument/&gt; element
         * 
         * @param contentsToSend - Object with InputStream and name of the file to be used for output and corresponding mimeType
         * @param dokumentContainer - (Dhl)&lt;Dokument/&gt; element to which &lt;SignedDoc/&gt; should be added as a child
         *            node
         * @return {@link #getDataFiles(Map)} based on input files
         */
        private DataFileType[] addSignedDocToDokument(Collection<ContentToSend> contentsToSend, DhlDokumentType dokumentContainer) {
            final DataFileType[] dataFiles = getDataFiles(contentsToSend);
            SignedDocType signedDoc = dokumentContainer.addNewSignedDoc();
            signedDoc.setFormat(SignedDoc.FORMAT_DIGIDOC_XML);
            signedDoc.setVersion(SignedDoc.VERSION_1_3);
            signedDoc.setDataFileArray(dataFiles);
            return dataFiles;
        }

        /**
         * @param contentsToSend - Object with InputStream and name of the file to be used for output and corresponding mimeType
         * @return array of &lt;DataFile &gt; elements to be included inside the digiDoc envelope called &lt;SignedDoc /&gt;
         */
        private DataFileType[] getDataFiles(Collection<ContentToSend> contentsToSend) {
            DataFileType[] files = new DataFileType[contentsToSend.size()];
            int fileIndex = 0;
            for (ContentToSend contentToSend : contentsToSend) {
                DataFileType dataFile = DataFileType.Factory.newInstance();
                dataFile.setFilename(contentToSend.getFileName());
                dataFile.setId("D" + fileIndex);// spec: Andmefailide tunnused algavad sümboliga 'D', millele järgneb faili järjekorranumber
                dataFile.setMimeType(contentToSend.getMimeType());
                dataFile.setContentType(DataFileType.ContentType.EMBEDDED_BASE_64);

                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final InputStream is = contentToSend.getInputStream();
                try {
                    IOUtils.copy(is, bos);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to get input to the file to be sent", e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
                byte[] fileContent = bos.toByteArray();

                dataFile.setSize(BigDecimal.valueOf(fileContent.length));
                dataFile.setStringValue(new String(Base64.encode(fileContent)));
                files[fileIndex++] = dataFile;
            }
            return files;
        }

        /**
         * Creates bytes from document. Removes namespace usage from SignedDoc element (Digidoc container) Amphora test
         * environment is not capable of receiving such xml
         */
        private byte[] getBytes(DokumentDocument dokumentDocument) {
            XmlOptions options = new XmlOptions();
            options.setCharacterEncoding("UTF8");
            options.setSavePrettyPrint();
            options.setUseDefaultNamespace();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try {
                dokumentDocument.save(result, options);
            } catch (IOException e) {
                // IOException when saving to ByteArrayOS should not happen
                log.error("Unexpected exception: " + e);
                throw new java.lang.RuntimeException(e);
            }
            return result.toByteArray();
        }

    }

    private static class MarkDocumentsReceived {
        private static byte[] createMarkDocumentsReceivedAttachmentBody(Collection<String> ids) {
            MarkDocumentsReceivedRequestTypeUnencoded body = MarkDocumentsReceivedRequestTypeUnencoded.Factory.newInstance();
            DocumentRefsArrayType documentRefsArray = DocumentRefsArrayType.Factory.newInstance();
            for (String id : ids) {
                documentRefsArray.addDhlId(id);
            }
            body.setDokumendid(documentRefsArray);
            log.debug("createMarkDocumentsReceivedAttachmentBody::" + body.toString());
            return gzipAndEncodeDocument(body.toString());
        }

    }

    /**
     * @param <T> instance of this class will be returned if parsing <code>inputXml</code> is successful.
     * @param inputXml string representing xml
     * @param responseClass class of returnable instance
     * @return instance of given class T that extends XmlObject, parsed from <code>inputXml</code>
     */
    private static <T extends XmlObject> T getTypeFromXml(String inputXml, Class<T> responseClass) {
        try {
            SchemaType sType = (SchemaType) responseClass.getField("type").get(null);
            if (log.isTraceEnabled()) {
                log.trace("Starting to parse '" + inputXml + "' to class: " + responseClass.getCanonicalName());
            }
            XmlOptions replaceRootNameOpts = new XmlOptions().setLoadReplaceDocumentElement(new QName("xml-fragment"));
            final String xmlFragment = XmlObject.Factory.parse(inputXml, replaceRootNameOpts).toString();
            @SuppressWarnings("unchecked")
            T result = (T) XmlObject.Factory.parse(xmlFragment, new XmlOptions().setDocumentType(sType));
            return result;
        } catch (XmlException e) {
            throw new RuntimeException("Failed to parse '" + inputXml + "' to class: " + responseClass.getCanonicalName(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) { // if above exceptions were not caught it must be because of bad class
            throw new IllegalArgumentException("Failed to get value of '" + responseClass.getCanonicalName()
                    + ".type' to get corresponding SchemaType object: ", e);
        }
    }

    private static <T> List<T> getTypeFromGzippedAndEncodedSoapArray(InputStream inputStream, Class<T> responseClass) {
        SchemaType unencodedType = null;
        try {
            unencodedType = (SchemaType) responseClass.getField("type").get(null);
            log.debug("unencodedType=" + unencodedType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get value of '" + responseClass.getCanonicalName() + ".type' to get corresponding SchemaType object: ", e);
        }
        String responseString = new String(unzipAndDecode(inputStream));
        if (StringUtils.isBlank(responseString)) {
            return Collections.emptyList();
        }
        ArrayList<T> result = null;
        try {
            responseString = "<root>" + responseString + "</root>";
            XmlOptions options = new XmlOptions();
            if (log.isTraceEnabled()) {
                log.trace("Starting to parse '" + responseString + "' to class: " + responseClass.getCanonicalName() + "\n\n");
            }
            XmlObject xmlObject = XmlObject.Factory.parse(responseString, options);
            XmlCursor cursor = xmlObject.newCursor();
            cursor.toFirstChild();
            cursor.toFirstChild();
            options.setDocumentType(unencodedType);

            result = new ArrayList<T>();
            XmlObject currentXmlObject;
            int i = 0;
            do {
                currentXmlObject = cursor.getObject();
                if (log.isTraceEnabled()) {
                    log.trace(i++ + " Token type: '" + cursor.currentTokenType() + "', text:\n" + currentXmlObject + "\n\n");
                }
                @SuppressWarnings("unchecked")
                T resultItem = (T) XmlObject.Factory.parse(cursor.getDomNode(), options);
                result.add(resultItem);
            } while (cursor.toNextSibling());// cursor.toNextSibling(namespace, elementName)
            cursor.dispose();
        } catch (XmlException e) {
            throw new RuntimeException("Failed to parse '" + responseString + "' to class: " + responseClass.getCanonicalName(), e);
        }
        return result;
    }

    private static byte[] gzipAndEncodeDocument(byte[] unencodedAndUnzippedBytes) {
        OutputStream os = null;
        ByteArrayOutputStream encodedOS = null;
        try {
            encodedOS = new ByteArrayOutputStream();
            os = new GZIPOutputStream(encodedOS);
            os.write(unencodedAndUnzippedBytes);
        } catch (IOException e1) {
            throw new RuntimeException("Failed to encode input", e1);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(encodedOS);
        }
        return Base64.encode(encodedOS.toByteArray());
    }

    private static byte[] gzipAndEncodeDocument(String inputString) {
        String DVK_MESSAGE_CHARSET = "UTF-8";
        try {
            return gzipAndEncodeDocument(inputString.getBytes(DVK_MESSAGE_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to get bytes from string because charset" + inputString, e);
        }
    }

    /**
     * Parses given attachments to the instances of the class given with <code>responseClass</code>
     * 
     * @param <T> type that is desired for the results
     * @param attachments input to parse
     * @param responseClass destination class of the type to parse input
     * @param hasRootElement - if attachment bodies contain xml-fragments witout single root element set it to false so that root element would be created to be
     *            able to parse document correctly
     * @return list of attachments parsed to given type
     */
    private static byte[] unzipAndDecode(InputStream inputStream) {
        GZIPInputStream gzipInputStream = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, bos);
            byte[] decoded = Base64.decode(bos.toByteArray());
            gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(decoded));
            bos = new ByteArrayOutputStream();
            IOUtils.copy(gzipInputStream, bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("failed to unzip and decode input", e);
        } finally {
            IOUtils.closeQuietly(gzipInputStream);
        }
    }

    private String resolveMessage(String queryMethod, XTeeServiceConsumptionException e) {
        return "Failed to excecute xtee query " + queryMethod + ". Fault(" + e.getFaultCode() + "): '" + e.getFaultString() + "'";
    }

    // START: getters/setters
    @Override
    public GetDvkOrganizationsHelper getDvkOrganizationsHelper() {
        return dvkOrganizationsHelper;
    }

    @Override
    public void setDvkOrganizationsHelper(GetDvkOrganizationsHelper dvkOrganizationsHelper) {
        this.dvkOrganizationsHelper = dvkOrganizationsHelper;
    }
    // END: getters/setters

}
