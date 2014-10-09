package ee.webmedia.alfresco.report.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;

/**
 * Adds the properties below to ReportResults for optimization purposes: </br>
 * ReportModel.Props.REPORT_TEMPLATE </br>
 * ReportModel.Props.REPORT_RESULT_FILE_NAME </br>
 * ReportModel.Props.REPORT_RESULT_FILE_REF
 */
public class AddAdditionalPropertiesToReportResultsBootstrap extends AbstractNodeUpdater {

    private NodeService nodeService;
    private FileFolderService fileFolderService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(ReportModel.Types.REPORT_RESULT),
                generatePropertyExactQuery(
                        ReportModel.Props.STATUS,
                        Arrays.asList(ReportStatus.EXCEL_FULL.name(), ReportStatus.EXCEL_FULL_DOWNLOADED.name(),
                                ReportStatus.FINISHED.name(), ReportStatus.FINISHED_DOWNLOADED.name()))
                ));
        List<ResultSet> result = new ArrayList<>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> props = new HashMap<>();
        List<FileInfo> fileInfoRefs = fileFolderService.listFiles(nodeRef);
        for (FileInfo fileInfo : fileInfoRefs) {
            if (nodeService.hasAspect(fileInfo.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_REPORT)) {
                props.put(ReportModel.Props.REPORT_TEMPLATE, fileInfo.getName());
            } else {
                props.put(ReportModel.Props.REPORT_RESULT_FILE_NAME, fileInfo.getName());
                props.put(ReportModel.Props.REPORT_RESULT_FILE_REF, fileInfo.getNodeRef());
            }
        }
        nodeService.addProperties(nodeRef, props);
        return new String[] { "updateReportResultsNode" };
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

}