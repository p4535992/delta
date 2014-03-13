package ee.webmedia.alfresco.signature.service;

import static ee.webmedia.alfresco.utils.CalendarUtil.duration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

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
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.sk.digidoc.Base64Util;
import ee.sk.digidoc.CertValue;
import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignatureProductionPlace;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.SignedProperties;
import ee.sk.digidoc.factory.SAXDigiDocFactory;
import ee.sk.digidocservice.DataFileDigest;
import ee.sk.digidocservice.DataFileDigestList;
import ee.sk.digidocservice.DigiDocServicePortType;
import ee.sk.digidocservice.ObjectFactory;
import ee.sk.utils.ConfigManager;
import ee.sk.xmlenc.EncryptedData;
import ee.sk.xmlenc.EncryptedKey;
import ee.sk.xmlenc.EncryptionProperty;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureChallenge;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;

public class SignatureServiceImpl implements SignatureService, InitializingBean {

    private static final String ID_CODE_COUNTRY_EE = "EE";
    private static final String ID_CODE_COUNTRY_LT = "LT";

    private static Logger log = Logger.getLogger(SignatureServiceImpl.class);

    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private MimetypeService mimetypeService;
    private JaxWsProxyFactoryBean digiDocServiceFactory;
    private boolean test = false;

    private String jDigiDocCfg;
    private String jDigiDocCfgTest;
    private String pkcs12Container;
    private String pkcs12Password;
    private String pkcs12CertSerial;
    private String digiDocServiceUrl;
    private String testDigiDocServiceUrl;
    private String mobileIdServiceName;
    private String testMobileIdServiceName;

    private DigiDocServicePortType digiDocService;

    private static final Map<String, Pair<String, String>> testPhoneNumbers = new HashMap<String, Pair<String, String>>();

    static {
        testPhoneNumbers.put("37200007", Pair.newInstance("14212128025", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37260000007", Pair.newInstance("51001091072", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200001", Pair.newInstance("38002240211", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200002", Pair.newInstance("14212128020", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200003", Pair.newInstance("14212128021", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200004", Pair.newInstance("14212128022", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200005", Pair.newInstance("14212128023", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200006", Pair.newInstance("14212128024", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200008", Pair.newInstance("14212128026", ID_CODE_COUNTRY_EE));
        testPhoneNumbers.put("37200009", Pair.newInstance("14212128027", ID_CODE_COUNTRY_EE));

        testPhoneNumbers.put("37060000007", Pair.newInstance("51001091072", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000001", Pair.newInstance("51001091006", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000002", Pair.newInstance("51001091017", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000003", Pair.newInstance("51001091028", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000004", Pair.newInstance("51001091039", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000005", Pair.newInstance("51001091050", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000006", Pair.newInstance("51001091061", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000008", Pair.newInstance("51001091083", ID_CODE_COUNTRY_LT));
        testPhoneNumbers.put("37060000009", Pair.newInstance("51001091094", ID_CODE_COUNTRY_LT));
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setDigiDocServiceFactory(JaxWsProxyFactoryBean digiDocServiceFactory) {
        this.digiDocServiceFactory = digiDocServiceFactory;
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

    public void setDigiDocServiceUrl(String digiDocServiceUrl) {
        this.digiDocServiceUrl = digiDocServiceUrl;
    }

    public void setTestDigiDocServiceUrl(String testDigiDocServiceUrl) {
        this.testDigiDocServiceUrl = testDigiDocServiceUrl;
    }

    public void setMobileIdServiceName(String mobileIdServiceName) {
        this.mobileIdServiceName = mobileIdServiceName;
    }

    public void setTestMobileIdServiceName(String testMobileIdServiceName) {
        this.testMobileIdServiceName = testMobileIdServiceName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isNotEmpty(pkcs12Container)) {
            File container = new File(pkcs12Container);
            if (!container.canRead()) {
                throw new RuntimeException("Cannot read PKCS12 container file: " + container);
            }
            if (!container.isFile()) {
                throw new RuntimeException("PKCS12 container is not a regular file: " + container);
            }
            if (!StringUtils.isNotEmpty(pkcs12Password)) {
                throw new RuntimeException("PKCS12 container password must not be empty");
            }
            if (!StringUtils.isNotEmpty(pkcs12CertSerial)) {
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
        digiDocServiceFactory.setAddress(isTest() ? testDigiDocServiceUrl : digiDocServiceUrl);
        digiDocServiceFactory.setServiceName(new javax.xml.namespace.QName("http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl", "DigiDocService"));
        digiDocServiceFactory.setEndpointName(new javax.xml.namespace.QName("http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl", "DigiDocService"));
        digiDocService = (DigiDocServicePortType) digiDocServiceFactory.create();
    }

    @Override
    public boolean isDigiDocContainer(NodeRef nodeRef) {
        FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
        return isDigiDocContainer(fileInfo);
    }

    @Override
    public boolean isDigiDocContainer(FileInfo fileInfo) {
        return FilenameUtil.isDigiDocFile(fileInfo.getName()) && !fileInfo.isFolder();
    }

    @Override
    public boolean isMobileIdEnabled() {
        return StringUtils.isNotEmpty(getMobileIdServiceName());
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
    public SignatureChallenge getSignatureChallenge(NodeRef nodeRef, String phoneNo, String idCode) throws SignatureException {
        SignedDoc signedDoc = null;
        try {
            signedDoc = getSignedDoc(nodeRef, true);
            SignatureChallenge signatureChallenge = getSignatureChallenge(signedDoc, phoneNo, idCode);
            return signatureChallenge;
        } catch (UnableToPerformException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException("Failed to create Mobile-ID signing request of ddoc file, nodeRef = " + nodeRef + ", phoneNo = " + phoneNo, e);
        }
    }

    @Override
    public SignatureDigest getSignatureDigest(List<NodeRef> contents, String certHex) throws SignatureException {
        SignedDoc signedDoc = null;
        try {
            signedDoc = createSignedDoc(contents);
            SignatureDigest signatureDigest = getSignatureDigest(signedDoc, certHex);
            return signatureDigest;
        } catch (Exception e) {
            throw new SignatureException("Failed to calculate signed info digest from contents = " + contents + ", certHex = " + certHex, e);
        }
    }

    @Override
    public SignatureChallenge getSignatureChallenge(List<NodeRef> contents, String phoneNo, String idCode) throws SignatureException {
        SignedDoc signedDoc = null;
        try {
            signedDoc = createSignedDoc(contents);
            SignatureChallenge signatureDigest = getSignatureChallenge(signedDoc, phoneNo, idCode);
            return signatureDigest;
        } catch (UnableToPerformException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException("Failed to create Mobile-ID signing request from contents = " + contents + ", phoneNo = " + phoneNo, e);
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
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to add signature and write ddoc to file " + filename + ", parent = " + parent + ", contents = "
                    + contents + ", signatureDigest = " + signatureDigest + ", signatureHex = " + signatureHex, e);
        }
    }

    @Override
    public NodeRef createContainer(NodeRef parent, List<NodeRef> contents, String filename, SignatureChallenge signatureChallenge, String signature) {
        try {
            SignedDoc signedDoc = createSignedDoc(contents);
            addSignature(signedDoc, signatureChallenge, signature);
            NodeRef newNodeRef = createContentNode(parent, filename);
            writeSignedDoc(newNodeRef, signedDoc);
            return newNodeRef;
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to add signature and write ddoc to file " + filename + ", parent = " + parent + ", contents = "
                    + contents + ", signatureChallenge = " + signatureChallenge, e);
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
    public void addSignature(NodeRef nodeRef, SignatureChallenge signatureChallenge, String signature) {
        SignedDoc signedDoc = null;
        try {
            signedDoc = getSignedDoc(nodeRef, true);
            addSignature(signedDoc, signatureChallenge, signature);
            writeSignedDoc(nodeRef, signedDoc);
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to add signature to ddoc file, nodeRef = " + nodeRef + ", signatureChallenge = " + signatureChallenge, e);
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

    @Override
    public void writeContainer(OutputStream output, List<NodeRef> contents) {
        try {
            SignedDoc signedDoc = createSignedDoc(contents);
            try {
                signedDoc.writeToStream(output);
            } finally {
                output.close();
            }
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to write ddoc file, output = " + output + ", contents = " + contents, e);
        }
    }

    private void addSignature(SignedDoc signedDoc, SignatureDigest signatureDigest, String signatureHex) throws Exception {
        byte[] signatureBytes = SignedDoc.hex2bin(signatureHex);
        Signature sig = prepareSignature(signedDoc, signatureDigest.getCertHex());

        sig.getSignedProperties().setSigningTime(signatureDigest.getDate());
        sig.getSignedInfo().getReferenceForSignedProperties(sig.getSignedProperties()).setDigestValue(sig.getSignedProperties().calculateDigest());

        if (!signatureDigest.getDigestHex().equals(SignedDoc.bin2hex(sig.calculateSignedInfoDigest()))) {
            throw new SignatureException("Signed info digest does not match, files were modified between " + signatureDigest.getDate() + " " + signatureDigest.getDate().getTime()
                    + " and now");
        }

        sig.setSignatureValue(signatureBytes);

        // If OCSP response is successful, then no exception is thrown
        long startTime = System.nanoTime();
        try {
            sig.getConfirmation();
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_OCSP);
        } catch (Exception e) {
            if (e instanceof DigiDocException && ((DigiDocException) e).getCode() == 88) {
                MonitoringUtil.logSuccess(MonitoredService.OUT_SK_OCSP);
            } else {
                MonitoringUtil.logError(MonitoredService.OUT_SK_OCSP, e);
            }
            throw e;
        }
        long stopTime = System.nanoTime();
        log.info("PERFORMANCE: query skOcspSignatureConfirmation - " + duration(startTime, stopTime) + " ms");

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
            String name = UserUtil.getPersonFullName(subjectFirstName, subjectLastName);

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

        return StringUtils.join(address, ", ");
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

    private SignatureChallenge getSignatureChallenge(SignedDoc signedDoc, String phoneNo, String idCode) throws DigiDocException {
        phoneNo = StringUtils.stripToEmpty(phoneNo);
        idCode = StringUtils.stripToEmpty(idCode);

        ObjectFactory objectFactory = new ObjectFactory();

        List<String> digestHexs = new ArrayList<String>();
        DataFileDigestList dataFileDigestList = objectFactory.createDataFileDigestList();
        for (int i = 0; i < signedDoc.countDataFiles(); i++) {
            DataFile df = signedDoc.getDataFile(i);
            String digestHex = Base64Util.encode(df.getDigest());
            digestHexs.add(digestHex);

            DataFileDigest dataFileDigest = objectFactory.createDataFileDigest();
            dataFileDigest.setId(objectFactory.createDataFileDigestId(df.getId()));
            dataFileDigest.setDigestType(objectFactory.createDataFileDigestDigestType(DataFile.DIGEST_TYPE_SHA1));
            dataFileDigest.setDigestValue(objectFactory.createDataFileDigestDigestValue(digestHex));
            dataFileDigestList.getDataFileDigest().add(dataFileDigest);
        }
        String signatureId = signedDoc.getNewSignatureId();
        String format = signedDoc.getFormat();
        String version = signedDoc.getVersion();

        // REQUEST
        // @formatter:off
        // <dig:MobileCreateSignature soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
        //    <PhoneNo xsi:type="xsd:string">+3725217229</PhoneNo>
        //    <Language xsi:type="xsd:string">EST</Language>
        //    <ServiceName xsi:type="xsd:string">Testimine</ServiceName>
        //    <MessageToDisplay xsi:type="xsd:string">TODO... Maksimaalne pikkus 40 tähemärki</MessageToDisplay>
        //    <DataFiles xsi:type="dig:DataFileDigestList">
        //       <DataFileDigest xsi:type="dig:DataFileDigest">
        //          <Id xsi:type="xsd:string">D0</Id>
        //          <DigestType xsi:type="xsd:string">sha1</DigestType>
        //          <DigestValue xsi:type="xsd:string">t8eRSrKTgR4PAAKTLYWGCjuTSJA=</DigestValue>
        //       </DataFileDigest>
        //    </DataFiles>
        //    <Format xsi:type="xsd:string">DIGIDOC-XML</Format>
        //    <Version xsi:type="xsd:string">1.3</Version>
        //    <SignatureID xsi:type="xsd:string">S0</SignatureID>
        //    <MessagingMode xsi:type="xsd:string">asynchClientServer</MessagingMode>
        // </dig:MobileCreateSignature>
        // @formatter:on

        Holder<Integer> sesscode = new Holder<Integer>();
        Holder<String> challengeId = new Holder<String>();
        Holder<String> status = new Holder<String>();
        long startTime = System.nanoTime();
        Pair<String, String> testIdCodeAndCountry = getTestPhoneNumberAndCountry(phoneNo);
        boolean isTestNumber = testIdCodeAndCountry != null;
        try {
            digiDocService.mobileCreateSignature(
                    isTestNumber ? testIdCodeAndCountry.getFirst() : idCode,
                    isTestNumber ? testIdCodeAndCountry.getSecond() : ID_CODE_COUNTRY_EE,
                    phoneNo,
                    "EST",
                    getMobileIdServiceName(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    dataFileDigestList,
                    format,
                    version,
                    signatureId,
                    "asynchClientServer",
                    0,
                    sesscode,
                    challengeId,
                    status);
            long stopTime = System.nanoTime();
            if (!"OK".equals(status.value)) {
                String string = "Error performing query skDigiDocServiceMobileCreateSignature - " + duration(startTime, stopTime) + " ms: status='" + status.value + "'";
                log.error(string);
                MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, string);
                throw new UnableToPerformException("sk_digidocservice_error");
            }
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
            log.info("PERFORMANCE: query skDigiDocServiceMobileCreateSignature - " + duration(startTime, stopTime) + " ms, status=OK");
        } catch (SOAPFaultException e) {
            long stopTime = System.nanoTime();
            handleDigiDocServiceSoapFault(e, startTime, stopTime, "skDigiDocServiceMobileCreateSignature");
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, e);
            throw e;
        }

        // RESPONSE
        // @formatter:off
        // <d:MobileCreateSignatureResponse>
        //    <Sesscode xsi:type="xsd:int">1756908565</Sesscode>
        //    <ChallengeID xsi:type="xsd:string">1712</ChallengeID>
        //    <Status xsi:type="xsd:string">OK</Status>
        // </d:MobileCreateSignatureResponse>
        // @formatter:on

        return new SignatureChallenge(sesscode.value, challengeId.value, digestHexs, signatureId, format, version);
    }

    private Pair<String, String> getTestPhoneNumberAndCountry(String phoneNo) {
        if (StringUtils.startsWith(phoneNo, "+")) {
            phoneNo = phoneNo.substring(1);
        }
        return testPhoneNumbers.get(phoneNo);
    }

    @Override
    public String getMobileIdSignature(SignatureChallenge signatureChallenge) {

        // REQUEST
        // @formatter:off
        // <dig:GetMobileCreateSignatureStatus soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
        //    <Sesscode xsi:type="xsd:int">1756908565</Sesscode>
        //    <WaitSignature xsi:type="xsd:boolean">false</WaitSignature>
        // </dig:GetMobileCreateSignatureStatus>
        // @formatter:on

        Holder<Integer> sesscode = new Holder<Integer>(signatureChallenge.getSesscode());
        Holder<String> status = new Holder<String>();
        Holder<String> signature = new Holder<String>();
        long startTime = System.nanoTime();
        try {
            digiDocService.getMobileCreateSignatureStatus(sesscode, false, status, signature);
            long stopTime = System.nanoTime();
            if (!Arrays.asList("SIGNATURE", "OUTSTANDING_TRANSACTION", "EXPIRED_TRANSACTION", "USER_CANCEL", "MID_NOT_READY", "PHONE_ABSENT", "SIM_ERROR", "SENDING_ERROR",
                    "REVOKED_CERTIFICATE", "INTERNAL_ERROR").contains(status.value)) {
                String string = "Error performing query skDigiDocServiceGetMobileCreateSignatureStatus - " + duration(startTime, stopTime) + " ms: status=" + status.value;
                log.error(string);
                MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, string);
                throw new UnableToPerformException("sk_digidocservice_error");
            }
            log.info("PERFORMANCE: query skDigiDocServiceGetMobileCreateSignatureStatus - " + duration(startTime, stopTime) + " ms, status=" + status.value);
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
            if ("SIGNATURE".equals(status.value)) {
                return signature.value;
            } else if ("OUTSTANDING_TRANSACTION".equals(status.value)) {
                return null;
            }
            UnableToPerformException unableToPerformException = new UnableToPerformException("ddoc_signature_failed_" + status.value);
            throw unableToPerformException;
        } catch (SOAPFaultException e) {
            long stopTime = System.nanoTime();
            handleDigiDocServiceSoapFault(e, startTime, stopTime, "skDigiDocServiceGetMobileCreateSignatureStatus");
        } catch (UnableToPerformException e) {
            // MonitoringUtil.logSuccess was called
            throw e;
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, e);
            throw e;
        }
        Assert.isTrue(false); // unreachable code
        return null;

        // RESPONSE:
        // @formatter:off
        // <d:GetMobileCreateSignatureStatusResponse>
        //    <Sesscode xsi:type="xsd:int">0</Sesscode>
        //    <Status xsi:type="xsd:string">SIGNATURE</Status>
        //    <Signature xsi:type="xsd:string"><![CDATA[<Signature Id="S0" xmlns="http://www.w3.org/2000/09/xmldsig#">...</Signature>]]></Signature>
        // </d:GetMobileCreateSignatureStatusResponse>
        // @formatter:on
    }

    private void handleDigiDocServiceSoapFault(SOAPFaultException e, long startTime, long stopTime, String queryName) {
        try {
            int faultCode = Integer.parseInt(e.getMessage());
            if (faultCode == 101 || (faultCode >= 300 && faultCode <= 305)) {
                log.info("PERFORMANCE: query " + queryName + " - " + duration(startTime, stopTime) + " ms, faultCode=" + faultCode);
                MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
                throw new UnableToPerformException("ddoc_signature_failed_" + faultCode);
            } else if (faultCode >= 200 && faultCode < 300) {
                log.info("PERFORMANCE: query " + queryName + " - " + duration(startTime, stopTime) + " ms, faultCode=" + faultCode);
                MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
                throw new UnableToPerformException("ddoc_signature_failed_2xx", Integer.toString(faultCode));
            } else if (faultCode >= 300 && faultCode < 400) {
                log.info("PERFORMANCE: query " + queryName + " - " + duration(startTime, stopTime) + " ms, faultCode=" + faultCode);
                MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
                throw new UnableToPerformException("ddoc_signature_failed_3xx", Integer.toString(faultCode));
            }
        } catch (NumberFormatException e2) {
            // do nothing
        }
        log.error("Error performing query " + queryName + " - " + duration(startTime, stopTime) + " ms: " + e.getMessage(), e);
        MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, e);
        throw new UnableToPerformException("sk_digidocservice_error");
    }

    private void addSignature(SignedDoc signedDoc, SignatureChallenge signatureChallenge, String signature) throws DigiDocException, SignatureException,
    UnsupportedEncodingException {

        List<String> digestHexs = new ArrayList<String>();
        for (int i = 0; i < signedDoc.countDataFiles(); i++) {
            DataFile df = signedDoc.getDataFile(i);
            String digestHex = Base64Util.encode(df.getDigest());
            digestHexs.add(digestHex);
        }
        if (!digestHexs.equals(signatureChallenge.getDigestHexs()) || !signedDoc.getNewSignatureId().equals(signatureChallenge.getSignatureId())
                || !signedDoc.getFormat().equals(signatureChallenge.getFormat()) || !signedDoc.getVersion().equals(signatureChallenge.getVersion())) {
            throw new SignatureException("Signed info digest does not match, files were modified in the meantime");
        }

        SAXDigiDocFactory digiDocFactory = new SAXDigiDocFactory();
        digiDocFactory.init();
        digiDocFactory.readSignature(signedDoc, new ByteArrayInputStream(signature.getBytes("UTF-8")));
    }

    private static void bindCleanTempFiles(final SignedDoc signedDoc) {
        try {
            Assert.notNull(AlfrescoTransactionSupport.getTransactionId(), "No transaction is present");

            if (log.isTraceEnabled()) {
                StringBuilder s = new StringBuilder("bindCleanTempFiles");
                s.append("\n  signedDoc=").append(ObjectUtils.identityToString(signedDoc));
                s.append("\n  countDataFiles=").append(signedDoc.countDataFiles());
                for (int i = 0; i < signedDoc.countDataFiles(); i++) {
                    s.append("\n  dataFile[").append(i).append("]=").append(signedDoc.getDataFile(i).getFileName());
                }
                log.trace(s.toString());
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
            if (log.isTraceEnabled()) {
                StringBuilder s = new StringBuilder("cleanTempFiles");
                s.append("\n  signedDoc=").append(ObjectUtils.identityToString(signedDoc));
                s.append("\n  countDataFiles=").append(signedDoc.countDataFiles());
                for (int i = 0; i < signedDoc.countDataFiles(); i++) {
                    s.append("\n  dataFile[").append(i).append("]=").append(signedDoc.getDataFile(i).getFileName());
                }
                log.trace(s.toString());
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

    private boolean isTest() {
        return test;
    }

    private String getMobileIdServiceName() {
        return isTest() ? testMobileIdServiceName : mobileIdServiceName;
    }

    @Override
    public List<X509Certificate> getCertificatesForEncryption(List<SkLdapCertificate> certificates) {
        ArrayList<X509Certificate> results = new ArrayList<X509Certificate>();
        outer: for (SkLdapCertificate skLdapCertificate : certificates) {
            try {
                X509Certificate cert = SignedDoc.readCertificate(skLdapCertificate.getUserCertificate());

                // In DigiDoc Client 2 and 3, only certificates which contain KeyEncipherment in KeyUsage, are suitable for encryption
                // (in DigiDoc Client < 3.6 DataEncipherment was checked)
                // According to https://svn.eesti.ee/projektid/idkaart_public/trunk/qdigidoc/crypto/KeyDialog.cpp
                // * c.keyUsage().contains( SslCertificate::KeyEncipherment )
                boolean keyEncipherment = cert.getKeyUsage()[2];
                if (!keyEncipherment) {
                    continue;
                }

                // In DigiDoc Client 3 (but not 2), additionally Mobile-ID certificates are filtered out (because decryption is not implemented in Mobile-ID)
                // According to https://svn.eesti.ee/projektid/idkaart_public/trunk/qdigidoc/crypto/KeyDialog.cpp
                // * c.type() != SslCertificate::MobileIDType
                // and https://svn.eesti.ee/projektid/idkaart_public/trunk/qdigidoc/common/SslCertificate.cpp
                // * QStringList p = policies();
                // * if( p.indexOf( QRegExp( "^1\\.3\\.6\\.1\\.4\\.1\\.10015\\.1\\.3.*" ) ) != -1 ||
                // * p.indexOf( QRegExp( "^1\\.3\\.6\\.1\\.4\\.1\\.10015\\.11\\.1.*" ) ) != -1 )
                // * return MobileIDType;
                List<String> objectIdentifiers = getPolicyObjectIdentifiers(cert);
                for (String objectIdentifier : objectIdentifiers) {
                    if (objectIdentifier.startsWith("1.3.6.1.4.1.10015.1.3") || objectIdentifier.startsWith("1.3.6.1.4.1.10015.11.1")) {
                        continue outer;
                    }
                }

                results.add(cert);
            } catch (Exception e) {
                throw new SignatureRuntimeException("Failed to parse certificate for " + skLdapCertificate.getCn(), e);
            }
        }
        return results;
    }

    private static List<String> getPolicyObjectIdentifiers(X509Certificate cert) {
        try {
            List<String> objectIdentifiers = new ArrayList<String>();
            byte[] policies = cert.getExtensionValue("2.5.29.32");
            if (policies != null) {
                ASN1Primitive derObject;
                derObject = toDerObject(policies);
                if (derObject instanceof DEROctetString) {
                    derObject = toDerObject(((DEROctetString) derObject).getOctets());
                }
                if (derObject instanceof DERSequence) {
                    collectObjectIdentifiers((DERSequence) derObject, objectIdentifiers);
                }
            }
            return objectIdentifiers;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get policy object identifiers from certificate: " + cert, e);
        }
    }

    private static ASN1Primitive toDerObject(byte[] data) throws IOException {
        return new ASN1InputStream(new ByteArrayInputStream(data)).readObject();
    }

    private static void collectObjectIdentifiers(DERSequence sequence, List<String> objectIdentifiers) {
        for (Enumeration<?> enumeration = sequence.getObjects(); enumeration.hasMoreElements();) {
            Object element = enumeration.nextElement();
            if (element instanceof DERObjectIdentifier) {
                objectIdentifiers.add(((DERObjectIdentifier) element).getId());
            } else if (element instanceof DERSequence) {
                collectObjectIdentifiers((DERSequence) element, objectIdentifiers);
            }
        }
    }

    @Override
    public void writeEncryptedContainer(OutputStream output, List<NodeRef> contents, List<X509Certificate> recipientCerts) {
        Assert.isTrue(!contents.isEmpty());
        Assert.isTrue(!recipientCerts.isEmpty());

        try {
            EncryptedData cdoc = new EncryptedData(null, null, null, EncryptedData.DENC_XMLNS_XMLENC, EncryptedData.DENC_ENC_METHOD_AES128);

            int idCounter = 1;
            for (X509Certificate recipientCert : recipientCerts) {

                X509Principal principal = PrincipalUtil.getSubjectX509Principal(recipientCert);
                Vector<?> values = principal.getValues(X509Name.CN);
                String cn = (String) values.get(0);

                String id = "ID" + idCounter++;
                EncryptedKey ekey = new EncryptedKey(id, cn, EncryptedData.DENC_ENC_METHOD_RSA1_5, null, null, recipientCert);
                cdoc.addEncryptedKey(ekey);
            }

            SignedDoc ddoc = createSignedDoc(contents);

            // Like cdoc.setPropRegisterDigiDoc(signedDoc), but with minor improvements
            for (int i = 0; i < ddoc.countDataFiles(); i++) {
                DataFile df = ddoc.getDataFile(i);
                StringBuffer sb = new StringBuffer();
                sb.append(df.getFileName());
                sb.append("|");
                sb.append(FilenameUtil.byteCountToDisplaySize(df.getSize()));
                sb.append("|");
                sb.append(df.getMimeType());
                sb.append("|");
                sb.append(df.getId());
                EncryptionProperty prop = new EncryptionProperty(EncryptedData.ENCPROP_ORIG_FILE, sb.toString());
                prop.setId(EncryptedData.ENCPROP_ORIG_FILE + i);
                cdoc.addProperty(prop);
            }

            File tmpFile = TempFileProvider.createTempFile("container-", ".ddoc");
            try {
                OutputStream tmpOutput = new BufferedOutputStream(new FileOutputStream(tmpFile));
                try {
                    ddoc.writeToStream(tmpOutput);
                } finally {
                    tmpOutput.close();
                }

                InputStream tmpInput = new BufferedInputStream(new FileInputStream(tmpFile));
                try {
                    cdoc.encryptStream(tmpInput, output, EncryptedData.DENC_COMPRESS_ALLWAYS);
                } finally {
                    tmpInput.close();
                }
            } finally {
                tmpFile.delete();
            }
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to encrypt, contents = " + contents + ", recipientCerts = " + recipientCerts, e);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

}
