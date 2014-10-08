<<<<<<< HEAD
/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.bean.trashcan;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.alfresco.repo.node.archive.RestoreNodeReport;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;

public class TrashcanRecoverListedItemsDialog extends TrashcanDialog
{
    private static final long serialVersionUID = 5500454626559426051L;
    
    private static final String RICHLIST_ID = "trashcan-list";
    private static final String RICHLIST_MSG_ID = "trashcan" + ':' + RICHLIST_ID;
    private static final String OUTCOME_RECOVERY_REPORT = "dialog:close";
    private static final String MSG_NO = "revert";
    private static final String MSG_YES = "recover_listed_items";
    private static final String MSG_RECOVER_ITEMS = "recover_listed_items_title";

    private String recoverListedItems(FacesContext context, String outcome)
    {
        if (property.isInProgress())
            return null;

        property.setInProgress(true);

        try
        {

            // restore the nodes - the user may have requested a restore to a
            // different parent
            List<NodeRef> nodeRefs = new ArrayList<NodeRef>(property.getListedItems().size());
            for (Node node : property.getListedItems())
            {
                nodeRefs.add(node.getNodeRef());
            }
            List<RestoreNodeReport> reports;
            if (property.getDestination() == null)
            {
                reports = property.getNodeArchiveService().restoreArchivedNodes(nodeRefs);
            }
            else
            {
                reports = property.getNodeArchiveService().restoreArchivedNodes(nodeRefs, property.getDestination(), null, null);
            }

            saveReportDetail(reports);
            String msg = Application.getMessage(context, "recovered_items_success");
            FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg);
            context.addMessage(RICHLIST_MSG_ID, facesMsg);
        }
        finally
        {
            property.setInProgress(false);
        }

        return OUTCOME_RECOVERY_REPORT;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception
    {

        return recoverListedItems(context, outcome);

    }

    @Override
    public String getCancelButtonLabel()
    {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_NO);
    }

    @Override
    public boolean getFinishButtonDisabled()
    {
        return false;
    }

    @Override
    public String getFinishButtonLabel()
    {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_YES);
    }
    
    @Override
    public String getContainerTitle()
    {
        
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_RECOVER_ITEMS);
    }

}
=======
/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.bean.trashcan;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.alfresco.repo.node.archive.RestoreNodeReport;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;

public class TrashcanRecoverListedItemsDialog extends TrashcanDialog
{
    private static final long serialVersionUID = 5500454626559426051L;
    
    private static final String RICHLIST_ID = "trashcan-list";
    private static final String RICHLIST_MSG_ID = "trashcan" + ':' + RICHLIST_ID;
    private static final String OUTCOME_RECOVERY_REPORT = "dialog:close";
    private static final String MSG_NO = "revert";
    private static final String MSG_YES = "recover_listed_items";
    private static final String MSG_RECOVER_ITEMS = "recover_listed_items_title";

    private String recoverListedItems(FacesContext context, String outcome)
    {
        if (property.isInProgress())
            return null;

        property.setInProgress(true);

        try
        {

            // restore the nodes - the user may have requested a restore to a
            // different parent
            List<NodeRef> nodeRefs = new ArrayList<NodeRef>(property.getListedItems().size());
            for (Node node : property.getListedItems())
            {
                nodeRefs.add(node.getNodeRef());
            }
            List<RestoreNodeReport> reports;
            if (property.getDestination() == null)
            {
                reports = property.getNodeArchiveService().restoreArchivedNodes(nodeRefs);
            }
            else
            {
                reports = property.getNodeArchiveService().restoreArchivedNodes(nodeRefs, property.getDestination(), null, null);
            }

            saveReportDetail(reports);
            String msg = Application.getMessage(context, "recovered_items_success");
            FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg);
            context.addMessage(RICHLIST_MSG_ID, facesMsg);
        }
        finally
        {
            property.setInProgress(false);
        }

        return OUTCOME_RECOVERY_REPORT;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception
    {

        return recoverListedItems(context, outcome);

    }

    @Override
    public String getCancelButtonLabel()
    {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_NO);
    }

    @Override
    public boolean getFinishButtonDisabled()
    {
        return false;
    }

    @Override
    public String getFinishButtonLabel()
    {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_YES);
    }
    
    @Override
    public String getContainerTitle()
    {
        
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_RECOVER_ITEMS);
    }

}
>>>>>>> develop-5.1
