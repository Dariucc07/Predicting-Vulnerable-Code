package org.cloudfoundry.identity.uaa.oauth.refresh;

import java.util.Map;
import java.util.Set;

public class RefreshTokenRequestData {
    public final String grantType;
    public final Set<String> scopes;
    public final String authorities;
    public final Set<String> resourceIds;
    public final String clientId;
    public final boolean revocable;
    public final Map<String, Object> externalAttributes;

    public RefreshTokenRequestData(String grantType,
                                   Set<String> scopes,
                                   String authorities,
                                   Set<String> resourceIds,
                                   String clientId,
                                   boolean revocable,
                                   Map<String,Object> externalAttributes) {
        this.grantType = grantType;
        this.scopes = scopes;
        this.authorities = authorities;
        this.resourceIds = resourceIds;
        this.clientId = clientId;
        this.revocable = revocable;
        this.externalAttributes = externalAttributes;
    }

}
