package ee.webmedia.alfresco.functions.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.repo.importer.Importer;
import org.alfresco.repo.importer.view.NodeContext;
import org.alfresco.repo.importer.view.ViewParser;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * ViewParser, that counts documents created/imported under series, volumes and cases.
 * NB! It just ignores missing references if found!
 */
public class DocListImportViewParser extends ViewParser {
    private static final Log logger = LogFactory.getLog(DocListImportViewParser.class);

    private long docsCount;
    private long docsCountOverCases;
    private long docsCountOverVolumes;
    private long docsCountOverFunctions;
    private Date startTime;

    @Override
    public void parse(Reader viewReader, Importer importer) {
        startTime = new Date();
        logger.info("Starting to import");
        resetCounters();
        super.parse(viewReader, importer);
        logger.info("Ending import");
    }

    @Override
    protected void processEndType(ParserContext parserContext, NodeContext node) {
        setContainingDocsCount(node);
        super.processEndType(parserContext, node);
    }

    @Override
    protected void resolveMissingRef(String pathRefAttr) {
        throw new ImporterMissingRefException("Cannot find node referenced by path " + pathRefAttr);
    }

    @Override
    protected void parseDocument(XmlPullParser xpp, ParserContext parserContext) throws XmlPullParserException, IOException {
        long ignoreEndElementCount = 0;
        for (int eventType = xpp.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xpp.next()) {
            switch (eventType) {
            case XmlPullParser.START_TAG: {
                if (xpp.getDepth() == 1) {
                    processRoot(xpp, parserContext);
                } else {
                    if (ignoreEndElementCount == 0) {
                        try {
                            processStartElement(xpp, parserContext);
                        } catch (ImporterMissingRefException e) {
                            if (logger.isInfoEnabled()) {
                                logger.info(ignoreEndElementCount + ". ignoring start element: " + getName(xpp));
                            }
                            ignoreEndElementCount++;
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info(ignoreEndElementCount + ". ignoring start element: " + getName(xpp));
                        }
                        ignoreEndElementCount++;
                    }
                }
                break;
            }
            case XmlPullParser.END_TAG: {
                if (ignoreEndElementCount == 0) {
                    processEndElement(xpp, parserContext);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info(ignoreEndElementCount + ". ignoring end element: " + getName(xpp));
                    }
                    ignoreEndElementCount--;
                }
                break;
            }
            }
        }
    }

    private void setContainingDocsCount(NodeContext node) {
        final QName nodeQName = node.getTypeDefinition().getName();
        if (dictionaryService.isSubClass(nodeQName, DocumentCommonModel.Types.DOCUMENT)) {
            docsCount++;
        } else if (nodeQName.equals(CaseModel.Types.CASE)) {
            docsCountOverCases += docsCount;
            setDocsCount(node, docsCount, CaseModel.Props.CONTAINING_DOCS_COUNT);
            BeanHelper.getCaseService().removeFromCache(node.getNodeRef());
            docsCount = 0;
        } else if (nodeQName.equals(VolumeModel.Types.VOLUME)) {
            Assert.isTrue(docsCount == 0 || docsCountOverCases == 0, "It seems that the volume directly contains both documents(" + docsCount
                    + " documents) and cases(" + docsCountOverCases + " documents over all child-cases), but it should only contain one of them");
            final long currentVolDocCount = docsCount + docsCountOverCases;
            docsCountOverVolumes += currentVolDocCount;
            setDocsCount(node, currentVolDocCount, VolumeModel.Props.CONTAINING_DOCS_COUNT);
            BeanHelper.getVolumeService().removeFromCache(node.getNodeRef());
            docsCountOverCases = 0;
            docsCount = 0;
        } else if (nodeQName.equals(SeriesModel.Types.SERIES)) {
            docsCountOverFunctions += docsCountOverVolumes;
            setDocsCount(node, docsCountOverVolumes, SeriesModel.Props.CONTAINING_DOCS_COUNT);
            BeanHelper.getSeriesService().removeFromCache(node.getNodeRef());
            docsCountOverVolumes = 0;
        } else if (nodeQName.equals(FunctionsModel.Types.FUNCTION)) {
            if (logger.isDebugEnabled()) {
                logger.debug("imported function, that contains " + docsCountOverFunctions + " documents. node=" + node);
            }
        }
    }

    private void setDocsCount(NodeContext node, long docsCount, final QName containingDocsCountQName) {
        final Map<QName, Serializable> properties = node.getProperties();
        if (logger.isInfoEnabled()) {
            final Integer exportedContainingDocsCount = DefaultTypeConverter.INSTANCE.convert(Integer.class, properties.get(containingDocsCountQName));
            if (exportedContainingDocsCount != null && exportedContainingDocsCount != docsCount) {
                final String msg = "exportedContainingDocsCount=" + exportedContainingDocsCount + " not equal to docsCount=" + docsCount + " for node:\n"
                        + node;
                logger.info(msg);
            }
        }
        if (node.getNodeRef() == null) {
            // not yet saved, can just set node property
            properties.put(containingDocsCountQName, docsCount);
        } else {
            // already saved, must set property using nodeservice
            nodeService.setProperty(node.getNodeRef(), containingDocsCountQName, docsCount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(node.getTypeDefinition().getName().getLocalName() + " node contains " + docsCount + " documents. Time from start="
                    + ((new Date().getTime() - startTime.getTime()) / 60000) + " minutes");
        }
    }

    private void resetCounters() {
        docsCount = 0;
        docsCountOverCases = 0;
        docsCountOverVolumes = 0;
        docsCountOverFunctions = 0;
    }

}
