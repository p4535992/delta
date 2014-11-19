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
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.help.model.HelpText;
import ee.webmedia.alfresco.help.model.HelpTextModel;
import ee.webmedia.alfresco.help.web.HelpTextUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * Help text service implementation.
 * <p>
 * Specification: <i>Kontekstitundlik abiinfo</i>.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class HelpTextServiceImpl implements HelpTextService {

    private NodeService nodeService;

    private GeneralService generalService;

    @Override
    public List<HelpText> getHelpTexts() {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(getRootNode());
        List<HelpText> results = new ArrayList<HelpText>(children.size());
        for (ChildAssociationRef childAssoc : children) {
            results.add(new HelpText(new WmNode(childAssoc.getChildRef(), HelpTextModel.Types.HELP_TEXT)));
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
        return addHelpText(HelpTextUtil.TYPE_DIALOG, code, content);
    }

    @Override
    public Node addFieldHelp(String code, String content) {
        return addHelpText(HelpTextUtil.TYPE_FIELD, code, content);
    }

    @Override
    public Node addDocumentTypeHelp(String code, String content) {
        return addHelpText(HelpTextUtil.TYPE_DOCUMENT_TYPE, code, content);
    }

    @Override
    public void editHelp(Node helpTextNode) {
        checkTypeAndCodeUnique(helpTextNode.getNodeRef(),
                (String) helpTextNode.getProperties().get(HelpTextModel.Props.CODE.toString()),
                (String) helpTextNode.getProperties().get(HelpTextModel.Props.NAME.toString()));

        nodeService.setProperties(helpTextNode.getNodeRef(), RepoUtil.toQNameProperties(helpTextNode.getProperties()));
    }

    private NodeRef getRootNode() {
        return generalService.getNodeRef(HelpTextModel.ROOT.toString());
    }

    private Node addHelpText(String type, String code, String content) {
        checkTypeAndCodeUnique(null, type, code);

        Map<QName, Serializable> props = new HashMap<QName, Serializable>(3, 1);
        props.put(HelpTextModel.Props.TYPE, type);
        props.put(HelpTextModel.Props.CODE, code);
        props.put(HelpTextModel.Props.CONTENT, content);
        NodeRef childRef = nodeService.createNode(getRootNode(), ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS, HelpTextModel.Types.HELP_TEXT, props).getChildRef();
        return generalService.fetchNode(childRef);
    }

    private void checkTypeAndCodeUnique(NodeRef nodeRef, String type, String code) {
        for (ChildAssociationRef childAssoc : nodeService.getChildAssocs(getRootNode())) {
            NodeRef childRef = childAssoc.getChildRef();

            if (!childRef.equals(nodeRef) && nodeService.getProperty(childRef, HelpTextModel.Props.TYPE).equals(type)
                    && nodeService.getProperty(childRef, HelpTextModel.Props.CODE).equals(code)) {
                throw new UnableToPerformException("help_text_unique_fail", nodeService.getProperty(childRef, HelpTextModel.Props.NAME), code);
            }
        }
    }

    // Dependency Injection Setters:

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
}
