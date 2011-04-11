package ee.webmedia.alfresco.simdhs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Export JSF lists as CSV file. Data is read using {@link ee.webmedia.alfresco.simdhs.DataReader}.
 * Generally {@link ee.webmedia.alfresco.simdhs.RichListDataReader} can be used to export data directly from rich list.
 * <p/>
 * Only {@link org.alfresco.web.ui.common.component.data.UIRichList} is supported for now, additional support must be added as necessity arises.
 * <p/>
 * If custom ordering must be done with data, then {@link CSVExporter#setOrderInfo(int, boolean)} must be used. By default no ordering of data is done.
 * <p/>
 * File is written in UTF-8 encoding with BOM, ; is used as separator.-
 * 
 * @author Romet Aidla
 */
public class CSVExporter {
    private static Logger log = Logger.getLogger(CSVExporter.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final char CSV_QUOTE = '"';
    protected static final String CHARSET = AppConstants.CHARSET;
    protected static final char SEPARATOR = ';';
    private static final char[] CSV_SEARCH_CHARS = new char[] { SEPARATOR, CSV_QUOTE, CharUtils.CR, CharUtils.LF };

    protected DataReader dataReader;

    protected boolean doOrdering = false;
    private int sortColumnNr = 0;
    private boolean isDescending = false;

    public CSVExporter(DataReader dataReader) {
        this.dataReader = dataReader;
    }

    public void export(String tableName) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Starting to export CSV file from " + tableName);
                log.debug("Using " + dataReader.getClass());
            }
            FacesContext facesContext = FacesContext.getCurrentInstance();

            UIComponent table = ComponentUtil.findComponentById(facesContext, facesContext.getViewRoot(), tableName);
            Assert.notNull(table, "Component was not found: " + tableName);

            if (!(table instanceof UIRichList)) { // only UIRichList is supported for now
                throw new RuntimeException("Wrong component type for export.");
            }
            UIRichList richList = (UIRichList) table;
            richList.bind();

            // set message bundle (needed for header labels)
            ResourceBundle bundle = Application.getBundle(facesContext);
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
            requestMap.put("msg", new BundleMap(bundle));

            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            response.setCharacterEncoding(CHARSET);
            OutputStream outputStream = response.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, CHARSET);
            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
            writer.write("\ufeff");

            writeRow(writer, dataReader.getHeaderRow(richList, facesContext));
            List<List<String>> dataRows = dataReader.getDataRows(richList, facesContext);
            if (doOrdering) {
                orderRows(dataRows);
            }
            writeData(writer, dataRows);

            response.setContentType("text/csv; charset=" + CHARSET);
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setHeader("Content-disposition", "attachment;filename=export.csv");

            writer.flush();
            writer.close();

            outputStream.flush();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (IOException e) {
            throw new RuntimeException("CSV export failed", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("CSV export successfully completed");
        }
    }

    public void setOrderInfo(int sortColumnNr, boolean isDescending) {
        doOrdering = true;
        this.sortColumnNr = sortColumnNr;
        this.isDescending = isDescending;
    }

    protected void orderRows(List<List<String>> rows) {
        // sort according to first column
        Collections.sort(rows, new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.get(sortColumnNr), o2.get(sortColumnNr)) * (isDescending ? -1 : 1);
            }
        });
    }

    private void writeData(Writer writer, List<List<String>> data) throws IOException {
        for (List<String> row : data) {
            writer.write(LINE_SEPARATOR);
            writeRow(writer, row);
        }
    }

    private void writeRow(Writer writer, List<String> row) throws IOException {
        for (Iterator<String> iterator = row.iterator(); iterator.hasNext();) {
            String val = iterator.next();
            writer.write(escape(val));
            if (iterator.hasNext()) {
                writer.write(SEPARATOR);
            }
        }
    }

    private String escape(String str) {
        StringBuilder result = new StringBuilder();
        if (StringUtils.containsNone(str, CSV_SEARCH_CHARS)) {
            if (str != null) {
                result.append(str);
            }
            return result.toString();
        }
        result.append(CSV_QUOTE);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == CSV_QUOTE) {
                result.append(CSV_QUOTE); // escape double quote
            }
            result.append(c);
        }
        result.append(CSV_QUOTE);
        return result.toString();
    }

    /**
     * Taken from {@link org.alfresco.web.ui.repo.tag.LoadBundleTag}. Needed for loading headed labels.
     */
    @SuppressWarnings("unchecked")
    protected static class BundleMap implements Map {
        private final ResourceBundle _bundle;
        private List<String> _values;

        public BundleMap(ResourceBundle bundle) {
            _bundle = bundle;
        }

        @Override
        public Object get(Object key) {
            try {
                return _bundle.getObject(key.toString());
            } catch (Exception e) {
                return "$$" + key + "$$";
            }
        }

        @Override
        public boolean isEmpty() {
            return !_bundle.getKeys().hasMoreElements();
        }

        @Override
        public boolean containsKey(Object key) {
            try {
                return _bundle.getObject(key.toString()) != null;
            } catch (MissingResourceException e) {
                return false;
            }
        }

        @Override
        public Collection<String> values() {
            if (_values == null) {
                _values = new ArrayList<String>();
                for (Enumeration<String> enumer = _bundle.getKeys(); enumer.hasMoreElements();) {
                    String v = _bundle.getString(enumer.nextElement());
                    _values.add(v);
                }
            }
            return _values;
        }

        @Override
        public int size() {
            return values().size();
        }

        @Override
        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        @Override
        public Set<Entry> entrySet() {
            Set<Entry> set = new HashSet<Entry>();
            for (Enumeration enumer = _bundle.getKeys(); enumer.hasMoreElements();) {
                final String k = (String) enumer.nextElement();
                set.add(new Map.Entry() {
                    @Override
                    public Object getKey() {
                        return k;
                    }

                    @Override
                    public Object getValue() {
                        return _bundle.getObject(k);
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
                    }
                });
            }
            return set;
        }

        @Override
        public Set keySet() {
            Set set = new HashSet();
            for (Enumeration enumer = _bundle.getKeys(); enumer.hasMoreElements();) {
                set.add(enumer.nextElement());
            }
            return set;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }

        @Override
        public void putAll(Map t) {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }

        @Override
        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }
    }
}
