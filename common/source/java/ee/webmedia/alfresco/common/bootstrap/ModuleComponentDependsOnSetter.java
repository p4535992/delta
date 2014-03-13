package ee.webmedia.alfresco.common.bootstrap;

import org.alfresco.repo.module.ModuleComponent;
import org.springframework.beans.factory.InitializingBean;

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
