package ee.webmedia.mobile.alfresco.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mobile.device.DeviceHandlerMethodArgumentResolver;
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import ee.webmedia.mobile.alfresco.common.ApplicationContextProvider;
import ee.webmedia.mobile.alfresco.common.holder.UserRequestInfo;
import ee.webmedia.mobile.alfresco.workflow.SigningFlowHolder;
import ee.webmedia.mobile.alfresco.workflow.model.StringToNodeRefConverter;

/**
 * Java based Spring configuration for mDelta
 */
@Configuration
@EnableWebMvc
public class MobileDeltaConfig extends WebMvcConfigurerAdapter {

    public static final String MESSAGE_SOURCE = "messageSource";
    private static final Locale DEFAULT_LOCALE = new Locale("et", "EE");

    public static Locale getLocale() {
        return DEFAULT_LOCALE;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/js/**").addResourceLocations("/mobile/js/").setCachePeriod(getDefaultCachePeriod());
        registry.addResourceHandler("/css/**").addResourceLocations("/mobile/css/").setCachePeriod(getDefaultCachePeriod());
        registry.addResourceHandler("/gfx/**").addResourceLocations("/mobile/gfx/").setCachePeriod(getDefaultCachePeriod());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(deviceResolverHandlerInterceptor());
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(deviceHandlerMethodArgumentResolver());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        converters.add(jacksonConverter());
    }

    @Bean
    MappingJackson2HttpMessageConverter jacksonConverter() {
        return new MappingJackson2HttpMessageConverter();
    }

    // ////////////////
    // Bean definitions
    // ////////////////

    @Bean
    public ApplicationContextProvider applicationContextProvider() {
        return new ApplicationContextProvider();
    }

    @Bean
    public DeviceResolverHandlerInterceptor deviceResolverHandlerInterceptor() {
        return new DeviceResolverHandlerInterceptor();
    }

    @Bean
    public DeviceHandlerMethodArgumentResolver deviceHandlerMethodArgumentResolver() {
        return new DeviceHandlerMethodArgumentResolver();
    }

    @Bean
    public InternalResourceViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/jsp/mobile/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToNodeRefConverter());
    }

    @Bean(name = SigningFlowHolder.BEAN_NAME)
    public SigningFlowHolder sessionSigningFlowHolder() {
        return new SigningFlowHolder();
    }

    @Bean(name = { MESSAGE_SOURCE })
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // Target only mobile translations
        // FIXME We should provide override for browser locale in web-app.
        messageSource.setBasenames("WEB-INF/classes/ee/webmedia/alfresco/document/web/document-webclient", "WEB-INF/classes/ee/webmedia/alfresco/workflow/web/workflow-webclient", "WEB-INF/classes/ee/webmedia/mobile/alfresco/message/messages");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserRequestInfo userRequestInfo() {
        return new UserRequestInfo();
    }

    private int getDefaultCachePeriod() {
        return 300;
    }

}
