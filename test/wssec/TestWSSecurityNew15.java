/*
 * Copyright  2003-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package wssec;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.AxisClient;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.configuration.NullProvider;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.message.WSSecEncrypt;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.util.Vector;

/**
 * Test that encrypt and decrypt a WS-Security envelope.
 * 
 * This test uses the RSA_15 alogrithm to transport (wrap) the symmetric key.
 * The test case creates a ReferenceList element that references EncryptedData
 * elements. The ReferencesList element is put into the Security header, not
 * as child of the EncryptedKey. The EncryptedData elements contain a KeyInfo
 * that references the EncryptedKey via a STR/Reference structure.
 * 
 * <p/>
 * 
 * Refer to OASIS WS Security spec 1.1, chap 7.7
 * 
 * @author Davanum Srinivas (dims@yahoo.com)
 * @author Werner Dittmann (werner@apache.org)
 */
public class TestWSSecurityNew15 extends TestCase implements CallbackHandler {
    private static Log log = LogFactory.getLog(TestWSSecurityNew15.class);

    static final String soapMsg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "   <soapenv:Body>"
            + "      <ns1:testMethod xmlns:ns1=\"uri:LogTestService2\"></ns1:testMethod>"
            + "   </soapenv:Body>" + "</soapenv:Envelope>";

    static final WSSecurityEngine secEngine = new WSSecurityEngine();

    static final Crypto crypto = CryptoFactory
            .getInstance("cryptoSKI.properties");

    MessageContext msgContext;

    Message message;

    /**
     * TestWSSecurity constructor <p/>
     * 
     * @param name
     *            name of the test
     */
    public TestWSSecurityNew15(String name) {
        super(name);
    }

    /**
     * JUnit suite <p/>
     * 
     * @return a junit test suite
     */
    public static Test suite() {
        return new TestSuite(TestWSSecurityNew15.class);
    }

    /**
     * Main method <p/>
     * 
     * @param args
     *            command line args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Setup method <p/>
     * 
     * @throws Exception
     *             Thrown when there is a problem in setup
     */
    protected void setUp() throws Exception {
        AxisClient tmpEngine = new AxisClient(new NullProvider());
        msgContext = new MessageContext(tmpEngine);
        message = getSOAPMessage();
    }

    /**
     * Constructs a soap envelope <p/>
     * 
     * @return soap envelope
     * @throws Exception
     *             if there is any problem constructing the soap envelope
     */
    protected Message getSOAPMessage() throws Exception {
        InputStream in = new ByteArrayInputStream(soapMsg.getBytes());
        Message msg = new Message(in);
        msg.setMessageContext(msgContext);
        return msg;
    }

    /**
     * Test that encrypt and decrypt a WS-Security envelope.
     * 
     * This test uses the RSA_15 alogrithm to transport (wrap) the symmetric
     * key.
     * 
     * @throws Exception
     *             Thrown when there is any problem in signing or verification
     */
    public void testEncryptionDecryptionRSA15() throws Exception {
        SOAPEnvelope unsignedEnvelope = message.getSOAPEnvelope();
        WSSecEncrypt builder = new WSSecEncrypt();
        builder.setUserInfo("wss4jcert");
        builder.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
        builder.setSymmetricEncAlgorithm(WSConstants.TRIPLE_DES);
        Document doc = unsignedEnvelope.getAsDocument();
        WSSecHeader secHeader = new WSSecHeader();
        secHeader.insertSecurityHeader(doc);
        log.info("Before Encryption Triple DES....");

        /*
         * Prepare the Encrypt object with the token, setup data structure
         */
        builder.prepare(doc, crypto);

        /*
         * Set up the parts structure to encrypt the body
         */
        SOAPConstants soapConstants = WSSecurityUtil.getSOAPConstants(doc
                .getDocumentElement());
        Vector parts = new Vector();
        WSEncryptionPart encP = new WSEncryptionPart(soapConstants
                .getBodyQName().getLocalPart(), soapConstants.getEnvelopeURI(),
                "Content");
        parts.add(encP);

        /*
         * Encrypt the parts (Body), create EncrypedData elements that reference
         * the EncryptedKey, and get a ReferenceList that can be put into the
         * Security header. Be sure that the ReferenceList is after the
         * EncryptedKey element in the Security header (strict layout)
         */
        Element refs = builder.encryptForExternalRef(null, parts);
        builder.addExternalRefElement(refs, secHeader);

        /*
         * now add (prepend) the EncryptedKey element, then a
         * BinarySecurityToken if one was setup during prepare
         */
        builder.prependToHeader(secHeader);

        builder.prependBSTElementToHeader(secHeader);

        Document encryptedDoc = doc;
        log.info("After Encryption Triple DES....");

        /*
         * convert the resulting document into a message first. The
         * toAxisMessage() method performs the necessary c14n call to properly
         * set up the signed document and convert it into a SOAP message. Check
         * that the contents can't be read (checking if we can find a specific
         * substring). After that we extract it as a document again for further
         * processing.
         */

        Message encryptedMsg = SOAPUtil.toAxisMessage(encryptedDoc);
        if (log.isDebugEnabled()) {
            log.debug("Encrypted message, RSA-15 keytransport, 3DES:");
            XMLUtils.PrettyElementToWriter(encryptedMsg.getSOAPEnvelope()
                    .getAsDOM(), new PrintWriter(System.out));
        }
        String encryptedString = encryptedMsg.getSOAPPartAsString();
        assertTrue(encryptedString.indexOf("LogTestService2") == -1 ? true
                : false);
        encryptedDoc = encryptedMsg.getSOAPEnvelope().getAsDocument();
        verify(encryptedDoc);
    }

    /**
     * Verifies the soap envelope <p/>
     * 
     * @param envelope
     * @throws Exception
     *             Thrown when there is a problem in verification
     */
    private void verify(Document doc) throws Exception {
        secEngine.processSecurityHeader(doc, null, this, crypto);
        SOAPUtil.updateSOAPMessage(doc, message);
        String decryptedString = message.getSOAPPartAsString();
        if (log.isDebugEnabled()) {
            System.out.println("\n" + decryptedString + "\n");
        }
        assertTrue(decryptedString.indexOf("LogTestService2") > 0 ? true
                : false);
    }

    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                /*
                 * here call a function/method to lookup the password for the
                 * given identifier (e.g. a user name or keystore alias) e.g.:
                 * pc.setPassword(passStore.getPassword(pc.getIdentfifier)) for
                 * Testing we supply a fixed name here.
                 */
                pc.setPassword("security");
            } else {
                throw new UnsupportedCallbackException(callbacks[i],
                        "Unrecognized Callback");
            }
        }
    }
}
