package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNotNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

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
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;

public class IncomingLetterADRVisibilityUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentTypeHelper.INCOMING_LETTER_TYPES),
                generatePropertyNotNullQuery(DocumentCommonModel.Props.REG_NUMBER),
                joinQueryPartsOr(Arrays.asList(
                        generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS),
                        generateStringExactQuery(DocumentStatus.STOPPED.getValueName(), DocumentCommonModel.Props.DOC_STATUS)
                ))
        ));

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

        Pair<Boolean, String> result = updateDocument(nodeRef, origProps);

        if (result.getFirst()) {
            Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
            setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            nodeService.addProperties(nodeRef, setProps);
        }

        return new String[] { result.getSecond() };
    }

    public Pair<Boolean, String> updateDocument(NodeRef nodeRef, Map<QName, Serializable> origProps) {
        boolean modified = false;
        List<String> actions = new ArrayList<String>();
        QName type = nodeService.getType(nodeRef);

        boolean isIncomingLetter = DocumentTypeHelper.isIncomingLetter(type);
        boolean isRegistered = StringUtils.isNotBlank((String) origProps.get(DocumentCommonModel.Props.REG_NUMBER));
        boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(origProps.get(DocumentCommonModel.Props.DOC_STATUS));

        if (isIncomingLetter && isRegistered && !isFinished) {
            nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIED, new Date());
            actions.add("modifiedDateUpdated");
            modified = true;
        } else {
            actions.add("modifiedDateUnchanged");
        }

        return new Pair<Boolean, String>(modified, StringUtils.join(actions, ','));
    }

}
