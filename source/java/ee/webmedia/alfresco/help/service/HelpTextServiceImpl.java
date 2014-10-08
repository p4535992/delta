package ee.webmedia.alfresco.help.service;

<<<<<<< HEAD
=======
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

>>>>>>> develop-5.1
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
<<<<<<< HEAD
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.GeneralService;
=======
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

>>>>>>> develop-5.1
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
 */
public class HelpTextServiceImpl implements HelpTextService {

    private NodeService nodeService;

    private GeneralService generalService;

    @Override
    public List<HelpText> getHelpTexts() {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(getRootNode());
=======
 */
public class HelpTextServiceImpl implements HelpTextService {

    @Override
    public List<HelpText> getHelpTexts() {
        List<ChildAssociationRef> children = getNodeService().getChildAssocs(getRootNode());
>>>>>>> develop-5.1
        List<HelpText> results = new ArrayList<HelpText>(children.size());
        for (ChildAssociationRef childAssoc : children) {
            results.add(new HelpText(new WmNode(childAssoc.getChildRef(), HelpTextModel.Types.HELP_TEXT)));
        }
        return results;
    }

    @Override
    public Map<String, Map<String, Boolean>> getHelpTextKeys() {
<<<<<<< HEAD
        List<ChildAssociationRef> children = nodeService.getChildAssocs(getRootNode());
        Map<String, Map<String, Boolean>> results = new HashMap<String, Map<String, Boolean>>(children.size(), 1);
        for (ChildAssociationRef childAssoc : children) {
            NodeRef childRef = childAssoc.getChildRef();
            String type = (String) nodeService.getProperty(childRef, HelpTextModel.Props.TYPE);
=======
        List<ChildAssociationRef> children = getNodeService().getChildAssocs(getRootNode());
        Map<String, Map<String, Boolean>> results = new HashMap<String, Map<String, Boolean>>(children.size(), 1);
        for (ChildAssociationRef childAssoc : children) {
            NodeRef childRef = childAssoc.getChildRef();
            String type = (String) getNodeService().getProperty(childRef, HelpTextModel.Props.TYPE);
>>>>>>> develop-5.1

            Map<String, Boolean> typeMap = results.get(type);
            if (typeMap == null) {
                typeMap = new HashMap<String, Boolean>();
                results.put(type, typeMap);
            }

<<<<<<< HEAD
            typeMap.put((String) nodeService.getProperty(childRef, HelpTextModel.Props.CODE), Boolean.TRUE);
=======
            typeMap.put((String) getNodeService().getProperty(childRef, HelpTextModel.Props.CODE), Boolean.TRUE);
>>>>>>> develop-5.1
        }
        return results;
    }

    @Override
    public String getHelpContent(String type, String code) {
<<<<<<< HEAD
        List<ChildAssociationRef> children = nodeService.getChildAssocs(getRootNode());
=======
        List<ChildAssociationRef> children = getNodeService().getChildAssocs(getRootNode());
>>>>>>> develop-5.1
        String result = null;
        for (ChildAssociationRef childAssoc : children) {
            NodeRef childRef = childAssoc.getChildRef();

<<<<<<< HEAD
            if (code.equals(nodeService.getProperty(childRef, HelpTextModel.Props.CODE)) && type.equals(nodeService.getProperty(childRef, HelpTextModel.Props.TYPE))) {
                result = (String) nodeService.getProperty(childRef, HelpTextModel.Props.CONTENT);
=======
            if (code.equals(getNodeService().getProperty(childRef, HelpTextModel.Props.CODE)) && type.equals(getNodeService().getProperty(childRef, HelpTextModel.Props.TYPE))) {
                result = (String) getNodeService().getProperty(childRef, HelpTextModel.Props.CONTENT);
>>>>>>> develop-5.1
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

<<<<<<< HEAD
        nodeService.setProperties(helpTextNode.getNodeRef(), RepoUtil.toQNameProperties(helpTextNode.getProperties()));
    }

    private NodeRef getRootNode() {
        return generalService.getNodeRef(HelpTextModel.ROOT.toString());
=======
        getNodeService().setProperties(helpTextNode.getNodeRef(), RepoUtil.toQNameProperties(helpTextNode.getProperties()));
    }

    private NodeRef getRootNode() {
        return getGeneralService().getNodeRef(HelpTextModel.ROOT.toString());
>>>>>>> develop-5.1
    }

    private Node addHelpText(String type, String code, String content) {
        checkTypeAndCodeUnique(null, type, code);

        Map<QName, Serializable> props = new HashMap<QName, Serializable>(3, 1);
        props.put(HelpTextModel.Props.TYPE, type);
        props.put(HelpTextModel.Props.CODE, code);
        props.put(HelpTextModel.Props.CONTENT, content);
<<<<<<< HEAD
        NodeRef childRef = nodeService.createNode(getRootNode(), ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS, HelpTextModel.Types.HELP_TEXT, props).getChildRef();
        return generalService.fetchNode(childRef);
    }

    private void checkTypeAndCodeUnique(NodeRef nodeRef, String type, String code) {
        for (ChildAssociationRef childAssoc : nodeService.getChildAssocs(getRootNode())) {
            NodeRef childRef = childAssoc.getChildRef();

            if (!childRef.equals(nodeRef) && nodeService.getProperty(childRef, HelpTextModel.Props.TYPE).equals(type)
                    && nodeService.getProperty(childRef, HelpTextModel.Props.CODE).equals(code)) {
                throw new UnableToPerformException("help_text_unique_fail", nodeService.getProperty(childRef, HelpTextModel.Props.NAME), code);
=======
        NodeRef childRef = getNodeService().createNode(getRootNode(), ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS, HelpTextModel.Types.HELP_TEXT, props).getChildRef();
        return getGeneralService().fetchNode(childRef);
    }

    private void checkTypeAndCodeUnique(NodeRef nodeRef, String type, String code) {
        for (ChildAssociationRef childAssoc : getNodeService().getChildAssocs(getRootNode())) {
            NodeRef childRef = childAssoc.getChildRef();

            if (!childRef.equals(nodeRef) && getNodeService().getProperty(childRef, HelpTextModel.Props.TYPE).equals(type)
                    && getNodeService().getProperty(childRef, HelpTextModel.Props.CODE).equals(code)) {
                throw new UnableToPerformException("help_text_unique_fail", getNodeService().getProperty(childRef, HelpTextModel.Props.NAME), code);
>>>>>>> develop-5.1
            }
        }
    }

<<<<<<< HEAD
    // Dependency Injection Setters:

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
=======
>>>>>>> develop-5.1
}
