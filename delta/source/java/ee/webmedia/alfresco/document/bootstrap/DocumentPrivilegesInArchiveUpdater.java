package ee.webmedia.alfresco.document.bootstrap;

import java.util.Collections;
import java.util.Set;

import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Update document privileges according to new document privileges system. Updates only ArchivalsStore.
 * 
 * @author Ats Uiboupin
 */
public class DocumentPrivilegesInArchiveUpdater extends DocumentPrivilegesUpdater {

    protected Set<StoreRef> getStores() {
    	return Collections.singleton(generalService.getArchivalsStoreRef());
    }

}
