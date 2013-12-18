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
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class RegisterServiceImpl implements RegisterService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(RegisterServiceImpl.class);

    private static BeanPropertyMapper<Register> registerBeanPropertyMapper;
    private static final int DEFAULT_COUNTER_INITIAL_VALUE = 0;
    static {
        registerBeanPropertyMapper = BeanPropertyMapper.newInstance(Register.class);
    }

    private GeneralService generalService;
    private NodeService nodeService;
    private LogService logService;
    private UserService userService;
    private SimpleJdbcTemplate jdbcTemplate;
    private final String REGISTER_TABLE_NAME = "delta_register";
    private boolean valueEditable;

    @Override
    public boolean isValueEditable() {
        return valueEditable;
    }

    public void setValueEditable(boolean valueEditable) {
        this.valueEditable = valueEditable;
    }

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
        final NodeRef registerRef = generalService.getNodeRef(RegisterModel.Repo.REGISTERS_SPACE + "/" + RegisterModel.NAMESPACE_PREFFIX + id);
        final Node registerNode = new Node(registerRef);
        final Map<String, Object> props = registerNode.getProperties();
        props.put(RegisterModel.Prop.COUNTER.toString(), getRegisterCounter(id));
        return registerNode;
    }

    private Integer getRegisterCounter(int registerId) {
        return jdbcTemplate.queryForInt("SELECT counter FROM " + REGISTER_TABLE_NAME + " WHERE register_id=?", registerId);
    }

    @Override
    public Register getRegister(Integer registerId) {
        final Node registerNode = getRegisterNode(registerId);
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
        prop.put(RegisterModel.Prop.ACTIVE, Boolean.TRUE);
        TransientNode transientNode = new TransientNode(
                RegisterModel.Types.REGISTER, QName.createQName(RegisterModel.URI, "temp").toString(), prop);
        transientNode.getProperties().put(RegisterModel.Prop.COUNTER.toString(), DEFAULT_COUNTER_INITIAL_VALUE);
        return transientNode;
    }

    @Override
    public void updateProperties(Node register) {
        Integer counter;
        String counterLabel = MessageUtil.getMessage("register_counter");
        Map<String, Object> prop = register.getProperties();
        try {
            counter = DefaultTypeConverter.INSTANCE.convert(Integer.class, prop.get(RegisterModel.Prop.COUNTER));
        } catch (TypeConversionException e) {
            throw new UnableToPerformException("validation_is_nonegative_int_number", counterLabel);
        }
        if (counter == null || counter < 0) {
            throw new UnableToPerformException("validation_is_nonegative_int_number", counterLabel);
        }
        // Check if node is new or it is being updated
        Map<QName, Serializable> newProps = RepoUtil.toQNameProperties(prop);
        if (!nodeService.exists(register.getNodeRef())) {
            Integer regId = getMaxRegisterId() + 1;
            newProps.put(RegisterModel.Prop.ID, regId);
            prop.put(RegisterModel.Prop.ID.toString(), regId); // Make ID available to caller
            createSequence(regId);
            nodeService.createNode(getRoot(), RegisterModel.Assoc.REGISTER,
                    QName.createQName(RegisterModel.URI, regId.toString()), RegisterModel.Types.REGISTER, newProps);
            setRegisterCounterValue(regId, counter);
            logService.addLogEntry(LogEntry.create(LogObject.REGISTER, userService, "applog_register_add", prop.get(RegisterModel.Prop.NAME.toString())));
        } else {
            Map<QName, Serializable> oldProps = nodeService.getProperties(register.getNodeRef());
            nodeService.setProperties(register.getNodeRef(), newProps);
            setRegisterCounterValue((Integer) prop.get(RegisterModel.Prop.ID), counter);

            String diff = new PropDiffHelper()
                    .label(RegisterModel.Prop.NAME, "register_name")
                    .label(RegisterModel.Prop.COUNTER, "register_counter")
                    .label(RegisterModel.Prop.ACTIVE, "register_active")
                    .label(RegisterModel.Prop.AUTO_RESET, "register_autoReset")
                    .label(RegisterModel.Prop.COMMENT, "register_comment")
                    .diff(oldProps, newProps);
            logService.addLogEntry(LogEntry.create(LogObject.REGISTER, userService, "applog_register_edit", prop.get(RegisterModel.Prop.NAME.toString()), diff));
        }
    }

    @Override
    public void updatePropertiesFromObject(Register register) {
        generalService.setPropertiesIgnoringSystem(registerBeanPropertyMapper.toProperties(register), register.getNodeRef());
    }

    @Override
    public int increaseCount(int registerId) {
        int currentCounter = jdbcTemplate.queryForInt("SELECT counter FROM " + REGISTER_TABLE_NAME + " WHERE register_id=? FOR UPDATE", registerId);
        currentCounter++;
        setRegisterCounterValue(registerId, currentCounter);
        return currentCounter;
    }

    private void setRegisterCounterValue(final int seqName, int seqValue) {
        jdbcTemplate.update("UPDATE " + REGISTER_TABLE_NAME + " SET counter=? WHERE register_id=?", seqValue, seqName);
    }

    @Override
    public void resetCounter(Node register) {
        final Map<String, Object> props = register.getProperties();
        int registerId = (Integer) props.get(RegisterModel.Prop.ID);
        setRegisterCounterValue(registerId, DEFAULT_COUNTER_INITIAL_VALUE);
        props.put(RegisterModel.Prop.COUNTER.toString(), DEFAULT_COUNTER_INITIAL_VALUE);
    }

    private void createSequence(int registerId) {
        jdbcTemplate.update("INSERT INTO " + REGISTER_TABLE_NAME + " (register_id,counter) values (?,?)", registerId, DEFAULT_COUNTER_INITIAL_VALUE);
        log.debug("created sequence: " + registerId);
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

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    // END: getters / setters

}