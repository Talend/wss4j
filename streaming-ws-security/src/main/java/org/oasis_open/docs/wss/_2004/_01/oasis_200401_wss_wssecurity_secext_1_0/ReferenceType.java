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
package org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0;


import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSUtils;
import org.swssf.xmlsec.ext.ParseException;
import org.swssf.xmlsec.ext.Parseable;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This type represents a reference to an external security token.
 * <p/>
 * <p>Java class for ReferenceType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="ReferenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="URI" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="ValueType" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReferenceType")
public class ReferenceType implements Parseable {

    private Parseable currentParseable;

    @XmlElement(name = "BinarySecurityToken")
    protected BinarySecurityTokenType binarySecurityTokenType;
    @XmlAttribute(name = "URI")
    @XmlSchemaType(name = "anyURI")
    protected String uri;
    @XmlAttribute(name = "ValueType")
    @XmlSchemaType(name = "anyURI")
    protected String valueType;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    public ReferenceType() {
    }

    public ReferenceType(StartElement startElement) {
        @SuppressWarnings("unchecked")
        Iterator<Attribute> attributeIterator = startElement.getAttributes();
        while (attributeIterator.hasNext()) {
            Attribute attribute = attributeIterator.next();
            if (attribute.getName().equals(WSSConstants.ATT_NULL_URI)) {
                this.uri = WSSUtils.dropReferenceMarker(attribute.getValue());
            } else if (attribute.getName().equals(WSSConstants.ATT_NULL_ValueType)) {
                this.valueType = attribute.getValue();
            }
        }
    }

    public boolean parseXMLEvent(XMLEvent xmlEvent) throws ParseException {
        if (currentParseable != null) {
            boolean finished = currentParseable.parseXMLEvent(xmlEvent);
            if (finished) {
                currentParseable = null;
            }
            return false;
        }

        switch (xmlEvent.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                StartElement startElement = xmlEvent.asStartElement();
                if (startElement.getName().equals(WSSConstants.TAG_wsse_BinarySecurityToken)) {
                    currentParseable = this.binarySecurityTokenType = new BinarySecurityTokenType(startElement);
                } else {
                    throw new ParseException("Unexpected Element: " + startElement.getName());
                }
                break;
            case XMLStreamConstants.END_ELEMENT:
                currentParseable = null;
                EndElement endElement = xmlEvent.asEndElement();
                if (endElement.getName().equals(WSSConstants.TAG_wsse_Reference)) {
                    return true;
                }
                break;
            default:
                throw new ParseException("Unexpected event received " + WSSUtils.getXMLEventAsString(xmlEvent));
        }
        return false;
    }

    public void validate() throws ParseException {
        if (uri == null) {
            throw new ParseException("Attribute \"URI\" is missing");
        }
    }

    /**
     * Gets the value of the uri property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getURI() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setURI(String value) {
        this.uri = value;
    }

    /**
     * Gets the value of the valueType property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * Sets the value of the valueType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setValueType(String value) {
        this.valueType = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * <p/>
     * <p/>
     * the map is keyed by the name of the attribute and
     * the value is the string value of the attribute.
     * <p/>
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     *
     * @return always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

    public BinarySecurityTokenType getBinarySecurityTokenType() {
        return binarySecurityTokenType;
    }

    public void setBinarySecurityTokenType(BinarySecurityTokenType binarySecurityTokenType) {
        this.binarySecurityTokenType = binarySecurityTokenType;
    }
}
