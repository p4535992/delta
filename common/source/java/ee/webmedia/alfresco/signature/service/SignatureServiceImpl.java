package ee.webmedia.alfresco.signature.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import ee.sk.digidoc.CertValue;
import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignatureProductionPlace;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.SignedProperties;
import ee.sk.digidoc.factory.SAXDigiDocFactory;
import ee.sk.utils.ConfigManager;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.utils.Timer;

public class SignatureServiceImpl implements SignatureService, InitializingBean {

    private static Logger log = Logger.getLogger(SignatureServiceImpl.class);

    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private MimetypeService mimetypeService;
    private boolean test = false;

    private String jDigiDocCfg;
    private String jDigiDocCfgTest;
    private String pkcs12Container;
    private String pkcs12Password;
    private String pkcs12CertSerial;

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setjDigiDocCfg(String jDigiDocCfg) {
        this.jDigiDocCfg = jDigiDocCfg;
    }

    public void setjDigiDocCfgTest(String jDigiDocCfg) {
        jDigiDocCfgTest = jDigiDocCfg;
    }

    public void setPkcs12Container(String pkcs12Container) {
        this.pkcs12Container = pkcs12Container;
    }

    public void setPkcs12Password(String pkcs12Password) {
        this.pkcs12Password = pkcs12Password;
    }

    public void setPkcs12CertSerial(String pkcs12CertSerial) {
        this.pkcs12CertSerial = pkcs12CertSerial;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.hasLength(pkcs12Container)) {
            File container = new File(pkcs12Container);
            if (!container.canRead()) {
                throw new RuntimeException("Cannot read PKCS12 container file: " + container);
            }
            if (!container.isFile()) {
                throw new RuntimeException("PKCS12 container is not a regular file: " + container);
            }
            if (!StringUtils.hasText(pkcs12Password)) {
                throw new RuntimeException("PKCS12 container password must not be empty");
            }
            if (!StringUtils.hasText(pkcs12CertSerial)) {
                throw new RuntimeException("PKCS12 certificate serial number must not be empty");
            }
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put("SIGN_OCSP_REQUESTS", "true");
            props.put("DIGIDOC_PKCS12_CONTAINER", pkcs12Container);
            props.put("DIGIDOC_PKCS12_PASSWD", pkcs12Password);
            props.put("DIGIDOC_OCSP_SIGN_CERT_SERIAL", pkcs12CertSerial);
            log.info("Signing OCSP requests with certificate (serial number " + pkcs12CertSerial + ") from PKCS12 container " + pkcs12Container);
            // This must be done first, because init(Hashtable) resets configuration
            ConfigManager.init(props);
        }
        // This is done second, because init(String) does not reset configuration
        if (!ConfigManager.init("jar://" + (isTest() ? jDigiDocCfgTest : jDigiDocCfg))) {
            throw new RuntimeException("JDigiDoc initialization failed");
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
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(NodeRef nodeRef, boolean includeData) throws SignatureException {
        SignedDoc signedDoc = null;
        try {
            signedDoc = getSignedDoc(nodeRef, includeData);
            return getDataItemsAndSignatureItems(signedDoc, nodeRef, includeData);
        } catch (Exception e) {
            throw new SignatureException("Failed to get ddoc data and signature items, nodeRef = " + nodeRef + " includeData = " + includeData, e);
        }
    }

    @Override
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData) throws SignatureException {
        SignedDoc signedDoc = null;
        try {
            signedDoc = getSignedDoc(inputStream, includeData);
            return getDataItemsAndSignatureItems(signedDoc, null, includeData);
        } catch (Exception e) {
            throw new SignatureException("Failed to get ddoc data and signature items, inputStream = "
                    + ObjectUtils.identityToString(inputStream) + ", includeData = " + includeData, e);
        }
    }

    @Override
    public SignatureDigest getSignatureDigest(NodeRef nodeRef, String certHex) throws SignatureException {
        SignedDoc signedDoc = null;
        try {
            signedDoc = getSignedDoc(nodeRef, true);
            SignatureDigest signatureDigest = getSignatureDigest(signedDoc, certHex);
            return signatureDigest;
        } catch (Exception e) {
            throw new SignatureException("Failed to calculate signed info digest of ddoc file, nodeRef = " + nodeRef + ", certHex = " + certHex, e);
        }
    }

    @Override
    public SignatureDigest getSignatureDigest(List<NodeRef> selectedNodeRefs, String certHex) throws SignatureException {
        SignedDoc signedDoc = null;
        try {
            signedDoc = createSignedDoc(selectedNodeRefs);
            SignatureDigest signatureDigest = getSignatureDigest(signedDoc, certHex);
            return signatureDigest;
        } catch (Exception e) {
            throw new SignatureException("Failed to calculate signed info digest from selected nodeRefs " + selectedNodeRefs + ", certHex = " + certHex, e);
        }
    }

    @Override
    public NodeRef createContainer(NodeRef parent, List<NodeRef> contents, String filename, SignatureDigest signatureDigest, String signatureHex) {
        SignedDoc signedDoc = null;
        try {
            signedDoc = createSignedDoc(contents);
            addSignature(signedDoc, signatureDigest, signatureHex);
            NodeRef newNodeRef = createContentNode(parent, filename);
            writeSignedDoc(newNodeRef, signedDoc);
            return newNodeRef;
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to add signature and write ddoc to file " + filename + ", parent = " + parent + ", contents = "
                    + contents + ", signatureDigest = " + signatureDigest + ", signatureHex = " + signatureHex, e);
        }
    }

    @Override
    public void addSignature(NodeRef nodeRef, SignatureDigest signatureDigest, String signatureHex) {
        SignedDoc signedDoc = null;
        try {
            signedDoc = getSignedDoc(nodeRef, true);
            addSignature(signedDoc, signatureDigest, signatureHex);
            writeSignedDoc(nodeRef, signedDoc);
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to add signature to ddoc file, nodeRef = " + nodeRef + ", signatureDigest = " + signatureDigest
                    + ", signatureHex = " + signatureHex, e);
        }
    }

    @Override
    public void writeContainer(NodeRef nodeRef, List<NodeRef> contents, SignatureDigest signatureDigest, String signatureHex) {
        SignedDoc signedDoc = null;
        try {
            signedDoc = createSignedDoc(contents);
            addSignature(signedDoc, signatureDigest, signatureHex);
            writeSignedDoc(nodeRef, signedDoc);
            nodeService.setProperty(nodeRef, ContentModel.PROP_NAME,
                    FilenameUtils.removeExtension((String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)) + ".ddoc");
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to change existing doc to ddoc or add signature to ddoc file, nodeRef = " + nodeRef
                    + ", signatureDigest = " + signatureDigest + ", signatureHex = " + signatureHex, e);
        }
    }

    private void addSignature(SignedDoc signedDoc, SignatureDigest signatureDigest, String signatureHex) throws DigiDocException, SignatureException {
        byte[] signatureBytes = SignedDoc.hex2bin(signatureHex);
        Signature sig = prepareSignature(signedDoc, signatureDigest.getCertHex());

        sig.getSignedProperties().setSigningTime(signatureDigest.getDate());
        sig.getSignedInfo().getReferenceForSignedProperties(sig.getSignedProperties()).setDigestValue(sig.getSignedProperties().calculateDigest());

        if (!signatureDigest.getDigestHex().equals(SignedDoc.bin2hex(sig.calculateSignedInfoDigest()))) {
            throw new SignatureException("Signed info digest does not match, files were modified between " + signatureDigest.getDate() + " " + signatureDigest.getDate().getTime()
                    + " and now");
        }

        sig.setSignatureValue(signatureBytes);
        Timer timer = new Timer("signatureConfirmation");

        // If OCSP response is successful, then no exception is thrown
        sig.getConfirmation();

        // If certificate has been revoked, then the following error is logged:
        // ERROR [ee.sk.digidoc.factory.BouncyCastleNotaryFactory] - <Certificate has been revoked!>
        // And the following exception is thrown:
        // DigiDocException.code: 88
        // DigiDocException.message: Certificate has been revoked!

        // If certificate is unknown, then the following error is logged:
        // ERROR [ee.sk.digidoc.factory.BouncyCastleNotaryFactory] - <Certificate status is unknown!>
        // And the following exception is thrown:
        // DigiDocException.code: 88
        // DigiDocException.message: Certificate status is unknown!

        // If OCSP server refuses request (for example there is no access allowed from this IP, or PKCS12 (juurdepääsutõend) is expired),
        // then the following error is logged:
        // ERROR [ee.sk.digidoc.factory.BouncyCastleNotaryFactory] - <The server could not authenticate you!>
        // And the following exception is thrown:
        // DigiDocException.code: 69
        // DigiDocException.message: OCSP response unsuccessfull!

        // If connection to OCSP server fails, then the following error is logged:
        // ERROR [ee.sk.digidoc.DigiDocException] - <java.net.ConnectException: Connection refused>
        // And the following exception is thrown:
        // DigiDocException.code: 65
        // DigiDocException.message: ERROR: 65java.net.ConnectException; nested exception is:
        // java.net.ConnectException: Connection refused

        // If connection to OCSP server fails, then the following error is logged:
        // ERROR [ee.sk.digidoc.DigiDocException] - <java.net.ConnectException: Connection timed out>
        // And the following exception is thrown:
        // DigiDocException.code: 65
        // DigiDocException.message: ERROR: 65java.net.ConnectException; nested exception is:
        // java.net.ConnectException: Connection timed out

        log.debug(timer);
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
        List<SignatureItem> signatureItems = getSignatureItems(nodeRef, ddoc);
        List<DataItem> dataItems = getDataItems(nodeRef, ddoc, includeData);
        return new SignatureItemsAndDataItems(signatureItems, dataItems);
    }

    private DataItem getDataItem(NodeRef nodeRef, SignedDoc ddoc, int id, boolean includeData) {
        DataFile dataFile = ddoc.getDataFile(id);
        String fileName = dataFile.getFileName();
        String mimeType = dataFile.getMimeType();
        String guessedMimetype = mimetypeService.guessMimetype(fileName);
        if (MimetypeMap.MIMETYPE_BINARY.equals(guessedMimetype) && org.apache.commons.lang.StringUtils.isNotBlank(mimeType)) {
            guessedMimetype = mimeType;
        }
        if (includeData) {
            return new DataItem(nodeRef, id, fileName, guessedMimetype, dataFile.getInitialCodepage(), dataFile.getSize(), dataFile);
        }
        return new DataItem(nodeRef, id, fileName, guessedMimetype, dataFile.getInitialCodepage(), dataFile.getSize());
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

    private SignedDoc getSignedDoc(NodeRef nodeRef, boolean fileContents) throws SignatureException {
        try {
            if (!isDigiDocContainer(nodeRef)) {
                throw new SignatureException("NodeRef is not a digidoc: " + nodeRef);
            }
            ContentReader reader = fileFolderService.getReader(nodeRef);
            if (reader == null) {
                throw new SignatureException("NodeRef has no content: " + nodeRef);
            }
            InputStream contentInputStream = reader.getContentInputStream();
            if (contentInputStream != null) {
                return getSignedDoc(contentInputStream, fileContents);
            }
            throw new SignatureException("NodeRef has no content: " + nodeRef);
        } catch (Exception e) {
            if (e instanceof SignatureException) {
                throw (SignatureException) e;
            }
            throw new SignatureException("Failed to parse ddoc file, nodeRef = " + nodeRef, e);
        }
    }

    private SignedDoc getSignedDoc(InputStream contentInputStream, boolean fileContents) throws DigiDocException, IOException {
        try {
            // ConfigManager (in some versions of JDigiDoc library) caches DigiDocFactory instance
            // and SAXDigiDocFactory is not thread-safe! So we create a new instance each time:
            SAXDigiDocFactory digiDocFactory = new SAXDigiDocFactory();
            digiDocFactory.init();

            // Cannot use more generic read method that detects type (DDOC/BDOC), beacuse it is buggy
            // (detect method reads from stream, and when parse is invoked, stream is not at the beginning any more)
            ArrayList<DigiDocException> errors = new ArrayList<DigiDocException>();
            SignedDoc signedDoc = digiDocFactory.readSignedDocFromStreamOfType(contentInputStream, false, errors);
            for (DigiDocException ex : errors) {
                // See DELTA-295
                if (ex.getCode() == DigiDocException.ERR_ISSUER_XMLNS) {
                    continue;
                }
                throw ex;
            }
            if (fileContents) {
                bindCleanTempFiles(signedDoc);
            }
            return signedDoc;
        } finally {
            contentInputStream.close();
        }
    }

    private List<SignatureItem> getSignatureItems(NodeRef nodeRef, SignedDoc ddoc) {
        int signNumber = ddoc.countSignatures();
        List<SignatureItem> items = new ArrayList<SignatureItem>(signNumber);
        for (int i = 0; i < signNumber; ++i) {
            Signature signature = ddoc.getSignature(i);
            CertValue certValue = signature.getCertValueOfType(CertValue.CERTVAL_TYPE_SIGNER);
            X509Certificate cert = certValue.getCert();
            String subjectFirstName = SignedDoc.getSubjectFirstName(cert);
            String subjectLastName = SignedDoc.getSubjectLastName(cert);
            String legalCode = SignedDoc.getSubjectPersonalCode(cert);
            String name = subjectFirstName + " " + subjectLastName;

            SignedProperties signedProperties = signature.getSignedProperties();
            int claimedRolesNumber = signedProperties.countClaimedRoles();
            List<String> roles = new ArrayList<String>(claimedRolesNumber);
            for (int j = 0; j < claimedRolesNumber; ++j) {
                roles.add(signedProperties.getClaimedRole(j));
            }
            // signedProperties.getSigningTime() - when digest is computed, before PIN2 dialog is displayed
            // unsignedProperties().getNotary().getProducedAt(); - after PIN2 is entered and OCSP is acquired - DigiDocClient also uses this
            Date signingTime = signature.getUnsignedProperties().getNotary().getProducedAt();
            String address = signatureProductionPlace2String(signedProperties.getSignatureProductionPlace());

            // 2nd arg - check the certs validity dates
            // 3rd arg - check OCSP confirmation
            List<?> errors = signature.verify(ddoc, false, true);
            if (!errors.isEmpty() && log.isDebugEnabled()) {
                log.debug("Signature (id = " + i + ") verification returned errors" + (nodeRef != null ? ", nodeRef = " + nodeRef : "") + " : \n" + errors);
            }
            boolean valid = errors.isEmpty();

            SignatureItem item = new SignatureItem(name, legalCode, signingTime, roles, address, valid);
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

    private void writeSignedDoc(NodeRef nodeRef, SignedDoc document) throws DigiDocException, IOException {
        ContentWriter writer = fileFolderService.getWriter(nodeRef);
        writer.setMimetype(SignatureService.DIGIDOC_MIMETYPE);
        writer.setEncoding(AppConstants.CHARSET);
        OutputStream os = writer.getContentOutputStream();
        try {
            document.writeToStream(os);
        } finally {
            os.close();
        }
    }

    private SignedDoc createSignedDoc(List<NodeRef> nodeRefs) throws DigiDocException, IOException {
        // DIGIDOC-XML 1.3 format is used. Version 1.4 only adds RFC 3161 timestamp support. But in Estonia the OCSP service provided by SK also provides
        // timestamping support, so there is no need to use RFC 3161 timestamps. Also DigiDoc Client software produces DIGIDOC-XML 1.3 files.
        // Refer to http://www.sk.ee/pages/0202070109 for further information.
        SignedDoc document = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
        bindCleanTempFiles(document);
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

    private void addDataFile(NodeRef nodeRef, SignedDoc document) throws DigiDocException, IOException {
        String fileName = getFileName(nodeRef);
        ContentReader reader = fileFolderService.getReader(nodeRef);
        String mimeType = reader.getMimetype();
        DataFile datafile = new DataFile(document.getNewDataFileId(), DataFile.CONTENT_EMBEDDED_BASE64, fileName, mimeType, document);

        datafile.createCacheFile();

        // Newlines must always be writtes as '\n', otherwise signature is not valid
        OutputStream os = new Base64OutputStream(new BufferedOutputStream(new FileOutputStream(datafile.getDfCacheFile())), true, 64, new byte[] { '\n' });
        reader.getContent(os); // closes both streams

        datafile.setSize(reader.getSize());

        // XXX reader.getEncoding() sometimes returns "utf-8", sometimes "UTF-8"
        datafile.setInitialCodepage(reader.getEncoding().toUpperCase());

        document.addDataFile(datafile);
    }

    private static void bindCleanTempFiles(final SignedDoc signedDoc) {
        try {
            Assert.notNull(AlfrescoTransactionSupport.getTransactionId(), "No transaction is present");

            if (log.isDebugEnabled()) {
                StringBuilder s = new StringBuilder("bindCleanTempFiles");
                s.append("\n  signedDoc=").append(ObjectUtils.identityToString(signedDoc));
                s.append("\n  countDataFiles=").append(signedDoc.countDataFiles());
                for (int i = 0; i < signedDoc.countDataFiles(); i++) {
                    s.append("\n  dataFile[").append(i).append("]=").append(signedDoc.getDataFile(i).getFileName());
                }
                log.debug(s.toString());
            }

            AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
                @Override
                public void beforeCompletion() {
                    cleanTempFiles(signedDoc);
                }
            });
        } catch (RuntimeException e) {
            cleanTempFiles(signedDoc);
            throw e;
        }
    }

    private static void cleanTempFiles(SignedDoc signedDoc) {
        try {
            if (log.isDebugEnabled()) {
                StringBuilder s = new StringBuilder("cleanTempFiles");
                s.append("\n  signedDoc=").append(ObjectUtils.identityToString(signedDoc));
                s.append("\n  countDataFiles=").append(signedDoc.countDataFiles());
                for (int i = 0; i < signedDoc.countDataFiles(); i++) {
                    s.append("\n  dataFile[").append(i).append("]=").append(signedDoc.getDataFile(i).getFileName());
                }
                log.debug(s.toString());
            }

            signedDoc.cleanupDfCache();
        } catch (Exception e) {
            log.error("Error cleaning temp files for ddoc " + ObjectUtils.identityToString(signedDoc), e);
            // Do nothing, because this method is usually called from a finally or catch block,
            // and then there may be the throwing of an exception already in progress
        }
    }

    /**
     * @param fileRef file NodeRef
     * @return filename that corresponds to filename rules (does not contain special characters and is <= 255 chars in length)
     */
    protected String getFileName(NodeRef fileRef) {
        return (String) nodeService.getProperty(fileRef, ContentModel.PROP_NAME);
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isTest() {
        return test;
    }

}
