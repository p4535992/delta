package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;

/**
 * Fix docsub:incomingLetter documents created by adding followUp to docsub:outgoingLetter.
 * Analysis mistake caused copying following properties(and adding aspects) to the followUp incomingLetter:
 * props of aspect recipient: recipientName, recipientEmail
 * props of aspect additionalRecipient: additionalRecipientName, additionalRecipientEmail
 */
public class IncomingLetterRecipientsRemoverUpdater extends AbstractNodeUpdater {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(IncomingLetterRecipientsRemoverUpdater.class);

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentSubtypeModel.Types.INCOMING_LETTER),
                generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                joinQueryPartsOr(Arrays.asList(
                        generateAspectQuery(DocumentCommonModel.Aspects.RECIPIENT),
                        generateAspectQuery(DocumentCommonModel.Aspects.ADDITIONAL_RECIPIENT)
                        ))
                ));
        return Arrays.asList(
                searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own ContentModel PROP_MODIFIER and PROP_MODIFIED values
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "documentRef", "removed *recipient*" };
    }

    @Override
    protected String[] updateNode(final NodeRef docRef) throws Exception {
        LOG.debug("starting to remove *recipient* props and aspects of incomingLetter: " + docRef);

        nodeService.removeAspect(docRef, DocumentCommonModel.Aspects.RECIPIENT);
        nodeService.removeAspect(docRef, DocumentCommonModel.Aspects.ADDITIONAL_RECIPIENT);

        return new String[] { "true" };
    }

}
