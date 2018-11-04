package ee.webmedia.alfresco.signature.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;
import static ee.webmedia.alfresco.utils.CalendarUtil.duration;
import static ee.webmedia.alfresco.utils.FilenameUtil.makeSafeFilename;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.digidoc4j.Configuration;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.DataFile;
import org.digidoc4j.DataToSign;
import org.digidoc4j.DigestAlgorithm;
import org.digidoc4j.EncryptionAlgorithm;
import org.digidoc4j.Signature;
import org.digidoc4j.SignatureBuilder;
import org.digidoc4j.SignatureProfile;
import org.digidoc4j.SignatureValidationResult;
import org.digidoc4j.X509Cert;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidocservice.DigiDocServicePortType;
import ee.sk.digidocserviceV2.GetMobileSignHashStatusRequest;
import ee.sk.digidocserviceV2.GetMobileSignHashStatusResponse;
import ee.sk.digidocserviceV2.HashType;
import ee.sk.digidocserviceV2.LanguageType;
import ee.sk.digidocserviceV2.MobileId;
import ee.sk.digidocserviceV2.MobileSignHashRequest;
import ee.sk.digidocserviceV2.MobileSignHashResponse;
import ee.sk.digidocserviceV2.ProcessStatusType;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureChallenge;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;


public class DigiDoc4JSignatureServiceImpl implements DigiDoc4JSignatureService, InitializingBean {

    private static final String ID_CODE_COUNTRY_EE = "EE";
    private static final String ID_CODE_COUNTRY_LT = "LT";
    private static final String CERTIFICATE_ALGORITHM_EC = "EC";

    private static Logger log = Logger.getLogger(DigiDoc4JSignatureServiceImpl.class);

    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private MimetypeService mimetypeService;
    private boolean test = false;
    private static final DigestAlgorithm DIGEST_ALGORITHM = DigestAlgorithm.SHA256;
    private Configuration configuration = new Configuration(Configuration.Mode.PROD);

    private JaxWsProxyFactoryBean digiDocServiceFactory;
    private JaxWsProxyFactoryBean digiDocServiceV2Factory;
    private String digiDocServiceUrl;
    private String testDigiDocServiceUrl;
    private String digiDocServiceV2Url;
    private String testDigiDocServiceV2Url;
    private String mobileIdServiceName;
    private String testMobileIdServiceName;

    private DigiDocServicePortType digiDocService;
    private MobileId digiDocServiceV2;

    private String pkcs12Container;
    private String pkcs12Password;

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
        testPhoneNumbers.put("37200000766", Pair.newInstance("11412090004", ID_CODE_COUNTRY_EE));

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

    public void setDigiDocServiceV2Factory(JaxWsProxyFactoryBean digiDocServiceV2Factory) {
        this.digiDocServiceV2Factory = digiDocServiceV2Factory;
    }

    public void setDigiDocServiceUrl(String digiDocServiceUrl) {
        this.digiDocServiceUrl = digiDocServiceUrl;
    }

    public void setTestDigiDocServiceUrl(String testDigiDocServiceUrl) {
        this.testDigiDocServiceUrl = testDigiDocServiceUrl;
    }

    public void setDigiDocServiceV2Url(String digiDocServiceV2Url) {
        this.digiDocServiceV2Url = digiDocServiceV2Url;
    }

    public void setTestDigiDocServiceV2Url(String testDigiDocServiceV2Url) {
        this.testDigiDocServiceV2Url = testDigiDocServiceV2Url;
    }

    public void setMobileIdServiceName(String mobileIdServiceName) {
        this.mobileIdServiceName = mobileIdServiceName;
    }

    public void setTestMobileIdServiceName(String testMobileIdServiceName) {
        this.testMobileIdServiceName = testMobileIdServiceName;
    }

    public void setPkcs12Container(String pkcs12Container) {
        this.pkcs12Container = pkcs12Container;
    }

    public void setPkcs12Password(String pkcs12Password) {
        this.pkcs12Password = pkcs12Password;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        configuration = (isTest())?new Configuration(Configuration.Mode.TEST):new Configuration(Configuration.Mode.PROD);
        if (!isTest()) {
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

                log.info("Signing OCSP requests with certificate from PKCS12 container " + pkcs12Container);

                configuration.setOCSPAccessCertificateFileName(pkcs12Container);
                configuration.setOCSPAccessCertificatePassword(pkcs12Password.toCharArray());
                configuration.setSignOCSPRequests(true);
            } else {
                // TODO: remove when digidoc4j will be changed to by default signOCSPRequests=false, now it true by default
                configuration.setSignOCSPRequests(false);
            }

        }
        digiDocServiceFactory.setAddress(isTest() ? testDigiDocServiceUrl : digiDocServiceUrl);
        digiDocServiceFactory.setServiceName(new javax.xml.namespace.QName("http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl", "DigiDocService"));
        digiDocServiceFactory.setEndpointName(new javax.xml.namespace.QName("http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl", "DigiDocService"));
        digiDocService = (DigiDocServicePortType) digiDocServiceFactory.create();

        digiDocServiceV2Factory.setAddress(isTest() ? testDigiDocServiceV2Url : digiDocServiceV2Url);
        digiDocServiceV2Factory.setServiceName(new javax.xml.namespace.QName("http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl", "MobileIdService"));
        digiDocServiceV2Factory.setEndpointName(new javax.xml.namespace.QName("http://www.sk.ee/DigiDocService/DigiDocService_2_3.wsdl", "MobileIdService"));
        digiDocServiceV2 = (MobileId) digiDocServiceV2Factory.create();
    }

    public DataToSign getDataToSign(Container containerToSign, String cert, boolean pem) {
        log.debug("getDataToSign: CERT: " + cert + " PEM: " + pem);
        X509Certificate certificate = (pem)?getCertificateFromPem(cert) : getCertificateFromHex(cert);
        EncryptionAlgorithm encryptionAlgorithm  = EncryptionAlgorithm.RSA;
        if (CERTIFICATE_ALGORITHM_EC.equals(certificate.getPublicKey().getAlgorithm())) {
            encryptionAlgorithm = EncryptionAlgorithm.ECDSA;
        }

        SignatureProfile signatureProfile = SignatureProfile.LT_TM;
        log.debug("DEFAULT SignatureProfile: " + signatureProfile );
        String paramDigidocFormat = getParametersService().getStringParameter(Parameters.DIGIDOC_FILE_FORMAT);
        log.debug("paramDigidocFormat: " + paramDigidocFormat);
        if (DIGIDOC_FORMAT_ASICE.equals(paramDigidocFormat) || DIGIDOC_FORMAT_BDOC_TS.equals(paramDigidocFormat)) {
            signatureProfile = SignatureProfile.LT;
            log.debug("Change SignatureProfile to: " + signatureProfile );
        }

        log.debug("Build data to sign...");
        DataToSign dataToSign = SignatureBuilder.
                aSignature(containerToSign).
                withSigningCertificate(certificate).
                withSignatureDigestAlgorithm(DIGEST_ALGORITHM).
                withSignatureProfile(signatureProfile).
                withEncryptionAlgorithm(encryptionAlgorithm).
                buildDataToSign();
        return dataToSign;
    }

    public void signContainer(Container container, DataToSign dataToSign, String signatureInHex) {
        log.debug("signContainer(): SignatureInHex: " + signatureInHex + "; DataToSing: " + dataToSign + "; Container: " + container.getType());
        byte[] signatureBytes = DatatypeConverter.parseHexBinary(signatureInHex);
        Signature signature = dataToSign.finalize(signatureBytes);
        container.addSignature(signature);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SignatureDigest getSignatureDigest(NodeRef nodeRef, String certHex) throws SignatureException {
        log.debug("getSignatureDigest() CERT HEX: " + certHex);
        Container containerToSign = null;
        try {
            log.debug("Get Container... nodeRef: " + nodeRef);
            containerToSign = getContainer(nodeRef);
            log.debug("Get Data to Sign....");
            DataToSign dataToSign = getDataToSign(containerToSign, certHex, false);
            SignatureDigest signatureDigest = new SignatureDigest(DatatypeConverter.printHexBinary(dataToSign.getDigestToSign()), certHex, new Date(), dataToSign);
            log.debug("SignatureDigest...." + signatureDigest);
            return signatureDigest;
        } catch (Exception e) {
            throw new SignatureException("Failed to calculate signed info digest of bdoc file, nodeRef = " + nodeRef + ", certHex = " + certHex, e);
        }
    }

    @Override
    public SignatureDigest getSignatureDigest(List<NodeRef> contents, String certHex) throws SignatureException {
        log.debug("getSignatureDigest(): CERT HEX: " + certHex + "; List noderefs: " + contents.size());
        Container containerToSign = null;
        try {
            log.debug("Get Container... List of nodeRefs objects: " + contents.size());
            containerToSign = createContainer(contents);
            log.debug("Get Data to Sign....");
            DataToSign dataToSign = getDataToSign(containerToSign, certHex, false);
            SignatureDigest signatureDigest = new SignatureDigest(DatatypeConverter.printHexBinary(dataToSign.getDigestToSign()), certHex, new Date(), dataToSign);
            log.debug("SignatureDigest...." + signatureDigest);
            return signatureDigest;
        } catch (Exception e) {
            throw new SignatureException("Failed to calculate signed info digest from contents = " + contents + ", certHex = " + certHex, e);
        }
    }

    @Override
    public SignatureChallenge getSignatureChallenge(NodeRef nodeRef, String phoneNo, String idCode) throws SignatureException {
        log.debug("getSignatureChallenge(): Phone nr: " + phoneNo + "; id-code: " + idCode + "; nodeRef: " + nodeRef);
        Container containerToSign = null;
        try {
            log.debug("Get mobile certificate....phoneNo: " + phoneNo + "; id-code: " + idCode);
            String certPem = getMobileCertificate(phoneNo, idCode);
            log.debug("MOBILECERTIFICATE: " + certPem);
            log.debug("Get container... nodeRef: " + nodeRef);
            containerToSign = getContainer(nodeRef);

            log.debug("Get Data to sign....");
            DataToSign dataToSign = getDataToSign(containerToSign, certPem, true);
            SignatureChallenge signatureChallenge = getSignatureChallenge(containerToSign, dataToSign, phoneNo, idCode);
            log.debug("SignatureChallenge: " + signatureChallenge);
            return signatureChallenge;
        } catch (UnableToPerformException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException("Failed to create Mobile-ID signing request of bdoc file, nodeRef = " + nodeRef + ", phoneNo = " + phoneNo, e);
        }
    }

    @Override
    public SignatureChallenge getSignatureChallenge(List<NodeRef> contents, String phoneNo, String idCode) throws SignatureException {
        log.debug("getSignatureChallenge(): Phone nr: " + phoneNo + "; id-code: " + idCode + "; List of nodeRef: " + contents.size());
        Container containerToSign = null;
        try {
            log.debug("Get mobile certificate....phoneNo: " + phoneNo + "; id-code: " + idCode);
            String certPem = getMobileCertificate(phoneNo, idCode);
            log.debug("MOBILE Certificate: " + certPem);
            log.debug("createContainer... List of nodeRefs objects: " + contents.size());
            containerToSign = createContainer(contents);

            log.debug("Get Data to sign....");
            DataToSign dataToSign = getDataToSign(containerToSign, certPem, true);

            SignatureChallenge signatureChallenge = getSignatureChallenge(containerToSign, dataToSign, phoneNo, idCode);
            log.debug("SignatureChallenge: " + signatureChallenge);
            return signatureChallenge;
        } catch (UnableToPerformException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException("Failed to create Mobile-ID signing request from contents = " + contents + ", phoneNo = " + phoneNo, e);
        }
    }


    @Override
    public NodeRef createAndSignContainer(NodeRef parent, List<NodeRef> contents, String filename, DataToSign dataToSign, String signatureHex) {
        log.debug("createAndSignContainer(): filename: " + filename + "; Signature hex: " + signatureHex + "; List of nodeRefs: " + contents.size() + "; DataToSign: " + dataToSign);
        try {
            log.debug("createContainer... List of nodeRefs objects: " + contents.size());
            Container container = createContainer(contents);
            log.debug("Sign container...");
            signContainer(container, dataToSign, signatureHex);
            NodeRef newNodeRef = createContentNode(parent, filename);
            log.debug("New nodeRef: " + newNodeRef);

            log.debug("WriteSignedContainer...");
            writeSignedContainer(newNodeRef, container);
            return newNodeRef;
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to add signature and write bdoc to file " + filename + ", parent = " + parent + ", contents = "
                    + contents + ", dataToSign = " + dataToSign + ", signatureHex = " + signatureHex, e);
        }
    }

    @Override
    public void addSignature(NodeRef nodeRef, DataToSign dataToSign, String signatureHex) {
        log.debug("addSignature: Signature hex: " + signatureHex + "; DataToSign: " + dataToSign + "; NodeRef: " + nodeRef);
        Container container = null;
        try {
            container = getContainer(nodeRef);
            signContainer(container, dataToSign, signatureHex);
            writeSignedContainer(nodeRef, container);
        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to add signature to bdoc file, nodeRef = " + nodeRef + ", dataToSign = " + dataToSign
                    + ", signatureHex = " + signatureHex, e);
        }
    }

    @Override
    public boolean isDigiDocContainer(NodeRef nodeRef) {
        log.debug("isDigiDocContainer(): " + nodeRef);
        FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
        return FilenameUtil.isDigiDocContainerFile(fileInfo);
    }

    public NodeRef createContentNode(NodeRef parentRef, String filename) {
        log.debug("createContentNode(): NodeRef" + parentRef + "; filename: " + filename);
        // assign the file name
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, filename);
        // create new content node under parentRef
        FileInfo fileInfo = fileFolderService.create(parentRef, filename, ContentModel.TYPE_CONTENT);
        return fileInfo.getNodeRef();
    }


    private void writeSignedContainer(NodeRef nodeRef, Container container) throws DigiDocException, IOException {
        log.debug("writeSignedContainer(): NodeRef: " + nodeRef + "; Container: " + container);
        ContentWriter writer = fileFolderService.getWriter(nodeRef);
        writer.setMimetype(SignatureService.DIGIDOC_MIMETYPE);
        writer.setEncoding(AppConstants.CHARSET);
        writer.putContent(container.saveAsStream());
    }

    private Container getContainer(NodeRef nodeRef) throws SignatureException {
        log.debug("getContainer: nodeRef: " + nodeRef);
        try {
            if (!isDigiDocContainer(nodeRef)) {
                throw new SignatureException("NodeRef is not a digidoc: " + nodeRef);
            }
            ContentReader reader = fileFolderService.getReader(nodeRef);
            if (reader == null) {
                throw new SignatureException("NodeRef has no content: " + nodeRef);
            }
            InputStream contentInputStream = reader.getContentInputStream();
            return getContainerFromStream(contentInputStream);
        } catch (Exception e) {
            log.error("getContainer(): ERROR: " + e.getMessage(), e);
            if (e instanceof SignatureException) {
                throw (SignatureException) e;
            }
            throw new SignatureException("Failed to parse ddoc file, nodeRef = " + nodeRef, e);
        }
    }

    /**
     *
     * @param contentInputStream
     * @param ext BDOC or DDOC type
     * @return
     * @throws SignatureException
     */
    private Container getContainerFromStream(InputStream contentInputStream, String ext) throws SignatureException {
        log.debug("getContainerFromStream... EXT: " + ext);
        try {
            if (contentInputStream != null) {
                log.debug("content input stream is not null. Creating conteiner...");
                Container container = null;
                if(ext == null){
                    container = ContainerBuilder.
                            aContainer().
                            fromStream(contentInputStream).
                            withConfiguration(configuration).
                            build();
                } else {
                    container = ContainerBuilder.
                            aContainer(ext).
                            fromStream(contentInputStream).
                            withConfiguration(configuration).
                            build();
                }
                log.debug("Return conteiner...");
                return container;
            }
            throw new SignatureException("contentInputStream is empty.");
        } catch (Exception e) {
            if (e instanceof SignatureException) {
                throw (SignatureException) e;
            }
            throw new SignatureException("Failed to parse digidoc file", e);
        }
    }

    private Container getContainerFromStream(InputStream contentInputStream) throws SignatureException {
        log.debug("getContainerFromStream(): ....");
        return getContainerFromStream(contentInputStream, null);
    }

    private X509Certificate getCertificateFromHex(String certificateInHex) {
        byte[] certificateBytes = DatatypeConverter.parseHexBinary(certificateInHex);
        InputStream inStream = null;
        try {
            inStream = new ByteArrayInputStream(certificateBytes);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate)cf.generateCertificate(inStream);
            return certificate;
        } catch (CertificateException e) {
            log.error("Error reading certificate: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inStream);
        }
    }

    private X509Certificate getCertificateFromPem(String certificateInPem) {
        log.debug("getCertificateFromPem: CertificateInPem: " + certificateInPem);
        X509Certificate cert = null;
        StringReader reader = null;
        PEMParser pr = null;
        try {
            reader = new StringReader(certificateInPem);
            pr = new PEMParser(reader);
            X509CertificateHolder holder = (X509CertificateHolder)pr.readObject();
            JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
            cert = certConverter.getCertificate(holder);
        } catch (IOException e) {
            log.error("Error reading certificate: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            log.error("Error reading certificate from holder: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(pr);
            IOUtils.closeQuietly(reader);
        }
        return cert;
    }

    private Container createContainer(List<NodeRef> nodeRefs) throws DigiDoc4JException, IOException {
        log.debug("createContainer(): List of NodeRef: " + nodeRefs.size());
        Container container = ContainerBuilder.
                aContainer().
                withConfiguration(configuration).
                build();
        addDataFiles(container, nodeRefs);
        return container;
    }

    private void addDataFiles(Container container, List<NodeRef> nodeRefs) throws DigiDoc4JException, IOException {
        log.debug("addDataFiles(): List on noderefs: " + nodeRefs.size());
        for (NodeRef ref : nodeRefs) {
            addDataFile(ref, container);
        }
    }

    public static void main(String args []) {
        System.out.println("phone = " + StringUtils.stripToEmpty("+372 454 23423"));
    }

    private String getMobileCertificate(String phoneNo, String idCode) throws DigiDoc4JException {
        log.debug("getMobileCertificate(): Phone nr: " + phoneNo + "; ID-code: " + idCode);
        phoneNo = StringUtils.stripToEmpty(phoneNo);
        idCode = StringUtils.stripToEmpty(idCode);
        Holder<String> authCertStatus = new Holder<String>();
        Holder<String> signCertStatus = new Holder<String>();
        Holder<String> authCertData = new Holder<String>();
        Holder<String> signCertData = new Holder<String>();
        long startTime = System.nanoTime();
        Pair<String, String> testIdCodeAndCountry = getTestPhoneNumberAndCountry(phoneNo);
        boolean isTestNumber = testIdCodeAndCountry != null;
        try {
            digiDocService.getMobileCertificate(isTestNumber ? testIdCodeAndCountry.getFirst() : idCode, isTestNumber ? testIdCodeAndCountry.getSecond() : ID_CODE_COUNTRY_EE, phoneNo, "sign", authCertStatus, signCertStatus, authCertData, signCertData);
            long stopTime = System.nanoTime();
            if (!"OK".equals(signCertStatus.value)) {
                String string = "Error performing query skDigiDocServiceMobileCreateSignature - " + duration(startTime, stopTime) + " ms: signCertStatus='" + signCertStatus.value + "'";
                log.error(string);
                MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, string);
                throw new UnableToPerformException("sk_digidocservice_error");
            }
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
            log.info("PERFORMANCE: query skDigiDocServiceGetMobileCertificate - " + duration(startTime, stopTime) + " ms, status=OK");
        } catch (SOAPFaultException e) {
            long stopTime = System.nanoTime();
            handleDigiDocServiceSoapFault(e, startTime, stopTime, "skDigiDocServiceGetMobileCertificate");
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, e);
            throw e;
        }
        log.debug("signCertData.value: " + signCertData.value);
        return signCertData.value;
    }

    private void addDataFile(NodeRef nodeRef, Container container) throws DigiDoc4JException, IOException {
        log.debug("addDataFile(): nodeRef: " + nodeRef + "; Container: " + container);
        InputStream is = null;
        ContentReader reader = fileFolderService.getReader(nodeRef);
        try {
            String fileName = getFileName(nodeRef);
            if (fileName != null && fileName.length() > 220) {
                String baseName = FilenameUtils.removeExtension(fileName);
                String extension = FilenameUtils.getExtension(fileName);
                fileName = baseName.substring(0, 210) + FilenameUtils.EXTENSION_SEPARATOR + extension;
            }
            // TODO: Alternative is: fileName = makeSafeFilename(fileName); // <-- currently 50 char is max filename...

            is = reader.getContentInputStream();
            container.addDataFile(is, fileName, reader.getMimetype());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private SignatureChallenge getSignatureChallenge(Container container, DataToSign dataToSign, String phoneNo, String idCode) throws DigiDocException {
        log.debug("getSignatureChallenge(): PhoneNr: " + phoneNo + "; ID-code: " + idCode);
        SignatureChallenge signatureChallenge = null;
        phoneNo = StringUtils.stripToEmpty(phoneNo);
        idCode = StringUtils.stripToEmpty(idCode);

        String hash = DatatypeConverter.printHexBinary(dataToSign.getDigestToSign());
        log.debug("Hash: " + hash);
        long startTime = System.nanoTime();
        Pair<String, String> testIdCodeAndCountry = getTestPhoneNumberAndCountry(phoneNo);
        boolean isTestNumber = testIdCodeAndCountry != null;

        MobileSignHashRequest mobileSignHashRequest = new MobileSignHashRequest();
        mobileSignHashRequest.setHash(hash);
        mobileSignHashRequest.setIDCode(isTestNumber ? testIdCodeAndCountry.getFirst() : idCode);
        mobileSignHashRequest.setLanguage(LanguageType.EST);
        mobileSignHashRequest.setPhoneNo(phoneNo);
        mobileSignHashRequest.setHashType(HashType.SHA_256);
        mobileSignHashRequest.setServiceName(getMobileIdServiceName());
        try {

            MobileSignHashResponse mobileSignHashResponse = digiDocServiceV2.mobileSignHash(mobileSignHashRequest);
            log.debug("mobileSignHashResponse.getStatus(): " + mobileSignHashResponse.getStatus());
            long stopTime = System.nanoTime();
            if (!"OK".equals(mobileSignHashResponse.getStatus())) {
                String string = "Error performing query skDigiDocServiceMobileSignHash - " + duration(startTime, stopTime) + " ms: status='" + mobileSignHashResponse.getStatus() + "'";
                log.error(string);
                MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, string);
                throw new UnableToPerformException("sk_digidocservice_error");
            }
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
            signatureChallenge = new SignatureChallenge(mobileSignHashResponse.getSesscode(), mobileSignHashResponse.getChallengeID(), dataToSign);
            log.info("PERFORMANCE: query skDigiDocServiceMobileSignHash - " + duration(startTime, stopTime) + " ms, status=OK");
        } catch (SOAPFaultException e) {
            long stopTime = System.nanoTime();
            handleDigiDocServiceSoapFault(e, startTime, stopTime, "skDigiDocServiceMobileSignHash");
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, e);
            throw e;
        }


        return signatureChallenge;
    }

    @Override
    public String getMobileIdSignature(SignatureChallenge signatureChallenge) {
        log.debug("getMobileIdSignature():...");
        GetMobileSignHashStatusRequest getMobileSignHashStatusRequest = new GetMobileSignHashStatusRequest();
        getMobileSignHashStatusRequest.setSesscode(signatureChallenge.getSesscode());
        getMobileSignHashStatusRequest.setWaitSignature(true);

        long startTime = System.nanoTime();
        try {

            GetMobileSignHashStatusResponse getMobileSignHashStatusResponse = digiDocServiceV2.getMobileSignHashStatus(getMobileSignHashStatusRequest);
            long stopTime = System.nanoTime();
            if (!Arrays.asList(ProcessStatusType.SIGNATURE, ProcessStatusType.OUTSTANDING_TRANSACTION, ProcessStatusType.EXPIRED_TRANSACTION,
                    ProcessStatusType.USER_CANCEL, ProcessStatusType.MID_NOT_READY, ProcessStatusType.PHONE_ABSENT, ProcessStatusType.SIM_ERROR, ProcessStatusType.SENDING_ERROR,
                    ProcessStatusType.REVOKED_CERTIFICATE, ProcessStatusType.INTERNAL_ERROR).contains(getMobileSignHashStatusResponse.getStatus())) {
                String string = "Error performing query skDigiDocServiceGetMobileSignHashStatus - " + duration(startTime, stopTime) + " ms: status=" + getMobileSignHashStatusResponse.getStatus().value();
                log.error(string);
                MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, string);
                throw new UnableToPerformException("sk_digidocservice_error");
            }
            log.info("PERFORMANCE: query skDigiDocServiceGetMobileSignHashStatus - " + duration(startTime, stopTime) + " ms, status=" + getMobileSignHashStatusResponse.getStatus().value());
            MonitoringUtil.logSuccess(MonitoredService.OUT_SK_DIGIDOCSERVICE);
            if (ProcessStatusType.SIGNATURE.equals(getMobileSignHashStatusResponse.getStatus())) {
                return DatatypeConverter.printHexBinary(getMobileSignHashStatusResponse.getSignature());
            } else if (ProcessStatusType.OUTSTANDING_TRANSACTION.equals(getMobileSignHashStatusResponse.getStatus())) {
                return null;
            }
            UnableToPerformException unableToPerformException = new UnableToPerformException("ddoc_signature_failed_" + getMobileSignHashStatusResponse.getStatus().value());
            throw unableToPerformException;
        } catch (SOAPFaultException e) {
            long stopTime = System.nanoTime();
            handleDigiDocServiceSoapFault(e, startTime, stopTime, "skDigiDocServiceGetMobileSignHashStatus");
        } catch (UnableToPerformException e) {
            // MonitoringUtil.logSuccess was called
            throw e;
        } catch (RuntimeException e) {
            MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, e);
            throw e;
        }
        Assert.isTrue(false); // unreachable code
        return null;
    }

    private Pair<String, String> getTestPhoneNumberAndCountry(String phoneNo) {
        if (StringUtils.startsWith(phoneNo, "+")) {
            phoneNo = phoneNo.substring(1);
        }
        return testPhoneNumbers.get(phoneNo);
    }

    private void handleDigiDocServiceSoapFault(SOAPFaultException e, long startTime, long stopTime, String queryName) {
        log.debug("handleDigiDocServiceSoapFault(): queryName: " + queryName);
        try {
            log.error("Error performing query " + queryName + " - " + duration(startTime, stopTime) + " ms: " + e.getMessage() + " fault: " + ((e.getFault() != null)?e.getFault().getDetail():""), e);
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
        //log.error("Error performing query " + queryName + " - " + duration(startTime, stopTime) + " ms: " + e.getMessage(), e);
        MonitoringUtil.logError(MonitoredService.OUT_SK_DIGIDOCSERVICE, e);
        throw new UnableToPerformException("sk_digidocservice_error");
    }

    /**
     * @param fileRef file NodeRef
     * @return filename that corresponds to filename rules (does not contain special characters and is <= 255 chars in length)
     */
    protected String getFileName(NodeRef fileRef) {
        log.debug("getFileName(): nodeRef: " + fileRef);
        String name = (String) nodeService.getProperty(fileRef, FileModel.Props.DISPLAY_NAME);
        if (StringUtils.isNotBlank(name)) {
            name = FilenameUtil.stripForbiddenWindowsCharactersAndRedundantWhitespaces(name);
        }
        return StringUtils.isNotBlank(name)?name:(String) nodeService.getProperty(fileRef, ContentModel.PROP_NAME);
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
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(NodeRef nodeRef, boolean includeData) throws SignatureException {
        log.debug("getDataItemsAndSignatureItems... nodeRef: " + nodeRef + "; include data: " + includeData);
        Container signedContainer = null;
        try {
            log.debug("Get digidoc conteiner from nodeRef...");
            signedContainer = getContainer(nodeRef);
            if(signedContainer == null){
                log.warn("Failed to get conteiner from nodeRef... NULL!");
            }
            log.debug("getDataItemsAndSignatureItems...");
            return getDataItemsAndSignatureItems(signedContainer, nodeRef, includeData);
        } catch (Exception e) {
            log.error("Failed to get digidoc data and signature items, nodeRef = " + nodeRef
                    + ", includeData = " + includeData);
            throw new SignatureException("Failed to get digidoc data and signature items, nodeRef = " + nodeRef
                    + ", includeData = " + includeData, e);
        }
    }

    @Override
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData, String filext) throws SignatureException {
        log.debug("getDataItemsAndSignatureItems... InputStream... include data: " + includeData + "; filext: " + filext);
        Container signedContainer = null;
        try {
            log.debug("Get digidoc conteiner from inputStream...");
            signedContainer = getContainerFromStream(inputStream, filext);
            if(signedContainer == null){
                log.warn("Failed to get conteiner from inputStream... NULL!");
            }
            log.debug("getDataItemsAndSignatureItems...");
            return getDataItemsAndSignatureItems(signedContainer, null, includeData);
        } catch (Exception e) {
            log.error("Failed to get digidoc data and signature items, inputStream = "
                    + ObjectUtils.identityToString(inputStream) + ", includeData = " + includeData);
            throw new SignatureException("Failed to get digidoc data and signature items, inputStream = "
                    + ObjectUtils.identityToString(inputStream) + ", includeData = " + includeData, e);
        }
    }

    @Override
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData) throws SignatureException {
        log.debug("getDataItemsAndSignatureItems(): inputStream... includeData" + includeData);
        return getDataItemsAndSignatureItems(inputStream, includeData, null);
    }
    
    /*
     * test method to validate digidoc container against prod
    private void validateProdContainer() throws Exception {
	  Container container = ContainerBuilder.
	    aContainer().
	    fromExistingFile("c:/dev/tmp/1-23 14.03.2013 Määrus.ddoc").
	    withConfiguration(new Configuration(Configuration.Mode.PROD)).
	    build();
	  ValidationResult result = container.validate();
	  String report = result.getReport();
	  System.out.println("valid = " + result.isValid() + "; report =  " + report);
	  
	}
	*/

    private SignatureItemsAndDataItems getDataItemsAndSignatureItems(Container container, NodeRef nodeRef, boolean includeData) {
        log.debug("Get signature items list... nodeRef: " + nodeRef + "; includeData: " + includeData);
        List<SignatureItem> signatureItems = getSignatureItems(nodeRef, container);
        log.debug("LIST of SIGNATUREITEMS: size(): " + signatureItems.size() );
        log.debug("Get data items list...");
        List<DataItem> dataItems = getDataItems(nodeRef, container, includeData);
        log.debug("LIST of DATAITEMS: size(): " + dataItems.size());
        log.debug("Returning signatureItemsAndDataItem object...");
        return new SignatureItemsAndDataItems(signatureItems, dataItems);
    }

    private List<SignatureItem> getSignatureItems(NodeRef nodeRef, Container bdoc) {
        log.debug("getSignatureItems: ... nodeRef: " + nodeRef);
        List<SignatureItem> items = new ArrayList<SignatureItem>();
        for (Signature signature: bdoc.getSignatures()) {
            log.debug("Signature method: " + signature.getSignatureMethod());
            log.debug("Signature id: " + signature.getId());


            X509Cert certValue = signature.getSigningCertificate();

            log.debug("X509Cert serial: " + certValue.getSerial());
            log.debug("X509Cert getX509Certificate type: " + certValue.getX509Certificate().getType());
            log.debug("X509Cert getX509Certificate sigAlgName: " + certValue.getX509Certificate().getSigAlgName());
            log.debug("X509Cert getX509Certificate sigAlgOID: " + certValue.getX509Certificate().getSigAlgOID());

            log.debug("X509Cert getX509Certificate version: " + certValue.getX509Certificate().getVersion());
            /*
             * TODO: change to this code when this issue is fixed https://github.com/open-eid/digidoc4j/issues/13
             */
            String subjectFirstName = certValue.getSubjectName(X509Cert.SubjectName.GIVENNAME);
            log.debug("X509Cert subject firstname: " + subjectFirstName);
            String subjectLastName = certValue.getSubjectName(X509Cert.SubjectName.SURNAME);
            log.debug("X509Cert subject lastname: " + subjectLastName);
            String legalCode = certValue.getSubjectName(X509Cert.SubjectName.SERIALNUMBER);
            log.debug("X509Cert subject serialnumber: " + legalCode);
            String name = UserUtil.getPersonFullName(subjectFirstName, subjectLastName);


//            X509Certificate cert = certValue.getX509Certificate();
//            String subjectFirstName = SignedDoc.getSubjectFirstName(cert);
//            String subjectLastName = SignedDoc.getSubjectLastName(cert);
//            String legalCode = SignedDoc.getSubjectPersonalCode(cert);
//            String name = UserUtil.getPersonFullName(subjectFirstName, subjectLastName);

            Date signingTime = signature.getOCSPResponseCreationTime();
            String address = getSignatureAddress(signature.getCity(), signature.getPostalCode(), signature.getCountryName());
            log.debug("Signature address: " + address);

            boolean isCertValid = false;
            try{
                SignatureValidationResult validationResult = signature.validateSignature();
                isCertValid = validationResult.isValid();
                if (!validationResult.isValid() && log.isDebugEnabled()) {
                    log.debug("Signature (id = " + signature.getId() + ") verification returned errors" + (nodeRef != null ? ", nodeRef = " + nodeRef : "") + " : \n" + validationResult.getErrors());
                }

            } catch (Exception e){
                log.error("Can't validate certificate! " + e.getMessage(), e);
            }

            String sigAlgName = certValue.getX509Certificate().getSigAlgName();
            String encryptionType = "";
            if(sigAlgName.toLowerCase().equals("sha1withrsa")){
                encryptionType = "SHA-1";
            } else if(sigAlgName.toLowerCase().equals("sha256withrsa")){
                encryptionType = "SHA-256";
            } else {
                encryptionType = sigAlgName;
            }
            SignatureItem item = new SignatureItem(name, legalCode, signingTime, signature.getSignerRoles(), address, isCertValid, encryptionType);
            log.debug("Add signature item to items list...");
            items.add(item);
        }
        return items;
    }

    private String getSignatureAddress(String city, String postalCode, String countryName) {
        StringBuilder address = new StringBuilder();
        if (StringUtils.isNotBlank(city)) {
            address.append(city);
        }
        if (StringUtils.isNotBlank(postalCode)) {
            if (address.length() > 0) {
                address.append(",");
            }
            address.append(postalCode);
        }
        if (StringUtils.isNotBlank(countryName)) {
            if (address.length() > 0) {
                address.append(",");
            }
            address.append(countryName);
        }
        if (address.length() > 0) {
            return address.toString();
        }

        return null;
    }

    private List<DataItem> getDataItems(NodeRef nodeRef, Container container, boolean includeData) {
        List<DataItem> items = new ArrayList<DataItem>();
        log.debug("Get data items from conteiner...");
        for (int i = 0; i < container.getDataFiles().size(); i++) {
            log.debug("data file: " + (i+1) + " of " + container.getDataFiles().size());
            DataFile dataFile = container.getDataFiles().get(i);
            log.debug("Get dataItem file...");
            DataItem item = getDataItem(nodeRef, dataFile, dataFile.getId(), includeData, i);
            log.debug("Add dataItem to items list...");
            items.add(item);
        }
        return items;
    }

    private DataItem getDataItem(NodeRef nodeRef, DataFile dataFile, String id, boolean includeData, int orderNr) {
        String fileName = dataFile.getName();
        log.debug("Conteiner datafile: fileName: " + fileName);
        String mimeType = dataFile.getMediaType();
        log.debug("Conteiner datafile: mimeType: " + mimeType);
        String guessedMimetype = mimetypeService.guessMimetype(fileName);
        log.debug("Conteiner datafile: guessedMimetype: " + guessedMimetype);
        if (MimetypeMap.MIMETYPE_BINARY.equals(guessedMimetype) && org.apache.commons.lang.StringUtils.isNotBlank(mimeType)) {
            guessedMimetype = mimeType;
        }
        log.debug("Check if include data to dataItem... " + includeData);
        if (includeData) {
            return new DataItem(nodeRef, id, fileName, guessedMimetype, dataFile.getFileSize(), dataFile, orderNr);
        }
        return new DataItem(nodeRef, id, fileName, guessedMimetype, dataFile.getFileSize(), orderNr);
    }

    @Override
    public boolean isBDocContainer(NodeRef nodeRef) {
        FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
        return FilenameUtil.isDigiDocContainerFile(fileInfo) && FilenameUtil.isBdocFile(fileInfo.getName());
    }

    @Override
    public boolean isMobileIdEnabled() {
        return StringUtils.isNotEmpty(getMobileIdServiceName());
    }

    @Override
    public void writeContainer(OutputStream output, List<NodeRef> contents) {
        try {
            Container container = createContainer(contents);
            IOUtils.copy(container.saveAsStream(),output);

        } catch (Exception e) {
            throw new SignatureRuntimeException("Failed to write bdoc file, output = " + output + ", contents = " + contents, e);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

}
