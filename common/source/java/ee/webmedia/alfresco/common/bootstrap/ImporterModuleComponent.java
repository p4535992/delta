package ee.webmedia.alfresco.common.bootstrap;

import java.util.List;
import java.util.Properties;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class ImporterModuleComponent extends org.alfresco.repo.module.ImporterModuleComponent {

    private boolean enabled = true;
    private List<Properties> bootstrapViews;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void executeInternal() throws Throwable {
        if (enabled) {
            super.executeInternal();
        }
    }

    @Override
    public void setBootstrapViews(List<Properties> bootstrapViews) {
        super.setBootstrapViews(bootstrapViews);
        this.bootstrapViews = bootstrapViews;
    }

    public List<Properties> getBootstrapViews() {
        return bootstrapViews;
    }

}
