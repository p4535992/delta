package ee.webmedia.alfresco.common.bootstrap;

/**
 * @author Alar Kvell
 */
public class ImporterModuleComponent extends org.alfresco.repo.module.ImporterModuleComponent {

    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (enabled) {
            super.executeInternal();
        }
    }

}
