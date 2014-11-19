package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class DocumentPartyPropsUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String searchable = SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE);
        String contract = SearchUtil.generateStringExactQuery(SystematicDocumentType.CONTRACT.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID);
        String documents = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);

        String query = SearchUtil.joinQueryPartsAnd(documents, searchable, contract);

        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        boolean hasPartyNameProp = nodeService.getProperty(docRef, DocumentSpecificModel.Props.PARTY_NAME) != null;

        if (!hasPartyNameProp) {
            List<String> names = new ArrayList<String>();
            List<String> contractPersons = new ArrayList<String>();
            List<String> emails = new ArrayList<String>();
            List<String> signers = new ArrayList<String>();
            List<ChildAssociationRef> childAssocsRefs = nodeService.getChildAssocs(docRef);
            for (ChildAssociationRef ref : childAssocsRefs) {
                NodeRef childRef = ref.getChildRef();
                Map<QName, Serializable> childProps = nodeService.getProperties(childRef);
                addToList(names, childProps.get(DocumentSpecificModel.Props.PARTY_NAME));
                addToList(contractPersons, childProps.get(DocumentSpecificModel.Props.PARTY_EMAIL));
                addToList(emails, childProps.get(DocumentSpecificModel.Props.PARTY_EMAIL));
                addToList(signers, childProps.get(DocumentSpecificModel.Props.PARTY_SIGNER));
            }

            nodeService.setProperty(docRef, DocumentSpecificModel.Props.PARTY_NAME, (Serializable) names);
            nodeService.setProperty(docRef, DocumentSpecificModel.Props.PARTY_CONTACT_PERSON, (Serializable) contractPersons);
            nodeService.setProperty(docRef, DocumentSpecificModel.Props.PARTY_EMAIL, (Serializable) emails);
            nodeService.setProperty(docRef, DocumentSpecificModel.Props.PARTY_SIGNER, (Serializable) signers);

            return new String[] { "docPartyDataUpdated " + docRef.toString() };
        }

        return new String[] { "docPartyDataNotUpdated " + docRef.toString() };
    }

    private void addToList(List<String> list, Serializable prop) {
        if (prop == null) {
            list.add("");
        } else {
            list.add((String) prop);
        }
    }

}