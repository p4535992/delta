package ee.webmedia.alfresco.document.search.web;

import ee.webmedia.alfresco.document.log.web.LogBlockBean;

public interface BlockBeanProviderProvider {

    public AbstractSearchBlockBean getSearch();

    public LogBlockBean getLog();
}
