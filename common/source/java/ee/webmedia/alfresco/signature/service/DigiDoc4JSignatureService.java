package ee.webmedia.alfresco.signature.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.digidoc4j.Container;
import org.digidoc4j.DataToSign;

import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureChallenge;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.model.SkLdapCertificate;

/**
 * Service class that handles DigiDoc containers.
 * Has methods that create, check, sign, retrieve signatures, signature digests and files from the container.
 */
public interface DigiDoc4JSignatureService {
	
	public static String DIGIDOC_FORMAT_BDOC_TM = "bdoc-tm";
	public static String DIGIDOC_FORMAT_BDOC_TS = "bdoc-ts";
	public static String DIGIDOC_FORMAT_ASICE = "asice";

    String BEAN_NAME = "DigiDoc4JSignatureService";
    String DIGIDOC_MIMETYPE = "application/digidoc";

    public DataToSign getDataToSign(Container containerToSign, String certificateInHex, boolean pem);
    public void signContainer(Container container, DataToSign dataToSign, String signatureInHex);
    
    /**
     * Returns the signature digest made from the cert and data pointed to by nodeRef. Used for adding a signature to existing bdoc file.
     *
     * @param nodeRef existing bdoc file that signature is added to
     * @param certHex
     * @return
     * @throws SignatureException
     */
    public SignatureDigest getSignatureDigest(NodeRef nodeRef, String certHex) throws SignatureException;
    /**
     * Returns the signature digest made from the cert and data pointed to by selectedNodeRefs. Used for creating a new bdoc file and adding a signature to it.
     *
     * @param contents files that are added into new bdoc container
     * @param certHex
     * @return
     * @throws SignatureException
     */
    public SignatureDigest getSignatureDigest(List<NodeRef> contents, String certHex) throws SignatureException;
    public boolean isDigiDocContainer(NodeRef nodeRef);
    public NodeRef createAndSignContainer(NodeRef parent, List<NodeRef> contents, String filename, DataToSign dataToSign, String signatureHex);
    public void addSignature(NodeRef nodeRef, DataToSign dataToSign, String signatureHex);
    
    /**
     * Starts Mobile-ID signing process (sends signing message to signer's phone). Used for adding a signature to existing bdoc file.
     *
     * @param nodeRef existing bdoc file that signature is added to
     * @param phoneNo signer's phone number, must start with international call prefix (e.g. +372)
     * @param idCode TODO
     * @return
     * @throws SignatureException
     */
    public SignatureChallenge getSignatureChallenge(NodeRef nodeRef, String phoneNo, String idCode) throws SignatureException;
    /**
     * Starts Mobile-ID signing process (sends signing message to signer's phone). Used for creating a new bdoc file and adding a signature to it.
     *
     * @param contents files that are added into new bdoc container
     * @param phoneNo signer's phone number, must start with international call prefix (e.g. +372)
     * @param idCode TODO
     * @return
     * @throws SignatureException
     */
    public SignatureChallenge getSignatureChallenge(List<NodeRef> contents, String phoneNo, String idCode) throws SignatureException;
    public String getMobileIdSignature(SignatureChallenge signatureChallenge);
    
    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files.
     *
     * @param nodeRef
     * @param includeData include file contents or not
     * @return
     * @throws SignatureException
     */
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(NodeRef nodeRef, boolean includeData) throws SignatureException;

    /**
     *
     * @param inputStream
     * @param includeData include file contents or not
     * @param fileext - DDOC, BDOC
     * @return
     * @throws SignatureException
     */
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData, String fileext) throws SignatureException;

    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files. Closes the inputStream itself.
     *
     * @param inputStream
     * @param includeData include file contents or not
     * @return
     * @throws SignatureException
     */
    public SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData) throws SignatureException;
    public boolean isBDocContainer(NodeRef nodeRef);
    
    /**
     * @return if Mobile-ID is configured
     */
    boolean isMobileIdEnabled();
    
    
    void writeContainer(OutputStream output, List<NodeRef> contents);

}
