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

package org.swssf.policy.secpolicy.model;

import org.apache.neethi.Assertion;
import org.apache.neethi.PolicyComponent;
import org.swssf.policy.OperationPolicy;
import org.swssf.policy.assertionStates.AssertionState;
import org.swssf.policy.assertionStates.EncryptedElementAssertionState;
import org.swssf.policy.assertionStates.SignedElementAssertionState;
import org.swssf.policy.secpolicy.SPConstants;
import org.swssf.wss.securityEvent.SecurityEvent;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;

/**
 * class lent from apache rampart
 */
public class SignedEncryptedElements extends AbstractSecurityAssertion {

    private List<String> xPathExpressions = new ArrayList<String>();

    private Map<String, String> declaredNamespaces = new HashMap<String, String>();

    private String xPathVersion;

    /**
     * Just a flag to identify whether this holds sign element info or encr
     * elements info
     */
    private boolean signedElements;

    public SignedEncryptedElements(Boolean signedElements, SPConstants spConstants) {
        this.signedElements = signedElements;
        setVersion(spConstants);
    }

    /**
     * @return Returns the xPathExpressions.
     */
    public List<String> getXPathExpressions() {
        return xPathExpressions;
    }

    public void addXPathExpression(String expr) {
        this.xPathExpressions.add(expr);
    }

    /**
     * @return Returns the xPathVersion.
     */
    public String getXPathVersion() {
        return xPathVersion;
    }

    /**
     * @param pathVersion The xPathVersion to set.
     */
    public void setXPathVersion(String pathVersion) {
        xPathVersion = pathVersion;
    }

    /**
     * @return Returns the signedElements.
     */
    public boolean isSignedElements() {
        return signedElements;
    }

    public Map getDeclaredNamespaces() {
        return declaredNamespaces;
    }

    public void addDeclaredNamespaces(String uri, String prefix) {
        declaredNamespaces.put(prefix, uri);
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:SignedElements> | <sp:EncryptedElements>
        writer.writeStartElement(prefix, localName, namespaceURI);

        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

        if (xPathVersion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.XPATH_VERSION, xPathVersion);
        }

        String xpathExpression;

        for (Iterator iterator = xPathExpressions.iterator(); iterator
                .hasNext(); ) {
            xpathExpression = (String) iterator.next();
            // <sp:XPath ..>
            writer.writeStartElement(prefix, SPConstants.XPATH_EXPR, namespaceURI);

            Iterator<String> namespaces = declaredNamespaces.keySet().iterator();

            while (namespaces.hasNext()) {
                prefix = namespaces.next();
                namespaceURI = declaredNamespaces.get(prefix);
                writer.writeNamespace(prefix, namespaceURI);
            }

            writer.writeCharacters(xpathExpression);
            writer.writeEndElement();
        }

        // </sp:SignedElements> | </sp:EncryptedElements>
        writer.writeEndElement();
    }

    public QName getName() {
        if (signedElements) {
            return spConstants.getSignedElements();
        }
        return spConstants.getEncryptedElements();
    }

    public PolicyComponent normalize() {
        return this;
    }

    @Override
    public SecurityEvent.Event[] getResponsibleAssertionEvents() {
        if (isSignedElements()) {
            return new SecurityEvent.Event[]{SecurityEvent.Event.SignedElement};
        } else {
            return new SecurityEvent.Event[]{SecurityEvent.Event.EncryptedElement};
        }
    }

    @Override
    public void getAssertions(Map<SecurityEvent.Event, Map<Assertion, List<AssertionState>>> assertionStateMap, OperationPolicy operationPolicy) {
        if (isSignedElements()) {
            Map<Assertion, List<AssertionState>> signedElementAssertionStates = assertionStateMap.get(SecurityEvent.Event.SignedElement);
            addAssertionState(signedElementAssertionStates, this, new SignedElementAssertionState(this, true, getQNamesFromXPath()));
        } else {
            Map<Assertion, List<AssertionState>> encryptedElementAssertionStates = assertionStateMap.get(SecurityEvent.Event.EncryptedElement);
            addAssertionState(encryptedElementAssertionStates, this, new EncryptedElementAssertionState(this, true, getQNamesFromXPath()));
        }
    }

    private List<QName> getQNamesFromXPath() {
        List<QName> qNames = new ArrayList<QName>(xPathExpressions.size());
        for (int i = 0; i < xPathExpressions.size(); i++) {
            String s = xPathExpressions.get(i);
            String prefix;
            String localName;
            if (s.contains(":")) {
                int idx = s.indexOf(":");
                prefix = s.substring(0, idx);
                localName = s.substring(idx + 1);
            } else {
                prefix = "";
                localName = s;
            }
            qNames.add(new QName(declaredNamespaces.get(prefix), localName));
        }
        return qNames;
    }
}
