package ee.webmedia.alfresco.substitute.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.SubstituteModel;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

/**
 * @author Romet Aidla
 */
public class SubstituteServiceImpl implements SubstituteService {
    private static final Log log = LogFactory.getLog(SubstituteServiceImpl.class);

    private NodeService nodeService;
    private GeneralService generalService;
    private SearchService searchService;

    private static BeanPropertyMapper<Substitute> substituteBeanPropertyMapper;

    static {
        substituteBeanPropertyMapper = BeanPropertyMapper.newInstance(Substitute.class);
    }

    @Override
    public List<Substitute> getSubstitutes(NodeRef userNodeRef) {
        NodeRef substitutesNodeRef = getSubstitutesNode(userNodeRef, false);
        if (substitutesNodeRef == null) {
            return new ArrayList<Substitute>();
        }
        List<ChildAssociationRef> substitutesRefs = nodeService.getChildAssocs(substitutesNodeRef);
        List<Substitute> substitutes = new ArrayList<Substitute>(substitutesRefs.size());
        for (ChildAssociationRef substituteRef : substitutesRefs) {
            substitutes.add(getSubstitute(substituteRef.getChildRef()));
        }

        return substitutes;
    }

    @Override
    public Substitute getSubstitute(NodeRef substituteRef) {
        Substitute substitute = substituteBeanPropertyMapper.toObject(nodeService.getProperties(substituteRef));
        substitute.setNodeRef(substituteRef);
        substitute.setReplacedPersonUserName(getReplacedPersonUserName(substituteRef));
        return substitute;
    }

    private String getReplacedPersonUserName(NodeRef substituteRef) {
        List<ChildAssociationRef> substitutionsAssocs = nodeService.getParentAssocs(substituteRef);
        if (substitutionsAssocs.size() != 1) {
            throw new RuntimeException("Substitute is expected to have only one parent association, but got " + substitutionsAssocs.size() + " matching the criteria.");
        }

        NodeRef substitutionsNodeRef = substitutionsAssocs.get(0).getParentRef();
        List<ChildAssociationRef> personAssocs = nodeService.getParentAssocs(substitutionsNodeRef);
        if (personAssocs.size() != 1) {
            throw new RuntimeException("Substitutions is expected to have only one parent association, but got " + personAssocs.size() + " matching the criteria.");
        }

        NodeRef personNodeRef = personAssocs.get(0).getParentRef();
        return (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
    }

    @Override
    public NodeRef addSubstitute(NodeRef userNodeRef, Substitute substitute) {
        Map<QName, Serializable> properties = substituteBeanPropertyMapper.toProperties(substitute);
        ChildAssociationRef assoc = nodeService.createNode(getSubstitutesNode(userNodeRef, true),
                SubstituteModel.Associations.SUBSTITUTE,
                SubstituteModel.Types.SUBSTITUTE,
                SubstituteModel.Types.SUBSTITUTE,
                properties);
        if (log.isDebugEnabled()) {
            log.debug("Substitute node added: " + assoc.getChildRef());
        }
        substitute.setNodeRef(assoc.getChildRef());
        return substitute.getNodeRef();
    }

    @Override
    public void updateSubstitute(Substitute substitute) {
        Assert.notNull(substitute, "Substitute must be provided");
        Assert.notNull(substitute.getNodeRef(), "Substitute must have node ref");
        Assert.isTrue(nodeService.exists(substitute.getNodeRef()), "Substitute must exist");

        nodeService.setProperties(substitute.getNodeRef(), substituteBeanPropertyMapper.toProperties(substitute));
        if (log.isDebugEnabled()) {
            log.debug("Substitute (" + substitute.getNodeRef() + ") properties updated");
        }
    }

    @Override
    public void deleteSubstitute(NodeRef substituteNodeRef) {
        Assert.notNull(substituteNodeRef, "Substitute reference not provided");
        if (log.isDebugEnabled()) {
            log.debug("Starting to delete substitute:" + substituteNodeRef);
        }
        nodeService.deleteNode(substituteNodeRef);
        if (log.isDebugEnabled()) {
            log.debug("Substitute (" + substituteNodeRef + ") deleted");
        }
    }

    @Override
    public List<Substitute> findActiveSubstitutionDuties(String userName) {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(SubstituteModel.Types.SUBSTITUTE));
        queryParts.add(generateStringExactQuery(userName, SubstituteModel.Props.SUBSTITUTE_ID));
        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        queryParts.add(generateDatePropertyRangeQuery(null, today, SubstituteModel.Props.SUBSTITUTION_START_DATE));
        queryParts.add(generateDatePropertyRangeQuery(today, null, SubstituteModel.Props.SUBSTITUTION_END_DATE));
        String query = joinQueryPartsAnd(queryParts);
        List<Substitute> substitutes = new ArrayList<Substitute>();
        ResultSet resultSet = doSearch(query);
        try {
            for (ResultSetRow row : resultSet) {
                substitutes.add(getSubstitute(row.getNodeRef()));
            }
        } finally {
            resultSet.close();
        }
        return substitutes;
    }

    private ResultSet doSearch(String query) {
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(query);
        sp.addStore(generalService.getStore());
        sp.setLimitBy(LimitBy.UNLIMITED);
        return searchService.query(sp);
    }

    private NodeRef getSubstitutesNode(NodeRef userNodeRef, boolean createIfNotExist) {
        List<ChildAssociationRef> subs = nodeService.getChildAssocs(userNodeRef, SubstituteModel.Associations.SUBSTITUTES, SubstituteModel.Types.SUBSTITUTES);

        NodeRef substitutesNodeRef;
        if (subs.size() == 0) {
            if (!createIfNotExist) {
                return null;
            }
            substitutesNodeRef = createSubstitutesNode(userNodeRef);
        } else if (subs.size() == 1) {
            substitutesNodeRef = subs.get(0).getChildRef();
        } else {
            throw new RuntimeException(String.format("There shouldn't be more than one substitutes root (currently: %d) for user:%s", subs.size(), userNodeRef));
        }

        return substitutesNodeRef;
    }

    private NodeRef createSubstitutesNode(NodeRef userNodeRef) {
        nodeService.addAspect(userNodeRef, SubstituteModel.Aspects.SUBSTITUTES, null);
        return nodeService.createNode(userNodeRef, SubstituteModel.Associations.SUBSTITUTES,
                SubstituteModel.Types.SUBSTITUTES, SubstituteModel.Types.SUBSTITUTES).getChildRef();
    }

    // START: getters / setters

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    // END: getters / setters
}
