package ee.webmedia.alfresco.docdynamic.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fix for 204783. Removes all non-numeric characters from document shortRegNumber property value and makes the same substitution in regNumber property value.
 */
public class InvalidShortRegNumberUpdater extends AbstractNodeUpdater {

    private boolean writeValues = false;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String alphabet = "abcdefghijklmnopqrsšzžtuvwõäöüxy";
        List<String> letterParts = new ArrayList<String>();
        for (int i = 0; i < alphabet.length(); i++) {
            letterParts.add(SearchUtil.generatePropertyWildcardQuery(DocumentCommonModel.Props.SHORT_REG_NUMBER, alphabet.substring(i, i + 1), true, true));
        }
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.joinQueryPartsOr(letterParts));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        String shortRegNumber = (String) props.get(DocumentCommonModel.Props.SHORT_REG_NUMBER);
        if (StringUtils.isEmpty(shortRegNumber) || StringUtils.isNumeric(shortRegNumber)) {
            return new String[] { "shortRegNumberIsEmptyOrNumeric", regNumber, shortRegNumber };
        }

        String newShortRegNumber = "";
        for (int i = 0; i < shortRegNumber.length(); i++) {
            char c = shortRegNumber.charAt(i);
            if (Character.isDigit(c)) {
                newShortRegNumber += c;
            }
        }
        if (StringUtils.isEmpty(shortRegNumber)) {
            return new String[] { "shortRegNumberDoesNotHaveDigits", regNumber, shortRegNumber };
        }
        if (shortRegNumber.indexOf(newShortRegNumber) < 0) {
            return new String[] { "shortRegNumberHasNonConsecutiveDigits", regNumber, shortRegNumber };
        }
        StringBuilder s = new StringBuilder(regNumber);
        int i = regNumber.lastIndexOf(shortRegNumber);
        if (i < 0) {
            return new String[] { "regNumberDoesNotContainShortRegNumber", regNumber, shortRegNumber };
        }
        s.replace(i, i + shortRegNumber.length(), newShortRegNumber);
        String newRegNumber = s.toString();

        if (writeValues) {
            HashMap<QName, Serializable> newProps = new HashMap<QName, Serializable>();
            newProps.put(DocumentCommonModel.Props.REG_NUMBER, newRegNumber);
            newProps.put(DocumentCommonModel.Props.SHORT_REG_NUMBER, newShortRegNumber);
            nodeService.addProperties(nodeRef, newProps);
        }

        return new String[] {
                regNumber.substring(0, i).indexOf(shortRegNumber) < 0 ? "fixed" : "fixedButManualReviewRecommended",
                regNumber,
                shortRegNumber,
                newRegNumber,
                newShortRegNumber };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "action", "oldRegNumber", "oldShortRegNumber", "newRegNumber", "newShortRegNumber" };
    }

    public boolean isWriteValues() {
        return writeValues;
    }

    public void setWriteValues(boolean writeValues) {
        this.writeValues = writeValues;
    }

}
