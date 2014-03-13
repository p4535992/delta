package ee.webmedia.alfresco.common.bootstrap;

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
