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

import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.swssf.policy.OperationPolicy;
import org.swssf.policy.assertionStates.AssertionState;
import org.swssf.policy.secpolicy.SPConstants;
import org.swssf.securityEvent.SecurityEvent;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;
import java.util.Map;

/**
 * Model bean for the IssuedToken assertion.
 */

/**
 * class lent from apache rampart
 */
public class IssuedToken extends Token {

    private OMElement issuerEpr;

    private OMElement issuerMex;

    private OMElement rstTemplate;

    boolean requireExternalReference;

    boolean requireInternalReference;

    public IssuedToken(SPConstants spConstants) {
        setVersion(spConstants);
    }

    /**
     * @return Returns the issuerEpr.
     */
    public OMElement getIssuerEpr() {
        return issuerEpr;
    }

    /**
     * @param issuerEpr The issuerEpr to set.
     */
    public void setIssuerEpr(OMElement issuerEpr) {
        this.issuerEpr = issuerEpr;
    }

    /**
     * @return Returns the requireExternalReference.
     */
    public boolean isRequireExternalReference() {
        return requireExternalReference;
    }

    /**
     * @param requireExternalReference The requireExternalReference to set.
     */
    public void setRequireExternalReference(boolean requireExternalReference) {
        this.requireExternalReference = requireExternalReference;
    }

    /**
     * @return Returns the requireInternalReference.
     */
    public boolean isRequireInternalReference() {
        return requireInternalReference;
    }

    /**
     * @param requireInternalReference The requireInternalReference to set.
     */
    public void setRequireInternalReference(boolean requireInternalReference) {
        this.requireInternalReference = requireInternalReference;
    }

    /**
     * @return Returns the rstTemplate.
     */
    public OMElement getRstTemplate() {
        return rstTemplate;
    }

    /**
     * @param rstTemplate The rstTemplate to set.
     */
    public void setRstTemplate(OMElement rstTemplate) {
        this.rstTemplate = rstTemplate;
    }

    public QName getName() {
        return spConstants.getIssuedToken();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localname = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix;
        String writerPrefix = writer.getPrefix(namespaceURI);

        if (writerPrefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);

        } else {
            prefix = writerPrefix;
        }

        // <sp:IssuedToken>
        writer.writeStartElement(prefix, localname, namespaceURI);

        if (writerPrefix == null) {
            writer.writeNamespace(prefix, namespaceURI);
        }

        String inclusion = spConstants.getAttributeValueFromInclusion(getInclusion());

        if (inclusion != null) {
            writer.writeAttribute(prefix, namespaceURI,
                    SPConstants.ATTR_INCLUDE_TOKEN, inclusion);
        }

        if (issuerEpr != null) {
            writer.writeStartElement(prefix, SPConstants.ISSUER,
                    namespaceURI);
            issuerEpr.serialize(writer);
            writer.writeEndElement();
        }

        if (rstTemplate != null) {
            // <sp:RequestSecurityTokenTemplate>
            rstTemplate.serialize(writer);

        }

        String policyLocalName = SPConstants.POLICY.getLocalPart();
        String policyNamespaceURI = SPConstants.POLICY.getNamespaceURI();

        String wspPrefix;

        String wspWriterPrefix = writer.getPrefix(policyNamespaceURI);

        if (wspWriterPrefix == null) {
            wspPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(wspPrefix, policyNamespaceURI);
        } else {
            wspPrefix = wspWriterPrefix;
        }

        if (isRequireExternalReference() || isRequireInternalReference() ||
                this.isDerivedKeys()) {

            // <wsp:Policy>
            writer.writeStartElement(wspPrefix, policyLocalName,
                    policyNamespaceURI);

            if (wspWriterPrefix == null) {
                // xmlns:wsp=".."
                writer.writeNamespace(wspPrefix, policyNamespaceURI);
            }

            if (isRequireExternalReference()) {
                // <sp:RequireExternalReference />
                writer.writeEmptyElement(prefix, SPConstants.REQUIRE_EXTERNAL_REFERNCE,
                        namespaceURI);
            }

            if (isRequireInternalReference()) {
                // <sp:RequireInternalReference />
                writer.writeEmptyElement(prefix, SPConstants.REQUIRE_INTERNAL_REFERNCE,
                        namespaceURI);
            }

            if (this.isDerivedKeys()) {
                // <sp:RequireDerivedKeys />
                writer.writeEmptyElement(prefix, SPConstants.REQUIRE_DERIVED_KEYS,
                        namespaceURI);
            }

            // <wsp:Policy>
            writer.writeEndElement();
        }

        // </sp:IssuedToken>
        writer.writeEndElement();
    }

    public OMElement getIssuerMex() {
        return issuerMex;
    }

    public void setIssuerMex(OMElement issuerMex) {
        this.issuerMex = issuerMex;
    }

    @Override
    public QName getXmlName() {
        return null;
    }

    @Override
    public void getAssertions(Map<SecurityEvent.Event, Map<Assertion, List<AssertionState>>> assertionStateMap, OperationPolicy operationPolicy) {
    }
}