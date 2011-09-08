package ee.webmedia.alfresco.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ChildAssociationDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * @author Alar Kvell
 */
public class BaseServiceImpl implements BaseService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(BaseServiceImpl.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;

    private final Map<QName, Class<? extends BaseObject>> typeMappings = new HashMap<QName, Class<? extends BaseObject>>();

    @Override
    public void addTypeMapping(QName type, Class<? extends BaseObject> clazz) {
        Assert.notNull(type, "type");
        Assert.notNull(clazz, "class");
        Assert.isTrue(!typeMappings.containsKey(type));
        typeMappings.put(type, clazz);
    }

    // TODO in the future, if there is need for it, implement limit loading of hierarchy in certain points

    @Override
    public <T extends BaseObject> T getObject(NodeRef nodeRef, Class<T> returnCompatibleClass) {
        NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        BaseObject object = getObject(nodeRef, parentRef, null);

        if (!returnCompatibleClass.isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("Based on nodeRef type object class should be " + object.getClass()
                    + " based on mapping, but developer incorrectly expected that it is " + returnCompatibleClass);
        }
        @SuppressWarnings("unchecked")
        T casted = (T) object;
        return casted;
    }

    @Override
    public <T extends BaseObject> T getChild(NodeRef parentRef, QName assocName, Class<T> childrenClass) {
        NodeRef childRef = generalService.getChildByAssocName(parentRef, assocName);
        if (childRef == null) {
            return null;
        }
        return getObject(childRef, childrenClass);
    }

    @Override
    public <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass) {
        return getChildren(parentRef, childrenClass, null);
    }

    @Override
    public <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass, Predicate<T> mustIncludePredicate) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentRef);
        List<NodeRef> resultRefs = new ArrayList<NodeRef>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            resultRefs.add(childAssoc.getChildRef());
        }
        return getObjects(resultRefs, childrenClass, mustIncludePredicate);
    }

    @Override
    public <T extends BaseObject> List<T> getObjects(List<NodeRef> resultRefs, Class<T> resultClass) {
        return getObjects(resultRefs, resultClass, null);
    }

    private <T extends BaseObject> List<T> getObjects(List<NodeRef> resultRefs, Class<T> resultClass, Predicate<T> mustIncludePredicate) {
        ArrayList<T> results = new ArrayList<T>(resultRefs.size());
        for (NodeRef nodeRef : resultRefs) {
            T object = getObject(nodeRef, resultClass);
            if (mustIncludePredicate == null || mustIncludePredicate.evaluate(object)) {
                results.add(object);
            }
        }
        return results;
    }

    private <T extends BaseObject> T getObject(NodeRef nodeRef, NodeRef parentRef, BaseObject parent) {

        Set<QName> aspects = RepoUtil.getAspectsIgnoringSystem(nodeService.getAspects(nodeRef));
        Map<QName, Serializable> properties = RepoUtil.getPropertiesIgnoringSystem(nodeService.getProperties(nodeRef), dictionaryService);
        WmNode node = new WmNode(nodeRef, nodeService.getType(nodeRef), aspects, properties);

        QName type = node.getType();
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) typeMappings.get(type);
        Assert.notNull(clazz, "No mapping registered for type: " + type);
        T object;
        if (parent == null) {
            object = BaseObject.createNewWithParentNodeRef(clazz, parentRef, node);
        } else {
            object = BaseObject.createNewWithParentObject(clazz, parent, node);
        }
        for (ChildAssociationRef childAssociationRef : nodeService.getChildAssocs(nodeRef)) {
            BaseObject child = getObject(childAssociationRef.getChildRef(), null, object);
            object.addLoadedChild(child);
        }
        return object;
    }

    @Override
    public boolean saveObject(BaseObject object) {
        try {
            boolean changed = false;
            WmNode node = object.getNode();
            Map<QName, Serializable> props = getSaveProperties(object.getChangedProperties());

            NodeRef parent = object.getParentNodeRef();
            Assert.isTrue(WmNode.isSaved(parent), "parent must be saved: " + node.toString());

            boolean wasSaved = object.isSaved();
            if (!wasSaved) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " + WmNode.toString(props, namespaceService));
                }
                if (!isDuplicateChildNamesAllowed(object, parent)) {
                    // make sure that when model forbids duplicates, then cm:name property is set that is used to check for duplicates
                    props.put(ContentModel.PROP_NAME, object.getAssocName().getLocalName());
                }
                NodeRef nodeRef = nodeService.createNode(parent, object.getAssocType(), object.getAssocName(), node.getType(), props).getChildRef();
                node.updateNodeRef(nodeRef);
                changed = true;

                // adding additional aspects not implemented - TODO if necessary
                // removing aspects is not implemented - not needed
            } else {
                if (!props.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Updating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " + WmNode.toString(props, namespaceService));
                    }

                    nodeService.addProperties(node.getNodeRef(), props); // do not replace non-changed properties
                    changed = true;

                    // adding/removing aspects is not implemented - not needed
                }
            }
            object.setChangedProperties(props);

            // TODO queue events if necessary

            // Remove children
            Map<Class<? extends BaseObject>, List<? extends BaseObject>> removedChildren = object.getRemovedChildren();
            for (Entry<Class<? extends BaseObject>, List<? extends BaseObject>> removedChildListEntry : removedChildren.entrySet()) {
                for (BaseObject removedChild : (List<? extends BaseObject>) removedChildListEntry.getValue()) {
                    WmNode removedNode = removedChild.getNode();
                    if (removedNode.isSaved()) {
                        Assert.isTrue(wasSaved, "Node that was just created cannot contain removed child that was previously saved");
                        nodeService.deleteNode(removedNode.getNodeRef());
                        changed = true;
                    }
                }
            }
            removedChildren.clear();

            // Order children by assocIndex
            Map<Integer, List<BaseObject>> childrenByAssocIndex = new TreeMap<Integer, List<BaseObject>>();
            for (Entry<Class<? extends BaseObject>, List<? extends BaseObject>> childListEntry : object.getChildren().entrySet()) {
                for (BaseObject child : (List<? extends BaseObject>) childListEntry.getValue()) {
                    int assocIndex = child.getAssocIndex();
                    if (assocIndex < -1) {
                        assocIndex = -1;
                    }
                    List<BaseObject> children = childrenByAssocIndex.get(assocIndex);
                    if (children == null) {
                        children = new ArrayList<BaseObject>();
                        childrenByAssocIndex.put(assocIndex, children);
                    }
                    children.add(child);
                }
            }

            // Create or update children
            for (Entry<Integer, List<BaseObject>> entry : childrenByAssocIndex.entrySet()) {
                Integer assocIndex = entry.getKey();
                for (BaseObject child : entry.getValue()) {
                    if (!wasSaved) {
                        Assert.isTrue(!child.isSaved(), "Node that was just created cannot contain child that was previously saved");
                        child.updateParentNodeRef();
                    }
                    boolean childChanged = saveObject(child);
                    changed |= childChanged;
                    if (assocIndex >= 0) {
                        ChildAssociationRef childAssocRef = nodeService.getPrimaryParent(child.getNodeRef());
                        if (childAssocRef.getNthSibling() != assocIndex) {
                            nodeService.setChildAssociationIndex(childAssocRef, assocIndex);
                            changed = true;
                        }
                    }
                }
            }

            return changed;
        } catch (DuplicateChildNodeNameException e) {
            NodeRef parentRef = object.getParentNodeRef();
            if (e.getParentNodeRef().equals(parentRef)) {
                // allow using custom message based on parent type and association name
                QName type = nodeService.getType(parentRef);
                String givenDuplicateName = e.getName();
                UnableToPerformException exception = new UnableToPerformException("baseService_duplicateChildName_"
                        + type.getLocalName() + "_" + e.getAssocTypeQName().getLocalName(), givenDuplicateName);
                exception.setFallbackMessage(new MessageDataImpl("system.err.duplicate_name", givenDuplicateName));
                throw exception;
            }
            throw e;
        } catch (RuntimeException e) {
            object.handleException(e);
            // if handleException didn't rethrow exception, then throw exception with generic message
            throw new UnableToPerformException(MessageSeverity.ERROR, "baseService_save_failed", e);
        }
    }

    private boolean isDuplicateChildNamesAllowed(BaseObject object, NodeRef parentRef) {
        QName parentType = nodeService.getType(parentRef);
        TypeDefinition typeDef = dictionaryService.getType(parentType);
        QName assocType = object.getAssocType();
        ChildAssociationDefinition childAssociationDefinition = typeDef.getChildAssociations().get(assocType);
        if (childAssociationDefinition == null) {
            for (QName aspectQName : generalService.getDefaultAspects(parentType)) {
                AspectDefinition aspectDef = dictionaryService.getAspect(aspectQName);
                childAssociationDefinition = aspectDef.getChildAssociations().get(assocType);
                if (childAssociationDefinition != null) {
                    break;
                }
            }
        }
        if (childAssociationDefinition == null) {
            // XXX täiendada, kui vaja - läbi on vaadatud tüübi ja tüübi otseste aspektide child-assoc'id,
            // aga mitte rekursiivselt kõigi aspektide child-associd.
            // Hetkel eeldatakse, et salvestatava objekti parent tüübil või mõnel aspektil on <child-association name="assocType"> definitsioon. See takistab hetkel salvestamast
            // child-nodesid, mille tüüp pole täpselt mudelis defineeritud child-association elemendi name attribuudi väärtus, vaid selle alamklass (seega võiks eelmises for
            // tsükklis vajadusel kontrollida ka assocType parent tüüpide järgi ChildAssociationDefinition'i küsimist)
            throw new IllegalArgumentException("Failed to check if duplicate childNames are allowed when saving " + object + "\n\nNode " + parentRef + " with type \n"
                    + parentType + "\n, doesn't seem to have childAssocDefinition with type\n" + assocType + "\n" + ". Maybe model or code needs improovements");
        }
        return childAssociationDefinition.getDuplicateChildNamesAllowed();
    }

    private Map<QName, Serializable> getSaveProperties(Map<QName, Serializable> props) {
        Map<QName, Serializable> filteredProps = RepoUtil.getPropertiesIgnoringSystem(props, dictionaryService);
        generalService.savePropertiesFiles(filteredProps);
        return filteredProps;
    }

    // START: setters
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    // END: setters
}
