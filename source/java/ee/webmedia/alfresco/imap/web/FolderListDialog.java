package ee.webmedia.alfresco.imap.web;

import java.util.List;

import ee.webmedia.alfresco.document.file.web.Subfolder;

public interface FolderListDialog {

    public boolean isShowFolderList();

    public List<Subfolder> getFolders();

    public String getFolderListTitle();

}
