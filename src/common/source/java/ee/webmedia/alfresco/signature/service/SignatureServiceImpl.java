package ee.webmedia.alfresco.signature.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import ee.sk.digidoc.CertValue;
import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignatureProductionPlace;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.SignedProperties;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.SAXDigiDocFactory;
import ee.sk.utils.ConfigManager;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;

public class SignatureServiceImpl implements SignatureService {

    private static Logger log = Logger.getLogger(SignatureServiceImpl.class);

    private FileFolderService fileFolderService;
    private NodeService nodeService;

    private String jDigiDocCfg;

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setjDigiDocCfg(String jDigiDocCfg) {
        this.jDigiDocCfg = jDigiDocCfg;
    }

    public void init() {
        if (!ConfigManager.init("jar://" + jDigiDocCfg)) {
            log.error("JDigiDoc initialization failed");
        }
    }

    @Override
    public boolean isDigiDocContainer(NodeRef nodeRef) {
        FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
        return isDigiDocContainer(fileInfo);
    }

    @Override
    public boolean isDigiDocContainer(FileInfo fileInfo) {
        return fileInfo.getName().toLowerCase().endsWith(".ddoc") && !fileInfo.isFolder();
    }
    
    @Override
    public List<SignatureItem> getSignatureItems(NodeRef nodeRef) throws SignatureException {
        SignedDoc ddoc = getSignedDoc(nodeRef);
        return getSignatureItems(nodeRef, ddoc);
    }

    @Override
    public List<DataItem> getDataItems(NodeRef nodeRef, boolean includeData) throws SignatureException {
        SignedDoc ddoc = getSignedDoc(nodeRef);
        return getDataItems(nodeRef, ddoc, includeData);
    }

    @Override
    public DataItem getDataItem(NodeRef nodeRef, int id, boolean includeData) throws SignatureException {
        SignedDoc ddoc = getSignedDoc(nodeRef);
        return getDataItem(nodeRef, ddoc, id, includeData);
    }

    @Override
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(NodeRef nodeRef, boolean includeData) throws SignatureException {
        SignedDoc ddoc = getSignedDoc(nodeRef);
        return getDataItemsAndSignatureItems(ddoc, nodeRef, includeData);
    }

    @Override
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData) throws SignatureException {
        try {
            SignedDoc ddoc = getSignedDoc(inputStream);
            return getDataItemsAndSignatureItems(ddoc, null, includeData);
        } catch (DigiDocException e) {
            throw new SignatureException("Failed to parse ddoc file from InputStream", e);
        } catch (IOException e) {
            throw new SignatureException("Failed to close the input stream");
        }
    }

    @Override
    public SignatureDigest getSignatureDigest(NodeRef nodeRef, String certHex) throws SignatureException {
        try {
            return getSignatureDigest(getSignedDoc(nodeRef), certHex);
        } catch (DigiDocException e) {
            throw new SignatureException("Failed to calculate signed info digest of ddoc file, nodeRef = " + nodeRef, e);
        }
    }

    @Override
    public SignatureDigest getSignatureDigest(List<NodeRef> selectedNodeRefs, String certHex) throws SignatureException {
        try {
            return getSignatureDigest(createSignedDoc(selectedNodeRefs), certHex);
        } catch (DigiDocException e) {
            throw new SignatureException("Failed to calculate signed info digest from selected nodeRefs " + selectedNodeRefs, e);
        }
    }

    @Override
    public NodeRef createContainer(NodeRef parent, List<NodeRef> contents, String filename, SignatureDigest signatureDigest, String signatureHex) {
        try {
            SignedDoc signedDoc = createSignedDoc(contents);
            addSignature(signedDoc, signatureDigest, signatureHex);
            NodeRef newNodeRef = createContentNode(parent, filename);
            writeSignedDoc(newNodeRef, signedDoc);
            return newNodeRef;
        } catch (DigiDocException e) {
            throw new SignatureRuntimeException("Failed to add signature and write ddoc to file " + filename + ", parent = " + parent + ", contents = " + contents, e);
        } catch (SignatureException e) {
            throw new SignatureRuntimeException("Failed to add signature and write ddoc to file " + filename + ", parent = " + parent + ", contents = " + contents, e);
        }
    }

    @Override
    public void addSignature(NodeRef nodeRef, SignatureDigest signatureDigest, String signatureHex) {
        try {
            SignedDoc signedDoc = getSignedDoc(nodeRef);
            addSignature(signedDoc, signatureDigest, signatureHex);
            writeSignedDoc(nodeRef, signedDoc);
        } catch (DigiDocException e) {
            throw new SignatureRuntimeException("Failed to add signature to ddoc file, nodeRef = " + nodeRef, e);
        } catch (SignatureException e) {
            throw new SignatureRuntimeException("Failed to add signature to ddoc file, nodeRef = " + nodeRef, e);
        }
    }

    @Override
    public void writeContainer(NodeRef nodeRef, List<NodeRef> contents, SignatureDigest signatureDigest, String signatureHex) {
        try {
            SignedDoc signedDoc = createSignedDoc(contents);
            addSignature(signedDoc, signatureDigest, signatureHex);
            writeSignedDoc(nodeRef, signedDoc);
            nodeService.setProperty(nodeRef, ContentModel.PROP_NAME, FilenameUtils.removeExtension((String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)) + ".ddoc");
        } catch (DigiDocException e) {
            throw new SignatureRuntimeException("Failed to change existing doc to ddoc or add signature to ddoc file, nodeRef = " + nodeRef, e);
        } catch (SignatureException e) {
            throw new SignatureRuntimeException("Failed to change existing doc to ddoc or add signature to ddoc file, nodeRef = " + nodeRef, e);
        }
    }

    private void addSignature(SignedDoc signedDoc, SignatureDigest signatureDigest, String signatureHex) throws DigiDocException, SignatureException {
        byte[] signatureBytes = SignedDoc.hex2bin(signatureHex);
        Signature sig = prepareSignature(signedDoc, signatureDigest.getCertHex());

        sig.getSignedProperties().setSigningTime(signatureDigest.getDate());
        sig.getSignedInfo().getReferenceForSignedProperties(sig.getSignedProperties()).setDigestValue(sig.getSignedProperties().calculateDigest());

        if (!signatureDigest.getDigestHex().equals(SignedDoc.bin2hex(sig.calculateSignedInfoDigest()))) {
            throw new SignatureException("Signed info digest does not match, files were modified between " + signatureDigest.getDate()
                    + " and now");
        }

        sig.setSignatureValue(signatureBytes);
        sig.getConfirmation();
    }

    private SignatureDigest getSignatureDigest(SignedDoc sd, String certHex) throws DigiDocException {
        Signature signature = prepareSignature(sd, certHex);
        byte[] digestBytes = signature.calculateSignedInfoDigest();
        String digestHex = SignedDoc.bin2hex(digestBytes);
        Date date = signature.getSignedProperties().getSigningTime();
        return new SignatureDigest(digestHex, certHex, date);
    }

    private Signature prepareSignature(SignedDoc signedDoc, String certHex) throws DigiDocException {
        byte[] sertBytes = SignedDoc.hex2bin(certHex);
        X509Certificate sert = SignedDoc.readCertificate(sertBytes);
        return signedDoc.prepareSignature(sert, null, null);
    }

    private SignatureItemsAndDataItems getDataItemsAndSignatureItems(SignedDoc ddoc, NodeRef nodeRef, boolean includeData) {
        SignatureItemsAndDataItems signatureItemsAndDataItems = new SignatureItemsAndDataItems();
        signatureItemsAndDataItems.setSignatureItems(getSignatureItems(nodeRef, ddoc));
        signatureItemsAndDataItems.setDataItems(getDataItems(nodeRef, ddoc, includeData));
        return signatureItemsAndDataItems;
    }

    private DataItem getDataItem(NodeRef nodeRef, SignedDoc ddoc, int id, boolean includeData) {
        DataFile dataFile = ddoc.getDataFile(id);
        if (includeData) {
            return new DataItem(nodeRef, id, dataFile.getFileName(), dataFile.getMimeType(), dataFile.getInitialCodepage(), dataFile.getSize(), dataFile
                    .getBodyAsData());
        }
        return new DataItem(nodeRef, id, dataFile.getFileName(), dataFile.getMimeType(), dataFile.getInitialCodepage(), dataFile.getSize());
    }

    private List<DataItem> getDataItems(NodeRef nodeRef, SignedDoc ddoc, boolean includeData) {
        int filesNumber = ddoc.countDataFiles();
        List<DataItem> items = new ArrayList<DataItem>(filesNumber);
        for (int i = 0; i < filesNumber; ++i) {
            DataItem item = getDataItem(nodeRef, ddoc, i, includeData);
            items.add(item);
        }
        return items;
    }

    private SignedDoc getSignedDoc(NodeRef nodeRef) throws SignatureException {
        if (!isDigiDocContainer(nodeRef)) {
            throw new SignatureException("NodeRef is not a digidoc: " + nodeRef);
        }
        try {
            InputStream contentInputStream = fileFolderService.getReader(nodeRef).getContentInputStream();
            return getSignedDoc(contentInputStream);
        } catch (DigiDocException e) {
            throw new SignatureException("Failed to parse ddoc file, nodeRef = " + nodeRef, e);
        } catch (IOException e) {
            throw new SignatureException("Failed to close the input stream, nodeRef = " + nodeRef);
        }
    }

    private SignedDoc getSignedDoc(InputStream contentInputStream) throws DigiDocException, IOException {
        try {
            DigiDocFactory digiDocFactory = new SAXDigiDocFactory();
            return digiDocFactory.readSignedDoc(contentInputStream);
        } finally {
            contentInputStream.close();
        }
    }

    private List<SignatureItem> getSignatureItems(NodeRef nodeRef, SignedDoc ddoc) {
        int signNumber = ddoc.countSignatures();
        List<SignatureItem> items = new ArrayList<SignatureItem>(signNumber);
        for (int i = 0; i < signNumber; ++i) {
            SignatureItem item = new SignatureItem();

            Signature signature = ddoc.getSignature(i);
            CertValue certValue = signature.getCertValueOfType(CertValue.CERTVAL_TYPE_SIGNER);
            X509Certificate cert = certValue.getCert();
            String subjectFirstName = SignedDoc.getSubjectFirstName(cert);
            String subjectLastName = SignedDoc.getSubjectLastName(cert);
            String subjectPersonalCode = SignedDoc.getSubjectPersonalCode(cert);

            item.setName(subjectFirstName + " " + subjectLastName);
            item.setLegalCode(subjectPersonalCode);

            SignedProperties signedProperties = signature.getSignedProperties();
            int claimedRolesNumber = signedProperties.countClaimedRoles();
            List<String> roles = new ArrayList<String>(claimedRolesNumber);
            for (int j = 0; j < claimedRolesNumber; ++j) {
                roles.add(signedProperties.getClaimedRole(j));
            }
            item.setClaimedRoles(roles);
            item.setSigningTime(signedProperties.getSigningTime());
            SignatureProductionPlace signatureProductionPlace = signedProperties.getSignatureProductionPlace();

            item.setAddress(signatureProductionPlace2String(signatureProductionPlace));

            // 2nd arg - check the certs validity dates
            // 3rd arg - check OCSP confirmation
            List<?> errors = signature.verify(ddoc, false, true);
            if (!errors.isEmpty() && log.isDebugEnabled()) {
                log.debug("Signature (id = " + i + ") verification returned errors" + (nodeRef != null ? ", nodeRef = " + nodeRef : "") + " : \n" + errors);
            }
            item.setValid(errors.isEmpty());

            items.add(item);

        }
        return items;
    }

    private String signatureProductionPlace2String(SignatureProductionPlace signatureProductionPlace) {
        if (signatureProductionPlace == null) {
            return null;
        }
        List<String> address = new ArrayList<String>();
        address.add(signatureProductionPlace.getCity());
        address.add(signatureProductionPlace.getPostalCode());
        address.add(signatureProductionPlace.getCountryName());

        return StringUtils.collectionToDelimitedString(address, ", ");
    }

    private void writeSignedDoc(NodeRef nodeRef, SignedDoc document) throws DigiDocException {
        ContentWriter writer = fileFolderService.getWriter(nodeRef);
        writer.setMimetype(SignatureService.DIGIDOC_MIMETYPE);
        writer.setEncoding("UTF-8");
        OutputStream os = writer.getContentOutputStream();
        document.writeToStream(os);
        try {
            os.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close output stream of file, nodeRef = " + nodeRef, e);
        }
    }

    private SignedDoc createSignedDoc(List<NodeRef> nodeRefs) throws DigiDocException {
        // DIGIDOC-XML 1.3 format is used. Version 1.4 only adds RFC 3161 timestamp support. But in Estonia the OCSP service provided by SK also provides
        // timestamping support, so there is no need to use RFC 3161 timestamps. Also DigiDoc Client software produces DIGIDOC-XML 1.3 files.
        // Refer to http://www.sk.ee/pages/0202070109 for further information.
        SignedDoc document = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
        for (NodeRef ref : nodeRefs) {
            addDataFile(ref, document);
        }
        return document;
    }

    private NodeRef createContentNode(NodeRef parentRef, String filename) {
        // assign the file name
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, filename);
        // create new content node under parentRef
        FileInfo fileInfo = fileFolderService.create(parentRef, filename, ContentModel.TYPE_CONTENT);
        return fileInfo.getNodeRef();
    }

    private void addDataFile(NodeRef nodeRef, SignedDoc document) throws DigiDocException {
        String fileName = fileFolderService.getFileInfo(nodeRef).getName();
        ContentReader reader = fileFolderService.getReader(nodeRef);
        String mimeType = reader.getMimetype();
        DataFile datafile = new DataFile(document.getNewDataFileId(), DataFile.CONTENT_EMBEDDED_BASE64, fileName, mimeType, document);
        ByteArrayOutputStream os = new ByteArrayOutputStream((int) reader.getSize());
        reader.getContent(os);
        // TODO reader.getEncoding() sometimes returns "utf-8", sometimes "UTF-8"
        datafile.setBody(os.toByteArray(), reader.getEncoding().toUpperCase());

        document.addDataFile(datafile);
    }

}
