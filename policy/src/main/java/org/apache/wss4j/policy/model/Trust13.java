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
package org.apache.wss4j.policy.model;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.wss4j.policy.SPConstants;

public class Trust13 extends Trust10 {

    private boolean requireRequestSecurityTokenCollection;
    private boolean requireAppliesTo;
    private boolean scopePolicy15;
    private boolean mustSupportInteractiveChallenge;

    public Trust13(SPConstants.SPVersion version, Policy nestedPolicy) {
        super(version, nestedPolicy);

        parseNestedTrust13Policy(nestedPolicy, this);
    }

    @Override
    public QName getName() {
        return getVersion().getSPConstants().getTrust13();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Trust13)) {
            return false;
        }

        Trust13 that = (Trust13)object;
        if (requireRequestSecurityTokenCollection != that.requireRequestSecurityTokenCollection
            || requireAppliesTo != that.requireAppliesTo
            || scopePolicy15 != that.scopePolicy15
            || mustSupportInteractiveChallenge != that.mustSupportInteractiveChallenge) {
            return false;
        }

        return super.equals(object);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Boolean.hashCode(requireRequestSecurityTokenCollection);
        result = 31 * result + Boolean.hashCode(requireAppliesTo);
        result = 31 * result + Boolean.hashCode(scopePolicy15);
        result = 31 * result + Boolean.hashCode(mustSupportInteractiveChallenge);

        return 31 * result + super.hashCode();
    }

    @Override
    protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
        return new Trust13(getVersion(), nestedPolicy);
    }

    protected void parseNestedTrust13Policy(Policy nestedPolicy, Trust13 trust13) {
        Iterator<List<Assertion>> alternatives = nestedPolicy.getAlternatives();
        //we just process the first alternative
        //this means that if we have a compact policy only the first alternative is visible
        //in contrary to a normalized policy where just one alternative exists
        if (alternatives.hasNext()) {
            List<Assertion> assertions = alternatives.next();
            for (int i = 0; i < assertions.size(); i++) {
                Assertion assertion = assertions.get(i);
                String assertionName = assertion.getName().getLocalPart();
                String assertionNamespace = assertion.getName().getNamespaceURI();

                QName requireRSTC = getVersion().getSPConstants().getRequireRequestSecurityTokenCollection();
                if (requireRSTC.getLocalPart().equals(assertionName)
                    && requireRSTC.getNamespaceURI().equals(assertionNamespace)) {
                    if (trust13.isRequireRequestSecurityTokenCollection()) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    trust13.setRequireRequestSecurityTokenCollection(true);
                    continue;
                }

                QName requireAppliesTo = getVersion().getSPConstants().getRequireAppliesTo();
                if (requireAppliesTo.getLocalPart().equals(assertionName)
                    && requireAppliesTo.getNamespaceURI().equals(assertionNamespace)) {
                    if (trust13.isRequireAppliesTo()) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    trust13.setRequireAppliesTo(true);
                    continue;
                }

                QName scopePolicy15 = getVersion().getSPConstants().getScopePolicy15();
                if (scopePolicy15.getLocalPart().equals(assertionName)
                    && scopePolicy15.getNamespaceURI().equals(assertionNamespace)) {
                    if (trust13.isScopePolicy15()) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    trust13.setScopePolicy15(true);
                    continue;
                }

                QName supportInteractiveChallenge =
                    getVersion().getSPConstants().getMustSupportInteractiveChallenge();
                if (supportInteractiveChallenge.getLocalPart().equals(assertionName)
                    && supportInteractiveChallenge.getNamespaceURI().equals(assertionNamespace)) {
                    if (trust13.isMustSupportInteractiveChallenge()) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    trust13.setMustSupportInteractiveChallenge(true);
                    continue;
                }
            }
        }
    }

    public boolean isRequireRequestSecurityTokenCollection() {
        return requireRequestSecurityTokenCollection;
    }

    protected void setRequireRequestSecurityTokenCollection(boolean requireRequestSecurityTokenCollection) {
        this.requireRequestSecurityTokenCollection = requireRequestSecurityTokenCollection;
    }

    public boolean isRequireAppliesTo() {
        return requireAppliesTo;
    }

    protected void setRequireAppliesTo(boolean requireAppliesTo) {
        this.requireAppliesTo = requireAppliesTo;
    }

    public boolean isScopePolicy15() {
        return scopePolicy15;
    }

    protected void setScopePolicy15(boolean scopePolicy15) {
        this.scopePolicy15 = scopePolicy15;
    }

    public boolean isMustSupportInteractiveChallenge() {
        return mustSupportInteractiveChallenge;
    }

    protected void setMustSupportInteractiveChallenge(boolean mustSupportInteractiveChallenge) {
        this.mustSupportInteractiveChallenge = mustSupportInteractiveChallenge;
    }
}
