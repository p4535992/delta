package ee.smit.digisign;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public interface DigiSignSearches {
    String BEAN_NAME = "DigiSignSearches";

    JSONObject getCertificatesByCn(String cn) throws JSONException;

    JSONObject getCertificatesByCnAndSerialNumber(String cn, String serialnumber)  throws JSONException;

    JSONObject getCertificatesBySerialNumber(String serialNumber) throws JSONException;

    void makeCdoc(List<X509Certificate> certs, List<NodeRef> fileRefs, OutputStream tmpOutput) throws Exception;

    JSONObject checkDigidocCrypto(String base64FileData) throws JSONException ;

    JSONObject checkDigidocCryptoAndTimestamp(List<JSONObject> files) throws JSONException;

    byte[] makeAsicS(String filename, byte[] fileBytes) throws IOException;

    byte[] makeAsicS(String filename, InputStream is);

    List<SignCertificate> checkJsonResponse(JSONObject json);

    List<SignCertificate> getCertificatesFromDigiSignService(String orgCode, String orgName);

    byte[] getContent(NodeRef nodeRef);

    InputStream getContentInputStream(NodeRef nodeRef);

}