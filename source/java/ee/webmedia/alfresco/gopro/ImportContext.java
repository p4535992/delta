package ee.webmedia.alfresco.gopro;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.privilege.model.Privilege;

public class ImportContext {

    private final NodeRef mainDocsList;

    private final NodeRef archDocsList;

    private final List<String> documentTypes;

    private DocumentTypeVersion generalCaseFileTypeVersion;

    private final List<NodeRef> closeFunctions = new ArrayList<NodeRef>();

    private final ImportSettings data;

    private final Map<StoreRef, Map<String, NodeRef>> functions = new HashMap<StoreRef, Map<String, NodeRef>>();

    private final Map<NodeRef, Map<String, NodeRef>> series = new HashMap<NodeRef, Map<String, NodeRef>>();

    private final String taskOwnerStructUnitAuthority;
    
    private final Set<Privilege> taskOwnerStructUnitAuthorityPrivileges;

    public ImportContext(NodeRef mainDocsList, NodeRef archDocsList, List<String> documentTypes, ImportSettings data, String taskOwnerStructUnitAuthority, Set<Privilege> taskOwnerStructUnitAuthorityPrivileges) {
        this.mainDocsList = mainDocsList;
        this.archDocsList = archDocsList;
        this.documentTypes = documentTypes;
        this.data = data;
        this.taskOwnerStructUnitAuthority = taskOwnerStructUnitAuthority;
        this.taskOwnerStructUnitAuthorityPrivileges = taskOwnerStructUnitAuthorityPrivileges;
    }

    public NodeRef getMainDocsList() {
        return mainDocsList;
    }

    public NodeRef getArchDocsList() {
        return archDocsList;
    }

    public List<String> getDocumentTypes() {
        return documentTypes;
    }

    public DocumentTypeVersion getGeneralCaseFileTypeVersion() {
        return generalCaseFileTypeVersion;
    }

    public void setGeneralCaseFileTypeVersion(DocumentTypeVersion generalCaseFileTypeVersion) {
        this.generalCaseFileTypeVersion = generalCaseFileTypeVersion;
    }

    public ImportSettings getData() {
        return data;
    }

    public String getTaskOwnerStructUnitAuthority() {
        return taskOwnerStructUnitAuthority;
    }
    
    public Set<Privilege> getTaskOwnerStructUnitAuthorityPrivileges() {
        return taskOwnerStructUnitAuthorityPrivileges;
    }
    

    public NodeRef getDocumentListRef(Date volumeEndDate) {
        return data.isVolumeOpen(volumeEndDate) ? mainDocsList : archDocsList;
    }

    public NodeRef getCachedFunction(NodeRef docsListRef, String mark) {
        Map<String, NodeRef> functionRefs = functions.get(docsListRef.getStoreRef());
        return functionRefs == null ? null : functionRefs.get(mark);
    }

    public void cacheFunction(String mark, NodeRef function) {
        Map<String, NodeRef> functionRefs = functions.get(function.getStoreRef());
        if (functionRefs == null) {
            functionRefs = new HashMap<String, NodeRef>();
            functions.put(function.getStoreRef(), functionRefs);
        }
        functionRefs.put(mark, function);
    }

    public void closeFunction(NodeRef functionRef) {
        if (functionRef != null) {
            closeFunctions.add(functionRef);
        }
    }

    public List<NodeRef> getCloseFunctions() {
        return closeFunctions;
    }

    public NodeRef getCachedSeries(NodeRef functionRef, String seriesIdentifier) {
        Map<String, NodeRef> seriesRefs = series.get(functionRef);
        return seriesRefs == null ? null : seriesRefs.get(seriesIdentifier);
    }

    public void cacheSeries(String seriesIdentifier, NodeRef seriesRef, NodeRef functionRef) {
        Map<String, NodeRef> seriesRefs = series.get(functionRef);
        if (seriesRefs == null) {
            seriesRefs = new HashMap<String, NodeRef>();
            series.put(functionRef, seriesRefs);
        }
        seriesRefs.put(seriesIdentifier, seriesRef);
    }
}
