package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNotEmptyQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * Used to add sendinfos to contracts (CL task 156770)
 * 
 * @author Alar Kvell
 */
public class ContractSendInfoUpdater extends AbstractNodeUpdater {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ContractSendInfoUpdater.class);

    private BehaviourFilter behaviourFilter;
    private SearchService searchService;
    private GeneralService generalService;
    private Date now = new Date();
    private Date dayBeforeYesterday = DateUtils.addDays(new Date(), -2);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // Copied from DocumentSearchServiceImpl.generateRecipientFinichedQuery and then modified
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentSubtypeModel.Types.CONTRACT_SIM, DocumentSubtypeModel.Types.CONTRACT_SMIT,
                DocumentSubtypeModel.Types.CONTRACT_MV));
        queryParts.add(generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME,
                DocumentSpecificModel.Props.SECOND_PARTY_NAME, DocumentSpecificModel.Props.THIRD_PARTY_NAME, DocumentCommonModel.Props.SEARCHABLE_PARTY_NAME));
        queryParts.add(generateStringExactQuery(DocumentStatus.FINISHED.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        queryParts.add(generateStringNullQuery(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE));

        String dateQueryPart = SearchUtil.generateDatePropertyRangeQuery(null, dayBeforeYesterday, DocumentCommonModel.Props.REG_DATE_TIME);
        queryParts.add(dateQueryPart);
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        log.info("Searching contract documents that are not sent out that have:\n  " + dateQueryPart);

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

        Map<QName, Serializable> sendInfoProps = new HashMap<QName, Serializable>();
        sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_RESOLUTION,
                "saatmata dokumentide menüü muudatusest tulenevate probleemide parandamiseks lisatud saatmise kirje");
        sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT, "DELTA");
        sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_SEND_MODE, SendMode.MAIL.getValueName());
        sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS, SendStatus.RECEIVED);
        sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME, now);

        // Perform sendInfo adding and searchableSendMode updating manually, because SendOutService.addSendInfo cannot be used when FacesContext is null
        nodeService.createNode(nodeRef, //
                DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Assocs.SEND_INFO, DocumentCommonModel.Types.SEND_INFO, sendInfoProps).getChildRef();

        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(nodeRef, RegexQNamePattern.MATCH_ALL, DocumentCommonModel.Assocs.SEND_INFO);
        ArrayList<String> sendModes = new ArrayList<String>(assocs.size());
        for (ChildAssociationRef assoc : assocs) {
            sendModes.add((String) nodeService.getProperty(assoc.getChildRef(), DocumentCommonModel.Props.SEND_INFO_SEND_MODE));
        }
        
        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        setProps.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendModes);
        setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, setProps);

        Date regDateTime = (Date) origProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
        return new String[] {
                (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER),
                regDateTime == null ? "" : regDateTime.toString(),
                (String) origProps.get(DocumentCommonModel.Props.DOC_NAME) };
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
