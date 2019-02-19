package ee.smit.digisign;

import com.google.common.io.ByteStreams;
import ee.smit.common.FileMessageResource;
import ee.smit.common.RestUtil;
import ee.smit.digisign.domain.SignCertificate;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.FilenameUtil;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static ee.smit.common.RestUtil.makePostRequest;

public class DigiSignSearchesImpl implements DigiSignSearches {
    protected final static Log log = LogFactory.getLog(DigiSignSearchesImpl.class);

    protected DigiSignService digiSignService;
    private String defaultRootDir;

    /**
     *
     * @param cn
     * @return
     * @throws Exception
     */
    public JSONObject getCertificatesByCn(String cn)  throws JSONException{
        String seachUri = digiSignService.getUri() + "/api/certificate/searchByCn/" + cn;
        return RestUtil.searchByGet(seachUri);
    }


    public JSONObject getCertificatesByCnAndSerialNumber(String cn, String serialnumber)  throws JSONException{
        if(cn == null){
            cn="";
        }
        if(serialnumber == null){
            serialnumber = "";
        }

        String seachUri = digiSignService.getUri() + "/api/certificate/search?cn=" + cn + "&serialnumber="+serialnumber;
        return RestUtil.searchByGet(seachUri);
    }

    /**
     *
     * @param serialNumber
     * @return
     * @throws Exception
     */
    public JSONObject getCertificatesBySerialNumber(String serialNumber)  throws JSONException{
        String seachUri = digiSignService.getUri() + "/api/certificate/searchBySerialNumber/" + serialNumber;
        log.debug("getCertificatesBySerialNumber: uri: " + seachUri);
        return RestUtil.searchByGet(seachUri);
    }

    private void getOutputStreamCdocResponse(OutputStream out, JSONObject json) throws JSONException, IOException {
        log.debug("RESPONSE status: " + json.get("status"));
        log.debug("RESPONSE status code: " + json.get("statusCode"));

        String cdocFileBase64Encoded = json.getString("file");
        log.debug("CDOC string length: " + (cdocFileBase64Encoded == null ? "NULL" : cdocFileBase64Encoded.length()));
        if(cdocFileBase64Encoded != null || !cdocFileBase64Encoded.isEmpty()){
            //log.trace("CDOC: [" + cdocFileBase64Encoded + "]");
            log.debug("CDOC string length: " + cdocFileBase64Encoded.length());
            byte[] file = Base64.decodeBase64(cdocFileBase64Encoded);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            out.write(file);
        }
        IOUtils.closeQuietly(out);

    }

    private List<JSONObject> getFilesByFileRefList(List<NodeRef> fileRefs) throws Exception {
        List<JSONObject> filesList = new ArrayList<>();
        for(NodeRef ref : fileRefs){
            JSONObject file = new JSONObject();
            byte[] fileBytes = getContent(ref);
            String filedata = Base64.encodeBase64String(fileBytes);
            file.put("filedata", filedata);
            file.put("filename", getFilenameByNodeRef(ref));
            filesList.add(file);
        }

        return filesList;
    }

    private FileInfo getFileinfoByRef(NodeRef ref){
        FileInfo fileInfo = BeanHelper.getFileFolderService().getFileInfo(ref);
        return fileInfo;
    }

    private String getFilenameByNodeRef(NodeRef ref){
        FileInfo fileInfo = BeanHelper.getFileFolderService().getFileInfo(ref);
        return FilenameUtil.makeSafeFilename(fileInfo.getName());
    }

    public byte[] getContent(NodeRef nodeRef){
        try{
            InputStream contentInputStream = getContentInputStream(nodeRef);
            if(contentInputStream != null){
                byte[] filedata = IOUtils.toByteArray(contentInputStream);
                IOUtils.closeQuietly(contentInputStream);
                return filedata;
            }
        } catch (Exception e){
            log.error(e.getMessage());
        }
        return  null;
    }

    public Path getContentPath(NodeRef nodeRef){
        try{

        } catch (Exception e){
            log.error(e.getMessage());
        }
        return null;
    }

    public InputStream getContentInputStream(NodeRef nodeRef){
        try{

            String rootLocation = getDefaultRootDir();
            ContentReader reader = BeanHelper.getFileFolderService().getReader(nodeRef);
            if (reader == null) {
                return null;
            }
            InputStream contentInputStream = reader.getContentInputStream();
            return contentInputStream;

        } catch (Exception e){
            log.error(e.getMessage());
        }
        return null;
    }


    private List<JSONObject> convertX509CertListToBase64List(List<X509Certificate> certs) throws CertificateEncodingException, JSONException {
        List<JSONObject> certList = new ArrayList<>();
        for(X509Certificate cert: certs){
            byte[] certBytes = cert.getEncoded();
            JSONObject j = new JSONObject();
            String certdata = Base64.encodeBase64String(certBytes);
            j.put("certificate", certdata);
            certList.add(j);

        }
        return certList;
    }

    public JSONObject checkDigidocCrypto(String base64FileData) throws JSONException {
        String postUri = digiSignService.getUri() + "/api/asics/checkCrypto";

        JSONObject request = new JSONObject();
        request.put("file", base64FileData);

        return makePostRequest(request, postUri);

    }

    public JSONObject checkDigidocCryptoAndTimestamp(List<JSONObject> files) throws JSONException{
        String postUri = digiSignService.getUri() + "/api/asics/checkCryptoAndMake";

        JSONObject request = new JSONObject();
        request.put("files", files);

        return makePostRequest(request, postUri);

    }


    public byte[] makeAsicS(String filename, byte[] fileBytes) throws IOException {
        String serviceUrl = digiSignService.getUri() + "/api/asics/create";
        //return createAsicsRequestByIs(filename, contentPath, serviceUrl);
        log.info("REST service url: " + serviceUrl + " - upload filesize: " + fileBytes.length + " bytes, filename: " + filename);
        return createAsicsRequestByFilePath(filename, fileBytes, serviceUrl);
    }

    public byte[] makeAsicS(String filename, InputStream is){
        String serviceUrl = digiSignService.getUri() + "/api/asics/create";
        return createAsicsRequestByIsV2(filename, is, serviceUrl);
    }
    private byte[] createAsicsRequestByIsV2(final String filename, final InputStream is, String serviceUrl){
        final RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void doWithRequest(final ClientHttpRequest request) throws IOException {
                request.getHeaders().add("Content-type", "application/octet-stream");
                request.getHeaders().add("Content-Disposition", "attachment; filename=\""+filename+"\"");
                IOUtils.copy(is, request.getBody());
                IOUtils.closeQuietly(is);
            }
        };
        final RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        restTemplate.setRequestFactory(requestFactory);

        byte[] data = restTemplate.execute(
                serviceUrl,
                HttpMethod.POST,
                requestCallback,
                new BinaryFileExtractor()
        );

        return data;
    }

    private byte[] createAsicsRequestByIs(String filename, String contentPath, String serviceUrl) throws IOException {
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();

        bodyMap.add("file", getFileResource(filename, contentPath));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<byte[]> response = restTemplate.exchange(
                serviceUrl,
                HttpMethod.POST,
                requestEntity,
                byte[].class);
        byte body[] = response.getBody();
        return body;
    }


    private byte[] createAsicsRequestByFilePath(String filename, byte[] fileBytes, String serviceUrl) throws IOException {
        log.info("Create ASICS request... filename: " + filename + ", filesize: " + (fileBytes == null ? "NULL" : fileBytes.length));
        final MultiValueMap<String,Object> data = new LinkedMultiValueMap<>();
        log.info("Add file to data map...");
        data.add("file", new FileMessageResource(fileBytes, filename));

        final HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(data);
        log.info("Make request...");
        try{
            final ResponseEntity<byte[]> response = new RestTemplate().exchange(serviceUrl, HttpMethod.POST, requestEntity, byte[].class);

            logReponse(response);
            if(response.getStatusCode().equals(HttpStatus.CREATED)){
                log.debug("Status CODE is " + response.getStatusCode());
                if(requestEntity.hasBody()){
                    log.debug("Return response body..");
                    return response.getBody();
                }
                log.warn("Response has no body. Return NULL!");
            }
        } catch (Exception e){
            log.error(e.getMessage());
        }

        return null;
    }

    private void logReponse(ResponseEntity<byte[]> response){
        try{
            log.debug("Response: statuscode: " + response.getStatusCode());
            log.debug("Response: hasbody: " + response.hasBody());
            log.debug("Response: headers: contentType" + response.getHeaders().getContentType());

        }catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public void setDefaultRootDir(String defaultRootDir) {
        this.defaultRootDir = defaultRootDir;
    }

    public String getDefaultRootDir() {
        return defaultRootDir;
    }

    public class BinaryFileExtractor  implements ResponseExtractor<byte[]> {
        @Override
        public byte[] extractData(ClientHttpResponse response) throws IOException {
            return ByteStreams.toByteArray(response.getBody());
        }
    }

    public static byte[] getBytesFromFilePath(String Path) throws IOException {
        Path path = Paths.get(Path);
        return Files.readAllBytes(path);
    }

    public static Resource getFileResource(String fileName, String path) throws IOException {
        String fileExt = FilenameUtil.getDigiDocExt(fileName).toLowerCase();
        String fileBaseName = FilenameUtils.getBaseName(fileName);
        //todo replace tempFile with a real file
        Path tempFile = Files.createTempFile(fileBaseName, "."+fileExt);

        Files.write(tempFile, getBytesFromFilePath(path));

        log.debug("uploading: " + tempFile);
        File file = tempFile.toFile();
        //to upload in-memory bytes use ByteArrayResource instead
        return new FileSystemResource(file);
    }

    /**
     *
     * @param certs (base64 encoded input)
     * @param fileRefs List of nodeRefs
     * @param out Outputstream
     */
    public void makeCdoc(List<X509Certificate> certs, List<NodeRef> fileRefs, OutputStream out) throws Exception {
        String postUri = digiSignService.getUri() + "/api/crypt/make";

        JSONObject request = new JSONObject();

        List<JSONObject> certList = convertX509CertListToBase64List(certs);

        request.put("certificates", certList);

        List<JSONObject> files = getFilesByFileRefList(fileRefs);
        request.put("files", files);

        JSONObject resp = makePostRequest(request, postUri);
        getOutputStreamCdocResponse(out, resp);
    }


    public List<SignCertificate> getCertificatesFromDigiSignService(String serialNumber, String cn){
        List<SignCertificate> signCertificateList = new ArrayList<>();
        log.debug("Get certificates by cn name: " + cn);

        if(!cn.isEmpty() && !serialNumber.isEmpty()){
            signCertificateList.addAll(getCertsByCnandSerialNr(cn, serialNumber));
        } else {
            if(!cn.isEmpty()) {
                signCertificateList.addAll(getCertsByCn(cn));
            }

            if(!serialNumber.isEmpty()){
                signCertificateList.addAll(getCertsBySerialNumber(serialNumber));
            }
        }
        log.debug("Certificate list size: " + signCertificateList.size());
        return signCertificateList;
    }

    private List<SignCertificate> getSignCertificates(JSONObject jsonByCn) {
        log.debug("Check CN JSON Response...");
        List<SignCertificate> certListCn = checkJsonResponse(jsonByCn);
        if(certListCn != null && !certListCn.isEmpty()){
            log.debug("Found crypt supported certificates by cn (name)..");
        } else {
            log.trace("JSON response is NULL!");
        }
        return certListCn;
    }

    private List<SignCertificate> getCertsByCnandSerialNr(String cn, String serialNumber){
        JSONObject jsonByCn = getCertByCnandSerialNr(cn, serialNumber);
        return getSignCertificates(jsonByCn);
    }

    private List<SignCertificate> getCertsByCn(String cn){
        JSONObject jsonByCn = getCertByCn(cn);
        return getSignCertificates(jsonByCn);
    }

    private List<SignCertificate> getCertsBySerialNumber(String serialNumber){
        log.debug("Get certificates by serialNumber: " + serialNumber);
        JSONObject jsonBySerialNumber = getCertBySerialNumber(serialNumber);
        log.debug("Check SerialNumber JSON Response...");
        List<SignCertificate> certListNr = checkJsonResponse(jsonBySerialNumber);
        if(certListNr != null && !certListNr.isEmpty()){
            log.debug("Found crypt supported certificates by serialNumber (id-code, organization reg code)..");
        } else {
            log.trace("JSON response is NULL!");
        }
        return certListNr;
    }

    public List<SignCertificate> checkJsonResponse(JSONObject json){
        if(json == null){
            return null;
        }
        List<SignCertificate> certList = new ArrayList<>();

        try{
            log.debug("DigiSign response status: " + json.getString("status"));
            if(json.getString("status").equals("NOT_FOUND")){
                return certList;
            }
            log.debug("-- GET certificates -----------------------------");

            log.debug("Try to get get(certificates)....");
            Object o = json.get("certificates");
            if(o == null){
                return certList;
            }
            log.debug("Object class: " + o.getClass());
            if(o instanceof ArrayList){
                List<Object> objectList = (ArrayList<Object>) o;
                for (Object o1 : objectList){
                    log.debug("ObjectList o1 class: " + o1.getClass());
                    if(o1 instanceof Map){
                        log.debug("It's a MAP!");
                        SignCertificate digiSignCert = new SignCertificate();
                        Map<String, Object> certObject = (Map<String, Object>) o1;

                        String cn = (String) certObject.get("cn");
                        log.debug("CN: " + cn);
                        digiSignCert.setCn(cn);

                        String serialNumber = (String )certObject.get("serialNumber");
                        log.debug("serialNumber: " + serialNumber);
                        digiSignCert.setSerialNumber(serialNumber);

                        String certDataBase64Encoded = (String) certObject.get("userEncryptionCertificate");
                        log.debug("Certificate data: " + certDataBase64Encoded);
                        digiSignCert.setData(Base64.decodeBase64(certDataBase64Encoded));

                        String issuerCn = (String)certObject.get("issuerCn");
                        log.debug("issuerCn: " + issuerCn);
                        digiSignCert.setIssuerCn(issuerCn);

                        String notAfter = (String)certObject.get("notAfter");
                        log.debug("notAfter date: " + notAfter);
                        digiSignCert.setNotAfter(digiSignStringToDate(notAfter));

                        String notBefore = (String)certObject.get("notBefore");
                        log.debug("notBefore date: " + notBefore);
                        digiSignCert.setNotBefore(digiSignStringToDate(notBefore));

                        boolean encryptionSupported = (boolean) certObject.get("encryptionSupported");
                        log.debug("encryptionSupported: " + encryptionSupported);
                        digiSignCert.setEncryptionSupported(encryptionSupported);

                        certList.add(digiSignCert);
                    }
                }
            }

        } catch (Exception e){
            log.error(e.getMessage(), e);
        }

        return certList;
    }

    public Date stringToDate(String dateValue){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return parseStringToDate(dateValue, format);
    }

    public Date digiSignStringToDate(String dateValue){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000+0000");
        return parseStringToDate(dateValue, format);
    }

    private Date parseStringToDate(String dateValue, SimpleDateFormat format){
        try {
            Date date = format.parse(dateValue);
            return date;
        } catch (ParseException e) {
            log.error("String to Date convert failed! " + e.getMessage(), e);
        }

        return null;

    }

    private JSONObject getCertBySerialNumber(String orgCode){
        JSONObject json = null;
        if (StringUtils.isNotBlank(orgCode)) {
            try{
                json = BeanHelper.getDigiSignSearches().getCertificatesBySerialNumber(orgCode);
            } catch (JSONException e){
                log.error("DigiSign certificate search by org code failed!" + e.getMessage(), e);
            }
        }
        return json;
    }

    private JSONObject getCertByCn(String orgName){
        JSONObject json = null;
        if (StringUtils.isNotBlank(orgName)) {
            try{
                json = BeanHelper.getDigiSignSearches().getCertificatesByCn(orgName);
            } catch (JSONException e){
                log.error("DigiSign certificate search by org code failed!" + e.getMessage(), e);
            }
        }
        return json;
    }

    private JSONObject getCertByCnandSerialNr(String orgName, String orgCode){
        JSONObject json = null;
        if (StringUtils.isNotBlank(orgName) && StringUtils.isNotBlank(orgCode)) {
            try{
                json = BeanHelper.getDigiSignSearches().getCertificatesByCnAndSerialNumber(orgName, orgCode);
            } catch (JSONException e){
                log.error("DigiSign certificate search by org code failed!" + e.getMessage(), e);
            }
        }
        return json;
    }
    public void setDigiSignService(DigiSignServiceImpl digiSignService) {
        this.digiSignService = digiSignService;
    }
}