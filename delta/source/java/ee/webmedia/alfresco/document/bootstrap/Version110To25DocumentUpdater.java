package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;

public class Version110To25DocumentUpdater extends AbstractNodeUpdater {

    private Version21DocumentUpdater version21DocumentUpdater;
    private ErrandAspectUpdater errandAspectUpdater;
    private VacationOrderTemplateAspectUpdater vacationOrderTemplateAspectUpdater;
    private ContractDetailsUpdater contractDetailsUpdater;
    private ContractSendInfoUpdater contractSendInfoUpdater;
    private IncomingLetterDummyWorkflowUpdater incomingLetterDummyWorkflowUpdater;
    private ShortRegNumberUpdater shortRegNumberUpdater;
    private PostipoissImportDocumentsUpdater postipoissImportDocumentsUpdater;
    private DocumentPrivilegesUpdater documentPrivilegesUpdater;

    private boolean limitForTesting = false;
    private Date limitCreatedBegin;
    private Date limitCreatedEnd;
    private Date limitRegisteredBegin;
    private Date limitRegisteredEnd;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
        Set<StoreRef> stores = new HashSet<StoreRef>();
        stores.add(generalService.getStore());
        if (limitForTesting) {
            log.warn("Limit is enabled, so only the following documents are updated in document store (archival store is skipped):"
                    + "\n  * documents created between " + dateFormat.format(limitCreatedBegin) + " and " + dateFormat.format(limitCreatedEnd)
                    + "\n  * documents registered between " + dateFormat.format(limitRegisteredBegin) + " and " + dateFormat.format(limitRegisteredEnd));
            queryParts.add(SearchUtil.joinQueryPartsOr(Arrays.asList(SearchUtil.generateDatePropertyRangeQuery(limitCreatedBegin, limitCreatedEnd, ContentModel.PROP_CREATED),
                    SearchUtil.generateDatePropertyRangeQuery(limitRegisteredBegin, limitRegisteredEnd, DocumentCommonModel.Props.REG_DATE_TIME))));
        } else {
            stores.add(generalService.getArchivalsStoreRef());
        }
        String query = joinQueryPartsAnd(queryParts);
        log.info("Search query: " + query);
        List<ResultSet> result = new ArrayList<ResultSet>(stores.size());
        for (StoreRef storeRef : stores) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        QName type = nodeService.getType(nodeRef);
        Set<QName> aspects = nodeService.getAspects(nodeRef);
        List<String> info = new ArrayList<String>();

        // Updaters which consume Map<QName, Serializable> style properties
        Map<QName, Serializable> newProps = RepoUtil.copyProperties(origProps);

        Pair<Boolean, String> version21DocumentUpdaterResult = version21DocumentUpdater.updateDocument(nodeRef, aspects);
        info.add("version21DocumentUpdater," + version21DocumentUpdaterResult.getSecond());

        Pair<Boolean, String> errandAspectUpdaterResult = errandAspectUpdater.updateDocument(nodeRef, aspects);
        info.add("errandAspectUpdater," + errandAspectUpdaterResult.getSecond());

        Pair<Boolean, String> contractDetailsUpdaterResult = contractDetailsUpdater.updateDocument(nodeRef, aspects);
        info.add("contractDetailsUpdater," + contractDetailsUpdaterResult.getSecond());

        Pair<Boolean, String[]> contractSendInfoUpdaterResult = contractSendInfoUpdater.updateDocument(nodeRef, type, aspects, origProps, newProps);
        info.add("contractSendInfoUpdater," + StringUtils.join(contractSendInfoUpdaterResult.getSecond(), ','));

        Pair<Boolean, String[]> incomingLetterDummyWorkflowUpdaterResult = incomingLetterDummyWorkflowUpdater.updateDocument(nodeRef, type, aspects, origProps, newProps);
        info.add("incomingLetterDummyWorkflowUpdater," + StringUtils.join(incomingLetterDummyWorkflowUpdaterResult.getSecond(), ','));

        String[] shortRegNumberUpdaterResult = shortRegNumberUpdater.updateShortRegNr(origProps, newProps);
        info.add("shortRegNumberUpdater," + StringUtils.join(shortRegNumberUpdaterResult, ','));

        if (postipoissImportDocumentsUpdater.isEnabledCustom()) {
            Pair<Boolean, String[]> postipoissImportDocumentsUpdaterResult = postipoissImportDocumentsUpdater.updateDocument(nodeRef, type, origProps, newProps);
            info.add("postipoissImportDocumentsUpdater," + StringUtils.join(postipoissImportDocumentsUpdaterResult.getSecond(), ','));
        } else {
            info.add("postipoissImportDocumentsUpdaterDisabled");
        }

        // Updaters which consume Map<String, Object> style properties
        Map<String, Object> newProps2 = RepoUtil.toStringProperties(newProps);

        Pair<Boolean, String> documentPrivilegesUpdaterResult = documentPrivilegesUpdater.updatePrivileges(nodeRef, aspects, newProps2);
        info.add("documentPrivilegesUpdater," + documentPrivilegesUpdaterResult.getSecond());

        // Final save
        newProps2.put(ContentModel.PROP_MODIFIER.toString(), origProps.get(ContentModel.PROP_MODIFIER));
        newProps2.put(ContentModel.PROP_MODIFIED.toPrefixString(), origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.setProperties(nodeRef, RepoUtil.toQNameProperties(newProps2));

        // This updater only removes 1 aspect; it fails, with the following error:
        // Found 2 integrity violations:
        // Mandatory aspect not set:
        // Node: workspace://ArchivalsStore/8addc4a5-7b4c-4ddf-b365-bddb4a49bddc
        // Type: {http://alfresco.webmedia.ee/model/document/subtype/1.0}vacationOrder
        // Aspect: {http://alfresco.webmedia.ee/model/privilege/1.0}userGroupMapping
        // Mandatory aspect not set:
        // Node: workspace://ArchivalsStore/8addc4a5-7b4c-4ddf-b365-bddb4a49bddc
        // Type: {http://alfresco.webmedia.ee/model/document/subtype/1.0}vacationOrder
        // Aspect: {http://alfresco.webmedia.ee/model/document/specific/1.0}vacationOrderV2
        /*
         * Pair<Boolean, String> vacationOrderTemplateAspectUpdaterResult = vacationOrderTemplateAspectUpdater.updateDocument(nodeRef, type, aspects);
         * info.add("vacationOrderTemplateAspectUpdater," + vacationOrderTemplateAspectUpdaterResult.getSecond());
         */
        // It fails even when removeAspect is done in a completely separate transaction
        // It seems that IntegrityChecker is stricter when removeAspect is invoked, than on other operations (addAspect, setProperties, addProperties)
        // IntegrityChecker requires all mandatory-aspects to be presents - which is not logical

        return info.toArray(new String[info.size()]);
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    public void setVersion21DocumentUpdater(Version21DocumentUpdater version21DocumentUpdater) {
        this.version21DocumentUpdater = version21DocumentUpdater;
    }

    public void setErrandAspectUpdater(ErrandAspectUpdater errandAspectUpdater) {
        this.errandAspectUpdater = errandAspectUpdater;
    }

    public void setVacationOrderTemplateAspectUpdater(VacationOrderTemplateAspectUpdater vacationOrderTemplateAspectUpdater) {
        this.vacationOrderTemplateAspectUpdater = vacationOrderTemplateAspectUpdater;
    }

    public void setContractDetailsUpdater(ContractDetailsUpdater contractDetailsUpdater) {
        this.contractDetailsUpdater = contractDetailsUpdater;
    }

    public void setContractSendInfoUpdater(ContractSendInfoUpdater contractSendInfoUpdater) {
        this.contractSendInfoUpdater = contractSendInfoUpdater;
    }

    public void setIncomingLetterDummyWorkflowUpdater(IncomingLetterDummyWorkflowUpdater incomingLetterDummyWorkflowUpdater) {
        this.incomingLetterDummyWorkflowUpdater = incomingLetterDummyWorkflowUpdater;
    }

    public void setShortRegNumberUpdater(ShortRegNumberUpdater shortRegNumberUpdater) {
        this.shortRegNumberUpdater = shortRegNumberUpdater;
    }

    public void setPostipoissImportDocumentsUpdater(PostipoissImportDocumentsUpdater postipoissImportDocumentsUpdater) {
        this.postipoissImportDocumentsUpdater = postipoissImportDocumentsUpdater;
    }

    public void setDocumentPrivilegesUpdater(DocumentPrivilegesUpdater documentPrivilegesUpdater) {
        this.documentPrivilegesUpdater = documentPrivilegesUpdater;
    }

    public void setLimitForTesting(boolean limitForTesting) {
        this.limitForTesting = limitForTesting;
    }

    public void setLimitCreatedBegin(String limitCreatedBegin) {
        try {
            this.limitCreatedBegin = dateFormat.parse(limitCreatedBegin);
        } catch (ParseException e) {
            throw new RuntimeException("Parsing limitCreatedBegin value failed: " + e.getMessage(), e);
        }
    }

    public void setLimitCreatedEnd(String limitCreatedEnd) {
        try {
            this.limitCreatedEnd = dateFormat.parse(limitCreatedEnd);
        } catch (ParseException e) {
            throw new RuntimeException("Parsing limitCreatedEnd value failed: " + e.getMessage(), e);
        }
    }

    public void setLimitRegisteredBegin(String limitRegisteredBegin) {
        try {
            this.limitRegisteredBegin = dateFormat.parse(limitRegisteredBegin);
        } catch (ParseException e) {
            throw new RuntimeException("Parsing limitRegisteredBegin value failed: " + e.getMessage(), e);
        }
    }

    public void setLimitRegisteredEnd(String limitRegisteredEnd) {
        try {
            this.limitRegisteredEnd = dateFormat.parse(limitRegisteredEnd);
        } catch (ParseException e) {
            throw new RuntimeException("Parsing limitRegisteredEnd value failed: " + e.getMessage(), e);
        }
    }

}
