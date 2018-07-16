/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;


/**
 * Abstract base class for {@link WebServiceConnection} implementations.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public abstract class AbstractWebServiceConnection implements WebServiceConnection {

    private TransportInputStream tis;

    private TransportOutputStream tos;

    private boolean closed = false;

    public final void send(WebServiceMessage message) throws IOException {
        checkClosed();
        onSendBeforeWrite(message);
        tos = createTransportOutputStream();
        if (tos == null) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        if(baos.toString().contains("<id:serviceCode>receiveDocuments</id:serviceCode>")){
        	fixReciveDocuments(baos);
        }else if(baos.toString().contains("<id:serviceCode>markDocumentsReceived</id:serviceCode>")){
        	fixMarkDocumentsReceived(baos);
        }else if(baos.toString().contains("<id:serviceCode>getSendingOptions</id:serviceCode>")){
        	fixGetSendingOptions(baos);
        }else if(baos.toString().contains("<id:serviceCode>sendDocuments</id:serviceCode>")){
        	fixSendDocuments(baos);
        }
        else{
        	message.writeTo(tos);
        }
        tos.flush();
        onSendAfterWrite(message);
    }
    
    private void fixReciveDocuments(ByteArrayOutputStream baos) throws IOException{
    	String msg = baos.toString().replace("</ns5:receiveDocuments></ns5:receiveDocuments>", "</ns5:receiveDocuments>");
        msg = msg.replace("<ns5:receiveDocuments xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\"><ns5:receiveDocuments>",
        		"<ns5:receiveDocuments xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\">");     
	    tos.addHeader("Accept", "text/xml, text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
	    tos.addHeader("SOAPAction", "");
	    tos.addHeader(TransportConstants.HEADER_CONTENT_TYPE, "text/xml");
	    tos.addHeader("Content-Length", Integer.toString(baos.size()));
	    tos.write(msg.getBytes("UTF-8"));
    }
    
    private void fixGetSendingOptions(ByteArrayOutputStream baos) throws IOException{
    	String msg = baos.toString().replace("</ns5:getSendingOptions></ns5:getSendingOptions>", "</ns5:getSendingOptions>");
        msg = msg.replace("<ns5:getSendingOptions xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\"><ns5:getSendingOptions>",
        		"<ns5:getSendingOptions xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\">");       
	    tos.addHeader("Accept", "text/xml, text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
	    tos.addHeader("SOAPAction", "");
	    tos.addHeader(TransportConstants.HEADER_CONTENT_TYPE, "text/xml");
	    tos.addHeader("Content-Length", Integer.toString(baos.size()));
	    tos.write(msg.getBytes("UTF-8"));
    }
    
    private void fixMarkDocumentsReceived(ByteArrayOutputStream baos) throws IOException{
    	String msg = baos.toString();
    	msg = msg.replace("</ns5:markDocumentsReceived></ns5:markDocumentsReceived>", "</ns5:markDocumentsReceived>");
        msg = msg.replace("<ns5:markDocumentsReceived xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\"><ns5:markDocumentsReceived>",
        		"<ns5:markDocumentsReceived xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\">");
        tos.addHeader("Accept", "text/xml, text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
	    tos.addHeader("SOAPAction", "");
        String contentTypeMultipart =  String.format("\"%s\"", msg.substring(2, msg.indexOf("\r")));
        tos.addHeader(TransportConstants.HEADER_CONTENT_TYPE, "multipart/related; type=\"text/xml\"; boundary=" + contentTypeMultipart);
	    tos.write(msg.getBytes("UTF-8"));
    }
    
    private void fixSendDocuments(ByteArrayOutputStream baos) throws IOException{
    	String msg = baos.toString().replace("</ns5:sendDocuments></ns5:sendDocuments>", "</ns5:sendDocuments>");
        msg = msg.replace("<ns5:sendDocuments xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\"><ns5:sendDocuments>",
        		"<ns5:sendDocuments xmlns:ns5=\"http://producers.dhl.xrd.riik.ee/producer/dhl\">");  
	    tos.addHeader("Accept", "text/xml, text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
	    tos.addHeader("SOAPAction", "");
	    String contentTypeMultipart =  String.format("\"%s\"", msg.substring(2, msg.indexOf("\r")));
        tos.addHeader(TransportConstants.HEADER_CONTENT_TYPE, "multipart/related; type=\"text/xml\"; boundary=" + contentTypeMultipart);
	    tos.write(msg.getBytes("UTF-8"));
    }
    
    /**
     * Called before the given message has been written to the <code>TransportOutputStream</code>. Called from {@link
     * #send(WebServiceMessage)}.
     * <p/>
     * Default implementation does nothing.
     *
     * @param message the message
     * @throws IOException when an I/O exception occurs
     */
    protected void onSendBeforeWrite(WebServiceMessage message) throws IOException {
    }

    /**
     * Returns a <code>TransportOutputStream</code> for the given message. Called from {@link
     * #send(WebServiceMessage)}.
     *
     * @return the output stream
     * @throws IOException when an I/O exception occurs
     */
    protected abstract TransportOutputStream createTransportOutputStream() throws IOException;

    /**
     * Called after the given message has been written to the <code>TransportOutputStream</code>. Called from {@link
     * #send(WebServiceMessage)}.
     * <p/>
     * Default implementation does nothing.
     *
     * @param message the message
     * @throws IOException when an I/O exception occurs
     */
    protected void onSendAfterWrite(WebServiceMessage message) throws IOException {
    }

    public final WebServiceMessage receive(WebServiceMessageFactory messageFactory) throws IOException {
        checkClosed();
        onReceiveBeforeRead();
        tis = createTransportInputStream();
        if (tis == null) {
            return null;
        }
        WebServiceMessage message = messageFactory.createWebServiceMessage(tis);
        onReceiveAfterRead(message);
        return message;
    }

    /**
     * Called before a message has been read from the <code>TransportInputStream</code>. Called from {@link
     * #receive(WebServiceMessageFactory)}.
     * <p/>
     * Default implementation does nothing.
     *
     * @throws IOException when an I/O exception occurs
     */
    protected void onReceiveBeforeRead() throws IOException {
    }

    /**
     * Returns a <code>TransportInputStream</code>. Called from {@link #receive(WebServiceMessageFactory)}.
     *
     * @return the input stream, or <code>null</code> if no response can be read
     * @throws IOException when an I/O exception occurs
     */
    protected abstract TransportInputStream createTransportInputStream() throws IOException;

    /**
     * Called when the given message has been read from the <code>TransportInputStream</code>. Called from {@link
     * #receive(WebServiceMessageFactory)}.
     * <p/>
     * Default implementation does nothing.
     *
     * @param message the message
     * @throws IOException when an I/O exception occurs
     */
    protected void onReceiveAfterRead(WebServiceMessage message) throws IOException {
    }

    public final void close() throws IOException {
        IOException ioex = null;
        if (tis != null) {
            try {
                tis.close();
            }
            catch (IOException ex) {
                ioex = ex;
            }
        }
        if (tos != null) {
            try {
                tos.close();
            }
            catch (IOException ex) {
                ioex = ex;
            }
        }
        onClose();
        closed = true;
        if (ioex != null) {
            throw ioex;
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Connection has been closed and cannot be reused.");
        }
    }

    /**
     * Template method invoked from {@link #close()}. Default implementation is empty.
     *
     * @throws IOException if an I/O error occurs when closing this connection
     */
    protected void onClose() throws IOException {
    }

}
