package ee.webmedia.alfresco.sharepoint;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class CaseFileKeywordsStringUpdater extends AbstractNodeUpdater {

    private String lisavaljaVaartusCsvPath;

    private Map<String, String> lisavaljaVaartusValueByMenetlusId;

    @Override
    protected boolean isRetryUpdaterInBackground() {
        return false;
    }

    @Override
    protected void executeUpdater() throws Exception {
        Assert.hasText(lisavaljaVaartusCsvPath, "Path to d_lisavaljaVaartus.csv must not be blank");
        CsvReader csv = ImportUtil.createLogReader(new File(lisavaljaVaartusCsvPath));
        lisavaljaVaartusValueByMenetlusId = new HashMap<String, String>();
        try {
            if (csv.readHeaders()) {
                while (csv.readRecord()) {
                    String procedureId = csv.get(0);
                    if (StringUtils.isBlank(procedureId)) {
                        continue;
                    }
                    if (lisavaljaVaartusValueByMenetlusId.containsKey(procedureId)) {
                        lisavaljaVaartusValueByMenetlusId.put(procedureId, null);
                    } else {
                        String value = StringUtils.defaultString(csv.get(6));
                        lisavaljaVaartusValueByMenetlusId.put(procedureId, value);
                    }
                }
            }
        } finally {
            csv.close();
        }
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generateStringExactQuery(CompoundWorkflowType.CASE_FILE_WORKFLOW.name(), WorkflowCommonModel.Props.TYPE)
                );
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef cwfRef) throws Exception {
        QName compoundWorkflowNodeType = nodeService.getType(cwfRef);
        if (!WorkflowCommonModel.Types.COMPOUND_WORKFLOW.equals(compoundWorkflowNodeType)) {
            return new String[] { "compoundWorkflowNodeTypeIsNotCompoundWorkflow" };
        }
        String compoundWorkflowType = (String) nodeService.getProperty(cwfRef, WorkflowCommonModel.Props.TYPE);
        if (!CompoundWorkflowType.CASE_FILE_WORKFLOW.name().equals(compoundWorkflowType)) {
            return new String[] { "compoundWorkflowTypeIsNotCaseFileWorkflow" };
        }
        String procedureId = (String) nodeService.getProperty(cwfRef, WorkflowCommonModel.Props.PROCEDURE_ID);
        if (StringUtils.isBlank(procedureId)) {
            return new String[] {
                    "procedureIdIsNullOrMissing",
                    procedureId == null ? "[null or missing]" : procedureId };
        }
        if (!lisavaljaVaartusValueByMenetlusId.containsKey(procedureId)) {
            return new String[] {
                    "lisavaljaVaartusNotFoundForProcedureId",
                    procedureId };
        }
        String newValue = lisavaljaVaartusValueByMenetlusId.get(procedureId);
        if (newValue == null) {
            return new String[] {
                    "multipleLisavaljaVaartusFoundForProcedureId",
                    procedureId };
        }
        NodeRef caseFileRef = nodeService.getPrimaryParent(cwfRef).getParentRef();
        QName caseFileNodeType = nodeService.getType(caseFileRef);
        if (!CaseFileModel.Types.CASE_FILE.equals(caseFileNodeType)) {
            return new String[] {
                    "compoundWorkflowParentIsNotCaseFile",
                    procedureId,
                    caseFileRef.toString(),
                    caseFileNodeType.toPrefixString(serviceRegistry.getNamespaceService()) };
        }
        Map<QName, Serializable> caseFileProps = nodeService.getProperties(caseFileRef);
        QName KEYWORDS_STRING = QName.createQName(DocumentDynamicModel.URI, "keywordsString");
        nodeService.setProperty(caseFileRef, KEYWORDS_STRING, newValue);
        Serializable oldValue = caseFileProps.get(KEYWORDS_STRING);
        return new String[] {
                "updatedKeywordsString",
                procedureId,
                caseFileRef.toString(),
                caseFileNodeType.toPrefixString(serviceRegistry.getNamespaceService()),
                oldValue == null ? "[null or missing]" : oldValue.toString(),
                newValue,
                (String) caseFileProps.get(VolumeModel.Props.VOLUME_MARK),
                (String) caseFileProps.get(VolumeModel.Props.TITLE) };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "compoundWorkflowNodeRef",
                "action",
                "compoundWorkflowProcedureId",
                "caseFileNodeRef",
                "caseFileNodeType",
                "oldValue",
                "newValue",
                "volumeMark",
                "volumeTitle" };
    }

    public String getLisavaljaVaartusCsvPath() {
        return lisavaljaVaartusCsvPath;
    }

    public void setLisavaljaVaartusCsvPath(String lisavaljaVaartusCsvPath) {
        this.lisavaljaVaartusCsvPath = lisavaljaVaartusCsvPath;
    }

}
