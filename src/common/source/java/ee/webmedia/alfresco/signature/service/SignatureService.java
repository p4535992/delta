package ee.webmedia.alfresco.signature.service;

import java.io.InputStream;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.sk.digidoc.DigiDocException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;

/**
 * Service class that handles DigiDoc containers.
 * Has methods that create, check, sign, retrieve signatures, signature digests and files from the container.
 * 
 * @author dmitrim
 */
public interface SignatureService {

    public static final String BEAN_NAME = "SignatureService";
    public static final String DIGIDOC_MIMETYPE = "application/digidoc";

    /**
     * Checks if the referenced file is a .ddoc
     * 
     * @param nodeRef
     * @return
     */
    boolean isDigiDocContainer(NodeRef nodeRef);

    /**
     * Returns the signature digest made from the cert and data pointed to by nodeRef.
     * Used for signing existing .ddoc files.
     * 
     * @param nodeRef
     * @param certHex
     * @return
     */
    SignatureDigest getSignatureDigest(NodeRef nodeRef, String certHex);

    /**
     * Returns the signature digest made from the cert and data pointed to by selectedNodeRefs.
     * Used in the process of creating and signing new .ddoc files.
     * 
     * @param selectedNodeRefs
     * @param certHex
     * @return
     */
    SignatureDigest getSignatureDigest(List<NodeRef> selectedNodeRefs, String certHex);

    /**
     * Add the signature to the document pointed to by nodeRef.
     * 
     * @param nodeRef
     * @param signatureDigest
     * @param signatureHex
     */
    void addSignature(NodeRef nodeRef, SignatureDigest signatureDigest, String signatureHex);

    /**
     * Given the <code>NodeRef</code> of the .ddoc, return its signatures.
     * 
     * @param nodeRef
     * @return signature list. empty if no signatures exist.
     * @throws DigiDocException
     */
    List<SignatureItem> getSignatureItems(NodeRef nodeRef);

    /**
     * Given the <code>NodeRef</code> of the .ddoc, return contained data files.
     * 
     * @param nodeRef
     * @param includeData include the data itself or not
     * @return data files list. empty if no files exist.
     */
    List<DataItem> getDataItems(NodeRef nodeRef, boolean includeData);

    /**
     * Given the <code>NodeRef</code> of the .ddoc, return a selected data file.
     * 
     * @param nodeRef
     * @param id
     * @param includeData include the data itself or not
     * @return
     */
    DataItem getDataItem(NodeRef nodeRef, int id, boolean includeData);

    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files.
     * 
     * @param nodeRef
     * @param includeData include the data itself or not
     * @return
     */
    SignatureItemsAndDataItems getDataItemsAndSignatureItems(NodeRef nodeRef, boolean includeData);

    /**
     * Returns data files and signatures in a map. Keys are "signatureItems" for signature items and
     * "dataItems" for data files.
     * 
     * @param inputStream
     * @param includeData include the data itself or not
     * @return
     */
    SignatureItemsAndDataItems getDataItemsAndSignatureItems(InputStream inputStream, boolean includeData);

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
     */
    NodeRef createContainer(NodeRef parent, List<NodeRef> contents, String filename, SignatureDigest signatureDigest, String signatureHex);

}
