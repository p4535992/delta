package ee.webmedia.alfresco.utils;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

/**
 * @author Alar Kvell
 */
public class MimeUtil {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MimeUtil.class);

    public static String getContentType(ContentReader reader) {
        return getContentType(reader.getMimetype(), reader.getEncoding());
    }

    public static String getContentType(String mimeType, String encoding) {
        String contentType;
        try {
            ContentType ct = new ContentType(mimeType);
            ct.setParameter("charset", encoding);
            contentType = ct.toString();
            if (contentType == null) {
                log.warn("ContentType conversion failed\n  mimeType=" + mimeType + "\n  encoding=" + encoding);
                contentType = mimeType;
            }
        } catch (ParseException e) {
            log.warn("ContentType parsing failed\n  mimeType=" + mimeType + "\n  encoding=" + encoding, e);
            contentType = mimeType;
        }
        return contentType;
    }

    public static Pair<String, String> getMimeTypeAndEncoding(String inputContentType) throws ParseException {
        ContentType contentType = new ContentType(inputContentType);
        String mimeType = contentType.getBaseType().toLowerCase();
        String encoding = contentType.getParameter("charset");
        if (StringUtils.isBlank(encoding)) {
            encoding = "UTF-8";
        }
        return new Pair<String, String>(mimeType, encoding);
    }

    public static boolean isPdf(String mimeType) {
        return MimetypeMap.MIMETYPE_PDF.equals(mimeType);
    }

}
