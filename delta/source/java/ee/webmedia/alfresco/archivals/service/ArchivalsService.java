package ee.webmedia.alfresco.archivals.service;

import ee.webmedia.alfresco.functions.model.Function;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;

/**
 * @author Romet Aidla
 */
public interface ArchivalsService {
    String BEAN_NAME = "ArchivalsService";

    NodeRef archiveVolume(NodeRef volumeNodeRef, String archivingNote);

    int destroyArchivedVolumes();

    List<Function> getArchivedFunctions();
}
