package ee.webmedia.alfresco.sharepoint;

import java.io.Serializable;
import java.util.ArrayList;
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

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Alar Kvell
 */
public class OwnerEmailUpdater extends AbstractNodeUpdater {

    private UserService userService;

    private Map<String, String> userEmails;

    @Override
    protected void executeUpdater() throws Exception {
        userEmails = new HashMap<String, String>();
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateStringExactQuery("test22@just.ee", DocumentCommonModel.Props.OWNER_EMAIL);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
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
        String ownerId = (String) origProps.get(DocumentCommonModel.Props.OWNER_ID);

        String oldOwnerEmail = (String) origProps.get(DocumentCommonModel.Props.OWNER_EMAIL);
        String newOwnerEmail = oldOwnerEmail;
        Boolean setValue = Boolean.FALSE;
        if ("test22@just.ee".equals(StringUtils.strip(oldOwnerEmail))) {
            if (StringUtils.isNotBlank(ownerId)) {
                if (userEmails.containsKey(ownerId)) {
                    newOwnerEmail = userEmails.get(ownerId);
                } else {
                    newOwnerEmail = userService.getUserEmail(ownerId);
                    userEmails.put(ownerId, newOwnerEmail);
                }
            } else {
                newOwnerEmail = null;
            }
            updatedProps.put(DocumentCommonModel.Props.OWNER_EMAIL, newOwnerEmail);
            updatedProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            updatedProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            setValue = Boolean.TRUE;
        }

        if (!updatedProps.isEmpty()) {
            nodeService.addProperties(nodeRef, updatedProps);
        }
        Date regDateTime = (Date) origProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
        return new String[] {
                setValue.toString(),
                ownerId,
                oldOwnerEmail,
                newOwnerEmail,
                (String) origProps.get(DocumentCommonModel.Props.DOC_NAME),
                (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER),
                regDateTime == null ? null : regDateTime.toString() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "nodeRef",
                "setOwnerEmail",
                "ownerId",
                "oldOwnerEmail",
                "newOwnerEmail",
                "docName",
                "regNumber",
                "regDateTime" };
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

}
