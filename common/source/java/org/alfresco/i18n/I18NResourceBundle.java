package org.alfresco.i18n;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import sun.util.ResourceBundleEnumeration;

import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

public class I18NResourceBundle extends ResourceBundle {

    private final Map<String, String> translations;

    public I18NResourceBundle(Map<String, String> translations) {
        Assert.notNull(translations);
        this.translations = translations;
    }

    @Override
    protected Object handleGetObject(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String result = translations.get(key);
        if (StringUtils.isBlank(result)) {
            return "$$" + key + "$$";
        }

        return result;
    }

    @Override
    public Enumeration<String> getKeys() {
        return new ResourceBundleEnumeration(translations.keySet(), null);
    }
}
