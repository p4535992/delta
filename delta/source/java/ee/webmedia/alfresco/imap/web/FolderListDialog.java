package ee.webmedia.alfresco.imap.web;

import java.util.List;

public interface FolderListDialog {

    String PARAM_PARENT_NODEREF = "parentNodeRef";

    public boolean isShowFolderList();

    public List<ImapFolder> getFolders();

    public String getFolderListTitle();

}
