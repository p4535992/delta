package ee.webmedia.alfresco.document.bootstrap;

import java.util.Collections;
import java.util.Set;

import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Populates shortRegNr field for registered documents. Updates only ArchivalsStore.
 * 
 * @author Kaarel Jõgeva
 */
public class ShortRegNumberInArchiveUpdater extends ShortRegNumberUpdater {

	@Override
	protected Set<StoreRef> getStores() {
    	return Collections.singleton(generalService.getArchivalsStoreRef());
	}

}
