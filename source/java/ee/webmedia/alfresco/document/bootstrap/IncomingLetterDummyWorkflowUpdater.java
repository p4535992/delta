package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNotNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Used to add dummy workflow to old incoming letters to prevent them from appearing in "Menetluses" menu (CL task 158704)
 * 
 * @author Alar Kvell
 */
public class IncomingLetterDummyWorkflowUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ContractSendInfoUpdater.class);

    private final Date now = new Date();
    private final Date dayBeforeYesterday = DateUtils.addDays(new Date(), -2);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        queryParts.add(generateTypeQuery(DocumentTypeHelper.INCOMING_LETTER_TYPES));
        queryParts.add(joinQueryPartsOr(Arrays.asList(
                generatePropertyBooleanQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, false),
                generatePropertyNullQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS)
                )));
        queryParts.add(generatePropertyNotNullQuery(DocumentCommonModel.Props.REG_NUMBER));
        String dateQueryPart = generateDatePropertyRangeQuery(null, dayBeforeYesterday, DocumentCommonModel.Props.REG_DATE_TIME);
        queryParts.add(dateQueryPart);
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        log.info("Searching incoming letter documents that are registered and don't have any started compound workflows and have:\n  " + dateQueryPart);

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
    protected String[] updateNode(NodeRef documentRef) throws Exception {
        QName type = nodeService.getType(documentRef);
        Set<QName> aspects = nodeService.getAspects(documentRef);
        Map<QName, Serializable> origProps = nodeService.getProperties(documentRef);
        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();

        Pair<Boolean, String[]> result = updateDocument(documentRef, type, aspects, origProps, setProps);

        if (result.getFirst()) {
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            nodeService.addProperties(documentRef, setProps);
        }
        return result.getSecond();
    }

    public Pair<Boolean, String[]> updateDocument(NodeRef documentRef, QName type, Set<QName> aspects, Map<QName, Serializable> origProps, Map<QName, Serializable> setProps) {
        if (!aspects.contains(DocumentCommonModel.Aspects.SEARCHABLE)) {
            return new Pair<Boolean, String[]>(false, new String[] { "doesNotHaveSearchableAspect" });
        }
        if (!DocumentTypeHelper.isIncomingLetter(type)) {
            return new Pair<Boolean, String[]>(false, new String[] { "isNotIncomingLetterType", type.toPrefixString(serviceRegistry.getNamespaceService()) });
        }
        if (Boolean.TRUE.equals(origProps.get(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS))) {
            return new Pair<Boolean, String[]>(false, new String[] { "searchableHasStartedCompoundWorkflowsIsTrue" });
        }
        String regNumber = (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER);
        if (regNumber == null) {
            return new Pair<Boolean, String[]>(false, new String[] { "regNumberIsNull" });
        }
        Date regDateTime = (Date) origProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
        if (regDateTime == null || (!dayBeforeYesterday.after(regDateTime) && !DateUtils.isSameDay(regDateTime, dayBeforeYesterday))) {
            return new Pair<Boolean, String[]>(false, new String[] { "regDateTimeIsLaterThanDayBeforeYesterday",
                    regDateTime == null ? "" : regDateTime.toString() });
        }

        Map<QName, Serializable> cwfProps = new HashMap<QName, Serializable>();
        cwfProps.put(WorkflowCommonModel.Props.STATUS, Status.FINISHED.getName());
        cwfProps.put(WorkflowCommonModel.Props.OWNER_ID, origProps.get(DocumentCommonModel.Props.OWNER_ID));
        cwfProps.put(WorkflowCommonModel.Props.OWNER_NAME, origProps.get(DocumentCommonModel.Props.OWNER_NAME));
        cwfProps.put(WorkflowCommonModel.Props.CREATOR_NAME, origProps.get(DocumentCommonModel.Props.OWNER_NAME));
        cwfProps.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, now);
        cwfProps.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);

        NodeRef cwfRef = nodeService.createNode(documentRef, //
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, cwfProps).getChildRef();

        setProps.put(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, Boolean.TRUE);

        return new Pair<Boolean, String[]>(true, new String[] {
                "createdWorkflowNode",
                cwfRef.toString(),
                regNumber,
                regDateTime.toString() });
    }
}
