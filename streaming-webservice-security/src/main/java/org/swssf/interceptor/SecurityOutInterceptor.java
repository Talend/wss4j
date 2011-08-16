/**
 * Copyright 2010, 2011 Marc Giger
 *
 * This file is part of the streaming-webservice-security-framework (swssf).
 *
 * The streaming-webservice-security-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The streaming-webservice-security-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the streaming-webservice-security-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.swssf.interceptor;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.swssf.WSSec;
import org.swssf.ext.OutboundWSSec;
import org.swssf.ext.SecurityProperties;
import org.swssf.ext.WSSecurityException;
import org.swssf.securityEvent.SecurityEvent;
import org.swssf.securityEvent.SecurityEventListener;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class SecurityOutInterceptor extends AbstractSoapInterceptor {

    public static final SecurityOutInterceptorEndingInterceptor ENDING = new SecurityOutInterceptorEndingInterceptor();
    public static final String OUTPUT_STREAM_HOLDER = SecurityOutInterceptor.class.getName() + ".outputstream";
    public static final String FORCE_START_DOCUMENT = "org.apache.cxf.stax.force-start-document";
    private OutboundWSSec outboundWSSec;

    public SecurityOutInterceptor(String p, SecurityProperties securityProperties) throws Exception {
        super(p);
        getBefore().add(StaxOutInterceptor.class.getName());

        outboundWSSec = WSSec.getOutboundWSSec(securityProperties);
    }

    public void handleMessage(SoapMessage soapMessage) throws Fault {

        OutputStream os = soapMessage.getContent(OutputStream.class);

        String encoding = getEncoding(soapMessage);

        final List<SecurityEvent> outgoingSecurityEventList = new ArrayList<SecurityEvent>();
        SecurityEventListener securityEventListener = new SecurityEventListener() {
            public void registerSecurityEvent(SecurityEvent securityEvent) throws WSSecurityException {
                outgoingSecurityEventList.add(securityEvent);
            }
        };
        soapMessage.getExchange().put(SecurityEvent.class.getName() + ".out", outgoingSecurityEventList);

        XMLStreamWriter newXMLStreamWriter;
        try {
            @SuppressWarnings("unchecked")
            final List<SecurityEvent> requestSecurityEvents = (List<SecurityEvent>) soapMessage.getExchange().get(SecurityEvent.class.getName() + ".in");
            newXMLStreamWriter = outboundWSSec.processOutMessage(os, encoding, requestSecurityEvents, securityEventListener);
            soapMessage.setContent(XMLStreamWriter.class, newXMLStreamWriter);
        } catch (WSSecurityException e) {
            throw new Fault(e);
        }

        soapMessage.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                Boolean.TRUE);
        soapMessage.put(FORCE_START_DOCUMENT, Boolean.TRUE);

        if (MessageUtils.getContextualBoolean(soapMessage, FORCE_START_DOCUMENT, false)) {
            try {
                newXMLStreamWriter.writeStartDocument(encoding, "1.0");
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
            soapMessage.removeContent(OutputStream.class);
            soapMessage.put(OUTPUT_STREAM_HOLDER, os);
        }

        // Add a final interceptor to write end elements
        soapMessage.getInterceptorChain().add(ENDING);
    }

    private String getEncoding(Message message) {
        Exchange ex = message.getExchange();
        String encoding = (String) message.get(Message.ENCODING);
        if (encoding == null && ex.getInMessage() != null) {
            encoding = (String) ex.getInMessage().get(Message.ENCODING);
            message.put(Message.ENCODING, encoding);
        }

        if (encoding == null) {
            encoding = "UTF-8";
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
    }

    public static class SecurityOutInterceptorEndingInterceptor extends AbstractPhaseInterceptor<Message> {

        public SecurityOutInterceptorEndingInterceptor() {
            super(Phase.PRE_STREAM_ENDING);
            getAfter().add(AttachmentOutInterceptor.AttachmentOutEndingInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            try {
                XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
                if (xtw != null) {
                    xtw.writeEndDocument();
                    xtw.flush();
                    xtw.close();
                }

                OutputStream os = (OutputStream) message.get(OUTPUT_STREAM_HOLDER);
                if (os != null) {
                    message.setContent(OutputStream.class, os);
                }
                message.removeContent(XMLStreamWriter.class);
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }
    }
}