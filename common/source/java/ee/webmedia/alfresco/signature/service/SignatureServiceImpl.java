package ee.webmedia.alfresco.signature.service;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidocservice.DigiDocServicePortType;
import ee.sk.utils.ConfigManager;
import ee.sk.xmlenc.EncryptedData;
import ee.sk.xmlenc.EncryptedKey;
import ee.sk.xmlenc.EncryptionProperty;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;
import ee.webmedia.alfresco.utils.FilenameUtil;

public class SignatureServiceImpl implements SignatureService, InitializingBean {

    private static final String ID_CODE_COUNTRY_EE = "EE";
    private static final String ID_CODE_COUNTRY_LT = "LT";
    private static final String CERTIFICATE_ALGORITHM_EC = "EC";

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


    private SignedDoc createSignedDDoc(List<NodeRef> nodeRefs) throws DigiDocException, IOException {
        SignedDoc document = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
        return addSignedDocDataFiles(document, nodeRefs);
    }


    private SignedDoc addSignedDocDataFiles(SignedDoc document, List<NodeRef> nodeRefs) throws DigiDocException, IOException {
        bindCleanTempFiles(document);
        for (NodeRef ref : nodeRefs) {
            addDataFile(ref, document);
        }
        return document;
    }


    private void addDataFile(NodeRef nodeRef, SignedDoc document) throws DigiDocException, IOException {
        String fileName = getFileName(nodeRef);
        ContentReader reader = fileFolderService.getReader(nodeRef);
        DataFile datafile = createDDocDataFile(document, reader, fileName);
        document.addDataFile(datafile);
    }

    

    private DataFile createDDocDataFile(SignedDoc signedDocument, ContentReader reader, String fileName) throws DigiDocException, IOException {
        if(signedDocument == null){
            log.error("SignedDocument: Signed digidoc is NULL!");
        } else {
            log.trace("SignedDocument: NewDataFileId: " + signedDocument.getNewDataFileId());
            log.trace("SignedDocument: path: " + signedDocument.getPath());
            log.trace("SignedDocument: version: " + signedDocument.getVersion());
            log.trace("SignedDocument: size: " + signedDocument.getSize());
            log.trace("SignedDocument: MIME type: " + signedDocument.getMimeType());
            try{
                ArrayList data = signedDocument.getDataFiles();
                if(data == null){
                    log.error("SignedDocument: getDataFiles Arraylist is NULL!");
                } else {
                    if (data.size() > 0) {
                        for (Object object : data) {
                            log.trace("DATA ArrayList object: " + object.toString());
                        }
                    }
                }

            } catch (Exception e){
                log.error("ERROR: " + e.getMessage(), e);
            }

        }
        if(fileName == null){
            log.error("Filename is NULL!!");
        } else{
            log.trace("Filename: " + fileName);
        }

        if(reader == null){
            log.error("ContentReader is NULL!");
        } else {
            try {
                log.trace("ContentReader getSize: " + reader.getSize());
                log.trace("ContentReader getContentUrl: " + reader.getContentUrl());
                log.trace("ContentReader getLastModified:" + reader.getLastModified());
                log.trace("ContentReader getEncoding: " + reader.getEncoding());
                log.trace("ContentReader getMimetype: " + reader.getMimetype());
            } catch (Exception e){
                log.error("ContentReader ERROR: " + e.getMessage(), e);
            }
        }
        DataFile dataFile = createDataFile(signedDocument, reader, fileName);

        log.trace("Try to get cached file...");
        File dataFileCached = dataFile.getDfCacheFile();

        //dataFileCached = null;
        if(dataFileCached == null){
            log.error("Can't get cached dataFile! File is NULL");

            String oldTmpDir = System.getProperty("java.io.tmpdir");

            log.trace("Attempt 2: Try change TEMP folder: " + oldTmpDir + " ==> /tmp");
            System.setProperty("java.io.tmpdir", "/tmp");
            System.setProperty("CATALINA_TMPDIR", "/tmp");

            createTestTmpfile("/tmp/");

            dataFile = createDataFile(signedDocument, reader, fileName);


            log.trace("Attempt 2: Changeing java.io.tmpdir back to Old value: " + oldTmpDir);
            System.setProperty("java.io.tmpdir", oldTmpDir);
            System.setProperty("CATALINA_TMPDIR", oldTmpDir);

            log.trace("Attempt 2: Try get cached file...");
            dataFileCached = dataFile.getDfCacheFile();
            if(dataFileCached == null){
                log.error("Attempt 2: dataFileCached is still NULL!");
            }
        } else {
            log.trace("Cached dataFile: absolutePath: " + dataFileCached.getAbsolutePath());
        }
        FileOutputStream fileOutputStream = new FileOutputStream(dataFileCached);
        if(fileOutputStream == null){
            log.error("New FileOutputStream is NULL!!");
        } else {
            log.trace("New FileOutputStream: CREATED!");
        }
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        if(bufferedOutputStream == null){
            log.error("New BufferedOutputStream is NULL!!");
        } else {
            log.trace("New BufferedOutputStream: CREATED!");
        }

        OutputStream os = new Base64OutputStream(bufferedOutputStream,true, 64, new byte[] { '\n' });
        if(os == null){
            log.error("New Base64OutputStream is NULL!!");
        } else {
            log.trace("New Base64OutputStream: CREATED!");
        }

        log.debug("Read content...");
        reader.getContent(os); // closes both streams
        if(reader != null){
            log.trace("Reader size(): " + reader.getSize());
        } else {
            log.error("Reader is NULL!!");
        }
        dataFile.setSize(reader.getSize());
        // XXX reader.getEncoding() sometimes returns "utf-8", sometimes "UTF-8"
        dataFile.setInitialCodepage(reader.getEncoding().toUpperCase());

        return dataFile;
    }

    /**
     * Creating test temp file...
     * @param path
     */
    public void createTestTmpfile(String path){
        try{
            String dfId = new Long(System.currentTimeMillis()).toString();
            File fCacheDir = new File(path);

            log.trace("tempdir: isAbsolute: " + fCacheDir.isAbsolute());
            log.trace("tempdir: canExecute: " + fCacheDir.canExecute());
            log.trace("tempdir: canRead: " + fCacheDir.canRead());
            log.trace("tempdir: canWrite: " + fCacheDir.canWrite());
            log.trace("tempdir: exists: " + fCacheDir.exists());
            log.trace("tempdir: getFreeSpace: " + fCacheDir.getFreeSpace());
            log.trace("tempdir: isDirectory: " + fCacheDir.isDirectory());
            log.trace("tempdir: isFile: " + fCacheDir.isFile());
            log.trace("tempdir: isHidden: " + fCacheDir.isHidden());
            log.trace("SET WRITABLE: " + fCacheDir.setWritable(true));

            File file = File.createTempFile(dfId, ".df", fCacheDir);
            log.trace("CREATED NEW TEST TEMP file:" + file.getAbsolutePath());

            log.trace("File: carWrite: " + file.canWrite());

            FileUtils.writeStringToFile(file, "üõöäžšÜÕÖÄŽŠ\nFIN!", "UTF-8");

            //file.delete();
        } catch (Exception e){
            log.error("ERROR: " + e.getMessage(), e);
        }

    }
    public DataFile createDataFile(SignedDoc signedDocument, ContentReader reader, String fileName) throws
            DigiDocException, IOException {

        DataFile dataFile = new DataFile(signedDocument.getNewDataFileId(), DataFile.CONTENT_EMBEDDED_BASE64,
                fileName, reader.getMimetype(), signedDocument);

        if(dataFile == null) {
            log.error("dataFile is NULL!!!");
        } else {
            log.trace("dataFile: is body BASH64: " + dataFile.getBodyIsBase64());
            log.trace("dataFile: hasAccessToDataFile: " + dataFile.hasAccessToDataFile());
        }
        String systemTmpPath = System.getProperty("java.io.tmpdir");
        log.trace("SYSTEM TMP PATH: " + systemTmpPath);
        String CATALINA_TMPDIR = System.getProperty("CATALINA_TMPDIR");
        log.trace("CATALINA_TMPDIR: " + CATALINA_TMPDIR);
        String digiDocFilePath = ConfigManager.instance().
                getStringProperty("DIGIDOC_DF_CACHE_DIR", systemTmpPath );
        log.trace("DIGIDOC_DF_CACHE_DIR: " + digiDocFilePath);

        createTestTmpfile(digiDocFilePath);

        log.trace("createCacheFile...");
        try {
            dataFile.createCacheFile();
            log.trace("dataFile: hasAccessToDataFile: " + dataFile.hasAccessToDataFile());
            log.trace("dataFile id: " + dataFile.getId());
            log.trace("dataFile body is BASE64?: " + dataFile.getBodyIsBase64());
            log.trace("dataFile Filename: " + dataFile.getFileName());
            log.trace("dataFile MIME type: " + dataFile.getMimeType());
        } catch (Exception e){
            log.error("dataFile: ERROR creating cache file: " + e.getMessage(), e);
            throw e;
        }
        log.trace("dataFile: Create cache file... Created!");
        return dataFile;
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
                if (derObject instanceof ASN1Sequence) {
                    collectObjectIdentifiers((ASN1Sequence) derObject, objectIdentifiers);
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

    private static void collectObjectIdentifiers(ASN1Sequence sequence, List<String> objectIdentifiers) {
        for (Enumeration<?> enumeration = sequence.getObjects(); enumeration.hasMoreElements();) {
            Object element = enumeration.nextElement();
            if (element instanceof DERObjectIdentifier) {
                objectIdentifiers.add(((DERObjectIdentifier) element).getId());
            } else if (element instanceof ASN1Sequence) {
                collectObjectIdentifiers((ASN1Sequence) element, objectIdentifiers);
            }
        }
    }

    @Override
    public void writeEncryptedContainer(OutputStream output, List<NodeRef> contents, List<X509Certificate> recipientCerts, String containerFileName) {
        Assert.isTrue(!contents.isEmpty());
        Assert.isTrue(!recipientCerts.isEmpty());

        try {
            EncryptedData cdoc = new EncryptedData(null, null, "http://www.sk.ee/DigiDoc/v1.3.0/digidoc.xsd", EncryptedData.DENC_XMLNS_XMLENC, EncryptedData.DENC_ENC_METHOD_AES128);

            int idCounter = 1;
            for (X509Certificate recipientCert : recipientCerts) {
            	if (CERTIFICATE_ALGORITHM_EC.equals(recipientCert.getPublicKey().getAlgorithm())) {
            		continue;
            	}
                X509Principal principal = PrincipalUtil.getSubjectX509Principal(recipientCert);
                Vector<?> values = principal.getValues(X509Name.CN);
                String cn = (String) values.get(0);

                String id = "ID" + idCounter++;
                EncryptedKey ekey = new EncryptedKey(id, cn, EncryptedData.DENC_ENC_METHOD_RSA1_5, null, null, recipientCert);
                cdoc.addEncryptedKey(ekey);
            }

            SignedDoc dDoc = createSignedDDoc(contents);

            // Like cdoc.setPropRegisterDigiDoc(signedDoc), but with minor improvements
            for (int i = 0; i < dDoc.countDataFiles(); i++) {
                DataFile df = dDoc.getDataFile(i);
                StringBuffer sb = new StringBuffer();
                sb.append(df.getFileName());
                sb.append("|");
                sb.append(df.getSize());
                sb.append("|");
                sb.append(df.getMimeType());
                sb.append("|");
                sb.append(df.getId());
                EncryptionProperty prop = new EncryptionProperty(EncryptedData.ENCPROP_ORIG_FILE, sb.toString());
                prop.setId(EncryptedData.ENCPROP_ORIG_FILE);
                cdoc.addProperty(prop);
            }

            File tmpFile = TempFileProvider.createTempFile("container-", ".ddoc");
            try {
                try (OutputStream tmpOutput = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
                    dDoc.writeToStream(tmpOutput);
                }
                try (InputStream tmpInput = new BufferedInputStream(new FileInputStream(tmpFile))) {
                    cdoc.encryptStream(tmpInput, output, EncryptedData.DENC_COMPRESS_NEVER);
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
