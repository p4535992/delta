package ee.webmedia.alfresco.sharepoint;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Alar Kvell
 */
public class OwnerPropsUpdater extends AbstractNodeUpdater {

    private UserService userService;

    private String kasutAdsiPath;
    private String defaultOwnerId;

    private Map<String, String> externalUsersById;
    private Map<String, String> systemUsersByFullName;

    @Override
    protected boolean isRetryUpdaterInBackground() {
        return false;
    }

    @Override
    protected void executeUpdater() throws Exception {
        Assert.hasText(kasutAdsiPath, "Path to d_kasut_adsi.csv must not be blank");
        Assert.hasText(defaultOwnerId, "default owner ID must not be blank");
        externalUsersById = DocumentImporter.loadExternalUsers(new File(kasutAdsiPath));
        systemUsersByFullName = DocumentImporter.loadSystemUsers();
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateStringWordsWildcardQuery(Arrays.asList("JUSTMIN"), false, true, DocumentCommonModel.Props.OWNER_NAME);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> updatedProps = new HashMap<QName, Serializable>();
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);

        String oldOwnerId = (String) origProps.get(DocumentCommonModel.Props.OWNER_ID);
        String newOwnerId = oldOwnerId;
        Boolean setOwnerId = Boolean.FALSE;
        String oldOwnerName = (String) origProps.get(DocumentCommonModel.Props.OWNER_NAME);
        String newOwnerName = oldOwnerName;
        Boolean setOwnerName = Boolean.FALSE;
        if (oldOwnerName != null && oldOwnerName.startsWith("JUSTMIN\\")) {
            // && defaultOwnerId.equals(oldOwnerId)

            if (Arrays.binarySearch(DocumentImporter.SYSTEM_OWNERS, oldOwnerName) < 0) {
                int separatorIndex = oldOwnerName.indexOf('\\');
                String userName = separatorIndex >= 0 ? oldOwnerName.substring(separatorIndex + 1) : oldOwnerName;
                String userFullName = externalUsersById.get(userName);

                if (userFullName != null && !DocumentImporter.USER_INFO_DUPL.equals(userFullName)) {
                    newOwnerId = systemUsersByFullName.get(userFullName.trim().toLowerCase());
                }
            }

            if (newOwnerId == null) {
                newOwnerId = defaultOwnerId;
            }

            Map<QName, Serializable> userProps = userService.getUserProperties(newOwnerId);
            if (userProps == null) {
                Assert.isTrue(!StringUtils.equals(newOwnerId, defaultOwnerId), "User with defaultOwnerId '" + defaultOwnerId + "' does not exist");
                newOwnerId = defaultOwnerId;
                userProps = userService.getUserProperties(newOwnerId);
                Assert.isTrue(userProps != null, "User with defaultOwnerId '" + defaultOwnerId + "' does not exist");
            }

            if (!StringUtils.equals(oldOwnerId, newOwnerId)) {
                updatedProps.put(DocumentCommonModel.Props.OWNER_ID, newOwnerId);
                setOwnerId = Boolean.TRUE;
            }

            newOwnerName = UserUtil.getPersonFullName1(userProps);
            updatedProps.put(DocumentCommonModel.Props.OWNER_NAME, newOwnerName);
            updatedProps.put(DocumentCommonModel.Props.OWNER_EMAIL, userProps.get(ContentModel.PROP_EMAIL));
            updatedProps.put(DocumentCommonModel.Props.OWNER_PHONE, userProps.get(ContentModel.PROP_TELEPHONE));
            updatedProps.put(DocumentCommonModel.Props.OWNER_JOB_TITLE, userProps.get(ContentModel.PROP_JOBTITLE));
            updatedProps.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, userProps.get(ContentModel.PROP_ORGANIZATION_PATH));
            updatedProps.put(DocumentDynamicModel.Props.OWNER_SERVICE_RANK, userProps.get(ContentModel.PROP_SERVICE_RANK));
            updatedProps.put(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS, userProps.get(ContentModel.PROP_STREET_HOUSE));
            setOwnerName = Boolean.TRUE;
        }

        String updatedPropsInfo = "propsUnchanged";
        if (!updatedProps.isEmpty()) {
            if (!setOwnerName) {
                updatedProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
                updatedProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
                updatedPropsInfo = "propsUpdatedAndModifiedPreserved";
            } else {
                updatedPropsInfo = "propsUpdatedAndModifiedUpdated";
            }
            nodeService.addProperties(nodeRef, updatedProps);
        }
        Date regDateTime = (Date) origProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
        return new String[] {
                updatedPropsInfo,
                setOwnerId.toString(),
                oldOwnerId,
                newOwnerId,
                setOwnerName.toString(),
                oldOwnerName,
                newOwnerName,
                (String) origProps.get(DocumentCommonModel.Props.DOC_NAME),
                (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER),
                regDateTime == null ? null : regDateTime.toString() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "nodeRef",
                "setProps",
                "setOwnerId",
                "oldOwnerId",
                "newOwnerId",
                "setOwnerName",
                "oldOwnerName",
                "newOwnerName",
                "docName",
                "regNumber",
                "regDateTime" };
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setKasutAdsiPath(String kasutAdsiPath) {
        this.kasutAdsiPath = kasutAdsiPath;
    }

    public String getKasutAdsiPath() {
        return kasutAdsiPath;
    }

    public void setDefaultOwnerId(String defaultOwnerId) {
        this.defaultOwnerId = defaultOwnerId;
    }

    public String getDefaultOwnerId() {
        return defaultOwnerId;
    }

}
