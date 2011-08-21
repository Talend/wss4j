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
package org.swssf.impl.processor.output;

import org.apache.commons.codec.binary.Base64;
import org.swssf.config.JCEAlgorithmMapper;
import org.swssf.crypto.Crypto;
import org.swssf.ext.*;
import org.swssf.impl.derivedKey.AlgoFactory;
import org.swssf.impl.derivedKey.ConversationException;
import org.swssf.impl.derivedKey.DerivationAlgorithm;
import org.swssf.impl.securityToken.ProcessorInfoSecurityToken;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class DerivedKeyTokenOutputProcessor extends AbstractOutputProcessor {

    public DerivedKeyTokenOutputProcessor(SecurityProperties securityProperties, Constants.Action action) throws WSSecurityException {
        super(securityProperties, action);
    }

    @Override
    public void processEvent(XMLEvent xmlEvent, OutputProcessorChain outputProcessorChain) throws XMLStreamException, WSSecurityException {
        try {

            String tokenId = outputProcessorChain.getSecurityContext().get(Constants.PROP_USE_THIS_TOKEN_ID_FOR_DERIVED_KEY);
            if (tokenId == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_ENCRYPTION);
            }
            SecurityTokenProvider wrappingSecurityTokenProvider = outputProcessorChain.getSecurityContext().getSecurityTokenProvider(tokenId);
            if (wrappingSecurityTokenProvider == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_ENCRYPTION);
            }
            final SecurityToken wrappingSecurityToken = wrappingSecurityTokenProvider.getSecurityToken(null);
            if (wrappingSecurityToken == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_ENCRYPTION);
            }

            final String wsuIdDKT = "DK-" + UUID.randomUUID().toString();

            int offset = 0;
            int length = 0;
            switch (getAction()) {
                case SIGNATURE_WITH_DERIVED_KEY:
                    length = JCEAlgorithmMapper.getAlgorithmMapping(getSecurityProperties().getSignatureAlgorithm()).getKeyLength() / 8;
                    break;
                case ENCRYPT_WITH_DERIVED_KEY:
                    length = JCEAlgorithmMapper.getAlgorithmMapping(getSecurityProperties().getEncryptionSymAlgorithm()).getKeyLength() / 8;
                    break;
            }

            byte[] label;
            try {
                label = (Constants.WS_SecureConversation_DEFAULT_LABEL + Constants.WS_SecureConversation_DEFAULT_LABEL).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new WSSecurityException("UTF-8 encoding is not supported", e);
            }

            byte[] nonce = new byte[16];
            Constants.secureRandom.nextBytes(nonce);

            byte[] seed = new byte[label.length + nonce.length];
            System.arraycopy(label, 0, seed, 0, label.length);
            System.arraycopy(nonce, 0, seed, label.length, nonce.length);

            DerivationAlgorithm derivationAlgorithm;
            try {
                derivationAlgorithm = AlgoFactory.getInstance(Constants.P_SHA_1);
            } catch (ConversationException e) {
                throw new WSSecurityException(e.getMessage(), e);
            }

            final byte[] derivedKeyBytes;
            try {
                byte[] secret;
                if (wrappingSecurityToken.getTokenType() == Constants.TokenType.SecurityContextToken) {
                    WSPasswordCallback passwordCallback = new WSPasswordCallback(wsuIdDKT, WSPasswordCallback.Usage.SECRET_KEY);
                    Utils.doSecretKeyCallback(securityProperties.getCallbackHandler(), passwordCallback, wsuIdDKT);
                    if (passwordCallback.getKey() == null) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noKey", wsuIdDKT);
                    }
                    secret = passwordCallback.getKey();
                } else {
                    secret = wrappingSecurityToken.getSecretKey(null, null).getEncoded();
                }

                derivedKeyBytes = derivationAlgorithm.createKey(secret, seed, offset, length);
            } catch (ConversationException e) {
                throw new WSSecurityException(e.getMessage(), e);
            }

            final ProcessorInfoSecurityToken derivedKeySecurityToken = new ProcessorInfoSecurityToken() {

                private Map<String, Key> keyTable = new Hashtable<String, Key>();
                private OutputProcessor outputProcessor;

                public String getId() {
                    return wsuIdDKT;
                }

                public void setProcessor(OutputProcessor outputProcessor) {
                    this.outputProcessor = outputProcessor;
                }

                public Object getProcessor() {
                    return outputProcessor;
                }

                public boolean isAsymmetric() {
                    return false;
                }

                public Key getSecretKey(String algorithmURI, Constants.KeyUsage keyUsage) throws WSSecurityException {
                    if (keyTable.containsKey(algorithmURI)) {
                        return keyTable.get(algorithmURI);
                    } else {
                        String algoFamily = JCEAlgorithmMapper.getJCERequiredKeyFromURI(algorithmURI);
                        Key key = new SecretKeySpec(derivedKeyBytes, algoFamily);
                        keyTable.put(algorithmURI, key);
                        return key;
                    }
                }

                public PublicKey getPublicKey(Constants.KeyUsage keyUsage) throws WSSecurityException {
                    return null;
                }

                public X509Certificate[] getX509Certificates() throws WSSecurityException {
                    return null;
                }

                public void verify() throws WSSecurityException {
                }

                public SecurityToken getKeyWrappingToken() {
                    return wrappingSecurityToken;
                }

                public String getKeyWrappingTokenAlgorithm() {
                    return null;
                }

                public Constants.TokenType getTokenType() {
                    return null;
                }
            };

            SecurityTokenProvider derivedKeysecurityTokenProvider = new SecurityTokenProvider() {
                public SecurityToken getSecurityToken(Crypto crypto) throws WSSecurityException {
                    return derivedKeySecurityToken;
                }

                public String getId() {
                    return wsuIdDKT;
                }
            };

            switch (getAction()) {
                case SIGNATURE_WITH_DERIVED_KEY:
                    outputProcessorChain.getSecurityContext().put(Constants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE, wsuIdDKT);
                    outputProcessorChain.getSecurityContext().put(Constants.PROP_APPEND_SIGNATURE_ON_THIS_ID, wsuIdDKT);
                    break;
                case ENCRYPT_WITH_DERIVED_KEY:
                    outputProcessorChain.getSecurityContext().put(Constants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION, wsuIdDKT);
                    break;
            }
            outputProcessorChain.getSecurityContext().registerSecurityTokenProvider(wsuIdDKT, derivedKeysecurityTokenProvider);
            FinalDerivedKeyTokenOutputProcessor finalDerivedKeyTokenOutputProcessor = new FinalDerivedKeyTokenOutputProcessor(getSecurityProperties(), getAction(), derivedKeySecurityToken, offset, length, new String(Base64.encodeBase64(nonce)));
            finalDerivedKeyTokenOutputProcessor.getBeforeProcessors().add(wrappingSecurityToken.getProcessor());
            derivedKeySecurityToken.setProcessor(finalDerivedKeyTokenOutputProcessor);
            outputProcessorChain.addProcessor(finalDerivedKeyTokenOutputProcessor);
        } finally {
            outputProcessorChain.removeProcessor(this);
        }
        outputProcessorChain.processEvent(xmlEvent);
    }

    class FinalDerivedKeyTokenOutputProcessor extends AbstractOutputProcessor {

        private SecurityToken securityToken;
        private int offset;
        private int length;
        private String nonce;

        FinalDerivedKeyTokenOutputProcessor(SecurityProperties securityProperties, Constants.Action action, SecurityToken securityToken, int offset, int length, String nonce) throws WSSecurityException {
            super(securityProperties, action);
            this.securityToken = securityToken;
            this.offset = offset;
            this.length = length;
            this.nonce = nonce;
        }

        @Override
        public void processEvent(XMLEvent xmlEvent, OutputProcessorChain outputProcessorChain) throws XMLStreamException, WSSecurityException {
            outputProcessorChain.processEvent(xmlEvent);
            if (xmlEvent.isStartElement()) {
                StartElement startElement = xmlEvent.asStartElement();
                if (outputProcessorChain.getDocumentContext().isInSecurityHeader() && startElement.getName().equals(Constants.TAG_wsse_Security)) {
                    OutputProcessorChain subOutputProcessorChain = outputProcessorChain.createSubChain(this);

                    Map<QName, String> attributes = new HashMap<QName, String>();
                    attributes.put(Constants.ATT_wsu_Id, securityToken.getId());
                    createStartElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_DerivedKeyToken, attributes);

                    createSecurityTokenReferenceStructureForDerivedKey(subOutputProcessorChain, securityToken, getSecurityProperties().getDerivedKeyKeyIdentifierType(), getSecurityProperties().getDerivedKeyTokenReference(), getSecurityProperties().isUseSingleCert());
                    createStartElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_Offset, null);
                    createCharactersAndOutputAsEvent(subOutputProcessorChain, "" + offset);
                    createEndElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_Offset);
                    createStartElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_Length, null);
                    createCharactersAndOutputAsEvent(subOutputProcessorChain, "" + length);
                    createEndElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_Length);
                    createStartElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_Nonce, null);
                    createCharactersAndOutputAsEvent(subOutputProcessorChain, nonce);
                    createEndElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_Nonce);
                    createEndElementAndOutputAsEvent(subOutputProcessorChain, Constants.TAG_wsc0502_DerivedKeyToken);

                    outputProcessorChain.removeProcessor(this);
                }
            }
        }
    }
}