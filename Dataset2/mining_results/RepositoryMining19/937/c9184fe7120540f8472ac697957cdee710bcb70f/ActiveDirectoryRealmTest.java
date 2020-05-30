package org.jsecurity;

import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.AuthenticationToken;
import org.jsecurity.authc.UsernamePasswordToken;
import org.jsecurity.authc.credential.CredentialMatcher;
import org.jsecurity.authc.support.SimpleAuthenticationInfo;
import org.jsecurity.authz.AuthorizationInfo;
import org.jsecurity.authz.support.SimpleAuthorizationInfo;
import org.jsecurity.context.SecurityContext;
import org.jsecurity.realm.support.AuthorizingRealm;
import org.jsecurity.realm.support.activedirectory.ActiveDirectoryRealm;
import org.jsecurity.realm.support.ldap.LdapContextFactory;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple test case for ActiveDirectoryRealm.
 *
 * todo:  While the original incarnation of this test case does not actually test the
 * heart of ActiveDirectoryRealm (no meaningful implemenation of queryForLdapAuthenticationInfo, etc) it obviously should.
 * This version was intended to mimic my current usage scenario in an effort to debug upgrade issues which were not related
 * to LDAP connectivity.
 *
 * @author Tim Veil
 */
public class ActiveDirectoryRealmTest {

    DefaultSecurityManager securityManager = null;
    AuthorizingRealm realm;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password";
    private static final int USER_ID = 12345;
    private static final String ROLE = "admin";

    @Before
    public void setup() {
        realm = new TestActiveDirectoryRealm();
        securityManager = new DefaultSecurityManager(realm);

    }

    @After
    public void tearDown() {
        securityManager.destroy();
    }

    @Test
    public void testDefaultConfig() {
        securityManager.init();
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        SecurityContext secCtx = securityManager.authenticate(new UsernamePasswordToken(USERNAME, PASSWORD, localhost));
        assertTrue(secCtx.isAuthenticated());
        assertTrue(secCtx.hasRole(ROLE));



        UsernamePrincipal usernamePrincipal = (UsernamePrincipal) secCtx.getPrincipalByType(UsernamePrincipal.class);
        assertTrue(usernamePrincipal.getUsername().equals(USERNAME));



        UserIdPrincipal userIdPrincipal = (UserIdPrincipal) secCtx.getPrincipalByType(UserIdPrincipal.class);
        assertTrue(userIdPrincipal.getUserId() == USER_ID);

        assertTrue(secCtx.getAllPrincipals().size() == 3);

        assertTrue(realm.hasRole(userIdPrincipal, ROLE));

        secCtx.invalidate();
    }

    public class TestActiveDirectoryRealm extends ActiveDirectoryRealm {

        /*--------------------------------------------
        |         C O N S T R U C T O R S           |
            ============================================*/
        CredentialMatcher credentialMatcher;

        public TestActiveDirectoryRealm() {
            super();


            credentialMatcher = new CredentialMatcher() {
                public boolean doCredentialsMatch(Object object, Object object1) {
                    return true;
                }
            };

            setCredentialMatcher(credentialMatcher);
        }


        protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

            SimpleAuthenticationInfo authInfo = (SimpleAuthenticationInfo) super.doGetAuthenticationInfo(token);

            if (authInfo != null) {

                List<Object> principals = new ArrayList<Object>();
                principals.add(new UserIdPrincipal(USER_ID));
                principals.add(1, new UsernamePrincipal(USERNAME));
                authInfo.setPrincipals( principals );

            }


            return authInfo;

        }

        protected AuthorizationInfo doGetAuthorizationInfo(Object principal) {


            UserIdPrincipal userIdPrincipal = (UserIdPrincipal) principal;
            
            assertTrue(userIdPrincipal.getUserId() == USER_ID);


            List<String> roles = new ArrayList<String>();
            roles.add(ROLE);

            return new SimpleAuthorizationInfo(roles, null);
        }

        // override ldap query because i don't care about testing that piece in this case
        protected AuthenticationInfo queryForLdapAuthenticationInfo(AuthenticationToken token, LdapContextFactory ldapContextFactory) throws NamingException {
            UsernamePasswordToken upToken = (UsernamePasswordToken) token;
            return new SimpleAuthenticationInfo(upToken.getUsername(), upToken.getPassword());
        }

    }


}