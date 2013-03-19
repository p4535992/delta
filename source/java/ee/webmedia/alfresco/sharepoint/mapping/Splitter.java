package ee.webmedia.alfresco.sharepoint.mapping;

import javax.faces.convert.ConverterException;

public interface Splitter {

    Object[] split(String s) throws ConverterException;
}