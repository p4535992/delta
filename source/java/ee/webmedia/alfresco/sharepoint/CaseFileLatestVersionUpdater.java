package ee.webmedia.alfresco.sharepoint;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Alar Kvell
 */
public class CaseFileLatestVersionUpdater extends AbstractNodeUpdater {

    private Integer newVersionNr;

    @Override
    protected boolean isRetryUpdaterInBackground() {
        return false;
    }

    @Override
    protected void executeUpdater() throws Exception {
        CaseFileType caseFileType = BeanHelper.getDocumentAdminService().getCaseFileType("generalCaseFile",
                DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
        newVersionNr = caseFileType.getLatestDocumentTypeVersion().getVersionNr();
        log.info("generalCaseFile latest versionNr is " + newVersionNr);
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(CaseFileModel.Types.CASE_FILE),
                SearchUtil.generateStringExactQuery("generalCaseFile", DocumentAdminModel.Props.OBJECT_TYPE_ID)
                );
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef caseFileRef) throws Exception {
        QName caseFileNodeType = nodeService.getType(caseFileRef);
        if (!CaseFileModel.Types.CASE_FILE.equals(caseFileNodeType)) {
            return new String[] { "caseFileNodeTypeIsNotCaseFile" };
        }
        String caseFileObjectTypeId = (String) nodeService.getProperty(caseFileRef, DocumentAdminModel.Props.OBJECT_TYPE_ID);
        if (!"generalCaseFile".equals(caseFileObjectTypeId)) {
            return new String[] { "caseFileObjectTypeIdIsNotGeneralCaseFile" };
        }
        Integer oldVersionNr = (Integer) nodeService.getProperty(caseFileRef, DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR);
        if (ObjectUtils.equals(oldVersionNr, newVersionNr)) {
            return new String[] { "caseFileVersionNrIsAlreadyLatest" };
        }
        nodeService.setProperty(caseFileRef, DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR, newVersionNr);
        return new String[] {
                "updatedCaseFileVersionNr",
                oldVersionNr.toString(),
                newVersionNr.toString() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "caseFileNodeRef",
                "action",
                "oldVersionNr",
                "newVersionNr" };
    }

}
