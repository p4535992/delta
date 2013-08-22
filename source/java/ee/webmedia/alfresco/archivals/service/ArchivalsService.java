package ee.webmedia.alfresco.archivals.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.functions.model.Function;

/**
 * @author Romet Aidla
 */
public interface ArchivalsService {
    String BEAN_NAME = "ArchivalsService";

    NodeRef archiveVolume(NodeRef volumeNodeRef, String archivingNote);

    int destroyArchivedVolumes();

    List<Function> getArchivedFunctions();
}
