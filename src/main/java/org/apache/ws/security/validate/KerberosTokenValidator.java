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

package org.apache.ws.security.validate;

import java.security.Key;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.message.token.KerberosServiceContext;
import org.apache.ws.security.message.token.KerberosServiceExceptionAction;

/**
 */
public class KerberosTokenValidator implements Validator {
    private static org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog(KerberosTokenValidator.class);

    private String serviceName;
    private CallbackHandler callbackHandler;
    private String contextName;
    private KerberosTokenDecoder kerberosTokenDecoder;
    private boolean isUsernameServiceNameForm;

    /**
     * Get the JAAS Login context name to use.
     * @return the JAAS Login context name to use
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Set the JAAS Login context name to use.
     * @param contextName the JAAS Login context name to use
     */
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    /**
     * @deprecated
     * Get the JAAS Login module name to use.
     * @return the JAAS Login module name to use
     */
    public String getJaasLoginModuleName() {
        return contextName;
    }

    /**
     * @deprecated
     * Set the JAAS Login module name to use.
     * @param jaasLoginModuleName the JAAS Login module name to use
     */
    public void setJaasLoginModuleName(String jaasLoginModuleName) {
        this.contextName = jaasLoginModuleName;
    }

    /**
     * Get the CallbackHandler to use with the LoginContext
     * @return the CallbackHandler to use with the LoginContext
     */
    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    /**
     * Set the CallbackHandler to use with the LoginContext. It can be null.
     * @param callbackHandler the CallbackHandler to use with the LoginContext
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    /**
     * The name of the service to use when contacting the KDC. This value can be null, in which
     * case it defaults to the current principal name.
     * @param serviceName the name of the service to use when contacting the KDC
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Get the name of the service to use when contacting the KDC. This value can be null, in which
     * case it defaults to the current principal name.
     * @return the name of the service to use when contacting the KDC
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Get the KerberosTokenDecoder instance used to extract a session key from the received Kerberos
     * token.
     * @return the KerberosTokenDecoder instance used to extract a session key
     */
    public KerberosTokenDecoder getKerberosTokenDecoder() {
        return kerberosTokenDecoder;
    }

    /**
     * Set the KerberosTokenDecoder instance used to extract a session key from the received Kerberos
     * token.
     * @param kerberosTokenDecoder the KerberosTokenDecoder instance used to extract a session key
     */
    public void setKerberosTokenDecoder(KerberosTokenDecoder kerberosTokenDecoder) {
        this.kerberosTokenDecoder = kerberosTokenDecoder;
    }

    /**
     * Validate the credential argument. It must contain a non-null BinarySecurityToken. 
     * 
     * @param credential the Credential to be validated
     * @param data the RequestData associated with the request
     * @throws WSSecurityException on a failed validation
     */
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null || credential.getBinarySecurityToken() == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "noCredential");
        }

        BinarySecurity binarySecurity = credential.getBinarySecurityToken();
        if (!(binarySecurity instanceof KerberosSecurity)) {
            return credential;
        }
        if (log.isDebugEnabled()) {
            try {
                String jaasAuth = System.getProperty("java.security.auth.login.config");
                String krbConf = System.getProperty("java.security.krb5.conf");
                log.debug("KerberosTokenValidator - Using JAAS auth login file: " + jaasAuth);
                log.debug("KerberosTokenValidator - Using KRB conf file: " + krbConf);
            } catch (SecurityException ex) {
                log.debug(ex.getMessage(), ex);
            }
        }

        // Get a TGT from the KDC using JAAS
        LoginContext loginContext = null;
        try {
            if (callbackHandler == null) {
                loginContext = new LoginContext(getContextName());
            } else {
                loginContext = new LoginContext(getContextName(), callbackHandler);
            }
            loginContext.login();
        } catch (LoginException ex) {
            if (log.isDebugEnabled()) {
                log.debug(ex.getMessage(), ex);
            }
            throw new WSSecurityException(
                WSSecurityException.FAILURE,
                "kerberosLoginError", 
                new Object[] {ex.getMessage()},
                ex
            );
        }
        if (log.isDebugEnabled()) {
            log.debug("Successfully authenticated to the TGT");
        }

        byte[] token = binarySecurity.getToken();

        // Get the service name to use - fall back on the principal
        Subject subject = loginContext.getSubject();
        String service = serviceName;
        if (service == null) {
            Set<Principal> principals = subject.getPrincipals();
            if (principals.isEmpty()) {
                throw new WSSecurityException(
                    WSSecurityException.FAILURE, 
                    "kerberosLoginError", 
                    new Object[] {"No Client principals found after login"}
                );
            }
            service = principals.iterator().next().getName();
        }

        // Validate the ticket
        KerberosServiceExceptionAction action = new KerberosServiceExceptionAction(token, service, isUsernameServiceNameForm());
        KerberosServiceContext krbServiceCtx = null;
        try {
            krbServiceCtx = (KerberosServiceContext)Subject.doAs(subject, action);
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof WSSecurityException) {
                throw (WSSecurityException) cause;
            } else {
                throw new WSSecurityException(
                    WSSecurityException.FAILURE, "kerberosTicketValidationError", new Object[] {}, cause
                );
            }
        }

        credential.setPrincipal(krbServiceCtx.getPrincipal());
        if (credential.getPrincipal() == null) {
            throw new WSSecurityException(
                WSSecurityException.FAILURE, "kerberosTicketValidationError"
            );
        }

        // Check to see if the session key is available in KerberosServiceContext
        log.debug("Trying to obtain the Session Key from the KerberosServiceContext.");
        Key sessionKey = krbServiceCtx.getSessionKey();
        if (null != sessionKey) {
            log.debug("Found session key in the KerberosServiceContext.");
            credential.setSecretKey(sessionKey.getEncoded());
        } else {
            log.debug("Session key is not found in the KerberosServiceContext.");
        }
        
        // Otherwise, try to extract the session key from the token if a KerberosTokenDecoder implementation is
        // available
        if (null == credential.getSecretKey() && kerberosTokenDecoder != null) {    
            KerberosTokenDecoder kerberosTokenDecoder = this.kerberosTokenDecoder;
            
            log.debug("KerberosTokenDecoder is set.Trying to obtain the session key from it.");            
            kerberosTokenDecoder.clear();
            kerberosTokenDecoder.setToken(token);
            kerberosTokenDecoder.setSubject(subject);
            byte[] key = kerberosTokenDecoder.getSessionKey();
            if (null != key) {
                log.debug("Session key obtained from the KerberosTokenDecoder.");
                credential.setSecretKey(key);
            } else {
                log.debug("Session key could not be obtained from the KerberosTokenDecoder.");
            }
        } else {
            log.debug("KerberosTokenDecoder is not set.");
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Successfully validated a ticket");
        }

        return credential;
    }

    /**
     * SPN can be configured to be in either <b>"hostbased"</b> or <b>"username"</b> form.<br/>
     *     - <b>"hostbased"</b> - specifies that the service principal name should be interpreted as a "host-based" name as specified in GSS API Rfc, section "4.1: Host-Based Service Name Form" - The service name, as it is specified in LDAP/AD, as it is listed in the KDC.<br/>
     *     - <b>"username"</b> - specifies that the service principal name should be interpreted as a "username" name as specified in GSS API Rfc, section "4.2: User Name Form" � This is usually the client username in LDAP/AD used for authentication to the KDC.
     * 
     * <br/><br/>Default is <b>"hostbased"</b>.
     * 
     * @return the isUsernameServiceNameForm
     */
    public boolean isUsernameServiceNameForm() {
        return isUsernameServiceNameForm;
    }

    /**
     * If true - sets the SPN form to "username"
     * <br/>If false<b>(default)</b> - the SPN form is "hostbased"
     * 
     * @see KerberosSecurity#retrieveServiceTicket(String, CallbackHandler, String, boolean)
     * 
     * @param isUsernameServiceNameForm the isUsernameServiceNameForm to set
     */
    public void setUsernameServiceNameForm(boolean isUsernameServiceNameForm) {
        this.isUsernameServiceNameForm = isUsernameServiceNameForm;
    }
    
}
