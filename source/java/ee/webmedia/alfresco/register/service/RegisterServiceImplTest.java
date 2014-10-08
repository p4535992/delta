package ee.webmedia.alfresco.register.service;

import junit.framework.Assert;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.register.model.Register;
import ee.webmedia.alfresco.register.model.RegisterModel;

public class RegisterServiceImplTest extends BaseAlfrescoSpringTest {

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

    public void testTransactionRollback() {
        RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
        txHelper.setMaxRetries(1);
        RetryingTransactionCallback<Object> cb = new RetryingTransactionCallback<Object>() {

            @Override
            public Object execute() throws Throwable {
                // TODO later
                return null;
            }
        };
        txHelper.doInTransaction(cb, false, true);
    }
}
