package org.keycloak.proxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;
import org.keycloak.representations.IDToken;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConstraintAuthorizationHandler implements HttpHandler {

    private final Map<String, HttpString> httpHeaderNames;
    protected HttpHandler next;
    protected String errorPage;
    protected boolean sendAccessToken;

    public ConstraintAuthorizationHandler(HttpHandler next, String errorPage, boolean sendAccessToken, Map<String, String> headerNames) {
        this.next = next;
        this.errorPage = errorPage;
        this.sendAccessToken = sendAccessToken;

        this.httpHeaderNames = new HashMap<>();
        this.httpHeaderNames.put("KEYCLOAK_SUBJECT", new HttpString(headerNames.getOrDefault("keycloak-subject", "KEYCLOAK_SUBJECT")));
        this.httpHeaderNames.put("KEYCLOAK_USERNAME", new HttpString(headerNames.getOrDefault("keycloak-username", "KEYCLOAK_USERNAME")));
        this.httpHeaderNames.put("KEYCLOAK_EMAIL", new HttpString(headerNames.getOrDefault("keycloak-email", "KEYCLOAK_EMAIL")));
        this.httpHeaderNames.put("KEYCLOAK_NAME", new HttpString(headerNames.getOrDefault("keycloak-name", "KEYCLOAK_NAME")));
        this.httpHeaderNames.put("KEYCLOAK_ACCESS_TOKEN", new HttpString(headerNames.getOrDefault("keycloak-access-token", "KEYCLOAK_ACCESS_TOKEN")));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        KeycloakUndertowAccount account = (KeycloakUndertowAccount)exchange.getSecurityContext().getAuthenticatedAccount();

        SingleConstraintMatch match = exchange.getAttachment(ConstraintMatcherHandler.CONSTRAINT_KEY);
        if (match == null || (match.getRequiredRoles().isEmpty() && match.getEmptyRoleSemantic() == SecurityInfo.EmptyRoleSemantic.AUTHENTICATE)) {
            authenticatedRequest(account, exchange);
            return;
        }

        if (match != null) {
            if(SecurityInfo.EmptyRoleSemantic.PERMIT_AND_INJECT_IF_AUTHENTICATED.equals(match.getEmptyRoleSemantic())) {
                authenticatedRequest(account, exchange);
                return;
            } else {
                for (String role : match.getRequiredRoles()) {
                    if (account.getRoles().contains(role)) {
                        authenticatedRequest(account, exchange);
                        return;
                    }
                }
            }
        }

        if (errorPage != null) {
            exchange.setRequestPath(errorPage);
            exchange.setRelativePath(errorPage);
            exchange.setResolvedPath(errorPage);
            next.handleRequest(exchange);
            return;

        }
        exchange.setResponseCode(403);
        exchange.endExchange();

    }

    public void authenticatedRequest(KeycloakUndertowAccount account, HttpServerExchange exchange) throws Exception {
        if (account != null) {
            IDToken idToken = account.getKeycloakSecurityContext().getToken();
            if (idToken == null) return;
            if (idToken.getSubject() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get("KEYCLOAK_SUBJECT"), idToken.getSubject());
            }

            if (idToken.getPreferredUsername() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get("KEYCLOAK_USERNAME"), idToken.getPreferredUsername());
            }
            if (idToken.getEmail() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get("KEYCLOAK_EMAIL"), idToken.getEmail());
            }
            if (idToken.getName() != null) {
                exchange.getRequestHeaders().put(httpHeaderNames.get("KEYCLOAK_NAME"), idToken.getName());
            }
            if (sendAccessToken) {
                exchange.getRequestHeaders().put(httpHeaderNames.get("KEYCLOAK_ACCESS_TOKEN"), account.getKeycloakSecurityContext().getTokenString());
            }
        }
        next.handleRequest(exchange);
    }
}
