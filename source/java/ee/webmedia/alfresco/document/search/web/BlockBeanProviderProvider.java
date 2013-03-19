package ee.webmedia.alfresco.document.search.web;

import ee.webmedia.alfresco.document.log.web.LogBlockBean;

/**
 * @author Riina Tens
 */
public interface BlockBeanProviderProvider {

    public AbstractSearchBlockBean getSearch();

    public LogBlockBean getLog();
}
