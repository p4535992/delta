package ee.webmedia.alfresco.common.bootstrap;

import java.io.File;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author Alar Kvell
 */
public class CreateOrMoveRootDirBootstrap implements InitializingBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CreateOrMoveRootDirBootstrap.class);

    private File dirRoot;
    private File dirRootShared;
    private File dirRootLocal;

    private File dirContentstore;
    private File dirContentstoreDeleted;
    private File dirAuditContentstore;
    private File dirIndexes;
    private File dirIndexesBackup;

    @Override
    public void afterPropertiesSet() throws Exception {
        File dirRootUpdater = new File(dirRootShared, "updater");

        File oldDirContentstore = new File(dirRoot, "contentstore");
        File oldDirContentstoreDeleted = new File(dirRoot, "contentstore.deleted");
        File oldDirAuditContentstore = new File(dirRoot, "audit.contentstore");
        File oldDirIndexes = new File(dirRoot, "lucene-indexes");
        File oldDirIndexesBackup = new File(dirRoot, "backup-lucene-indexes");

        if (!dirRootShared.exists()
                && !dirRootLocal.exists()
                && (oldDirContentstore.exists() || oldDirContentstoreDeleted.exists() || oldDirAuditContentstore.exists() || oldDirIndexes.exists() || oldDirIndexesBackup.exists())) {
            LOG.info("Automatically moving directories from dir.root [" + dirRoot.getAbsolutePath() + "] to dir.root.shared [" + dirRootShared.getAbsolutePath()
                    + "] and dir.root.local [" + dirRootLocal.getAbsolutePath() + "]");

            if (dirContentstore.exists() || dirContentstoreDeleted.exists() || dirAuditContentstore.exists() || dirIndexes.exists() || dirIndexesBackup.exists()
                    || !oldDirContentstore.exists() || !oldDirContentstoreDeleted.exists() || !oldDirAuditContentstore.exists() || !oldDirIndexes.exists()
                    || !oldDirIndexesBackup.exists()) {
                throw new RuntimeException(
                        "Automatic moving cannot be performed, either one of old directories does not exist or one of new directories exists. Please move directories manually.");
            }
            if (!dirRootShared.mkdirs()) {
                throw new RuntimeException("Failed to create directory dir.root.shared [" + dirRootShared.getAbsolutePath() + "]");
            }
            if (!dirRootLocal.mkdirs()) {
                throw new RuntimeException("Failed to create directory dir.root.local [" + dirRootLocal.getAbsolutePath() + "]");
            }

            rename(oldDirContentstore, dirContentstore);
            rename(oldDirContentstoreDeleted, dirContentstoreDeleted);
            rename(oldDirAuditContentstore, dirAuditContentstore);
            rename(oldDirIndexes, dirIndexes);
            rename(oldDirIndexesBackup, dirIndexesBackup);

            if (!dirRootUpdater.exists()) {
                if (!dirRootUpdater.mkdirs()) {
                    throw new RuntimeException("Failed to create directory ${dir.root}/updater [" + dirRootUpdater.getAbsolutePath() + "]");
                }
                for (File file : dirRoot.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".csv")) {
                        file.renameTo(new File(dirRootUpdater, file.getName()));
                    }
                }
            }
            LOG.info("Automatic moving completed");
        }
        if (!dirRootUpdater.exists()) {
            if (!dirRootUpdater.mkdirs()) {
                throw new RuntimeException("Failed to create directory ${dir.root}/updater [" + dirRootUpdater.getAbsolutePath() + "]");
            }
        }
    }

    private void rename(File oldDir, File newDir) {
        LOG.info("Renaming " + oldDir.getAbsolutePath() + " --> " + newDir.getAbsolutePath());
        if (!oldDir.renameTo(newDir)) {
            throw new RuntimeException("Failed to rename " + oldDir.getAbsolutePath() + " --> " + newDir.getAbsolutePath()
                    + " -- if source and destination are on different filesystems, you have to move all directories and files manually.");
        }
    }

    public void setDirRoot(String dirRoot) {
        this.dirRoot = new File(dirRoot);
    }

    public void setDirRootShared(String dirRootShared) {
        this.dirRootShared = new File(dirRootShared);
    }

    public void setDirRootLocal(String dirRootLocal) {
        this.dirRootLocal = new File(dirRootLocal);
    }

    public void setDirContentstore(String dirContentstore) {
        this.dirContentstore = new File(dirContentstore);
    }

    public void setDirContentstoreDeleted(String dirContentstoreDeleted) {
        this.dirContentstoreDeleted = new File(dirContentstoreDeleted);
    }

    public void setDirAuditContentstore(String dirAuditContentstore) {
        this.dirAuditContentstore = new File(dirAuditContentstore);
    }

    public void setDirIndexes(String dirIndexes) {
        this.dirIndexes = new File(dirIndexes);
    }

    public void setDirIndexesBackup(String dirIndexesBackup) {
        this.dirIndexesBackup = new File(dirIndexesBackup);
    }

}
