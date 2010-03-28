package ee.webmedia.alfresco.signature.service;

import java.io.InputStream;
import java.util.List;

import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.sk.digidoc.DigiDocException;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;

/**
 * Service class that handles DigiDoc containers.
 * Has methods that create, check, sign, retrieve signatures, signature digests and files from the container.
 * 
 * @author Dmitri Melnikov
 */
public interface SignatureService {

    String BEAN_NAME = "SignatureService";
    String DIGIDOC_MIMETYPE = "application/digidoc";

    /**
     * Checks if the referenced file is a .ddoc
     * 
     * @param nodeRef
     * @return
     */
    boolean isDigiDocContainer(NodeRef nodeRef);

    /**
     * Checks if the referenced file is a .ddoc
     * 
     * @param fileInfo
     * @return
     */
    boolean isDigiDocContainer(FileInfo fileInfo);
    
    /**
     * Returns the signature digest made from the cert and data pointed to by nodeRef.
     * Used for signing existing .ddoc files.
     * 
     * @param nodeRef
     * @param certHex
     * @return
     * @throws SignatureException 
     */
    SignatureDigest getSignatureDigest(NodeRef nodeRef, String certHex) throws SignatureException;

    /**
     * Returns the signature digest made from the cert and data pointed to by selectedNodeRefs.
     * Used in the process of creating and signing new .ddoc files.
     * 
     * @param selectedNodeRefs
     * @param certHex
     * @return
     * @throws SignatureException 
     */
    SignatureDigest getSignatureDigest(List<NodeRef> selectedNodeRefs, String certHex) throws SignatureException;

    /**
     * Given the <code>NodeRef</code> of the .ddoc, return its signatures.
     * 
     * @param nodeRef
     * @return signature list. empty if no signatures exist.
     * @throws SignatureException 
     * @throws DigiDocException
     */
    List<SignatureItem> getSignatureItems(NodeRef nodeRef) throws SignatureException;

    /**
     * Given the <code>NodeRef</code> of the .ddoc, return contained data files.
     * 
     * @param nodeRef
     * @param includeData include the data itself or not
     * @return data files list. empty if no files exist.
     * @throws SignatureException 
     */
    List<DataItem> getDataItems(NodeRef nodeRef, boolean includeData) throws SignatureException;

    /**
     * Given the <code>NodeRef</code> of the .ddoc, return a selected data file.
     * 
     * @param nodeRef
     * @param id
     * @param includeData include the data itself or not
     * @return
     * @throws SignatureException 
     */
    DataItem getDataItem(NodeRef nodeRef, int id, boolean includeData) throws SignatureException;

    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files.
     * 
     * @param nodeRef
     * @param includeData include the data itself or not
     * @return
     * @throws SignatureException 
     */
    SignatureItemsAndDataItems getDataItemsAndSignatureItems(NodeRef nodeRef, boolean includeData) throws SignatureException;

    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files.
     * 
     * @param inputStream
     * @param includeData include the data itself or not
     * @return
     * @throws SignatureException 
     */
    SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData) throws SignatureException;

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
     * Create a ddoc file from the file(s) pointed to by contents with the given filename
     * in the folder pointed to by parent and also sign the created ddoc using signatureDigest
     * and signatureHex.
     * 
     * @param parent
     * @param contents
     * @param filename
     * @param signatureDigest
     * @param signatureHex
     * @return
     * @throws SignatureRuntimeException 
     */
    NodeRef createContainer(NodeRef parent, List<NodeRef> contents, String filename, SignatureDigest signatureDigest, String signatureHex);

    /**
     * Changes an existing document to ddoc and signs it
     * @param nodeRef - nodeRef of existing document
     * @param contents - the contents
     * @param signatureDigest
     * @param signatureHex
     * @throws SignatureRuntimeException 
     */
    void writeContainer(NodeRef nodeRef, List<NodeRef> contents, SignatureDigest signatureDigest, String signatureHex);

}
