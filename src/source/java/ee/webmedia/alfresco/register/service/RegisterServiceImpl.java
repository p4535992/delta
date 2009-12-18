package ee.webmedia.alfresco.register.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class RegisterServiceImpl implements RegisterService {

    private static BeanPropertyMapper<Register> registerBeanPropertyMapper;
    static {
        registerBeanPropertyMapper = BeanPropertyMapper.newInstance(Register.class);
    }

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    public List<Register> getRegisters() {
        NodeRef root = getRoot();
        List<ChildAssociationRef> registerRefs = nodeService.getChildAssocs(root);
        List<Register> registers = new ArrayList<Register>(registerRefs.size());
        for (ChildAssociationRef registerRef : registerRefs) {
            Register reg = registerBeanPropertyMapper.toObject(nodeService.getProperties(registerRef.getChildRef()));
            reg.setNodeRef(registerRef.getChildRef());
            registers.add(reg);
        }

        return registers;
    }

    public Node getRegisterNode(int id) {
        Node node = new Node(generalService.getNodeRef(RegisterModel.Repo.REGISTERS_SPACE + "/" + RegisterModel.NAMESPACE_PREFFIX + id));
        node.getProperties();
        return node;
    }

    @Override
    public NodeRef getRoot() {
        return generalService.getNodeRef(RegisterModel.Repo.REGISTERS_SPACE);
    }

    @Override
    public int getMaxRegisterId() {
        int max = 0;
        for (Register r : getRegisters()) {
            max = Math.max(max, r.getId());
        }
        return max;
    }

    @Override
    public Node createRegister() {
        // Set the default values
        Map<QName, Serializable> prop = new HashMap<QName, Serializable>();
        int id = getMaxRegisterId() + 1;
        prop.put(RegisterModel.Prop.ID, id);
        prop.put(RegisterModel.Prop.COUNTER, new Integer(0));
        prop.put(RegisterModel.Prop.ACTIVE, Boolean.TRUE);
        TransientNode transientNode = new TransientNode(RegisterModel.Types.REGISTER, QName.createQName(RegisterModel.URI, Integer.toString(id)).toString(), prop);
        transientNode.getProperties();
        return transientNode;
    }

    @Override
    public void updateProperties(Node register) {
        // Check if node is new or it is being updated
        Map<String, Object> prop = register.getProperties();
        if(!nodeService.exists(register.getNodeRef())) {
            nodeService.createNode(getRoot(), RegisterModel.Assoc.REGISTER,
                  QName.createQName(RegisterModel.URI, prop.get(RegisterModel.Prop.ID).toString()), RegisterModel.Types.REGISTER, RepoUtil.toQNameProperties(prop));
        } else {
            nodeService.setProperties(register.getNodeRef(), RepoUtil.toQNameProperties(prop));
        }
    }

    // START: getters / setters

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // END: getters / setters

}
