/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package wssec;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.apache.ws.security.message.WSSecHeader;
import org.w3c.dom.Document;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * This is a test for processing a Username Token to enforce either a plaintext or digest
 * password type. See WSS-255.
 */
public class PasswordTypeTest extends TestCase implements CallbackHandler {
    private static final Log LOG = LogFactory.getLog(PasswordTypeTest.class);
    private static final String SOAPMSG = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
        + "<SOAP-ENV:Envelope "
        +   "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        +   "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
        +   "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" 
        +   "<SOAP-ENV:Body>" 
        +       "<add xmlns=\"http://ws.apache.org/counter/counter_port_type\">" 
        +           "<value xmlns=\"\">15</value>" 
        +       "</add>" 
        +   "</SOAP-ENV:Body>" 
        + "</SOAP-ENV:Envelope>";

    /**
     * TestWSSecurity constructor
     * 
     * @param name name of the test
     */
    public PasswordTypeTest(String name) {
        super(name);
    }

    /**
     * JUnit suite
     * 
     * @return a junit test suite
     */
    public static Test suite() {
        return new TestSuite(PasswordTypeTest.class);
    }


    /**
     * Test that adds a UserNameToken with password Digest to a WS-Security envelope
     */
    public void testPasswordDigest() throws Exception {
        WSSecUsernameToken builder = new WSSecUsernameToken();
        builder.setUserInfo("wernerd", "verySecret");
        Document doc = SOAPUtil.toSOAPPart(SOAPMSG);
        WSSecHeader secHeader = new WSSecHeader();
        secHeader.insertSecurityHeader(doc);
        Document signedDoc = builder.build(doc, secHeader);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Message with UserNameToken PW Digest:");
            String outputString = 
                org.apache.ws.security.util.XMLUtils.PrettyDocumentToString(signedDoc);
            LOG.debug(outputString);
        }
        WSSecurityEngine secEngine = new WSSecurityEngine();
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        
        //
        // It should pass with PASSWORD_DIGEST
        //
        wssConfig.setRequiredPasswordType(WSConstants.PASSWORD_DIGEST);
        secEngine.setWssConfig(wssConfig);
        secEngine.processSecurityHeader(doc, null, this, null);
        
        //
        // It should pass with null
        //
        wssConfig.setRequiredPasswordType(null);
        secEngine.setWssConfig(wssConfig);
        secEngine.processSecurityHeader(doc, null, this, null);
        
        //
        // It should fail with PASSWORD_TEXT
        //
        try {
            wssConfig.setRequiredPasswordType(WSConstants.PASSWORD_TEXT);
            secEngine.setWssConfig(wssConfig);
            secEngine.processSecurityHeader(doc, null, this, null);
            fail("Expected failure on the wrong password type");
        } catch (WSSecurityException ex) {
            assertTrue(ex.getErrorCode() == WSSecurityException.FAILED_AUTHENTICATION);
            // expected
        }
    }
    
    /**
     * Test that adds a UserNameToken with password text to a WS-Security envelope
     */
    public void testUsernameTokenText() throws Exception {
        WSSecUsernameToken builder = new WSSecUsernameToken();
        builder.setPasswordType(WSConstants.PASSWORD_TEXT);
        builder.setUserInfo("wernerd", "verySecret");
        Document doc = SOAPUtil.toSOAPPart(SOAPMSG);
        WSSecHeader secHeader = new WSSecHeader();
        secHeader.insertSecurityHeader(doc);
        Document signedDoc = builder.build(doc, secHeader);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Message with UserNameToken PW Text:");
            String outputString = 
                org.apache.ws.security.util.XMLUtils.PrettyDocumentToString(signedDoc);
            LOG.debug(outputString);
        }
        WSSecurityEngine secEngine = new WSSecurityEngine();
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        
        //
        // It should pass with PASSWORD_TEXT
        //
        wssConfig.setRequiredPasswordType(WSConstants.PASSWORD_TEXT);
        secEngine.setWssConfig(wssConfig);
        secEngine.processSecurityHeader(doc, null, this, null);
        
        //
        // It should pass with null
        //
        wssConfig.setRequiredPasswordType(null);
        secEngine.setWssConfig(wssConfig);
        secEngine.processSecurityHeader(doc, null, this, null);
        
        //
        // It should fail with PASSWORD_DIGEST
        //
        try {
            wssConfig.setRequiredPasswordType(WSConstants.PASSWORD_DIGEST);
            secEngine.setWssConfig(wssConfig);
            secEngine.processSecurityHeader(doc, null, this, null);
            fail("Expected failure on the wrong password type");
        } catch (WSSecurityException ex) {
            assertTrue(ex.getErrorCode() == WSSecurityException.FAILED_AUTHENTICATION);
            // expected
        }
        
    }
    
    /**
     * Test that adds a UserNameToken via WSHandler
     */
    public void testUsernameTokenWSHandler() throws Exception {
        MyHandler handler = new MyHandler();
        Document doc = SOAPUtil.toSOAPPart(SOAPMSG);
        
        RequestData reqData = new RequestData();
        java.util.Map<String, Object> config = new java.util.TreeMap<String, Object>();
        config.put("password", "verySecret");
        config.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        reqData.setUsername("wernerd");
        reqData.setMsgContext(config);
        
        java.util.List<Integer> actions = new java.util.ArrayList<Integer>();
        actions.add(new Integer(WSConstants.UT));
        
        handler.send(WSConstants.UT, doc, reqData, actions, true);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Username Token via WSHandler");
            String outputString = 
                org.apache.ws.security.util.XMLUtils.PrettyDocumentToString(doc);
            LOG.debug(outputString);
        }
        
        //
        // It should pass even on a different password type, as we haven't set the
        // processing to be strict
        //
        config.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        reqData.setMsgContext(config);
        handler.receive(WSConstants.UT, reqData);
        WSSecurityEngine secEngine = new WSSecurityEngine();
        secEngine.setWssConfig(reqData.getWssConfig());
        secEngine.processSecurityHeader(doc, null, this, null);
        
        //
        // It should fail on strict password type processing
        //
        config.put(WSHandlerConstants.PASSWORD_TYPE_STRICT, "true");
        reqData.setMsgContext(config);
        handler.receive(WSConstants.UT, reqData);
        try {
            secEngine.processSecurityHeader(doc, null, this, null);
            fail("Expected failure on the wrong password type");
        } catch (WSSecurityException ex) {
            // expected
        }
    }
    

    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                if (pc.getUsage() == WSPasswordCallback.USERNAME_TOKEN
                    && "wernerd".equals(pc.getIdentifier())) {
                    pc.setPassword("verySecret");
                } else {
                    throw new IOException("Authentication failed");
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }
}