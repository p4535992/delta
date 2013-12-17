package ee.webmedia.alfresco.document.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.register.model.RegNrHolder;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * @author Keit Tehvan
 */
public class RegistrationNumberReinventedUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateStringNotEmptyQuery(DocumentCommonModel.Props.REG_NUMBER);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        String regNr = (String) nodeService.getProperty(nodeRef, DocumentCommonModel.Props.REG_NUMBER);
        if (StringUtils.isBlank(regNr)) {
            return new String[] { "did nothing" };
        }
        RegNrHolder holder = new RegNrHolder(regNr);
        if (StringUtils.isNotBlank(holder.getShortRegNrWithoutIndividualizingNr())) {
            nodeService.setProperty(nodeRef, DocumentCommonModel.Props.SHORT_REG_NUMBER, holder.getShortRegNrWithoutIndividualizingNr());
        }
        if (holder.getIndividualizingNr() != null && holder.getIndividualizingNr().intValue() > 1) {
            nodeService.setProperty(nodeRef, DocumentCommonModel.Props.INDIVIDUAL_NUMBER, holder.getIndividualizingNr().intValue() + "");
        }
        return new String[] { regNr, holder.getShortRegNrWithoutIndividualizingNr(), holder.getIndividualizingNr() + "" };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "regNr", "short", "individual" };
    }
}
