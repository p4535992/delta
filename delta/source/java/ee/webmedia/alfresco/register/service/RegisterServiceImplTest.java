package ee.webmedia.alfresco.register.service;

import java.util.Map;

import junit.framework.Assert;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.utils.RepoUtil;

public class RegisterServiceImplTest extends BaseAlfrescoSpringTest {

    private final String SEQ_REGISTER_PREFIX = "register_";
    private final String SEQ_REGISTER_SUFFIX = "_seq";

    public void testCreateRegister() {
        RegisterService registerService = BeanHelper.getRegisterService();
        Node registerNode = registerService.createRegister();
        assertNotNull(registerNode);

        registerService.updateProperties(registerNode);
        Integer regId = (Integer) registerNode.getProperties().get(RegisterModel.Prop.ID);
        Register register = registerService.getRegister(regId);
        Assert.assertEquals(0, register.getCounter());

        registerService.increaseCount(regId);
        register = registerService.getRegister(regId);
        Assert.assertEquals(1, register.getCounter());

        registerService.increaseCount(regId);
        register = registerService.getRegister(regId);
        Assert.assertEquals(2, register.getCounter());

        registerService.resetCounter(registerNode);
        register = registerService.getRegister(regId);
        Assert.assertEquals(0, register.getCounter());

        registerService.increaseCount(regId);
        register = registerService.getRegister(regId);
        Assert.assertEquals(1, register.getCounter());

        registerService.increaseCount(regId);
        register = registerService.getRegister(regId);
        Assert.assertEquals(2, register.getCounter());
    }

    public void testUpdateRegisterSequence() {
        RegisterService registerService = BeanHelper.getRegisterService();
        Node registerSeqFromOneNode = registerService.createRegister();
        Integer regId;
        { // updateProperties
            Map<String, Object> props = registerSeqFromOneNode.getProperties();
            regId = registerService.getMaxRegisterId() + 1;
            props.put(RegisterModel.Prop.ID.toString(), regId);
            { // createSequence(regId);
                final String seqName = SEQ_REGISTER_PREFIX + regId + SEQ_REGISTER_SUFFIX;
                jdbcTemplate.update("CREATE SEQUENCE " + seqName + " START 1");// old way
            }
            NodeRef root = BeanHelper.getGeneralService().getNodeRef(RegisterModel.Repo.REGISTERS_SPACE);
            nodeService.createNode(root, RegisterModel.Assoc.REGISTER,
                        QName.createQName(RegisterModel.URI, regId.toString()), RegisterModel.Types.REGISTER, //
                    RepoUtil.toQNameProperties(props));
        }
        regId = (Integer) registerSeqFromOneNode.getProperties().get(RegisterModel.Prop.ID);
        Register registerSeqFromOne = registerService.getRegister(regId);
        Assert.assertEquals(1, registerSeqFromOne.getCounter());

        registerService.updateRegisterSequence(regId, registerSeqFromOne.getCounter());
        registerSeqFromOne = registerService.getRegister(regId);
        assertEquals(0, registerSeqFromOne.getCounter());
    }
}
