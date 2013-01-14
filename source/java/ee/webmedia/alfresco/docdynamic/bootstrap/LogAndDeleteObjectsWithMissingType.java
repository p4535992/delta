package ee.webmedia.alfresco.docdynamic.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Log and optionally delete documents and case files (since 3.11 branch) that have no corresponding dynamic type definition.
 * Deleting should be performed only in test environments (currently only 3.13 branch).
 * 
 * @author Riina Tens
 */
public class LogAndDeleteObjectsWithMissingType extends AbstractNodeUpdater {

    private boolean delete;
    private Set<String> documentTypes;
    private Set<String> caseFileTypes;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(joinQueryPartsOr(generateTypeQuery(DocumentCommonModel.Types.DOCUMENT), generateTypeQuery(CaseFileModel.Types.CASE_FILE)), 
                generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        String[] result = null;
        boolean isDocument = DocumentCommonModel.Types.DOCUMENT.equals(type);
        if (!isDocument && !CaseFileModel.Types.CASE_FILE.equals(type)) {
            result = new String[] { "not document or case file type: " + type + ", skipping" };
        } else {
            String typeId = (String) nodeService.getProperty(nodeRef, DocumentAdminModel.Props.OBJECT_TYPE_ID);
            if ((isDocument && !getExistingDocumentTypes().contains(typeId)) 
                    || (!isDocument && !getExistingCaseFileTypes().contains(typeId))) {
                result = new String[] { (new WmNode(nodeRef, type)).toString() };
                if (delete) {
                    nodeService.deleteNode(nodeRef);
                }
            }
        }
        return result;
    }

    private Set<String> getExistingDocumentTypes() {
        if (documentTypes == null) {
            documentTypes = BeanHelper.getDocumentAdminService().getDocumentTypeNames(null).keySet();
        }
        return documentTypes;
    }
    
    private Set<String> getExistingCaseFileTypes() {
        if (caseFileTypes == null) {
            caseFileTypes = BeanHelper.getDocumentAdminService().getCaseFileTypeNames(null).keySet();
        }
        return caseFileTypes;
    }    

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

}
