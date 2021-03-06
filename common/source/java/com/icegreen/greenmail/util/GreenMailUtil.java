/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
*
*/
package com.icegreen.greenmail.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.icegreen.greenmail.imap.commands.AppendCommand;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 29, 2006 Changed newMimeMessage(String param) for UTF-8 support.
 */
public class GreenMailUtil {
    private static final Log log = LogFactory.getLog(GreenMailUtil.class);

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm-ss-SSSZ");

    private static String messageCopyFolder = null;
    private static boolean saveOriginalToRepo = false;
    public static String SAVE_ORIGINAL_TO_REPO_CONTENT_DATA_HEADER_NAME = "X-Delta-ContentData";

    public static void setMessageCopyFolder(String value) {
        messageCopyFolder = value;
    }

    public static String getMessageCopyFolder() {
        return messageCopyFolder;
    }

    public static boolean isSaveOriginalToRepo() {
        return saveOriginalToRepo;
    }
    
    public static void setSaveOriginalToRepo(boolean saveOriginalToRepo) {
        GreenMailUtil.saveOriginalToRepo = saveOriginalToRepo;
    }

    /**
     * used internally for {@link #random()}
     */
    private static int generateCount = 0;
    private static final String generateSet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPRSTUVWXYZ23456789";
    private static final int generateSetSize = generateSet.length();

    private static GreenMailUtil instance = new GreenMailUtil();
    private GreenMailUtil() {
        //empty
    }

    public static GreenMailUtil instance() {
        return instance;
    }

    /**
     * Writes the content of an input stream to an output stream
     *
     * @throws IOException
     */
    public static void copyStream(final InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = src.read(buffer)) > -1) {
            dest.write(buffer, 0, read);
        }
        dest.flush();
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from an input stream
     */
    public static  MimeMessage newMimeMessage(InputStream inputStream)  {
        try {
            return new MimeMessage(Session.getDefaultInstance(new Properties()), inputStream);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method which creates a new {@link MimeMessage} from a string
     *
     * @throws MessagingException
     */
    public static MimeMessage newMimeMessage(String mailString) throws MessagingException
    {
        try
        {
            // Writer w = new FileWriter("Message_from_GreenMailUtil_" + System.currentTimeMillis() + ".eml");
            // w.write(mailString);
            // w.flush();
            // w.close();
            byte[] bytes = mailString.getBytes("UTF-8");
            MimeMessage message = newMimeMessage(new ByteArrayInputStream(bytes));
            if (StringUtils.isNotBlank(messageCopyFolder)) {
                try {
                    String filename = "ImapMessage-DataCommand-" + dateFormat.format(new Date());
                    File messageFile = new File(messageCopyFolder, filename);
                    FileOutputStream messageOutputStream = new FileOutputStream(messageFile);
                    try {
                        IOUtils.write(bytes, messageOutputStream);
                    } finally {
                        IOUtils.closeQuietly(messageOutputStream);
                    }
                    log.info("Wrote message to file " + messageFile);
                } catch (Exception e) {
                    log.error("Error copying message contents to file", e);
                }
            } else {
                log.info("Not writing message to file, imap.messageFolder is blank");
            }
            if (saveOriginalToRepo) {
                ContentWriter writer = BeanHelper.getContentService().getWriter(null, null, false);
                writer.setMimetype(MimetypeMap.MIMETYPE_RFC822);
                OutputStream os = writer.getContentOutputStream();
                try {
                    IOUtils.write(bytes, os);
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error saving bytes to content store: " + writer.getContentData(), e);
                }
                message.setHeader(SAVE_ORIGINAL_TO_REPO_CONTENT_DATA_HEADER_NAME, writer.getContentData().toString());
            }
            MonitoringUtil.logSuccess(MonitoredService.IN_IMAP);
            return message;
        }
        catch (UnsupportedEncodingException e)
        {
            MonitoringUtil.logError(MonitoredService.IN_IMAP,e);
            throw new RuntimeException(e);
        }
        catch (RuntimeException e)
        {
            MonitoringUtil.logError(MonitoredService.IN_IMAP,e);
            throw e;
        }
        // catch (IOException e)
        // {
        // throw new RuntimeException(e);
        // }
    }

    public static boolean hasNonTextAttachments(Part m) {
        try {
            Object content = m.getContent();
            if (content instanceof MimeMultipart) {
                MimeMultipart mm = (MimeMultipart) content;
                for (int i=0;i<mm.getCount();i++) {
                    BodyPart p = mm.getBodyPart(i);
                    if (hasNonTextAttachments(p)) {
                        return true;
                    }
                }
                return false;
            } else {
                return !m.getContentType().trim().toLowerCase().startsWith("text");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Returns the number of lines in any string
     */
    public static int getLineCount(String str) {
        BufferedReader reader = new BufferedReader(new StringReader(str));
        int ret = 0;
        try {
            while (reader.readLine() != null) {
                ret++;
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The content of an email (or a Part)
     */
    public static String getBody(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return all.substring(i + 4, all.length());
    }

    /**
     * @return The headers of an email (or a Part)
     */
    public static String getHeaders(Part msg) {
        String all = getWholeMessage(msg);
        int i = all.indexOf("\r\n\r\n");
        return all.substring(0, i);
    }

    /**
     * @return The both header and body for an email (or a Part)
     */
    public static String getWholeMessage(Part msg) {
        try {
            ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
            msg.writeTo(bodyOut);
            return bodyOut.toString("US-ASCII").trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getBodyAsBytes(Part msg) {
        return getBody(msg).getBytes();
    }

    public static  byte[] getHeaderAsBytes(Part part) {
        return getHeaders(part).getBytes();
    }

    /**
     * @return same as {@link #getWholeMessage(javax.mail.Part)} }
     */
    public static String toString(Part msg) {
        return getWholeMessage(msg);
    }


    /**
     * Generates a random generated password consisting of letters and digits
     * with a length variable between 5 and 8 characters long.
     * Passwords are further optimized for displays
     * that could potentially display the characters <i>1,l,I,0,O,Q</i> in a way
     * that a human could easily mix them up.
     *
     * @return
     */
    public static String random() {
        Random r = new Random();
        int nbrOfLetters = r.nextInt(3) + 5;
        return random(nbrOfLetters);
    }

    public static String random(int nbrOfLetters) {
        Random r = new Random();
        StringBuffer ret = new StringBuffer();
        for (/* empty */; nbrOfLetters > 0; nbrOfLetters--) {
            int pos = (r.nextInt(generateSetSize) + (++generateCount)) % generateSetSize;
            ret.append(generateSet.charAt(pos));
        }
        return ret.toString();
    }

    public static void sendTextEmailTest(String to, String from, String subject, String msg) {
        try {
            sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTP);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static  void sendTextEmailSecureTest(String to, String from, String subject, String msg) {
        try {
            sendTextEmail(to, from, subject, msg, ServerSetupTest.SMTPS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAddressList(Address[] addresses) {
        if (null == addresses) {
            return null;
        }
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < addresses.length; i++) {
            if (i>0) {
                ret.append(", ");
            }
            ret.append(addresses[i].toString());
        }
        return ret.toString();
    }

    public static void sendTextEmail(String to, String from, String subject, String msg, final ServerSetup setup) {
        try {
            Session session = getSession(setup);

            Address[] tos = new javax.mail.Address[0];
            tos = new InternetAddress[]{new InternetAddress(to)};
            Address[] froms = new InternetAddress[]{new InternetAddress(from)};
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setSubject(subject);
            mimeMessage.setFrom(froms[0]);

            mimeMessage.setText(msg);
            Transport.send(mimeMessage, tos);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Session getSession(final ServerSetup setup) {
        Properties props = new Properties();
        props.put("mail.smtps.starttls.enable", Boolean.TRUE);
        if (setup.isSecure()) {
            props.setProperty("mail.smtp.socketFactory.class", DummySSLSocketFactory.class.getName());
        }
        props.setProperty("mail.transport.protocol", setup.getProtocol());
        props.setProperty("mail.smtp.port", String.valueOf(setup.getPort()));
        props.setProperty("mail.smtps.port", String.valueOf(setup.getPort()));
        Session session = Session.getInstance(props, null);
        return session;
    }

    public static void sendAttachmentEmail(String to, String from, String subject, String msg, final byte[] attachment, final String contentType, final String filename, final String description, final ServerSetup setup) throws MessagingException, IOException {
        Session session = getSession(setup);

        Address[] tos = new InternetAddress[]{new InternetAddress(to)};
        Address[] froms = new InternetAddress[]{new InternetAddress(from)};
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setSubject(subject);
        mimeMessage.setFrom(froms[0]);

        MimeMultipart multiPart = new MimeMultipart();

        MimeBodyPart textPart = new MimeBodyPart();
        multiPart.addBodyPart(textPart);
        textPart.setText(msg);

        MimeBodyPart binaryPart = new MimeBodyPart();
        multiPart.addBodyPart(binaryPart);

        DataSource ds = new DataSource() {
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(attachment);
            }

            public OutputStream getOutputStream() throws IOException {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                byteStream.write(attachment);
                return byteStream;
            }

            public String getContentType() {
                return contentType;
            }

            public String getName() {
                return filename;
            }
        };
        binaryPart.setDataHandler(new DataHandler(ds));
        binaryPart.setFileName(filename);
        binaryPart.setDescription(description);

        mimeMessage.setContent(multiPart);
        Transport.send(mimeMessage, tos);
    }

}
