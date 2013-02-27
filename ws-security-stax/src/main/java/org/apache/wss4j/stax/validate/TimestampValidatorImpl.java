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
package org.apache.wss4j.stax.validate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wss4j.binding.wsu10.TimestampType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSConstants;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class TimestampValidatorImpl implements TimestampValidator {

    private static final transient Log log = LogFactory.getLog(TimestampValidatorImpl.class);

    @Override
    public void validate(TimestampType timestampType, TokenContext tokenContext) throws WSSecurityException {

        if (timestampType.getCreated() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "missingCreated");
        }

        try {
            // Validate whether the security semantics have expired
            //created and expires is optional per spec. But we enforce the created element in the validation
            Date createdDate = null;
            if (timestampType.getCreated() != null) {
                XMLGregorianCalendar created;
                try {
                    created = WSSConstants.datatypeFactory.newXMLGregorianCalendar(timestampType.getCreated().getValue());
                } catch (IllegalArgumentException e) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, e);
                }
                log.debug("Timestamp created: " + created);
                createdDate = created.toGregorianCalendar().getTime();
            }

            Date expiresDate = null;
            if (timestampType.getExpires() != null) {
                XMLGregorianCalendar expires;
                try {
                    expires = WSSConstants.datatypeFactory.newXMLGregorianCalendar(timestampType.getExpires().getValue());
                } catch (IllegalArgumentException e) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, e);
                }
                log.debug("Timestamp expires: " + expires);
                expiresDate = expires.toGregorianCalendar().getTime();
            }

            Date rightNow = new Date();
            int ttl = tokenContext.getWssSecurityProperties().getTimestampTTL();
            int futureTTL = tokenContext.getWssSecurityProperties().getTimeStampFutureTTL();

            if (expiresDate != null && tokenContext.getWssSecurityProperties().isStrictTimestampCheck() 
                && expiresDate.before(rightNow)) {
                log.debug("Time now: " + WSSConstants.datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat());
                throw new WSSecurityException(WSSecurityException.ErrorCode.MESSAGE_EXPIRED, "invalidTimestamp",
                        "The security semantics of the message have expired");
            }

            if (createdDate != null && !verifyCreated(createdDate, ttl, futureTTL)) {
                log.debug("Time now: " + WSSConstants.datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat());
                throw new WSSecurityException(WSSecurityException.ErrorCode.MESSAGE_EXPIRED, "invalidTimestamp",
                        "The security semantics of the message have expired");
            }

        } catch (IllegalArgumentException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, e);
        }
    }
    
    /**
     * Return true if the "Created" value is before the current time minus the timeToLive
     * argument, and if the Created value is not "in the future".
     */
    private boolean verifyCreated(
        Date createdDate,
        int timeToLive,
        int futureTimeToLive
    ) {
        Date validCreation = new Date();
        long currentTime = validCreation.getTime();
        if (futureTimeToLive > 0) {
            validCreation.setTime(currentTime + ((long)futureTimeToLive * 1000L));
        }
        // Check to see if the created time is in the future
        if (createdDate.after(validCreation)) {
            log.debug("Validation of Timestamp: The message was created in the future!");
            return false;
        }

        // Calculate the time that is allowed for the message to travel
        currentTime -= ((long)timeToLive * 1000L);
        validCreation.setTime(currentTime);

        // Validate the time it took the message to travel
        if (createdDate.before(validCreation)) {
            log.debug("Validation of Timestamp: The message was created too long ago");
            return false;
        }

        log.debug("Validation of Timestamp: Everything is ok");
        
        return true;
    }
}
