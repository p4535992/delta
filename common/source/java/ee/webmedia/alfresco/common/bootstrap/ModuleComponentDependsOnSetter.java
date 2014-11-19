package ee.webmedia.alfresco.common.bootstrap;

import org.alfresco.repo.module.ModuleComponent;
import org.springframework.beans.factory.InitializingBean;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class ModuleComponentDependsOnSetter implements InitializingBean {

    private ModuleComponent source;
    private ModuleComponent dependsOn;

    public void setSource(ModuleComponent source) {
        this.source = source;
    }

    public void setDependsOn(ModuleComponent dependsOn) {
        this.dependsOn = dependsOn;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        source.getDependsOn().add(dependsOn);
    }

}
