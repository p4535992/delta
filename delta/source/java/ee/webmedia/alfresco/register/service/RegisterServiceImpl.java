package ee.webmedia.alfresco.register.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class RegisterServiceImpl implements RegisterService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(RegisterServiceImpl.class);

    private static BeanPropertyMapper<Register> registerBeanPropertyMapper;
    private static final int DEFAULT_COUNTER_INITIAL_VALUE = 1;
    static {
        registerBeanPropertyMapper = BeanPropertyMapper.newInstance(Register.class);
    }

    private GeneralService generalService;
    private NodeService nodeService;
    private SimpleJdbcTemplate jdbcTemplate;
    private final String SEQ_REGISTER_PREFIX = "register_";
    private final String SEQ_REGISTER_SUFFIX = "_seq";

    @Override
    public List<Register> getRegisters() {
        NodeRef root = getRoot();
        List<ChildAssociationRef> registerRefs = nodeService.getChildAssocs(root);
        List<Register> registers = new ArrayList<Register>(registerRefs.size());
        for (ChildAssociationRef registerRef : registerRefs) {
            Register reg = registerBeanPropertyMapper.toObject(nodeService.getProperties(registerRef.getChildRef()));
            reg.setNodeRef(registerRef.getChildRef());
            reg.setCounter(getRegisterCounter(reg.getId()));
            registers.add(reg);
        }

        return registers;
    }

    @Override
    public Node getRegisterNode(int id) {
        Node node = getRegisterNodeRefById(id);
        return node;
    }

    private Node getRegisterNodeRefById(int registerId) {
        final NodeRef registerRef = generalService.getNodeRef(RegisterModel.Repo.REGISTERS_SPACE + "/" + RegisterModel.NAMESPACE_PREFFIX + registerId);
        final Node registerNode = new Node(registerRef);
        final Map<String, Object> props = registerNode.getProperties();
        props.put(RegisterModel.Prop.COUNTER.toString(), getRegisterCounter(registerId));
        return registerNode;
    }

    private Integer getRegisterCounter(int registerId) {
        // XXX: millegi pärast viisakam lahendus ei tööta: BadSqlGrammarException: PreparedStatementCallback; bad SQL grammar [SELECT last_value FROM ?]; nested exception is
        // org.postgresql.util.PSQLException: ERROR: syntax error at or near "$1"
        // return jdbcTemplate.queryForInt("SELECT last_value FROM ?", getSequenceName(registerId));
        return jdbcTemplate.queryForInt("SELECT last_value FROM " + getSequenceName(registerId));
    }

    private String getSequenceName(int registerId) {
        final String seqName = SEQ_REGISTER_PREFIX + registerId + SEQ_REGISTER_SUFFIX;
        return seqName;
    }

    @Override
    public Register getRegister(Integer registerId) {
        final Node registerNode = getRegisterNodeRefById(registerId);
        final Map<String, Object> props = registerNode.getProperties();
        Register reg = registerBeanPropertyMapper.toObject(RepoUtil.toQNameProperties(props));
        reg.setNodeRef(registerNode.getNodeRef());
        // counter is not mappable(stored in sequence ant put manually into props)
        reg.setCounter((Integer) props.get(RegisterModel.Prop.COUNTER.toString()));
        return reg;
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
        prop.put(RegisterModel.Prop.COUNTER, DEFAULT_COUNTER_INITIAL_VALUE);
        prop.put(RegisterModel.Prop.ACTIVE, Boolean.TRUE);
        TransientNode transientNode = new TransientNode( //
                RegisterModel.Types.REGISTER, QName.createQName(RegisterModel.URI, "temp").toString(), prop);
        transientNode.getProperties();
        return transientNode;
    }

    @Override
    public void updateProperties(Node register) {
        // Check if node is new or it is being updated
        Map<String, Object> prop = register.getProperties();
        if (!nodeService.exists(register.getNodeRef())) {
            Integer regId = getMaxRegisterId() + 1;
            prop.put(RegisterModel.Prop.ID.toString(), regId);
            createSequence(regId);
            nodeService.createNode(getRoot(), RegisterModel.Assoc.REGISTER,
                    QName.createQName(RegisterModel.URI, regId.toString()), RegisterModel.Types.REGISTER, //
                    RepoUtil.toQNameProperties(prop));
        } else {
            nodeService.setProperties(register.getNodeRef(), RepoUtil.toQNameProperties(prop));
        }
    }

    @Override
    public void updatePropertiesFromObject(Register register) {
        generalService.setPropertiesIgnoringSystem(registerBeanPropertyMapper.toProperties(register), register.getNodeRef());
    }

    @Override
    public int increaseCount(int registerId) {
        // NB! even if value 0 is not allowed as initial value of sequence,
        // it is compensated so that first call to nextval() will not increment 1 by 1, but leave it to 1 unlike consequent calls)
        return jdbcTemplate.queryForInt("SELECT nextval(?)", getSequenceName(registerId));
    }

    @Override
    public void resetCounter(Node register) {
        final Map<String, Object> props = register.getProperties();
        int registerId = (Integer) props.get(RegisterModel.Prop.ID);
        // "false" argument of setval ensures that next call to nextval() will return 1, not 2
        jdbcTemplate.queryForInt("SELECT setval(?, ?, false)", getSequenceName(registerId), DEFAULT_COUNTER_INITIAL_VALUE);
        props.put(RegisterModel.Prop.COUNTER.toString(), DEFAULT_COUNTER_INITIAL_VALUE);
    }

    private void createSequence(int registerId) {
        final String seqName = getSequenceName(registerId);
        jdbcTemplate.update("CREATE SEQUENCE " + seqName + " START 1"); // parameetritega ei toimi
        log.debug("created sequence: " + seqName);
    }

    // START: getters / setters
    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // END: getters / setters

}
