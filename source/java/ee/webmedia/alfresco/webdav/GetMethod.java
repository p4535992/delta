package ee.webmedia.alfresco.webdav;

import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Implements the WebDAV GET method, checks if {@link DocumentCommonModel.Privileges#VIEW_DOCUMENT_FILES} is granted to the document of the file
 * 
 * @author Ats Uiboupin
 */
public class GetMethod extends org.alfresco.repo.webdav.GetMethod {

    @Override
    protected void checkPreConditions(FileInfo nodeInfo) throws WebDAVServerException {
        NodeRef fileRef = nodeInfo.getNodeRef();
        WebDAVCustomHelper.checkDocumentFileReadPermission(fileRef);
        super.checkPreConditions(nodeInfo);
    }

}
