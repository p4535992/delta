package ee.webmedia.alfresco.common.bootstrap;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.user.service.UserService;

public class FoldersPermissionForDocManagerUpdater extends AbstractNodeUpdater {



    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        log.info("Searching nodes from repository");
        Set<NodeRef> nConcurrentHashMapodeSet = Collections.newSetFromMap(new ConcurrentHashMap<NodeRef, Boolean>());
        
        nConcurrentHashMapodeSet.add(BeanHelper.getGeneralService().getNodeRef("/doccom:dvkReceived"));
        nConcurrentHashMapodeSet.add(BeanHelper.getGeneralService().getNodeRef("/doccom:dvkReceivedCorruptDocuments"));
        nConcurrentHashMapodeSet.add(BeanHelper.getGeneralService().getNodeRef("/doccom:scannedDocs"));
        nConcurrentHashMapodeSet.add(BeanHelper.getGeneralService().getNodeRef("/doccom:webServiceReceived"));
        nConcurrentHashMapodeSet.add(BeanHelper.getGeneralService().getNodeRef("/imap-ext:imap-root"));
        nConcurrentHashMapodeSet.add(BeanHelper.getGeneralService().getNodeRef("/doccom:drafts"));
        
        
        return nConcurrentHashMapodeSet;
    }


    @Override
    protected String[] updateNode(NodeRef folderRef) throws Exception {
    	String result = "empty";
    	if (folderRef != null && nodeService.exists(folderRef)) {
	        BeanHelper.getPrivilegeService().setInheritParentPermissions(folderRef, false);
	        BeanHelper.getPrivilegeService().setPermissions(folderRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES, Privilege.EDIT_DOCUMENT);
	    	
	        result = nodeService.getPath(folderRef).toString();
    	}
        
        return new String[] { result };
    }

    @Override
    protected boolean usePreviousInputState() {
        return false;
    }

	@Override
	protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
