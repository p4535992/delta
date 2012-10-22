package ee.webmedia.alfresco.testdata;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAuthorityService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getContentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDictionaryService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPolicyBehaviourFilter;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSubstituteService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.menu.model.MenuModel;
import ee.webmedia.alfresco.notification.model.NotificationModel;
import ee.webmedia.alfresco.orgstructure.service.OrganizationStructureService;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.substitute.model.Substitute;
import ee.webmedia.alfresco.substitute.model.SubstituteModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;

/**
 * @author Alar Kvell
 */
public class UserDataRestoreService {
    protected final Log log = LogFactory.getLog(getClass());

    private String myContentstore;
    private BasicDataSource otherDataSource;
    private String dbName = "";
    private String dbUsername = "";
    private String dbPassword = "";
    private String dbHost = "";
    private String dbPort = "";
    private String otherContentstore = "";
    private String validUsers = "";

    private DateTimeFormatter dtf;
    private DateTimeFormatter dtf2;
    private Map<String, NodeRef> myPersonRefs;

    public synchronized void execute(@SuppressWarnings("unused") ActionEvent event) {
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
        log.info("STARTING USER DATA RESTORE from database " + url + " and contentstore folder '" + otherContentstore + "'");
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
        dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dtf2 = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        // db.url=jdbc:postgresql://${db.host}:${db.port}/${db.name}
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(url);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        ds.setDriverClassName(otherDataSource.getDriverClassName());
        ds.setInitialSize(otherDataSource.getInitialSize());
        ds.setMaxActive(otherDataSource.getMaxActive());
        ds.setDefaultAutoCommit(otherDataSource.getDefaultAutoCommit());
        ds.setDefaultTransactionIsolation(otherDataSource.getDefaultTransactionIsolation());
        final SimpleJdbcTemplate otherJdbcTemplate = new SimpleJdbcTemplate(ds);

        Set<String> validUsersSet = new HashSet<String>();
        String[] validUsersArray = validUsers.split("\\s");
        for (String string : validUsersArray) {
            string = StringUtils.strip(string);
            if (StringUtils.isNotEmpty(string)) {
                validUsersSet.add(string);
            }
        }
        log.info("Valid users input " + WmNode.toString(validUsersSet));
        Map<Long, String> persons = queryPersons(otherJdbcTemplate, validUsersSet);
        myPersonRefs = new HashMap<String, NodeRef>(persons.size());

        processFavorites(otherJdbcTemplate, persons); // Also adds aspects to person
        processSubstitutes(otherJdbcTemplate, persons);
        processGroups(otherJdbcTemplate, persons);
        processPersonProperty(otherJdbcTemplate, persons, ContentModel.SHOW_EMPTY_TASK_MENU);
        processPersonProperty(otherJdbcTemplate, persons, MenuModel.Props.SHORTCUTS);
        processNotificationPreferences(otherJdbcTemplate, persons);
        processReports(otherJdbcTemplate, persons);
        processSearchFilters(otherJdbcTemplate, persons);

        log.info("COMPLETED USER DATA RESTORE");
    }

    private void processSubstitutes(SimpleJdbcTemplate otherJdbcTemplate, Map<Long, String> persons) {
        List<Pair<Long, Long>> substitutes = queryNodesWithGrandparentByNodeType(otherJdbcTemplate, SubstituteModel.Types.SUBSTITUTE);
        log.info("Got " + substitutes.size() + " substitutes, processing...");
        for (Pair<Long, Long> substitute : substitutes) {
            Map<QName, Serializable> props = queryNodeProps(otherJdbcTemplate, substitute.getFirst(), null, null);
            String userName = persons.get(substitute.getSecond());
            if (userName == null) {
                log.info("PersonNodeId " + substitute.getSecond() + " substitution=" + WmNode.toString(props, getNamespaceService())
                        + "\n - skipped, person does not exist or is not included in restore input list");
                continue;
            }
            NodeRef myPersonRef = getMyPersonRef(userName);
            if (myPersonRef == null) {
                log.warn("Person '" + userName + "' substitution=" + WmNode.toString(props, getNamespaceService()) + "\n - skipped, person does not exist");
                continue;
            }
            List<Substitute> mySubstitutes = getSubstituteService().getSubstitutes(myPersonRef); // TODO cache?
            boolean found = false;
            for (Substitute mySubstitute : mySubstitutes) {
                if (ObjectUtils.equals(mySubstitute.getSubstituteId(), props.get(SubstituteModel.Props.SUBSTITUTE_ID))
                        && ObjectUtils.equals(mySubstitute.getSubstitutionStartDate(), props.get(SubstituteModel.Props.SUBSTITUTION_START_DATE))
                        && ObjectUtils.equals(mySubstitute.getSubstitutionEndDate(), props.get(SubstituteModel.Props.SUBSTITUTION_END_DATE))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Substitute newSubstitute = Substitute.newInstance();
                newSubstitute.setSubstituteId((String) props.get(SubstituteModel.Props.SUBSTITUTE_ID));
                newSubstitute.setSubstituteName((String) props.get(SubstituteModel.Props.SUBSTITUTE_NAME));
                newSubstitute.setSubstitutionStartDate((Date) props.get(SubstituteModel.Props.SUBSTITUTION_START_DATE));
                newSubstitute.setSubstitutionEndDate((Date) props.get(SubstituteModel.Props.SUBSTITUTION_END_DATE));
                getSubstituteService().addSubstitute(myPersonRef, newSubstitute);
            }
            log.info("Person '" + userName + "' substitution=" + WmNode.toString(props, getNamespaceService()) + "\n - " + (!found ? "created" : "skipped, already exists"));
        }
        log.info("Completed processing substitutes");
    }

    private void processGroups(SimpleJdbcTemplate template, Map<Long, String> persons) {
        Set<String> myAllGroups = getAuthorityService().getAllAuthorities(AuthorityType.GROUP);
        Set<String> myStructUnitGroups = getAuthorityService().getAllAuthoritiesInZone(OrganizationStructureService.STRUCT_UNIT_BASED, AuthorityType.GROUP);
        myAllGroups.removeAll(myStructUnitGroups);
        log.info("There are " + myAllGroups.size() + " non-orgStructUnit based groups in our repo, considering only them: " + WmNode.toString(myAllGroups));

        List<Map<String, Object>> rows = template.queryForList("SELECT alf_child_assoc.child_node_id, alf_node_properties.string_value FROM alf_child_assoc "
                + "JOIN alf_qname type_qname ON alf_child_assoc.type_qname_id = type_qname.id "
                + "JOIN alf_namespace type_ns ON type_qname.ns_id = type_ns.id "
                + "JOIN alf_node_properties ON alf_child_assoc.parent_node_id = alf_node_properties.node_id "
                + "JOIN alf_qname prop_qname ON alf_node_properties.qname_id = prop_qname.id "
                + "JOIN alf_namespace prop_ns ON prop_qname.ns_id = prop_ns.id "
                + "WHERE type_ns.uri = 'http://www.alfresco.org/model/content/1.0' "
                + "AND type_qname.local_name = 'member' "
                + "AND prop_ns.uri = 'http://www.alfresco.org/model/content/1.0' "
                + "AND prop_qname.local_name = 'authorityName'");
        log.info("Got " + rows.size() + " authority membership rows from other database, processing...");
        for (Map<String, Object> row : rows) {
            Long personNodeId = (Long) row.get("child_node_id");
            String authorityName = (String) row.get("string_value");
            String userName = persons.get(personNodeId);
            if (userName == null) {
                log.info("PersonNodeId " + personNodeId + " member of authority '" + authorityName + "' - skipped, person does not exist or is not included in restore input list");
                continue;
            }
            AuthorityType authorityType = AuthorityType.getAuthorityType(authorityName);
            if (AuthorityType.GROUP != authorityType) {
                log.info("Person '" + userName + "' member of authority '" + authorityName + "' - skipped, authority is not a group");
                continue;
            }
            if (!myAllGroups.contains(authorityName)) {
                log.info("Person '" + userName + "' member of authority '" + authorityName + "' - skipped, authority does not exist or is a orgStructUnit based group");
                continue;
            }
            if (getAuthorityService().getAuthoritiesForUser(userName).contains(authorityName)) { // TODO cache?
                log.info("Person '" + userName + "' member of authority '" + authorityName + "' - skipped, already exists");
                continue;
            }
            getAuthorityService().addAuthority(authorityName, userName);
            log.info("Person '" + userName + "' member of authority '" + authorityName + "' - created");
        }
        log.info("Completed processing authority membership rows");
    }

    private void processFavorites(final SimpleJdbcTemplate otherJdbcTemplate, Map<Long, String> persons) {
        for (final Entry<Long, String> otherPerson : persons.entrySet()) {
            String userName = otherPerson.getValue();
            AuthenticationUtil.runAs(new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    processFavorites(otherJdbcTemplate, otherPerson);
                    return null;
                }
            }, userName);
        }
    }

    private void processFavorites(SimpleJdbcTemplate template, Entry<Long, String> otherPerson) {
        String userName = otherPerson.getValue();
        Long otherPersonNodeId = otherPerson.getKey();

        log.info("Person '" + userName + "' - adding aspects if necessary");
        Set<QName> aspects = queryNodeAspects(template, otherPersonNodeId);
        aspects.remove(QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "configurable"));
        addAspects(getMyPersonRef(userName), aspects);

        // root container (no directory)
        processFavoritesContainer(template, userName, null, otherPersonNodeId);

        List<Map<String, Object>> rows = template.queryForList("SELECT alf_child_assoc.qname_localname, alf_child_assoc.child_node_id FROM alf_child_assoc "
                + "JOIN alf_qname type_qname ON alf_child_assoc.type_qname_id = type_qname.id "
                + "JOIN alf_namespace type_ns ON type_qname.ns_id = type_ns.id "
                + "WHERE alf_child_assoc.parent_node_id = ? "
                + "AND type_ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0' "
                + "AND type_qname.local_name = 'favoriteDirectory'", otherPersonNodeId);
        for (Map<String, Object> row : rows) {
            String favoriteDirectoryName = (String) row.get("qname_localname");
            Long otherFavoriteDirectoryNodeId = (Long) row.get("child_node_id");
            processFavoritesContainer(template, userName, favoriteDirectoryName, otherFavoriteDirectoryNodeId);
        }
    }

    private void processFavoritesContainer(SimpleJdbcTemplate template, String userName, String favoriteDirName, Long otherContainerNodeId) {
        List<Map<String, Object>> rows = template.queryForList("SELECT alf_store.protocol, alf_store.identifier, alf_node.uuid FROM alf_node_assoc "
                + "JOIN alf_qname type_qname ON alf_node_assoc.type_qname_id = type_qname.id "
                + "JOIN alf_namespace type_ns ON type_qname.ns_id = type_ns.id "
                + "JOIN alf_node ON alf_node_assoc.target_node_id = alf_node.id "
                + "JOIN alf_store ON alf_node.store_id = alf_store.id "
                + "WHERE type_ns.uri = 'http://alfresco.webmedia.ee/model/document/common/1.0' "
                + "AND type_qname.local_name = 'favorite' "
                + "AND alf_node_assoc.source_node_id = ? "
                + "AND alf_node.node_deleted = false", otherContainerNodeId);
        for (Map<String, Object> row : rows) {
            NodeRef docRef = new NodeRef((String) row.get("protocol"), (String) row.get("identifier"), (String) row.get("uuid"));
            if (getDocumentService().addFavorite(docRef, favoriteDirName, false)) {
                log.info("Person '" + userName + "' favorite document " + docRef + " - created, under favorite directory "
                        + (favoriteDirName == null ? "root" : "'" + favoriteDirName + "'"));
            } else {
                log.info("Person '" + userName + "' favorite document " + docRef + " - skipped, already exists under some favorite directory");
            }
        }
    }

    private void processPersonProperty(final SimpleJdbcTemplate template, Map<Long, String> persons, QName propQName) {
        for (Entry<Long, String> entry : persons.entrySet()) {
            Long otherPersonNodeId = entry.getKey();
            String userName = entry.getValue();
            Map<QName, Serializable> otherProps = queryNodeProps(template, otherPersonNodeId, propQName, null);
            if (otherProps.isEmpty()) {
                log.info("Person '" + userName + "' " + propQName.getLocalName() + " - skipping, has not been set");
            } else {
                getNodeService().addProperties(getMyPersonRef(userName), otherProps);
                log.info("Person '" + userName + "' " + propQName.getLocalName() + " - set values " + otherProps);
            }
        }
    }

    private void processNotificationPreferences(final SimpleJdbcTemplate template, Map<Long, String> persons) {
        List<Pair<Long, Long>> preferences = queryNodesWithGrandparentByChildAssocName(template, ContentModel.TYPE_CMOBJECT,
                QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "preferences"));
        for (Pair<Long, Long> preference : preferences) {
            Long otherPersonNodeId = preference.getSecond();
            String userName = persons.get(otherPersonNodeId);
            if (userName == null) {
                log.info("PersonNodeId " + otherPersonNodeId + " notification preferences - skipped, person does not exist or is not included in restore input list");
                continue;
            }
            Map<QName, Serializable> otherProps = queryNodeProps(template, preference.getFirst(), new QNamePattern() {
                @Override
                public boolean isMatch(QName qname) {
                    return NotificationModel.URI.equals(qname.getNamespaceURI());
                }
            }, getDictionaryService().getDataType(DataTypeDefinition.BOOLEAN));
            if (otherProps.isEmpty()) {
                log.info("Person '" + userName + "' notification preferences - skipping, has not been set");
                continue;
            }
            Node userPrefsNode = new Node(getUserService().retrieveUsersPreferenceNodeRef(userName));
            getNodeService().addProperties(userPrefsNode.getNodeRef(), otherProps);
            log.info("Person '" + userName + "' notification preferences - set values " + WmNode.toString(otherProps, getNamespaceService()));
        }
    }

    private void processReports(final SimpleJdbcTemplate template, Map<Long, String> persons) {
        List<Pair<Long, Long>> reports = queryNodesWithGrandparentByNodeType(template, ReportModel.Types.REPORT_RESULT);
        log.info("Got " + reports.size() + " reports, processing...");
        for (Pair<Long, Long> report : reports) {
            Long otherPersonNodeId = report.getSecond();
            final String userName = persons.get(otherPersonNodeId);
            if (userName == null) {
                log.info("PersonNodeId " + otherPersonNodeId + " report - skipped, person does not exist or is not included in restore input list");
                continue;
            }
            final Long reportNodeId = report.getFirst();
            final Map<QName, Serializable> otherProps = queryNodeProps(template, reportNodeId, null, null);
            otherProps.putAll(queryNodeAuditableProps(template, reportNodeId));
            final Set<QName> otherAspects = queryNodeAspects(template, reportNodeId);
            BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    getPolicyBehaviourFilter().disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
                    NodeRef userReportFolderRef = getUserService().retrieveUserReportsFolderRef(userName);
                    if (userReportFolderRef == null) {
                        log.warn("Person '" + userName + "' report named '" + otherProps.get(ReportModel.Props.REPORT_NAME) + "' - skipped, report folder could not be created");
                        return null;
                    }
                    log.info("Person '" + userName + "' report named '" + otherProps.get(ReportModel.Props.REPORT_NAME) + "' - creating nodes");
                    NodeRef newReportRef = getNodeService().createNode(userReportFolderRef, ReportModel.Assocs.REPORT_RESULT, ReportModel.Assocs.REPORT_RESULT,
                            ReportModel.Types.REPORT_RESULT, otherProps).getChildRef();
                    addAspects(newReportRef, otherAspects);
                    log.info("Person '" + userName + "' report named '" + otherProps.get(ReportModel.Props.REPORT_NAME) + "' - created node with values "
                            + WmNode.toString(otherProps, getNamespaceService()));
                    copyChildNodes(template, reportNodeId, newReportRef, null);
                    return null;
                }
            });
        }
        log.info("Completed processing reports");
    }

    private void processSearchFilters(final SimpleJdbcTemplate template, Map<Long, String> persons) {
        log.info("Processing document and task search filters...");
        for (Entry<Long, String> person : persons.entrySet()) {
            final Long personNodeId = person.getKey();
            final String userName = person.getValue();
            final NodeRef myPersonRef = getMyPersonRef(person.getValue());
            AuthenticationUtil.runAs(new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
                        @Override
                        public Void execute() throws Throwable {

                            getPolicyBehaviourFilter().disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
                            log.info("Person '" + userName + "' search filters - searching and creating");
                            copyChildNodes(template, personNodeId, myPersonRef, new CopyChildNodesHelper() {
                                @Override
                                public boolean isMatch(QName assocType, QName nodeType, QName assocName) {
                                    return DocumentSearchModel.Types.FILTER.equals(nodeType) || TaskSearchModel.Types.FILTER.equals(nodeType);
                                }

                                @Override
                                public void beforeCreateCallback(QName assocType, QName nodeType, QName assocName, Map<QName, Serializable> props, Set<QName> aspects) {
                                    // Do nothing
                                }
                            });
                            return null;
                        }
                    });
                    return null;
                }
            }, userName);
        }
        log.info("Completed processing document and task search filters");
    }

    private NodeRef getMyPersonRef(String userName) {
        NodeRef myPersonRef;
        if (myPersonRefs.containsKey(userName)) {
            myPersonRef = myPersonRefs.get(userName);
        } else {
            myPersonRef = getUserService().getPerson(userName);
            myPersonRefs.put(userName, myPersonRef);
        }
        return myPersonRef;
    }

    private Map<Long, String> queryPersons(SimpleJdbcTemplate otherJdbcTemplate, Set<String> validUsersSet) {
        List<Map<String, Object>> rows = otherJdbcTemplate.queryForList("SELECT alf_node.id, alf_node_properties.string_value AS userName FROM alf_node "
                + "JOIN alf_qname type_qname ON alf_node.type_qname_id = type_qname.id "
                + "JOIN alf_namespace type_ns ON type_qname.ns_id = type_ns.id "
                + "JOIN alf_store ON alf_node.store_id = alf_store.id "
                + "JOIN alf_node_properties ON alf_node.id = alf_node_properties.node_id "
                + "JOIN alf_qname prop_qname ON alf_node_properties.qname_id = prop_qname.id "
                + "JOIN alf_namespace prop_ns ON prop_qname.ns_id = prop_ns.id "
                + "WHERE alf_node.node_deleted = false "
                + "AND type_ns.uri = 'http://www.alfresco.org/model/content/1.0' "
                + "AND type_qname.local_name = 'person' "
                + "AND alf_store.protocol = 'workspace' "
                + "AND alf_store.identifier = 'SpacesStore' "
                + "AND prop_ns.uri = 'http://www.alfresco.org/model/content/1.0' "
                + "AND prop_qname.local_name = 'userName' ");
        Map<Long /* nodeId */, String /* userName */> persons = new HashMap<Long, String>(rows.size());
        log.info("Found " + rows.size() + " persons from otherDb, starting filtering");
        for (Map<String, Object> row : rows) {
            String userName = (String) row.get("username");
            if (StringUtils.isBlank(userName) || !validUsersSet.remove(userName)) {
                log.info("Ignoring person '" + userName + "'");
                continue;
            }
            Assert.isTrue(!persons.containsValue(userName), userName);
            Long nodeId = (Long) row.get("id");
            persons.put(nodeId, userName);
            log.info("Adding person '" + userName + "', nodeId=" + nodeId);
        }
        log.info("Completed filtering persons list, it contains now " + persons.size() + " persons");
        log.info("Valid users input, that were not found in backup database " + WmNode.toString(validUsersSet));
        return persons;
    }

    private List<Pair<Long /* nodeId */, Long /* grandParentId */>> queryNodesWithGrandparentByNodeType(SimpleJdbcTemplate template, QName type) {
        String queryWhereClause = "AND type_ns.uri = ? AND type_qname.local_name = ? ";
        Object[] queryArgs = new Object[] { type.getNamespaceURI(), type.getLocalName() };
        return queryNodesWithGrandparent(template, "", queryWhereClause, queryArgs);
    }

    private List<Pair<Long /* nodeId */, Long /* grandParentId */>> queryNodesWithGrandparentByChildAssocName(SimpleJdbcTemplate template, QName type, QName childAssocName) {
        String queryJoinClause = "JOIN alf_namespace parent_assoc_ns ON parent.qname_ns_id = parent_assoc_ns.id ";
        String queryWhereClause = "AND type_ns.uri = ? AND type_qname.local_name = ? AND parent_assoc_ns.uri = ? AND parent.qname_localname = ? ";
        Object[] queryArgs = new Object[] { type.getNamespaceURI(), type.getLocalName(), childAssocName.getNamespaceURI(), childAssocName.getLocalName() };
        return queryNodesWithGrandparent(template, queryJoinClause, queryWhereClause, queryArgs);
    }

    private List<Pair<Long, Long>> queryNodesWithGrandparent(SimpleJdbcTemplate template, String queryJoinClause, String queryWhereClause, Object[] queryArgs) {
        List<Map<String, Object>> substitutes = template.queryForList("SELECT alf_node.id, grandparent.parent_node_id FROM alf_node "
                + "JOIN alf_qname type_qname ON alf_node.type_qname_id = type_qname.id "
                + "JOIN alf_namespace type_ns ON type_qname.ns_id = type_ns.id "
                + "JOIN alf_store ON alf_node.store_id = alf_store.id "
                + "JOIN alf_child_assoc parent ON alf_node.id = parent.child_node_id "
                + "JOIN alf_child_assoc grandparent ON parent.parent_node_id = grandparent.child_node_id "
                + queryJoinClause
                + "WHERE alf_node.node_deleted = false "
                + queryWhereClause
                + "AND alf_store.protocol = 'workspace' " + "AND alf_store.identifier = 'SpacesStore' ", queryArgs);
        List<Pair<Long, Long>> results = new ArrayList<Pair<Long, Long>>(substitutes.size());
        for (Map<String, Object> substitute : substitutes) {
            results.add(Pair.newInstance((Long) substitute.get("id"), (Long) substitute.get("parent_node_id")));
        }
        return results;
    }

    private Map<QName, Serializable> queryNodeAuditableProps(SimpleJdbcTemplate template, Long nodeId) {
        Map<String, Object> row = template.queryForMap("SELECT alf_node.audit_creator, alf_node.audit_created, alf_node.audit_modifier, alf_node.audit_modified FROM alf_node "
                + "WHERE alf_node.id = ?", nodeId);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(4);
        props.put(ContentModel.PROP_CREATOR, (String) row.get("audit_creator"));
        props.put(ContentModel.PROP_CREATED, parseDateTime((String) row.get("audit_created")));
        props.put(ContentModel.PROP_MODIFIER, (String) row.get("audit_modifier"));
        props.put(ContentModel.PROP_MODIFIED, parseDateTime((String) row.get("audit_modified")));
        return props;
    }

    private Map<QName, Serializable> queryNodeProps(SimpleJdbcTemplate template, Long nodeId, QNamePattern propNamePattern, DataTypeDefinition dataTypeOverride) {
        List<Map<String, Object>> rows = template.queryForList("SELECT alf_node_properties.*, prop_ns.uri, prop_qname.local_name FROM alf_node_properties "
                + "JOIN alf_qname prop_qname ON alf_node_properties.qname_id = prop_qname.id "
                + "JOIN alf_namespace prop_ns ON prop_qname.ns_id = prop_ns.id "
                + "WHERE alf_node_properties.node_id = ? ORDER BY list_index ASC", nodeId);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(rows.size());
        for (Map<String, Object> row : rows) {
            QName propQName = QName.createQName((String) row.get("uri"), (String) row.get("local_name"));
            if (propNamePattern != null && !propNamePattern.isMatch(propQName)) {
                continue;
            }
            DataTypeDefinition dataType = dataTypeOverride;
            boolean multiValued = false;
            if (dataType == null) {
                PropertyDefinition propDef;
                if (RepoUtil.TRANSIENT_PROPS_NAMESPACE.equals(propQName.getNamespaceURI())) {
                    continue;
                }
                if (propQName.getLocalName().endsWith("_AfterLabelBoolean")) {
                    dataType = getDictionaryService().getDataType(DataTypeDefinition.BOOLEAN);
                } else if (propQName.getLocalName().endsWith(DateGenerator.PICKER_PREFIX)) {
                    dataType = getDictionaryService().getDataType(DataTypeDefinition.TEXT);
                } else {

                    QName q = propQName;

                    if (DocumentDynamicModel.URI.equals(propQName.getNamespaceURI())) {
                        propDef = getDocumentConfigService().getPropertyDefinitionById(q.getLocalName());
                    } else {
                        propDef = getDictionaryService().getProperty(q);
                    }
                    Assert.notNull(propDef, q.toString());
                    multiValued = propDef.isMultiValued();
                    dataType = propDef.getDataType();
                }
            }
            Serializable propValue = null;
            Integer persistedType = (Integer) row.get("persisted_type_n");
            if (0 == persistedType) {
                propValue = null;
            } else if (DataTypeDefinition.TEXT.equals(dataType.getName())) {
                propValue = (String) row.get("string_value");
            } else if (DataTypeDefinition.DATETIME.equals(dataType.getName()) || DataTypeDefinition.DATE.equals(dataType.getName())) {
                propValue = parseDateTime((String) row.get("string_value"));
            } else if (DataTypeDefinition.BOOLEAN.equals(dataType.getName())) {
                propValue = (Boolean) row.get("boolean_value");
            } else if (DataTypeDefinition.LONG.equals(dataType.getName())) {
                propValue = (Long) row.get("long_value");
            } else if (DataTypeDefinition.DOUBLE.equals(dataType.getName())) {
                propValue = (Double) row.get("double_value");
            } else if (DataTypeDefinition.CONTENT.equals(dataType.getName())) {
                Long contentDataId = (Long) row.get("long_value");
                propValue = queryContentData(template, contentDataId);
            } else if (DataTypeDefinition.NODE_REF.equals(dataType.getName())) {
                String nodeRefString = (String) row.get("string_value");
                if (StringUtils.isNotEmpty(nodeRefString)) {
                    propValue = new NodeRef(nodeRefString);
                }
            } else if (DataTypeDefinition.QNAME.equals(dataType.getName())) {
                String nodeRefString = (String) row.get("string_value");
                if (StringUtils.isNotEmpty(nodeRefString)) {
                    propValue = QName.createQName(nodeRefString);
                }
            } else {
                throw new RuntimeException("Unknown data type '" + dataType + "' for '" + propQName.toPrefixString(getNamespaceService()));
            }
            if (multiValued) {
                ArrayList<Serializable> list = (ArrayList<Serializable>) props.get(propQName);
                if (list == null) {
                    list = new ArrayList<Serializable>();
                    props.put(propQName, list);
                }
                list.add(propValue);
            } else {
                Assert.isTrue(!props.containsKey(propQName));
                props.put(propQName, propValue);
            }
        }
        return props;
    }

    private ContentData queryContentData(SimpleJdbcTemplate template, Long contentDataId) {
        Map<String, Object> row = template.queryForMap("SELECT alf_content_url.content_url, alf_mimetype.mimetype_str, alf_encoding.encoding_str "
                + "FROM alf_content_data "
                + "JOIN alf_content_url ON alf_content_data.content_url_id = alf_content_url.id "
                + "JOIN alf_mimetype ON alf_content_data.content_mimetype_id = alf_mimetype.id "
                + "JOIN alf_encoding ON alf_content_data.content_encoding_id = alf_encoding.id "
                + "WHERE alf_content_data.id = ?", contentDataId);
        ContentWriter writer = getContentService().getWriter(null, null, false);
        writer.setEncoding((String) row.get("encoding_str"));
        writer.setMimetype((String) row.get("mimetype_str"));
        String contentUrl = (String) row.get("content_url");
        try {
            FileCopyUtils.copy(new FileInputStream(new File(myContentstore, StringUtils.replace(contentUrl, "store://", ""))), writer.getContentOutputStream());
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return writer.getContentData();
    }

    private Date parseDateTime(String dateTimeString) {
        Date dateTime = null;
        if (StringUtils.isNotEmpty(dateTimeString)) {
            if (dateTimeString.endsWith("Z")) {
                dateTimeString = StringUtils.removeEnd(dateTimeString, "Z");
                DateTime dateTimeJoda = dtf2.parseDateTime(dateTimeString);
                dateTime = new Date(dateTimeJoda.getMillis());
            } else {
                DateTime dateTimeJoda = dtf.parseDateTime(dateTimeString);
                dateTime = new Date(dateTimeJoda.getMillis());
            }
        }
        return dateTime;
    }

    private Set<QName> queryNodeAspects(SimpleJdbcTemplate template, Long nodeId) {
        List<Map<String, Object>> rows = template.queryForList("SELECT alf_namespace.uri, alf_qname.local_name FROM alf_node_aspects "
                + "JOIN alf_qname ON alf_node_aspects.qname_id = alf_qname.id "
                + "JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id "
                + "WHERE alf_node_aspects.node_id = ?", nodeId);
        Set<QName> aspects = new HashSet<QName>(rows.size());
        for (Map<String, Object> row : rows) {
            QName aspect = QName.createQName((String) row.get("uri"), (String) row.get("local_name"));
            aspects.add(aspect);
        }
        return aspects;
    }

    private interface CopyChildNodesHelper {
        boolean isMatch(QName assocType, QName nodeType, QName assocName);

        void beforeCreateCallback(QName assocType, QName nodeType, QName assocName, Map<QName, Serializable> props, Set<QName> aspects);
    }

    private void copyChildNodes(SimpleJdbcTemplate template, Long otherParentNodeId, NodeRef myParentRef, CopyChildNodesHelper helper) {
        List<Map<String, Object>> rows = template.queryForList("SELECT alf_child_assoc.child_node_id, "
                + "assoctype_ns.uri AS assoctype_ns, assoctype_qname.local_name AS assoctype_ln, "
                + "nodetype_ns.uri AS nodetype_ns, nodetype_qname.local_name AS nodetype_ln, "
                + "assocname_ns.uri AS assocname_ns, alf_child_assoc.qname_localname AS assocname_ln "
                + "FROM alf_child_assoc "
                + "JOIN alf_qname assoctype_qname ON alf_child_assoc.type_qname_id = assoctype_qname.id "
                + "JOIN alf_namespace assoctype_ns ON assoctype_qname.ns_id = assoctype_ns.id "
                + "JOIN alf_node ON alf_child_assoc.child_node_id = alf_node.id "
                + "JOIN alf_qname nodetype_qname ON alf_node.type_qname_id = nodetype_qname.id "
                + "JOIN alf_namespace nodetype_ns ON nodetype_qname.ns_id = nodetype_ns.id "
                + "JOIN alf_namespace assocname_ns ON alf_child_assoc.qname_ns_id = assocname_ns.id "
                + "WHERE alf_child_assoc.parent_node_id = ? "
                + "AND alf_node.node_deleted = false", otherParentNodeId);
        for (Map<String, Object> row : rows) {
            Long childNodeId = (Long) row.get("child_node_id");
            QName assocType = QName.createQName((String) row.get("assoctype_ns"), (String) row.get("assoctype_ln"));
            QName nodeType = QName.createQName((String) row.get("nodetype_ns"), (String) row.get("nodetype_ln"));
            QName assocName = QName.createQName((String) row.get("assocname_ns"), (String) row.get("assocname_ln"));
            if (helper != null && !helper.isMatch(assocType, nodeType, assocName)) {
                continue;
            }
            Map<QName, Serializable> otherProps = queryNodeProps(template, childNodeId, null, null);
            otherProps.putAll(queryNodeAuditableProps(template, childNodeId));
            Set<QName> otherAspects = queryNodeAspects(template, childNodeId);
            if (helper != null) {
                helper.beforeCreateCallback(assocType, nodeType, assocName, otherProps, otherAspects);
            }
            NodeRef newChildRef = getNodeService().createNode(myParentRef, assocType, assocName, nodeType, otherProps).getChildRef();
            log.info("Created node type " + nodeType.toPrefixString(getNamespaceService()) + ", assoc type " + assocType.toPrefixString(getNamespaceService()) + ", assoc name "
                    + assocName.toPrefixString(getNamespaceService()) + ", values " + WmNode.toString(otherProps, getNamespaceService()));
            addAspects(newChildRef, otherAspects);
        }
    }

    private void addAspects(NodeRef newNodeRef, final Set<QName> aspects) {
        for (QName aspect : aspects) {
            if (!getNodeService().hasAspect(newNodeRef, aspect)) {
                getNodeService().addAspect(newNodeRef, aspect, null);
                log.info("Aspect " + aspect.toPrefixString(getNamespaceService()) + " added");
            } else {
                log.info("Aspect " + aspect.toPrefixString(getNamespaceService()) + " skipped, already exists");
            }
        }
    }

    public void setMyContentstore(String myContentstore) {
        this.myContentstore = myContentstore;
    }

    public void setOtherDataSource(BasicDataSource otherDataSource) {
        this.otherDataSource = otherDataSource;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getOtherContentstore() {
        return otherContentstore;
    }

    public void setOtherContentstore(String otherContentstore) {
        this.otherContentstore = otherContentstore;
    }

    public String getValidUsers() {
        return validUsers;
    }

    public void setValidUsers(String validUsers) {
        this.validUsers = validUsers;
    }

}
