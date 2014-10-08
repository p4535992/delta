package ee.webmedia.alfresco.signature.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

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
public interface SignatureService {

    String BEAN_NAME = "SignatureService";
    String DIGIDOC_MIMETYPE = "application/digidoc";

    /**
     * Checks if the referenced file is a .ddoc of .bdoc file
     *
     * @param nodeRef
     * @return
     */
    boolean isDigiDocContainer(NodeRef nodeRef);

    boolean isBDocContainer(NodeRef nodeRef);

    /**
     * @return if Mobile-ID is configured
     */
    boolean isMobileIdEnabled();

    /**
     * Returns the signature digest made from the cert and data pointed to by nodeRef. Used for adding a signature to existing .bdoc file.
     *
     * @param nodeRef existing .bdoc file that signature is added to
     * @param certHex
     * @return
     * @throws SignatureException
     */
    SignatureDigest getSignatureDigest(NodeRef nodeRef, String certHex) throws SignatureException;

    /**
     * Starts Mobile-ID signing process (sends signing message to signer's phone). Used for adding a signature to existing .bdoc file.
     *
     * @param nodeRef existing .bdoc file that signature is added to
     * @param phoneNo signer's phone number, must start with international call prefix (e.g. +372)
     * @param idCode TODO
     * @return
     * @throws SignatureException
     */
    SignatureChallenge getSignatureChallenge(NodeRef nodeRef, String phoneNo, String idCode) throws SignatureException;

    /**
     * Returns the signature digest made from the cert and data pointed to by selectedNodeRefs. Used for creating a new .bdoc file and adding a signature to it.
     *
     * @param contents files that are added into new .bdoc container
     * @param certHex
     * @return
     * @throws SignatureException
     */
    SignatureDigest getSignatureDigest(List<NodeRef> contents, String certHex) throws SignatureException;

    /**
     * Starts Mobile-ID signing process (sends signing message to signer's phone). Used for creating a new .bdoc file and adding a signature to it.
     *
     * @param contents files that are added into new .bdoc container
     * @param phoneNo signer's phone number, must start with international call prefix (e.g. +372)
     * @param idCode TODO
     * @return
     * @throws SignatureException
     */
    SignatureChallenge getSignatureChallenge(List<NodeRef> contents, String phoneNo, String idCode) throws SignatureException;

    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files.
     *
     * @param nodeRef
     * @param includeData include file contents or not
     * @return
     * @throws SignatureException
     */
    SignatureItemsAndDataItems getDataItemsAndSignatureItems(NodeRef nodeRef, boolean includeData, boolean isBdoc) throws SignatureException;

    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files. Closes the inputStream itself.
     *
     * @param inputStream
     * @param includeData include file contents or not
     * @return
     * @throws SignatureException
     */
    SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData, boolean isBdoc) throws SignatureException;

    String getMobileIdSignature(SignatureChallenge signatureChallenge);

    /**
     * Add the signature to the document pointed to by nodeRef.
     *
     * @param nodeRef
     * @param signatureDigest
     * @param signatureHex
     * @throws SignatureRuntimeException
     */
    void addSignature(NodeRef nodeRef, SignatureDigest signatureDigest, String signatureHex);

    /**
     * Finishes Mobile-ID signing process (gets response from signer's phone and writes to .bdoc). Used for adding a signature to existing .bdoc file.
     *
     * @param nodeRef existing .bdoc file that signature is added to
     * @param signatureChallenge
     * @throws SignatureRuntimeException
     */
    void addSignature(NodeRef nodeRef, SignatureChallenge signatureChallenge, String signature);

    /**
     * Create a bdoc file from the file(s) pointed to by contents with the given filename
     * in the folder pointed to by parent and also sign the created bdoc using signatureDigest
     * and signatureHex.
     *
     * @param parent
     * @param contents files that are added into new .bdoc container
     * @param filename
     * @param signatureDigest
     * @param signatureHex
     * @return
     * @throws SignatureRuntimeException
     */
    NodeRef createContainer(NodeRef parent, List<NodeRef> contents, String filename, SignatureDigest signatureDigest, String signatureHex);

    /**
     * Finishes Mobile-ID signing process (gets response from signer's phone and writes to .bdoc). Used for creating a new .bdoc file and adding a signature to it.
     *
     * @param parent
     * @param contents files that are added into new .bdoc container
     * @param filename
     * @param signatureChallenge
     * @return
     * @throws SignatureRuntimeException
     */
    NodeRef createContainer(NodeRef parent, List<NodeRef> contents, String filename, SignatureChallenge signatureChallenge, String signature);

    /**
     * Changes an existing document to bdoc and signs it
     *
     * @param nodeRef nodeRef of existing document
     * @param contents the contents
     * @param signatureDigest
     * @param signatureHex
     * @throws SignatureRuntimeException
     */
    void writeContainer(NodeRef nodeRef, List<NodeRef> contents, SignatureDigest signatureDigest, String signatureHex);

    void writeContainer(OutputStream output, List<NodeRef> contents);

    /**
     * Parse certificates and return only certificates suitable for encryption recipients.
     *
     * @param certificates
     * @throws SignatureRuntimeException
     * @return
     */
    List<X509Certificate> getCertificatesForEncryption(List<SkLdapCertificate> certificates);

    /**
     * Create a CDOC file (encrypted container).
     *
     * @param output output stream that newly created CDOC file is written to; stream is closed automatically.
     * @param contents input files that are encrypted; at least one file is required.
     * @param recipientCerts certificates of recipient or recipient; at least one is required.
     * @throws SignatureRuntimeException
     */
    void writeEncryptedContainer(OutputStream output, List<NodeRef> contents, List<X509Certificate> recipientCerts);

}
