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

package org.apache.cxf.fediz.core.config;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.fediz.core.config.jaxb.CallbackType;
import org.apache.cxf.fediz.core.config.jaxb.ClaimType;
import org.apache.cxf.fediz.core.config.jaxb.ClaimTypesRequested;
import org.apache.cxf.fediz.core.config.jaxb.FederationProtocolType;
import org.apache.cxf.fediz.core.config.jaxb.ProtocolType;
import org.apache.cxf.fediz.core.saml.SAMLTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FederationProtocol extends Protocol {

    private static final Logger LOG = LoggerFactory.getLogger(FederationProtocol.class);
    
    private Object request;
    private Object authenticationType;
    private Object homeRealm;
    private Object freshness;
    private Object signInQuery;
    
    public FederationProtocol(ProtocolType protocolType) {
        super(protocolType);
        
        // add SAMLTokenValidator as the last one
        // Fediz chooses the first validator in the list if its
        // canHandleToken or canHandleTokenType method return true
        SAMLTokenValidator validator = new SAMLTokenValidator();
        getTokenValidators().add(getTokenValidators().size(), validator);
    }

    protected FederationProtocolType getFederationProtocol() {
        return (FederationProtocolType)super.getProtocolType();
    }

    protected void setFederationProtocol(FederationProtocolType federationProtocol) {
        super.setProtocolType(federationProtocol);
    }


    public String getApplicationServiceURL() {
        return getFederationProtocol().getApplicationServiceURL();
    }

    public void setApplicationServiceURL(String value) {
        getFederationProtocol().setApplicationServiceURL(value);
    }

    public Object getAuthenticationType() {
        if (this.authenticationType != null) {
            return this.authenticationType;
        }
        CallbackType cbt = getFederationProtocol().getAuthenticationType();
        this.authenticationType = loadCallbackType(cbt, "AuthenticationType");
        return this.authenticationType;
    }

    public void setAuthenticationType(Object value) {
        final boolean isString = value instanceof String;
        final boolean isCallbackHandler = value instanceof CallbackHandler;
        if (isString || isCallbackHandler) {
            this.authenticationType = value;
        } else {
            LOG.error("Unsupported 'AuthenticationType' object");
            throw new IllegalArgumentException("Unsupported 'AuthenticationType' object. Type must be "
                                               + "java.lang.String or javax.security.auth.callback.CallbackHandler.");
        }
    }
    
    public Object getHomeRealm() {
        if (this.homeRealm != null) {
            return this.homeRealm;
        }
        CallbackType cbt = getFederationProtocol().getHomeRealm();
        this.homeRealm = loadCallbackType(cbt, "HomeRealm");
        return this.homeRealm;
    }

    public void setHomeRealm(Object value) {
        final boolean isString = value instanceof String;
        final boolean isCallbackHandler = value instanceof CallbackHandler;
        if (isString || isCallbackHandler) {
            this.homeRealm = value;
        } else {
            LOG.error("Unsupported 'HomeRealm' object");
            throw new IllegalArgumentException("Unsupported 'HomeRealm' object. Type must be "
                                               + "java.lang.String or javax.security.auth.callback.CallbackHandler.");
        }
    }
    
    public Object getFreshness() {
        if (this.freshness != null) {
            return this.freshness;
        }
        CallbackType cbt = getFederationProtocol().getFreshness();
        this.freshness = loadCallbackType(cbt, "Freshness");
        return this.freshness;
    }

    public void setFreshness(Object value) {
        final boolean isString = value instanceof String;
        final boolean isCallbackHandler = value instanceof CallbackHandler;
        if (isString || isCallbackHandler) {
            this.freshness = value;
        } else {
            LOG.error("Unsupported 'Freshness' object");
            throw new IllegalArgumentException("Unsupported 'Freshness' object. Type must be "
                                               + "java.lang.String or javax.security.auth.callback.CallbackHandler.");
        }
    }
    
    public Object getSignInQuery() {
        if (this.signInQuery != null) {
            return this.signInQuery;
        }
        CallbackType cbt = getFederationProtocol().getSignInQuery();
        this.signInQuery = loadCallbackType(cbt, "SignInQuery");
        return this.signInQuery;
    }

    public void setSignInQuery(Object value) {
        final boolean isString = value instanceof String;
        final boolean isCallbackHandler = value instanceof CallbackHandler;
        if (isString || isCallbackHandler) {
            this.signInQuery = value;
        } else {
            LOG.error("Unsupported 'SignInQuery' object");
            throw new IllegalArgumentException("Unsupported 'SignInQuery' object. Type must be "
                                               + "java.lang.String or javax.security.auth.callback.CallbackHandler.");
        }
    }
    
    public Object getRequest() {
        if (this.request != null) {
            return this.request;
        }
        CallbackType cbt = getFederationProtocol().getRequest();
        this.request = loadCallbackType(cbt, "Request");
        return this.request;
    }
    
    public void setRequest(Object value) {
        final boolean isString = value instanceof String;
        final boolean isCallbackHandler = value instanceof CallbackHandler;
        if (isString || isCallbackHandler) {
            this.request = value;
        } else {
            LOG.error("Unsupported 'Request' object");
            throw new IllegalArgumentException("Unsupported 'Request' object. Type must be "
                                               + "java.lang.String or javax.security.auth.callback.CallbackHandler.");
        }
    }

    public String getReply() {
        return getFederationProtocol().getReply();
    }

    public void setReply(String value) {
        getFederationProtocol().setReply(value);
    }

    public List<Claim> getClaimTypesRequested() {
        ClaimTypesRequested claimsRequested = getFederationProtocol().getClaimTypesRequested();
        List<Claim> claims = new ArrayList<Claim>();
        for (ClaimType c:claimsRequested.getClaimType()) {
            claims.add(new Claim(c));
        }
        return claims;
    }

    public void setClaimTypesRequested(ClaimTypesRequested value) {
        getFederationProtocol().setClaimTypesRequested(value);
    }

    public String getVersion() {
        return getFederationProtocol().getVersion();
    }

    public void setVersion(String value) {
        getFederationProtocol().setVersion(value);
    }

    public String toString() {
        return getFederationProtocol().toString();
    }
    
}
