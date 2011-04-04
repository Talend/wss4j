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

package org.apache.ws.security.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;
import org.w3c.dom.Element;

import java.util.List;

public class UsernameTokenProcessor implements Processor {
    private static Log log = LogFactory.getLog(UsernameTokenProcessor.class.getName());
    
    public List<WSSecurityEngineResult> handleToken(
        Element elem, 
        RequestData data,
        WSDocInfo wsDocInfo
    ) throws WSSecurityException {
        if (log.isDebugEnabled()) {
            log.debug("Found UsernameToken list element");
        }
        
        Validator validator = data.getValidator(WSSecurityEngine.USERNAME_TOKEN);
        Credential credential = handleUsernameToken(elem, validator, data);
        UsernameToken token = credential.getUsernametoken();
        
        WSUsernameTokenPrincipal principal = 
            new WSUsernameTokenPrincipal(token.getName(), token.isHashed());
        principal.setNonce(token.getNonce());
        principal.setPassword(token.getPassword());
        principal.setCreatedTime(token.getCreated());
        principal.setPasswordType(token.getPasswordType());
        
        int action = WSConstants.UT;
        if (token.getPassword() == null) { 
            action = WSConstants.UT_NOPASSWORD;
        }
        WSSecurityEngineResult result = 
            new WSSecurityEngineResult(action, token, principal);
        result.put(WSSecurityEngineResult.TAG_ID, token.getID());
        if (credential.getTransformedToken() != null) {
            result.put(
                WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN, credential.getTransformedToken()
            );
        }
        
        if (validator != null) {
            result.put(WSSecurityEngineResult.TAG_VALIDATED_TOKEN, Boolean.TRUE);
        }
        wsDocInfo.addTokenElement(elem);
        wsDocInfo.addResult(result);
        return java.util.Collections.singletonList(result);
    }

    /**
     * Check the UsernameToken element and validate it.
     *
     * @param token the DOM element that contains the UsernameToken
     * @param data The RequestData object from which to obtain configuration
     * @return a Credential object corresponding to the (validated) Username Token
     * @throws WSSecurityException
     */
    public Credential 
    handleUsernameToken(
        Element token, 
        Validator validator,
        RequestData data
    ) throws WSSecurityException {
        boolean allowNamespaceQualifiedPasswordTypes = false;
        boolean bspCompliant = true;
        WSSConfig wssConfig = data.getWssConfig();
        if (wssConfig != null) {
            allowNamespaceQualifiedPasswordTypes = 
                wssConfig.getAllowNamespaceQualifiedPasswordTypes();
            bspCompliant = wssConfig.isWsiBSPCompliant();
        }
        
        //
        // Parse and validate the UsernameToken element
        //
        UsernameToken ut = 
            new UsernameToken(token, allowNamespaceQualifiedPasswordTypes, bspCompliant);
        Credential credential = new Credential();
        credential.setUsernametoken(ut);
        if (validator != null) {
            return validator.validate(credential, data);
        }
        return credential;
    }

}