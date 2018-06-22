package ee.smit.tera;

import ee.smit.tera.model.TeraFilesEntry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface TeraService {
    String BEAN_NAME = "teraService";

    void addFileEnrty(String nodeRef, String filename, String filetype, String crypt);

    void updateCryptAndAsics(Long id, String crypt, boolean asics);

    void updateTeraFilesRow(String nodeRef, String fileName, String fileType, String crypt, String statusInfo, boolean fileChecked, boolean asicsCreated);

    void updateTeraFilesProcessStatus(String nodeRef, String crypt, boolean asics);

    List<Map<String, Object>> findAllDigidocfiles();

    List<TeraFilesEntry> getTeraFilesEntrys(int limit, int offset);

    int countAllFiles();

    boolean checkFileEntryByNodeRef(String nodeRef);

    String checkFileNameSymbols(String filename);

    void fixModifier(NodeService nodeService, FileFolderService fileFolderService, NodeRef nodeRef, String CREATOR_MODIFIER);

    Map<QName, Serializable> renameFile(NodeService nodeService, FileInfo fileInfo, String fileBaseName, String fileExt, String fileType, String CREATOR_MODIFIER);

    String getAndFixFilename(FileInfo fileInfo);
}
