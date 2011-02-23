package ee.webmedia.alfresco.document.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Checks document's searchable properties and if needed adds missing properties
 * Used to add searchable properties to documents imported from Postipoiss (CL task 143388)
 * 
 * @author Riina Tens
 */
public class SearchablePropertiesUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SearchablePropertiesUpdater.class);

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    private NodeService nodeService;
    private GeneralService generalService;
    private DocumentService documentService;
    private DictionaryService dictionaryService;
    private SendOutService sendOutService;
    private boolean enabled = false;
    
    @Override
    protected void executeInternal() throws Throwable{
        if (!enabled) {
            log.debug("Skipping searchable properties update, execution parameter (searchablePropertiesUpdater.enabled) set to false.");
            return;
        }
        super.executeInternal();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {

        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        String[] resultLog = new String[16];
        resultLog[0] = nodeRef.toString();
        
        if(!hasAllLocationProperties(origProps)){

            Map<QName, NodeRef> parentRefs = getDocumentParents(nodeRef);
            if(!origProps.containsKey(DocumentCommonModel.Props.FUNCTION)){
                setProps.put(DocumentCommonModel.Props.FUNCTION, parentRefs.get(DocumentCommonModel.Props.FUNCTION));
                resultLog[1] = emptyIfNullStr(parentRefs.get(DocumentCommonModel.Props.FUNCTION));
            }
            if(!origProps.containsKey(DocumentCommonModel.Props.SERIES)){
                setProps.put(DocumentCommonModel.Props.SERIES, parentRefs.get(DocumentCommonModel.Props.SERIES));
                resultLog[2] = emptyIfNullStr(parentRefs.get(DocumentCommonModel.Props.SERIES));
            }
            if(!origProps.containsKey(DocumentCommonModel.Props.VOLUME)){
                setProps.put(DocumentCommonModel.Props.VOLUME, parentRefs.get(DocumentCommonModel.Props.VOLUME));
                resultLog[3] = emptyIfNullStr(parentRefs.get(DocumentCommonModel.Props.VOLUME));
            }
            if(!origProps.containsKey(DocumentCommonModel.Props.CASE)){
                setProps.put(DocumentCommonModel.Props.CASE, parentRefs.get(DocumentCommonModel.Props.CASE));
                resultLog[4] = emptyIfNullStr(parentRefs.get(DocumentCommonModel.Props.CASE));
            }
        }
      //this property was actually created and updated during import, so this should never be called
        if(!origProps.containsKey(DocumentCommonModel.Props.FILE_NAMES)){
            Serializable fileNames = (Serializable)documentService.getSearchableFileNames(nodeRef);
            setProps.put(DocumentCommonModel.Props.FILE_NAMES, fileNames);
            resultLog[5] = fileNames.toString();
        }
      //this property was actually created and updated during import, so this should never be called
        if(!origProps.containsKey(DocumentCommonModel.Props.FILE_CONTENTS)){
            Serializable fileContents = (Serializable) documentService.getSearchableFileContents(nodeRef);
            setProps.put(DocumentCommonModel.Props.FILE_CONTENTS, fileContents);
            resultLog[6] = emptyIfNullStr(fileContents);            
        }
        //this property was actually created and updated during import, so this should never be called
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE)){
            Serializable sendMode = sendOutService.buildSearchableSendMode(nodeRef);
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendMode);
            resultLog[7] = emptyIfNullStr(sendMode);
        }
        
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
        
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER)){
            Serializable value = collectProperties(nodeRef, childAssocs, DocumentSpecificModel.Props.COST_MANAGER);
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER, value);
            resultLog[8] = emptyIfNullStr(value);
        }        
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME)){
            Serializable value = collectProperties(nodeRef, childAssocs, DocumentSpecificModel.Props.APPLICANT_NAME,
                    DocumentSpecificModel.Props.PROCUREMENT_APPLICANT_NAME);  
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME, value);
            resultLog[9] = emptyIfNullStr(value);
        }  
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE)){
            Serializable value = collectProperties(nodeRef, childAssocs, DocumentSpecificModel.Props.ERRAND_BEGIN_DATE);
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE, value);
            resultLog[10] = emptyIfNullStr(value);
        }
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE)){
            Serializable value = collectProperties(nodeRef, childAssocs, DocumentSpecificModel.Props.ERRAND_END_DATE);
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE, value);
            resultLog[11] = emptyIfNullStr(value);
        } 
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY)){
            Serializable value = collectProperties(nodeRef, childAssocs, DocumentSpecificModel.Props.ERRAND_COUNTRY);
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY, value);
            resultLog[12] = emptyIfNullStr(value);
        }         
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY)){
            Serializable value = collectProperties(nodeRef, childAssocs, DocumentSpecificModel.Props.ERRAND_COUNTY);
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY, value);
            resultLog[13] = emptyIfNullStr(value);
        }         
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY)){
            Serializable value = collectProperties(nodeRef, childAssocs, DocumentSpecificModel.Props.ERRAND_CITY);
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY, value);
            resultLog[14] = emptyIfNullStr(value);
        }         
        if(!origProps.containsKey(DocumentCommonModel.Props.SEARCHABLE_SUB_NODE_PROPERTIES)){
            String childProps = documentService.getChildNodesPropsForIndexing(nodeRef, new StringBuilder()).toString();
            setProps.put(DocumentCommonModel.Props.SEARCHABLE_SUB_NODE_PROPERTIES, childProps); 
            resultLog[15] = childProps;
        }  
        
        if(setProps.size() > 0){
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));            
            nodeService.addProperties(nodeRef, setProps);
        }

        return resultLog;
    }

    private String emptyIfNullStr(Object object) {
        if(object == null){
            return "";
        }
        return object.toString();
    }

    private Map<QName, NodeRef> getDocumentParents(NodeRef documentRef) {
        Map<QName, NodeRef> parents = new HashMap<QName, NodeRef>();
        NodeRef caseRef = generalService.getParentNodeRefWithType(documentRef, CaseModel.Types.CASE);
        parents.put(DocumentCommonModel.Props.CASE, caseRef);
        NodeRef docOrCaseRef;
        if (caseRef != null) {
            docOrCaseRef = caseRef;
        }
        else{
            docOrCaseRef = documentRef;
        }
        NodeRef volumeRef = generalService.getParentNodeRefWithType(docOrCaseRef, VolumeModel.Types.VOLUME);
        parents.put(DocumentCommonModel.Props.VOLUME, volumeRef);
        NodeRef seriesRef = null;
        if(volumeRef != null){
            seriesRef = generalService.getParentNodeRefWithType(volumeRef, SeriesModel.Types.SERIES);
        }
        parents.put(DocumentCommonModel.Props.SERIES, seriesRef);
        NodeRef functionRef = null;
        if(seriesRef != null){
            functionRef = generalService.getParentNodeRefWithType(seriesRef, FunctionsModel.Types.FUNCTION);
        }
        parents.put(DocumentCommonModel.Props.FUNCTION, functionRef);
        return parents;
    }

    public boolean hasAllLocationProperties(Map<QName, Serializable> origProps) {
        return origProps.containsKey(DocumentCommonModel.Props.FUNCTION)
                && origProps.containsKey(DocumentCommonModel.Props.SERIES)
                && origProps.containsKey(DocumentCommonModel.Props.VOLUME)
                && origProps.containsKey(DocumentCommonModel.Props.CASE);
    }
    
    public ArrayList<Serializable> collectProperties(NodeRef nodeRef, List<ChildAssociationRef> childAssocs, QName... propNames) {
        ArrayList<Serializable> values = new ArrayList<Serializable>();
        for (QName propName : propNames) {
            PropertyDefinition propDef = dictionaryService.getProperty(propName);
            QName aspect = ((AspectDefinition) propDef.getContainerClass()).getName();
            collectProperties(values, nodeRef, propName, aspect, childAssocs);
        }
        return values;
    }

    private void collectProperties(List<Serializable> values, NodeRef nodeRef, QName propName, QName aspect, List<ChildAssociationRef> childAssocs) {
        if (nodeService.getAspects(nodeRef).contains(aspect)) {
            Serializable value = nodeService.getProperty(nodeRef, propName);
            if (value instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Serializable> list = (List<Serializable>) value;
                values.addAll(list);
            } else {
                values.add(value);
            }
        }
        List<ChildAssociationRef> childAssocRefs = childAssocs;
        if(childAssocRefs == null){
            childAssocRefs = nodeService.getChildAssocs(nodeRef);
        }
        for (ChildAssociationRef childAssocRef : nodeService.getChildAssocs(nodeRef)) {
            collectProperties(values, childAssocRef.getChildRef(), propName, aspect, null);
        }
    }    

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
