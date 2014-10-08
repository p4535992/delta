package ee.webmedia.alfresco.substitute.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentSearchService;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.SubstituteModel;
import ee.webmedia.alfresco.substitute.model.UnmodifiableSubstitute;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class SubstituteServiceImpl implements SubstituteService, BeanFactoryAware {
    private static final Log log = LogFactory.getLog(SubstituteServiceImpl.class);

    private NodeService nodeService;
    private BeanFactory beanFactory;
    /** User to list of people who are substituting this person */
    private SimpleCache<String, List<Substitute>> substituteCache;
    /** User to list of people who he/she is substituting */
    private SimpleCache<NodeRef, List<UnmodifiableSubstitute>> userToSubstitutesCache;

    private static BeanPropertyMapper<Substitute> substituteBeanPropertyMapper;

    static {
        substituteBeanPropertyMapper = BeanPropertyMapper.newInstance(Substitute.class);
    }

    @Override
    public List<UnmodifiableSubstitute> getUnmodifiableSubstitutes(NodeRef userNodeRef) {
        if (userToSubstitutesCache.contains(userNodeRef)) {
            return userToSubstitutesCache.get(userNodeRef);
        }
        NodeRef substitutesNodeRef = getSubstitutesNodeRef(userNodeRef, false);
        if (substitutesNodeRef == null) {
            return new ArrayList<>();
        }
        List<ChildAssociationRef> substitutesRefs = nodeService.getChildAssocs(substitutesNodeRef);
        List<UnmodifiableSubstitute> substitutes = new ArrayList<>(substitutesRefs.size());
        for (ChildAssociationRef substituteRef : substitutesRefs) {
            substitutes.add(new UnmodifiableSubstitute(getSubstitute(substituteRef.getChildRef())));
        }
        userToSubstitutesCache.put(userNodeRef, substitutes);
        return substitutes;
    }

    @Override
    public List<Substitute> getSubstitutes(NodeRef userNodeRef) {
        boolean userInCache = userToSubstitutesCache.contains(userNodeRef);
        NodeRef substitutesNodeRef = getSubstitutesNodeRef(userNodeRef, userInCache);
        if (substitutesNodeRef == null) {
            return new ArrayList<>();
        }
        List<ChildAssociationRef> substitutesRefs = nodeService.getChildAssocs(substitutesNodeRef);
        List<Substitute> substitutes = new ArrayList<>(substitutesRefs.size());
        List<UnmodifiableSubstitute> unmodifiableSubstitutes = new ArrayList<>(substitutesRefs.size());
        for (ChildAssociationRef substituteRef : substitutesRefs) {
            Substitute substitute = getSubstitute(substituteRef.getChildRef());
            substitutes.add(substitute);
            if (!userInCache) {
                unmodifiableSubstitutes.add(new UnmodifiableSubstitute(substitute));
            }
        }
        if (!userInCache) {
            userToSubstitutesCache.put(userNodeRef, unmodifiableSubstitutes);
        }
        return substitutes;
    }

    private NodeRef getSubstitutesNodeRef(NodeRef userNodeRef, boolean userInCache) {
        NodeRef substitutesNodeRef = getSubstitutesNode(userNodeRef, false);
        if (substitutesNodeRef == null && !userInCache) {
            userToSubstitutesCache.put(userNodeRef, new ArrayList<UnmodifiableSubstitute>());
        }
        return substitutesNodeRef;
    }

    @Override
    public String getSubstituteLabel(String userName) {
        NodeRef userRef = BeanHelper.getUserService().getPerson(userName);
        if (userRef != null) {
            for (UnmodifiableSubstitute substitute : getUnmodifiableSubstitutes(userRef)) {
                if (substitute.isActive()) {
                    return substitute.getLabel();
                }
            }
        }
        return "";
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

        addLogEntry(userNodeRef, substitute, "applog_user_substitute_set");

        substitute.setNodeRef(assoc.getChildRef());
        substituteCache.remove(substitute.getSubstituteId());
        userToSubstitutesCache.remove(userNodeRef);
        return substitute.getNodeRef();
    }

    @Override
    public void updateSubstitute(Substitute substitute) {
        Assert.notNull(substitute, "Substitute must be provided");
        Assert.notNull(substitute.getNodeRef(), "Substitute must have node ref");
        Assert.isTrue(nodeService.exists(substitute.getNodeRef()), "Substitute must exist");

        Map<QName, Serializable> oldProps = nodeService.getProperties(substitute.getNodeRef());
        Map<QName, Serializable> newProps = substituteBeanPropertyMapper.toProperties(substitute);

        String oldSubstituteId = (String) oldProps.get(SubstituteModel.Props.SUBSTITUTE_ID);
        String newSubstituteId = substitute.getSubstituteId();
        substituteCache.remove(oldSubstituteId);
        substituteCache.remove(newSubstituteId);

        String diff = new PropDiffHelper()
                .label(SubstituteModel.Props.SUBSTITUTE_NAME, "substitute_name")
                .label(SubstituteModel.Props.SUBSTITUTION_START_DATE, "substitute_startdate")
                .label(SubstituteModel.Props.SUBSTITUTION_END_DATE, "substitute_enddate")
                .diff(oldProps, newProps);

        nodeService.setProperties(substitute.getNodeRef(), newProps);
        if (log.isDebugEnabled()) {
            log.debug("Substitute (" + substitute.getNodeRef() + ") properties updated");
        }

        if (diff != null) {
            NodeRef userRef = nodeService.getPrimaryParent(nodeService.getPrimaryParent(substitute.getNodeRef()).getParentRef()).getParentRef();
            userToSubstitutesCache.remove(userRef);
            addLogEntry(userRef, null, diff);
        }
    }

    @Override
    public void deleteSubstitute(NodeRef substituteNodeRef) {
        Assert.notNull(substituteNodeRef, "Substitute reference not provided");

        NodeRef userRef = nodeService.getPrimaryParent(nodeService.getPrimaryParent(substituteNodeRef).getParentRef()).getParentRef();
        Substitute substitute = getSubstitute(substituteNodeRef);
        substituteCache.remove(substitute.getSubstituteId());
        userToSubstitutesCache.remove(userRef);
        addLogEntry(userRef, substitute, "applog_user_substitute_rem");

        if (log.isDebugEnabled()) {
            log.debug("Starting to delete substitute:" + substituteNodeRef);
        }
        nodeService.deleteNode(substituteNodeRef);
        if (log.isDebugEnabled()) {
            log.debug("Substitute (" + substituteNodeRef + ") deleted");
        }
    }

    @Override
    public List<Substitute> searchActiveSubstitutionDuties(String userName) {
        List<Substitute> substitutes = substituteCache.get(userName);
        if (substitutes != null) {
            return substitutes;
        }
        substitutes = new ArrayList<>();

        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(SubstituteModel.Types.SUBSTITUTE));
        queryParts.add(generateStringExactQuery(userName, SubstituteModel.Props.SUBSTITUTE_ID));
        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        queryParts.add(generateDatePropertyRangeQuery(null, today, SubstituteModel.Props.SUBSTITUTION_START_DATE));
        queryParts.add(generateDatePropertyRangeQuery(today, null, SubstituteModel.Props.SUBSTITUTION_END_DATE));
        String query = joinQueryPartsAnd(queryParts);

        List<NodeRef> nodeRefs = getDocumentSearchService().searchNodes(query, -1, "activeSubstitutionDuties");
        for (NodeRef nodeRef : nodeRefs) {
            substitutes.add(getSubstitute(nodeRef));
        }
        substituteCache.put(userName, Collections.unmodifiableList(substitutes));
        return substitutes;
    }

    @Override
    public boolean canBeSubstituting(String otherUserName) {
        String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
        for (Substitute substitute : searchActiveSubstitutionDuties(fullyAuthenticatedUser)) {
            if (StringUtils.equals(substitute.getReplacedPersonUserName(), otherUserName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Substitute> findSubstitutionDutiesInPeriod(NodeRef userNodeRef, Date startDate, Date endDate) {
        List<Substitute> substitutes = new ArrayList<Substitute>();
        if (startDate == null || endDate == null) {
            return substitutes;
        }
        String userName = (String) nodeService.getProperty(userNodeRef, ContentModel.PROP_USERNAME);

        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(SubstituteModel.Types.SUBSTITUTE),
                generateStringExactQuery(userName, SubstituteModel.Props.SUBSTITUTE_ID),
                joinQueryPartsOr(Arrays.asList(
                        joinQueryPartsOr(Arrays.asList(
                                generateDatePropertyRangeQuery(startDate, endDate, SubstituteModel.Props.SUBSTITUTION_START_DATE),
                                generateDatePropertyRangeQuery(startDate, endDate, SubstituteModel.Props.SUBSTITUTION_END_DATE)
                                )),
                        joinQueryPartsAnd(Arrays.asList(
                                generateDatePropertyRangeQuery(null, startDate, SubstituteModel.Props.SUBSTITUTION_START_DATE),
                                generateDatePropertyRangeQuery(endDate, null, SubstituteModel.Props.SUBSTITUTION_END_DATE)
                                ))

                        ))
                ));

        List<NodeRef> nodeRefs = getDocumentSearchService().searchNodes(query, -1, "substitutionDutiesInPeriod");
        for (NodeRef nodeRef : nodeRefs) {
            substitutes.add(getSubstitute(nodeRef));
        }
        return substitutes;
    }

    @Override
    public String clearCache() {
        int subsCacheSize = substituteCache.getKeys().size();
        int userToSubsCacheSize = userToSubstitutesCache.getKeys().size();
        substituteCache.clear();
        userToSubstitutesCache.clear();
        return String.format("Removed %d elements from substituteCache and %d elements from userToSubstitutesCache", subsCacheSize, userToSubsCacheSize);
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setSubstituteCache(SimpleCache<String, List<Substitute>> substituteCache) {
        this.substituteCache = substituteCache;
    }

    public void setUserToSubstitutesCache(SimpleCache<NodeRef, List<UnmodifiableSubstitute>> userToSubstitutesCache) {
        this.userToSubstitutesCache = userToSubstitutesCache;
    }

    private void addLogEntry(NodeRef userRef, Substitute substitute, String msgCode) {
        LogService logService = BeanHelper.getLogService();
        UserService userService = BeanHelper.getUserService();

        String username = (String) nodeService.getProperty(userRef, ContentModel.PROP_USERNAME);

        if (substitute != null) {
            logService.addLogEntry(LogEntry.create(LogObject.USER, userService, userRef, msgCode,
                    userService.getUserFullNameAndId(username),
                    substitute.getSubstitutionStartDate(), substitute.getSubstitutionEndDate(),
                    userService.getUserFullNameAndId(substitute.getSubstituteId())));
        } else {
            logService.addLogEntry(LogEntry.create(LogObject.USER, userService, userRef, "applog_user_substitute_edit",
                    userService.getUserFullNameAndId(username), msgCode));
        }
    }
    // END: getters / setters
}
