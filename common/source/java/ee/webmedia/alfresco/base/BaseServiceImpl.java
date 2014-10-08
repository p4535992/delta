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
import org.alfresco.service.namespace.QNamePattern;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class BaseServiceImpl implements BaseService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(BaseServiceImpl.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;

    private final Map<QName, Class<? extends BaseObject>> typeMappings = new HashMap<QName, Class<? extends BaseObject>>();

    @Override
    public void addTypeMapping(QName type, Class<? extends BaseObject> clazz) {
        Assert.notNull(type, "type");
        Assert.notNull(clazz, "class");
        if (!BeanHelper.getApplicationService().isTest()) {
            // this check is disabled in development to allow JRebel do it's magic when reloading spring context
            Assert.isTrue(!typeMappings.containsKey(type), "type is already mapped");
        }
        typeMappings.put(type, clazz);
    }

    @Override
    public <T extends BaseObject> T getObject(NodeRef nodeRef, Class<T> returnCompatibleClass) {
        return getObject(nodeRef, returnCompatibleClass, null);
    }

    @Override
    public <T extends BaseObject> T getObject(NodeRef nodeRef, Class<T> returnCompatibleClass, Effort effort) {
        NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        BaseObject object = getObject(nodeRef, parentRef, null, effort);

        if (returnCompatibleClass != null && !returnCompatibleClass.isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("Based on type of the nodeRef " + nodeRef + " object class should be " + object.getClass()
                    + " based on mapping, but developer incorrectly expected that it is " + returnCompatibleClass);
        }
        @SuppressWarnings("unchecked")
        T casted = (T) object;
        return casted;
    }

    @Override
    public <T extends BaseObject> T getChild(NodeRef parentRef, QNamePattern assocNamePattern, Class<T> childrenClass) {
        NodeRef childRef = generalService.getChildByAssocName(parentRef, assocNamePattern);
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
        return getChildren(parentRef, childrenClass, mustIncludePredicate, null);
    }

    @Override
    public <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass, Predicate<T> mustIncludePredicate, Effort effort) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentRef);
        List<NodeRef> resultRefs = new ArrayList<NodeRef>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            resultRefs.add(childAssoc.getChildRef());
        }
        return getObjects(resultRefs, childrenClass, mustIncludePredicate, effort);
    }

    @Override
    public <T extends BaseObject> List<T> getChildren(NodeRef parentRef, Class<T> childrenClass, QNamePattern typeQNamePattern, QNamePattern qnamePattern, Effort effort) {
        List<ChildAssociationRef> docTypeFollowupAssocs = nodeService.getChildAssocs(parentRef, typeQNamePattern, qnamePattern);
        List<T> assocModels = new ArrayList<T>(docTypeFollowupAssocs.size());
        for (ChildAssociationRef childAssociationRef : docTypeFollowupAssocs) {
            assocModels.add(getObject(childAssociationRef.getChildRef(), childrenClass, effort));
        }
        return assocModels;
    }

    @Override
    public <T extends BaseObject> List<T> getObjects(List<NodeRef> resultRefs, Class<T> resultClass) {
        return getObjects(resultRefs, resultClass, null, null);
    }

    private <T extends BaseObject> List<T> getObjects(List<NodeRef> resultRefs, Class<T> resultClass, Predicate<T> mustIncludePredicate, Effort effort) {
        ArrayList<T> results = new ArrayList<T>(resultRefs.size());
        for (NodeRef nodeRef : resultRefs) {
            if (!nodeService.exists(nodeRef)) {
                continue;
            }
            T object = getObject(nodeRef, resultClass, effort);
            if (mustIncludePredicate == null || mustIncludePredicate.evaluate(object)) {
                results.add(object);
            }
        }
        return results;
    }

    private <T extends BaseObject> T getObject(NodeRef nodeRef, NodeRef parentRef, BaseObject parent, Effort effort) {
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
        if (effort == null || effort.isReturnChildren(object)) {
            loadChildrenInner(object, effort);
        } else {
            object.setProp(CHILDREN_NOT_LOADED, true);
        }
        return object;
    }

    @Override
    public <T extends BaseObject> void loadChildren(T parent, Effort effort) {
        Assert.isTrue(parent.getPropBoolean(CHILDREN_NOT_LOADED), "trying to load children of object that doesn't claim that its children have not been loaded!");
        loadChildrenInner(parent, effort);
        parent.getNode().getProperties().remove(CHILDREN_NOT_LOADED);
    }

    private <T extends BaseObject> void loadChildrenInner(T parent, Effort effort) {
        parent.setProp(CHILDREN_LOADING_IN_PROGRESS, true);
        for (ChildAssociationRef childAssociationRef : nodeService.getChildAssocs(parent.getNodeRef())) {
            NodeRef childRef = childAssociationRef.getChildRef();
            loadChild(parent, childRef, effort);
        }
        parent.getNode().getProperties().remove(CHILDREN_LOADING_IN_PROGRESS);
    }

    private <T extends BaseObject> void loadChild(T object, NodeRef childRef, Effort effort) {
        BaseObject child = getObject(childRef, null, object, effort);
        object.addLoadedChild(child);
    }

    @Override
    public boolean saveObject(BaseObject object) {
        try {
            boolean changed = false;
            WmNode node = object.getNode();
            Object skipSave = node.getProperties().remove(SKIP_SAVE);
            if (skipSave != null) {
                return false; // Object shouldn't be saved
            }
            Map<QName, Serializable> props = getSaveProperties(object.getChangedProperties());

            NodeRef parent = object.getParentNodeRef();
            Assert.isTrue(WmNode.isSaved(parent), "parent must be saved: " + node.toString());

            boolean wasSaved = object.isSaved();
            if (!wasSaved) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Creating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " + WmNode.toString(props, namespaceService));
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
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Updating node (type '" + node.getType().toPrefixString(namespaceService) + "') with properties " + WmNode.toString(props, namespaceService));
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
<<<<<<< HEAD
                        if (wasSaved) {
                            nodeService.deleteNode(removedNode.getNodeRef());
                        } else {
                            if (LOG.isTraceEnabled()) {
                                LOG.debug("not deleting node, that was previously saved under some other parent node: " + removedNode.getNodeRef());
=======
                        NodeRef removedNodeRef = removedNode.getNodeRef();
                        if (wasSaved && nodeService.exists(removedNodeRef)) {
                            nodeService.deleteNode(removedNodeRef);
                        } else {
                            if (LOG.isTraceEnabled()) {
                                LOG.debug("not deleting node, that was previously saved under some other parent node: " + removedNodeRef);
>>>>>>> develop-5.1
                            }
                        }
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
                        Assert.isTrue(child.isUnsaved(), "Node that was just created cannot contain child that was previously saved");
                        child.updateParentNodeRef();
                    }
                    boolean childChanged = saveObject(child);
                    changed |= childChanged;
                    // If this node was not saved, then all children are newly created, then childAssociationIndexes are already in the correct order
                    if (wasSaved && assocIndex >= 0) {
                        // TODO could further optimize that if no previous children have changed childAssociationIndex, then this index should be correct
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
