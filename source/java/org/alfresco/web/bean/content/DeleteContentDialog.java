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
package org.alfresco.web.bean.content;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.ml.MultilingualContentService;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Bean implementation for the "Delete Content" dialog
 *
 * @author gavinc
 */
public class DeleteContentDialog extends BaseDialogBean
{
    private static final long serialVersionUID = 4199496011879649213L;

    transient private MultilingualContentService multilingualContentService;

    private static final Log logger = LogFactory.getLog(DeleteContentDialog.class);

    private static final String MSG_DELETE = "delete";

    // ------------------------------------------------------------------------------
    // Dialog implementation

    @Override
    public String getFinishButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_DELETE);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome)
    throws Exception
    {
        // get the content to delete
        Node node = getNodeToDelete();
        if (node != null)
        {
            if(ContentModel.TYPE_MULTILINGUAL_CONTAINER.equals(node.getType()))
            {
                if (logger.isDebugEnabled()) {
                    logger.debug("Trying to delete multilingual container: " + node.getId() + " and its translations" );
                }

                // delete the mlContainer and its translations
                getMultilingualContentService().deleteTranslationContainer(node.getNodeRef());
            }
            else
            {
                if (logger.isDebugEnabled()) {
                    logger.debug("Trying to delete content node: " + node.getId());
                }

                // delete the node
                getNodeService().deleteNode(node.getNodeRef());
            }

        }
        else
        {
            logger.warn("WARNING: delete called without a current Document!");
        }

        return outcome;
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome)
    {
        // clear action context
        browseBean.setDocument(null);

        // setting the outcome will show the browse view again
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME +
        AlfrescoNavigationHandler.OUTCOME_SEPARATOR + "browse";
    }

    @Override
    protected String getErrorMessageId()
    {
        return "error_delete_file";
    }

    @Override
    public boolean getFinishButtonDisabled()
    {
        return false;
    }

    // ------------------------------------------------------------------------------
    // Bean Getters and Setters

    /**
     * Returns the confirmation to display to the user before deleting the content.
     *
     * @return The formatted message to display
     */
    public String getConfirmMessage()
    {
        final Pair<String, Object[]> pair = getConfirmMessageKeyAndPlaceholders();
        return MessageUtil.getMessage(pair.getFirst(), pair.getSecond());
    }

    protected Pair<String, Object[]> getConfirmMessageKeyAndPlaceholders() {
        String fileConfirmMsg = null;

        Node document = getNodeToDelete();
        String documentName = document.getName();
        String displayName = (String) getNodeService().getProperty(document.getNodeRef(), FileModel.Props.DISPLAY_NAME);
        documentName = (displayName == null) ? documentName : displayName;

        if(document.getType().equals(ContentModel.TYPE_MULTILINGUAL_CONTAINER))
        {
            fileConfirmMsg = "delete_ml_container_confirm";
        }
        else if(document.hasAspect(ContentModel.ASPECT_MULTILINGUAL_EMPTY_TRANSLATION))
        {
            fileConfirmMsg = "delete_empty_translation_confirm";
        }
        else if(document.hasAspect(ContentModel.ASPECT_MULTILINGUAL_DOCUMENT))
        {
            fileConfirmMsg = "delete_translation_confirm";

            // XXX: could decouple getting confirmation message in a generic way, so this class could be moved to common
        } else if (DocumentTypeHelper.isOutgoingLetter(document.getType())) {
            fileConfirmMsg = "imap_delete_outgoing_letter_confirm";
            documentName = (String) document.getProperties().get(DocumentCommonModel.Props.DOC_NAME);
        } else if (DocumentTypeHelper.isIncomingLetter(document.getType())) {
            fileConfirmMsg = "imap_delete_incoming_letter_confirm";
            documentName = (String) document.getProperties().get(DocumentCommonModel.Props.DOC_NAME);
        }
        else
        {
            String strHasMultipleParents = parameters.get("hasMultipleParents");
            if (strHasMultipleParents != null && "true".equals(strHasMultipleParents))
            {
                fileConfirmMsg = "delete_file_multiple_parents_confirm";
            }
            else
            {
                fileConfirmMsg = "delete_file_confirm";
            }
        }
        final Pair<String, Object[]> pair = new Pair<String, Object[]>(fileConfirmMsg, new Object[] {documentName});
        return pair;
    }

    protected Node getNodeToDelete() {
        return browseBean.getDocument();
    }

    @Override
    public String getContainerTitle() {
        String title = getContainerTitleMsgKey();
        if(title!=null) {
            return MessageUtil.getMessage(title);
        }
        Node document = getNodeToDelete();

        // XXX: could decouple getting container title in a generic way, so this class could be moved to common
        if (DocumentTypeHelper.isOutgoingLetter(document.getType())) {
            title = MessageUtil.getMessage("imap_delete_outgoing_letter_title");
        } else if (DocumentTypeHelper.isIncomingLetter(document.getType())) {
            title = MessageUtil.getMessage("imap_delete_incoming_letter_title");
        }
        return title;
    }

    /**
     * Container title for confirm page
     */
    public String getContainerTitleMsgKey() {
        return null;
    }

    /**
     * @param multilingualContentService the Multilingual Content Service to set
     */
    public void setMultilingualContentService(MultilingualContentService multilingualContentService)
    {
        this.multilingualContentService = multilingualContentService;
    }

    protected MultilingualContentService getMultilingualContentService()
    {
        if (multilingualContentService == null)
        {
            multilingualContentService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getMultilingualContentService();
        }
        return multilingualContentService;
    }

}
