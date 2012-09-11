package ee.webmedia.alfresco.document.bootstrap;

import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.register.model.Register;

/**
 * Used to update register sequences. Sets all register sequences MINVALUE to "0".
 * 
 * @author Vladimir Drozdik
 */
public class RegistersSequencesUpdater extends AbstractModuleComponent {

    @Override
    protected void executeInternal() throws Throwable {
        List<Register> registers = BeanHelper.getRegisterService().getRegisters();
        for (Register register : registers) {
            BeanHelper.getRegisterService().updateRegisterSequence(register.getId(), register.getCounter());
        }
    }

}
