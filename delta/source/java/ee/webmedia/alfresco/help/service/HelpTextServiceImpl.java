package ee.webmedia.alfresco.help.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.help.model.HelpTextModel;
import ee.webmedia.alfresco.utils.RepoUtil;

public class HelpTextServiceImpl implements HelpTextService {

    private static final String TYPE_DIALOG = "dialog";
    private static final String TYPE_DOCUMENT_TYPE = "documentType";
    private static final String TYPE_FIELD = "field";

    private NodeService nodeService;

    private GeneralService generalService;

    @Override
    public List<Node> getHelpTexts() {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(getRootNode());
        List<Node> results = new ArrayList<Node>(children.size());
        for (ChildAssociationRef childAssoc : children) {
            results.add(generalService.fetchNode(childAssoc.getChildRef()));
        }
        return results;
    }

    @Override
    public Map<String, Map<String, Boolean>> getHelpTextKeys() {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(getRootNode());
        Map<String, Map<String, Boolean>> results = new HashMap<String, Map<String, Boolean>>(children.size(), 1);
        for (ChildAssociationRef childAssoc : children) {
            NodeRef childRef = childAssoc.getChildRef();
            String type = (String) nodeService.getProperty(childRef, HelpTextModel.Props.TYPE);

            Map<String, Boolean> typeMap = results.get(type);
            if (typeMap == null) {
                typeMap = new HashMap<String, Boolean>();
                results.put(type, typeMap);
            }

            typeMap.put((String) nodeService.getProperty(childRef, HelpTextModel.Props.CODE), Boolean.TRUE);
        }
        return results;
    }

    @Override
    public String getHelpContent(String type, String code) {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(getRootNode());
        String result = null;
        for (ChildAssociationRef childAssoc : children) {
            NodeRef childRef = childAssoc.getChildRef();

            if (code.equals(nodeService.getProperty(childRef, HelpTextModel.Props.CODE)) && type.equals(nodeService.getProperty(childRef, HelpTextModel.Props.TYPE))) {
                result = (String) nodeService.getProperty(childRef, HelpTextModel.Props.CONTENT);
                break;
            }
        }
        return result;
    }

    @Override
    public Node addDialogHelp(String code, String content) {
        return addHelpText(TYPE_DIALOG, code, content);
    }

    @Override
    public Node addFieldHelp(String code, String content) {
        return addHelpText(TYPE_FIELD, code, content);
    }

    @Override
    public Node addDocumentTypeHelp(String code, String content) {
        return addHelpText(TYPE_DOCUMENT_TYPE, code, content);
    }

    @Override
    public void editHelp(Node helpTextNode) {
        nodeService.setProperties(helpTextNode.getNodeRef(), RepoUtil.toQNameProperties(helpTextNode.getProperties()));
    }

    @Override
    public void deleteHelp(NodeRef helpTextRef) {
        if (helpTextRef != null && nodeService.exists(helpTextRef)) {
            nodeService.deleteNode(helpTextRef);
        }
    }

    private NodeRef getRootNode() {
        return generalService.getNodeRef(HelpTextModel.ROOT.toString());
    }

    private Node addHelpText(String type, String code, String content) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(3, 1);
        props.put(HelpTextModel.Props.TYPE, type);
        props.put(HelpTextModel.Props.CODE, code);
        props.put(HelpTextModel.Props.CONTENT, content);
        NodeRef childRef = nodeService.createNode(getRootNode(), ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS, HelpTextModel.Types.HELP_TEXT, props).getChildRef();
        return generalService.fetchNode(childRef);
    }

    // Dependency Injection Setters:

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
}
