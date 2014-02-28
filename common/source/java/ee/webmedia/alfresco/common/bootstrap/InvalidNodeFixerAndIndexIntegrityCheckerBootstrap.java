package ee.webmedia.alfresco.common.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;

/**
 * @author Alar Kvell
 */
public class InvalidNodeFixerAndIndexIntegrityCheckerBootstrap extends AbstractModuleComponent {

    private InvalidNodeFixerBootstrap invalidNodeFixerBootstrap;
    private IndexIntegrityCheckerBootstrap indexIntegrityCheckerBootstrap;

    @Override
    protected void executeInternal() throws Throwable {
        invalidNodeFixerBootstrap.execute();
        indexIntegrityCheckerBootstrap.execute(true, null);
    }

    public void setInvalidNodeFixerBootstrap(InvalidNodeFixerBootstrap invalidNodeFixerBootstrap) {
        this.invalidNodeFixerBootstrap = invalidNodeFixerBootstrap;
    }

    public void setIndexIntegrityCheckerBootstrap(IndexIntegrityCheckerBootstrap indexIntegrityCheckerBootstrap) {
        this.indexIntegrityCheckerBootstrap = indexIntegrityCheckerBootstrap;
    }

}
