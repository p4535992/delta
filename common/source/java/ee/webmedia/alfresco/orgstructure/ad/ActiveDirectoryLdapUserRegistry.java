package ee.webmedia.alfresco.orgstructure.ad;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.utils.CalendarUtil.duration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.Name;
import javax.naming.directory.SearchControls;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.sync.NodeDescription;
import org.alfresco.repo.security.sync.UserRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextProcessor;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapEncoder;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.simple.AbstractParameterizedContextMapper;
import org.springframework.ldap.core.simple.ParameterizedContextMapper;
import org.springframework.ldap.core.simple.SimpleLdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.support.SingleContextSource;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;
import ee.webmedia.alfresco.orgstructure.dao.OrgStructDao;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructureModel;
import ee.webmedia.alfresco.orgstructure.model.PersonOrgDto;
import ee.webmedia.alfresco.utils.UserUtil;

public class ActiveDirectoryLdapUserRegistry implements UserRegistry, InitializingBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ActiveDirectoryLdapUserRegistry.class);

    private ContextSource contextSource;
    private int pageSize = 1000;
    private String personSearchBase;
    private String personSecondarySearchBase;
    private String personQuery;
    private String personIdCodeQuery;
    private String personUsernameQuery;
    private String personGroupQuery;
    private String systematicGroupSearchBase;
    private String groupSearchBase;
    private String groupQuery;
    private String organizationalUnitSearchQuery;
    private String organizationalUnitSearchBase;
    private String organizationalUnitSecondarySearchQuery;
    private Map<String, String> personAttributes;
    private Map<String, String> groupAttributes;
    private Map<String, String> orgStructAttributes;
    private Map<String, String> systematicGroupQueryFilters;

    private String[] personAttributeNames;
    private String[] groupAttributeNames;
    private String[] orgStructAttributeNames;
    private final QName groupDnProp = QName.createQName("groupDn", getNamespaceService());
    
    private Boolean fromDatabase;
    private OrgStructDao orgStructDao;

    @Override
    public void afterPropertiesSet() throws Exception {
        personAttributeNames = personAttributes.values().toArray(new String[orgStructAttributes.size()]);
        groupAttributeNames = groupAttributes.values().toArray(new String[groupAttributes.size()]);
        orgStructAttributeNames = orgStructAttributes.values().toArray(new String[orgStructAttributes.size()]);
    }

    @Override
    public Iterator<NodeDescription> getPersonByIdCode(String idCode) {
        SingleContextSource ctx = createSingleContextSource();
        try {

            String filter = StringUtils.replace(personIdCodeQuery, "{0}", LdapEncoder.filterEncode(idCode));
            return searchPerson(ctx, "personByIdCode", filter);

        } finally {
            ctx.destroy();
        }
    }

    @Override
    public Iterator<NodeDescription> getPersonByUsername(String username) {
        SingleContextSource ctx = createSingleContextSource();
        try {

            String filter = StringUtils.replace(personUsernameQuery, "{0}",
                    LdapEncoder.filterEncode(username));
            return searchPerson(ctx, "personByUsername", filter);

        } finally {
            ctx.destroy();
        }
    }

    private Iterator<NodeDescription> searchPerson(SingleContextSource ctx, String queryName, String filter) {
        LinkedHashMap<String, NodeDescription> personsByIdCode = new LinkedHashMap<String, NodeDescription>();
        searchAndAddPersons(ctx, queryName, personsByIdCode, personSearchBase, filter);
        if (personsByIdCode.isEmpty() && StringUtils.isNotBlank(personSecondarySearchBase)) {
            searchAndAddPersons(ctx, queryName, personsByIdCode, personSecondarySearchBase, filter);
        }
        Collection<NodeDescription> results = personsByIdCode.size() > 1 ? Collections.singleton(personsByIdCode.values().iterator().next()) : personsByIdCode.values();

        for (NodeDescription person : results) {
            String organizationId = null;
            List<String> organizationPath = null;
            if (fromDatabase) {
            	String username = (String)person.getProperties().get(ContentModel.PROP_USERNAME);
            	PersonOrgDto personOrgDto = orgStructDao.getPersonOrg(username);
            	if (personOrgDto != null) {
            		OrganizationStructure orgStruct = getOrganizationStructureService().getOrganizationStructure(personOrgDto.getUnitId());
	                if (orgStruct != null) {
	                    organizationId = orgStruct.getUnitId();
	                    organizationPath = orgStruct.getOrganizationPath();
	                }
            	}
            } else {
	            DistinguishedName dn = (DistinguishedName) person.getProperties().get(ContentModel.PROP_ORGID);
	            while (!dn.isEmpty()) {
	                String superUnitId = dn.toString();
	                OrganizationStructure orgStruct = getOrganizationStructureService().getOrganizationStructure(superUnitId);
	                if (orgStruct != null) {
	                    organizationId = orgStruct.getUnitId();
	                    organizationPath = orgStruct.getOrganizationPath();
	                    break;
	                }
	                dn.removeLast();
	            }
            }
            person.getProperties().put(ContentModel.PROP_ORGID, organizationId);
            person.getProperties().put(ContentModel.PROP_ORGANIZATION_PATH, (Serializable) organizationPath);
        }

        LOG.info("Found " + results.size() + " users:\n" + WmNode.toString(results, true));
        return results.iterator();
    }

    @Override
    public Iterator<NodeDescription> getPersons(Date modifiedSince) {
        SingleContextSource ctx = createSingleContextSource();
        try {

            Map<String, NodeDescription> personsByIdCode = new HashMap<String, NodeDescription>();
            searchAndAddPersons(ctx, "persons", personsByIdCode, personSearchBase,
                    personQuery);
            if (StringUtils.isNotBlank(personSecondarySearchBase)) {
                searchAndAddPersons(ctx, "persons", personsByIdCode, personSecondarySearchBase, personQuery);
            }

            List<OrganizationStructure> orgStructs = BeanHelper.getOrganizationStructureService().getAllOrganizationStructures();
            Map<String, List<String>> orgStructsPathsById = new HashMap<String, List<String>>();
            for (OrganizationStructure orgStruct : orgStructs) {
                orgStructsPathsById.put(orgStruct.getUnitId(), orgStruct.getOrganizationPath());
            }
            for (NodeDescription person : personsByIdCode.values()) {
                String organizationId = null;
                List<String> organizationPath = null;
                if (fromDatabase) {
                	String username = (String)person.getProperties().get(ContentModel.PROP_USERNAME);
                	PersonOrgDto personOrgDto = orgStructDao.getPersonOrg(username);
                	if (personOrgDto != null) {
                		OrganizationStructure orgStruct = getOrganizationStructureService().getOrganizationStructure(personOrgDto.getUnitId());
    	                if (orgStruct != null) {
    	                    organizationId = orgStruct.getUnitId();
    	                    organizationPath = orgStruct.getOrganizationPath();
    	                }
                	}
                } else {
	                DistinguishedName dn = (DistinguishedName) person.getProperties().get(ContentModel.PROP_ORGID);
	                while (dn != null && !dn.isEmpty()) {
	                    String superUnitId = dn.toString();
	                    if (orgStructsPathsById.containsKey(superUnitId)) {
	                        organizationId = superUnitId;
	                        organizationPath = orgStructsPathsById.get(superUnitId);
	                        break;
	                    }
	                    dn.removeLast();
	                }
                }
                person.getProperties().put(ContentModel.PROP_ORGID, organizationId);
               	person.getProperties().put(ContentModel.PROP_ORGANIZATION_PATH, (Serializable) organizationPath);
            }

            LOG.info("Found " + personsByIdCode.size() + " users:\n" + WmNode.toString(personsByIdCode.values(), true));
            return personsByIdCode.values().iterator();

        } finally {
            ctx.destroy();
        }
    }
    
    private void searchAndAddPersons(ContextSource ctx, String queryName, Map<String, NodeDescription> personsByIdCode, String searchBase, String personSearchFilter) {
        List<NodeDescription> persons = searchPaged(
                ctx,
                queryName,
                searchBase,
                personSearchFilter,
                personAttributeNames,
                personMapper);

        // Keep persons which have unique non-blank idCode; first person wins among non-unique
        for (NodeDescription person : persons) {
            String idCode = (String) person.getProperties().get(ContentModel.PROP_USERNAME);
            if (StringUtils.isNotBlank(idCode) && !personsByIdCode.containsKey(idCode)) {
                personsByIdCode.put(idCode, person);
            }
        }
    }

    private void searchAndAddPersonIdCodes(ContextSource ctx, Set<String> personIdCodes, String searchBase, String personSearchFilter) {
        List<String> persons = searchPaged(
                ctx,
                "groupMembers",
                searchBase,
                personSearchFilter,
                new String[] { personAttributes.get("cm:userName") },
                personUserNameMapper);

        // Keep persons which have unique non-blank idCode; first person wins among non-unique
        for (String idCode : persons) {
            if (StringUtils.isNotBlank(idCode) && !personIdCodes.contains(idCode)) {
                personIdCodes.add(idCode);
            }
        }
    }

    private Set<String> searchPersonIdCodes(ContextSource ctx, String groupDn) {
        String filter = StringUtils.replace(personGroupQuery, "{0}", LdapEncoder.filterEncode(groupDn));
        Set<String> personIdCodes = new HashSet<String>();
        searchAndAddPersonIdCodes(ctx, personIdCodes, personSearchBase, filter);
        if (StringUtils.isNotBlank(personSecondarySearchBase)) {
            searchAndAddPersonIdCodes(ctx, personIdCodes, personSecondarySearchBase, filter);
        }
        return personIdCodes;
    }

    @Override
    public Iterator<NodeDescription> getGroups(Date modifiedSince) {
        SingleContextSource ctx = createSingleContextSource();
        try {
            Map<String, NodeDescription> groupsByName = new HashMap<String, NodeDescription>();
            Set<String> systematicGroupsOriginalNames = new HashSet<String>();

            for (Entry<String, String> entry : systematicGroupQueryFilters.entrySet()) {
                if (StringUtils.isBlank(entry.getValue())) {
                    continue;
                }
                List<NodeDescription> results = searchPaged(
                        ctx,
                        "systematicGroup",
                        systematicGroupSearchBase,
                        entry.getValue(),
                        groupAttributeNames,
                        groupMapper);
                if (results.isEmpty()) {
                    continue;
                }
                NodeDescription primaryGroup = null;
                for (NodeDescription group : results) {
                    String name = (String) group.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    
                    if (primaryGroup == null) {
                    	systematicGroupsOriginalNames.add(name);
                    	String groupDn = (String) group.getProperties().remove(groupDnProp);
                    	
                    	primaryGroup = new NodeDescription();
                        primaryGroup.setLastModified(group.getLastModified());
                        primaryGroup.getProperties().putAll(group.getProperties());
                        String primaryName = PermissionService.GROUP_PREFIX + entry.getKey();
                        primaryGroup.getProperties().put(ContentModel.PROP_AUTHORITY_NAME, primaryName);
                        
                        Set<String> personIdCodes = searchPersonIdCodes(ctx, groupDn);
                        group.getChildAssociations().addAll(personIdCodes);
                        primaryGroup.getChildAssociations().addAll(personIdCodes);
                        //groupsByName.put(name, group);
                        groupsByName.put(primaryName, primaryGroup);
                    } else {
                        // add members
                        String groupDn = (String) group.getProperties().remove(groupDnProp);
                        Set<String> personIdCodes = searchPersonIdCodes(ctx, groupDn);
                        group.getChildAssociations().addAll(personIdCodes);
                        // also add systematic ldap subgroups to synch
                        groupsByName.put(name, group);
                        primaryGroup.getChildAssociations().addAll(personIdCodes);
                    }
                }
            }

            List<NodeDescription> results = searchPaged(
                    ctx,
                    "groups",
                    groupSearchBase,
                    groupQuery,
                    groupAttributeNames,
                    groupMapper);
            for (NodeDescription group : results) {
                String name = (String) group.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
                if (StringUtils.isBlank(name) || groupsByName.containsKey(name) || systematicGroupsOriginalNames.contains(name)) {
                    continue;
                }
                String groupDn = (String) group.getProperties().remove(groupDnProp);
                group.getChildAssociations().addAll(searchPersonIdCodes(ctx, groupDn));
                groupsByName.put(name, group);
            }

            Collection<NodeDescription> uniqueGroups = groupsByName.values();
            LOG.info("Total " + uniqueGroups.size() + " unique groups:\n" + WmNode.toString(uniqueGroups, true));
            return uniqueGroups.iterator();

        } finally {
            ctx.destroy();
        }
    }

    @Override
    public Iterator<NodeDescription> getOrganizationStructures() {
        List<NodeDescription> orgStructs = new ArrayList<NodeDescription>();
        SingleContextSource ctx = createSingleContextSource();
        try {

            List<Name> rootDistinguishedNames = searchPaged(
                    ctx,
                    "organizationalUnitRootDns",
                    organizationalUnitSearchBase,
                    organizationalUnitSearchQuery,
                    new String[] {},
                    distinguishedNameMapper);
            LOG.info("Got " + rootDistinguishedNames.size() + " root DN-s: " + rootDistinguishedNames);

            for (Name name : rootDistinguishedNames) {
                List<NodeDescription> results = searchPaged(
                        ctx,
                        "organizationalUnitsUnderRootDn",
                        name,
                        organizationalUnitSecondarySearchQuery,
                        orgStructAttributeNames,
                        organizationStructureMapper);
                LOG.info("Got " + results.size() + " orgStructs for root DN: " + name);
                orgStructs.addAll(results);
            }
            LOG.info("Total " + orgStructs.size() + " orgStructs");

        } finally {
            ctx.destroy();
        }

        // Keep orgStructs which have unique non-blank ID; first orgStruct wins among non-unique
        Map<String, NodeDescription> orgStructsById = new HashMap<String, NodeDescription>();
        for (NodeDescription orgStruct : orgStructs) {
            String unitId = (String) orgStruct.getProperties().get(OrganizationStructureModel.Props.UNIT_ID);
            if (StringUtils.isNotBlank(unitId) && !orgStructsById.containsKey(unitId)) {
                orgStructsById.put(unitId, orgStruct);
            }
        }

        // Set super unit ID-s
        OUTER: for (NodeDescription orgStruct : orgStructsById.values()) {
            DistinguishedName dn = (DistinguishedName) orgStruct.getProperties().get(OrganizationStructureModel.Props.SUPER_UNIT_ID);
            while (!dn.isEmpty()) {
                String superUnitId = dn.toString();
                if (orgStructsById.containsKey(superUnitId)) {
                    orgStruct.getProperties().put(OrganizationStructureModel.Props.SUPER_UNIT_ID, superUnitId);
                    continue OUTER;
                }
                dn.removeLast();
            }
            orgStruct.getProperties().remove(OrganizationStructureModel.Props.SUPER_UNIT_ID);
        }

        // Set organizationPath values
        setOrganizationPathRecursively(orgStructsById.values(), null, Collections.<String> emptyList());

        Collection<NodeDescription> uniqueOrgStructs = orgStructsById.values();
        LOG.info("Total " + uniqueOrgStructs.size() + " unique orgStructs:\n" + WmNode.toString(uniqueOrgStructs, true));
        return uniqueOrgStructs.iterator();
    }

    private void setOrganizationPathRecursively(Collection<NodeDescription> allOrgStructs, final String superUnitId, List<String> superOrganizationPath) {
        @SuppressWarnings("unchecked")
        Collection<NodeDescription> select = CollectionUtils.select(allOrgStructs, new Predicate() {
            @Override
            public boolean evaluate(Object orgStruct) {
                String currentSuperUnitId = (String) ((NodeDescription) orgStruct).getProperties().get(OrganizationStructureModel.Props.SUPER_UNIT_ID);
                return StringUtils.equals(superUnitId, currentSuperUnitId);
            }
        });
        for (NodeDescription orgStruct : select) {
            Assert.isNull(orgStruct.getProperties().get(OrganizationStructureModel.Props.ORGANIZATION_PATH));

            ArrayList<String> path = new ArrayList<String>(superOrganizationPath);
            String pathEntryPrefix = UserUtil.getDisplayUnit(path);
            String name = (String) orgStruct.getProperties().get(OrganizationStructureModel.Props.NAME);
            String pathEntry = StringUtils.isBlank(pathEntryPrefix) ? name : pathEntryPrefix + ", " + name;
            Assert.isTrue(!path.contains(pathEntry));
            path.add(pathEntry);
            orgStruct.getProperties().put(OrganizationStructureModel.Props.ORGANIZATION_PATH, path);

            String unitId = (String) orgStruct.getProperties().get(OrganizationStructureModel.Props.UNIT_ID);
            setOrganizationPathRecursively(allOrgStructs, unitId, path);
        }
    }

    private final ParameterizedContextMapper<NodeDescription> personMapper = new AbstractParameterizedContextMapper<NodeDescription>() {

        @Override
        protected NodeDescription doMapFromContext(DirContextOperations ctx) {
            NodeDescription person = new NodeDescription();
            setProperties(person, ctx, personAttributes);
            DistinguishedName dn = (DistinguishedName) ctx.getDn();
            dn.removeLast();
            person.getProperties().put(ContentModel.PROP_ORGID, dn);
            return person;
        }

    };

    private final ParameterizedContextMapper<String> personUserNameMapper = new AbstractParameterizedContextMapper<String>() {

        @Override
        protected String doMapFromContext(DirContextOperations ctx) {
            String attributeName = personAttributes.get("cm:userName");
            return StringUtils.isBlank(attributeName) ? null : ctx.getStringAttribute(attributeName);
        }

    };

    private final ParameterizedContextMapper<NodeDescription> groupMapper = new AbstractParameterizedContextMapper<NodeDescription>() {

        @Override
        protected NodeDescription doMapFromContext(DirContextOperations ctx) {
            NodeDescription group = new NodeDescription();
            setProperties(group, ctx, groupAttributes);
            String name = (String) group.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            if (StringUtils.isNotBlank(name)) {
                name = PermissionService.GROUP_PREFIX + name;
            } else {
                name = null;
            }
            group.getProperties().put(ContentModel.PROP_AUTHORITY_NAME, name);
            group.getProperties().put(groupDnProp, ctx.getDn().toString());
            return group;
        }

    };

    private final ParameterizedContextMapper<NodeDescription> organizationStructureMapper = new AbstractParameterizedContextMapper<NodeDescription>() {

        @Override
        protected NodeDescription doMapFromContext(DirContextOperations ctx) {
            NodeDescription orgStruct = new NodeDescription();
            setProperties(orgStruct, ctx, orgStructAttributes);
            DistinguishedName dn = (DistinguishedName) ctx.getDn();
            orgStruct.getProperties().put(OrganizationStructureModel.Props.UNIT_ID, dn.toString());
            dn.removeLast();
            orgStruct.getProperties().put(OrganizationStructureModel.Props.SUPER_UNIT_ID, dn);
            return orgStruct;
        }

    };

    private final ParameterizedContextMapper<Name> distinguishedNameMapper = new AbstractParameterizedContextMapper<Name>() {

        @Override
        protected Name doMapFromContext(DirContextOperations ctx) {
            return ctx.getDn();
        }

    };

    private void setProperties(NodeDescription node, DirContextOperations ctx, Map<String, String> attributes) {
        for (Entry<String, String> entry : attributes.entrySet()) {
            String attributeName = entry.getValue();
            if (StringUtils.isNotBlank(attributeName)) {
                node.getProperties().put(QName.createQName(entry.getKey(), getNamespaceService()), ctx.getStringAttribute(attributeName));
            }
        }
    }

    private SimpleLdapTemplate createSimpleTemplate(ContextSource ldapContextSource) {
        LdapTemplate template = new LdapTemplate(ldapContextSource);
        template.setIgnorePartialResultException(true);
        SimpleLdapTemplate simpleTemplate = new SimpleLdapTemplate(template);
        return simpleTemplate;
    }

    private <T> List<T> searchPaged(ContextSource ldapContextSource, String queryName, final String base, final String filter, final String[] attributeNames,
            final ParameterizedContextMapper<T> mapper) {
        return searchPaged(ldapContextSource, queryName, new SearchCallback<T>() {
            @Override
            public List<T> search(SimpleLdapTemplate simpleTemplate, SearchControls controls, DirContextProcessor processor) {
                return simpleTemplate.search(base, filter, controls, mapper, processor);
            }
        }, attributeNames);
    }

    private <T> List<T> searchPaged(ContextSource ldapContextSource, String queryName, final Name base, final String filter, final String[] attributeNames,
            final ParameterizedContextMapper<T> mapper) {
        return searchPaged(ldapContextSource, queryName, new SearchCallback<T>() {
            @Override
            public List<T> search(SimpleLdapTemplate simpleTemplate, SearchControls controls, DirContextProcessor processor) {
                return simpleTemplate.search(base, filter, controls, mapper, processor);
            }
        }, attributeNames);
    }

    public interface SearchCallback<T> {
        List<T> search(SimpleLdapTemplate simpleTemplate, SearchControls controls, DirContextProcessor processor);
    }

    private <T> List<T> searchPaged(ContextSource ldapContextSource, String queryName, SearchCallback<T> searchCallback, String[] attributeNames) {
        SearchControls controls = createSearchControls(attributeNames);
        PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(pageSize);
        List<T> list = new ArrayList<T>();

        SimpleLdapTemplate simpleTemplate = createSimpleTemplate(ldapContextSource);
        do {
            try {
                long startTime = System.nanoTime();
                List<T> localList = searchCallback.search(simpleTemplate, controls, processor);
                long stopTime = System.nanoTime();
                list.addAll(localList);
                processor = new PagedResultsDirContextProcessor(pageSize, processor.getCookie());
                MonitoringUtil.logSuccess(MonitoredService.OUT_AD_LDAP);
                LOG.info("PERFORMANCE: query adLdap." + queryName + " - " + duration(startTime, stopTime) + " ms|" + localList.size());
            } catch (NamingException e) {
                MonitoringUtil.logError(MonitoredService.OUT_AD_LDAP, e);
                throw new AuthenticationException("Failed to execute LDAP query: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                MonitoringUtil.logError(MonitoredService.OUT_AD_LDAP, e);
                throw e;
            }
        } while (processor.getCookie().getCookie() != null);
        return list;
    }

    private SearchControls createSearchControls(String[] attributeNames) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningObjFlag(true);
        controls.setReturningAttributes(attributeNames);
        return controls;
    }

    private SingleContextSource createSingleContextSource() {
        try {
            return new SingleContextSource(contextSource.getReadOnlyContext());
        } catch (NamingException e) {
            throw new AuthenticationException("Failed to connect to LDAP server: " + e.getMessage(), e);
        }
    }

    public void setLdapContextSource(LdapContextSource ldapContextSource) {
        contextSource = ldapContextSource;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setPersonSearchBase(String personSearchBase) {
        this.personSearchBase = personSearchBase;
    }

    public void setPersonSecondarySearchBase(String personSecondarySearchBase) {
        this.personSecondarySearchBase = personSecondarySearchBase;
    }

    public void setPersonQuery(String personQuery) {
        this.personQuery = personQuery;
    }

    public void setPersonIdCodeQuery(String personIdCodeQuery) {
        this.personIdCodeQuery = personIdCodeQuery;
    }

    public void setPersonUsernameQuery(String personUsernameQuery) {
        this.personUsernameQuery = personUsernameQuery;
    }

    public void setPersonGroupQuery(String personGroupQuery) {
        this.personGroupQuery = personGroupQuery;
    }

    public void setSystematicGroupSearchBase(String systematicGroupSearchBase) {
        this.systematicGroupSearchBase = systematicGroupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public void setGroupQuery(String groupQuery) {
        this.groupQuery = groupQuery;
    }

    public void setOrganizationalUnitSearchQuery(String organizationalUnitSearchQuery) {
        this.organizationalUnitSearchQuery = organizationalUnitSearchQuery;
    }

    public void setOrganizationalUnitSearchBase(String organizationalUnitSearchBase) {
        this.organizationalUnitSearchBase = organizationalUnitSearchBase;
    }

    public void setOrganizationalUnitSecondarySearchQuery(String organizationalUnitSecondarySearchQuery) {
        this.organizationalUnitSecondarySearchQuery = organizationalUnitSecondarySearchQuery;
    }

    public void setPersonAttributes(Map<String, String> personAttributes) {
        this.personAttributes = personAttributes;
    }

    public void setGroupAttributes(Map<String, String> groupAttributes) {
        this.groupAttributes = groupAttributes;
    }

    public void setOrgStructAttributes(Map<String, String> orgStructAttributes) {
        this.orgStructAttributes = orgStructAttributes;
    }

    public void setSystematicGroupQueryFilters(Map<String, String> systematicGroupQueryFilters) {
        this.systematicGroupQueryFilters = systematicGroupQueryFilters;
    }
    
    public void setFromDatabase(Boolean fromDatabase) {
    	this.fromDatabase = fromDatabase;
    }
    
    public void setOrgStructDao(OrgStructDao orgStructDao) {
        this.orgStructDao = orgStructDao;
    }

}
