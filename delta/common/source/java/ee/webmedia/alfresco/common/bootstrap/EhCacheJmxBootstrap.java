package ee.webmedia.alfresco.common.bootstrap;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

public class EhCacheJmxBootstrap implements InitializingBean {
    protected final Log LOG = LogFactory.getLog(getClass());

    private CacheManager cacheManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info("Registering EhCache MBeans to JMX");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ManagementService.registerMBeans(cacheManager, mBeanServer, true, true, true, true);
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

}
