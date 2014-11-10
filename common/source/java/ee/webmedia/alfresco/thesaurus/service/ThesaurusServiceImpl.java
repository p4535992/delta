package ee.webmedia.alfresco.thesaurus.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.XStream;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.thesaurus.model.HierarchicalKeyword;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;
import ee.webmedia.alfresco.thesaurus.model.ThesaurusModel;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Implementation for ThesaurusService.
 */
public class ThesaurusServiceImpl implements ThesaurusService {

    private NodeService nodeService;
    private DocumentSearchService documentSearchService;

    @Override
    public boolean isThesaurusUsed(String thesaurusName) {
        // TODO DLSeadist maybe need to cache the result - field using thesaurus will always remain using that thesaurus even if it is changed
        // (new field is created under new DocumentTypeVersion)
        boolean used = documentSearchService.isMatch(
                joinQueryPartsAnd(
                        joinQueryPartsOr(
                                generateTypeQuery(DocumentAdminModel.Types.FIELD),
                                generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION),
                                generateTypeQuery(DocumentAdminModel.Types.FIELD_GROUP)
                        ),
                        generateStringExactQuery(thesaurusName, DocumentAdminModel.Props.THESAURUS))
                );
        return used;
    }

    @Override
    public Thesaurus saveThesaurus(Thesaurus thesaurus) {
        if (StringUtils.isBlank(thesaurus.getName())) {
            throw new UnableToPerformException("common_propertysheet_validator_mandatory", MessageUtil.getMessage("thesaurus_name"));
        }

        NodeRef thesaurusRef = thesaurus.getNodeRef();
        if (RepoUtil.isUnsaved(thesaurusRef)) {
            thesaurusRef = createThesaurus(thesaurus);
        } else {
            nodeService.setProperty(thesaurusRef, ThesaurusModel.Prop.DESCRIPTION, StringUtils.trim(thesaurus.getDescription()));
        }

        updateKeywords(thesaurusRef, thesaurus.getKeywords());
        for (HierarchicalKeyword keyword : thesaurus.getRemovedKeywords()) {
            nodeService.deleteNode(keyword.getNodeRef());
        }

        thesaurus.setNodeRef(thesaurusRef);
        thesaurus.setRemovedKeywords(new ArrayList<HierarchicalKeyword>());
        thesaurus.setKeywords(getThesaurusKeywords(thesaurusRef));

        return thesaurus;
    }

    private boolean updateKeywords(NodeRef thesaurusRef, Collection<HierarchicalKeyword> newKeywords) {
        if (newKeywords == null || newKeywords.isEmpty()) {
            return false;
        }

        boolean keywordAdded = false;
        for (HierarchicalKeyword keyword : newKeywords) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
            props.put(ThesaurusModel.Prop.KEYWORD_LEVEL_1, StringUtils.trim(keyword.getKeywordLevel1()));
            props.put(ThesaurusModel.Prop.KEYWORD_LEVEL_2, StringUtils.trim(keyword.getKeywordLevel2()));

            NodeRef keywordRef = keyword.getNodeRef();
            if (RepoUtil.isUnsaved(keywordRef)) {
                nodeService.createNode(thesaurusRef, ThesaurusModel.Assoc.HIERARCHICAL_KEYWORD, ThesaurusModel.Assoc.HIERARCHICAL_KEYWORD,
                        ThesaurusModel.Types.HIERARCHICAL_KEYWORD, props);
                keywordAdded = true;
            } else {
                nodeService.addProperties(keywordRef, props);
            }
        }

        return keywordAdded;
    }

    private NodeRef createThesaurus(Thesaurus thesaurus) {
        String name = StringUtils.trim(thesaurus.getName());
        if (!name.matches("[A-Za-z]*")) {
            throw new UnableToPerformException("validator_onlyLetters_constraint_FIELD_NAME_ONLY", null, new MessageDataImpl("thesaurus_name"));
        }

        Map<QName, Serializable> props = new HashMap<QName, Serializable>(3);
        props.put(ThesaurusModel.Prop.NAME, name);
        props.put(ContentModel.PROP_NAME, name);
        props.put(ThesaurusModel.Prop.DESCRIPTION, StringUtils.trim(thesaurus.getDescription()));
        try {
            thesaurus.setNodeRef(nodeService.createNode(BeanHelper.getConstantNodeRefsBean().getThesauriRoot(), ThesaurusModel.Assoc.THESAURUS,
                    QName.createQName(ThesaurusModel.URI, name),
                    ThesaurusModel.Types.THESAURUS, props).getChildRef());
        } catch (DuplicateChildNodeNameException e) {
            throw new UnableToPerformException("thesaurus_name_duplicate");
        }

        return thesaurus.getNodeRef();
    }

    @Override
    public Thesaurus getThesaurus(String name, boolean fetchKeywords) {
        NodeRef thesaurusRef = nodeService.getChildByName(BeanHelper.getConstantNodeRefsBean().getThesauriRoot(), ThesaurusModel.Assoc.THESAURUS, name);
        return thesaurusRef == null ? null : getThesaurus(thesaurusRef, fetchKeywords);
    }

    @Override
    public int getThesauriCount() {
        return nodeService.getChildAssocs(BeanHelper.getConstantNodeRefsBean().getThesauriRoot()).size();
    }

    @Override
    public List<Thesaurus> getThesauri(boolean fetchKeywords) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(BeanHelper.getConstantNodeRefsBean().getThesauriRoot());
        List<Thesaurus> thesauri = new ArrayList<Thesaurus>(childAssocs.size());
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            thesauri.add(getThesaurus(childAssociationRef.getChildRef(), fetchKeywords));
        }

        return thesauri;
    }

    @Override
    public Thesaurus getThesaurus(NodeRef nodeRef, boolean fetchKeywords) {
        String name = (String) nodeService.getProperty(nodeRef, ThesaurusModel.Prop.NAME);
        String description = (String) nodeService.getProperty(nodeRef, ThesaurusModel.Prop.DESCRIPTION);
        Thesaurus thesaurus = new Thesaurus(nodeRef, name, description);

        if (fetchKeywords) {
            thesaurus.setKeywords(getThesaurusKeywords(nodeRef));
        }

        return thesaurus;
    }

    @Override
    public List<HierarchicalKeyword> getThesaurusKeywords(NodeRef thesaurusNodeRef) {
        if (RepoUtil.isUnsaved(thesaurusNodeRef)) {
            return Collections.emptyList();
        }

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(thesaurusNodeRef);
        List<HierarchicalKeyword> keywords = new ArrayList<HierarchicalKeyword>(childAssocs.size());
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            keywords.add(getKeyword(childAssociationRef.getChildRef()));
        }

        return keywords;
    }

    @Override
    public HierarchicalKeyword getKeyword(NodeRef keywordNodeRef) {
        String level1 = (String) nodeService.getProperty(keywordNodeRef, ThesaurusModel.Prop.KEYWORD_LEVEL_1);
        String level2 = (String) nodeService.getProperty(keywordNodeRef, ThesaurusModel.Prop.KEYWORD_LEVEL_2);
        return new HierarchicalKeyword(keywordNodeRef, level1, level2);
    }

    @Override
    public void exportThesauri(OutputStreamWriter writer) throws IOException {
        XStream xstream = new XStream();
        xstream.processAnnotations(Thesaurus.class);
        xstream.processAnnotations(HierarchicalKeyword.class);
        writer.write("<?xml version='1.0' encoding='" + writer.getEncoding() + "'?>\n");
        xstream.toXML(getThesauri(true), writer);
    }

    @Override
    public boolean importThesauri(List<Thesaurus> importedThesauri) {
        boolean changed = false;
        for (Thesaurus thesaurus : importedThesauri) {
            Thesaurus repoThesaurus = getThesaurus(thesaurus.getName(), true);

            // New thesaurus
            if (repoThesaurus == null) {
                saveThesaurus(thesaurus);
                changed = true;
                continue;
            }

            if (!StringUtils.equals(repoThesaurus.getDescription(), thesaurus.getDescription())) {
                changed = true;
                nodeService.setProperty(repoThesaurus.getNodeRef(), ThesaurusModel.Prop.DESCRIPTION, thesaurus.getDescription());
            }

            // Add new keywords
            @SuppressWarnings("unchecked")
            Collection<HierarchicalKeyword> newKeywords = CollectionUtils.subtract(thesaurus.getKeywords(), repoThesaurus.getKeywords());
            changed |= updateKeywords(repoThesaurus.getNodeRef(), newKeywords);
        }

        return changed;
    }

    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    // END: getters / setters

}
