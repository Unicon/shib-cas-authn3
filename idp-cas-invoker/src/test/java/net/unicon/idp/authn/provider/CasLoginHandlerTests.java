package net.unicon.idp.authn.provider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.opensaml.util.storage.StorageService;

import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContextEntry;
import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.provider.ExternalAuthnSystemLoginHandler;

/**
 * Validates the CasLoginHandler code
 * @author chasegawa@unicon.net
 */
public class CasLoginHandlerTests {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    /**
     * Mock out the request, response and session
     */
    @Before
    public void beforeTests() {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        session = Mockito.mock(HttpSession.class);
        BDDMockito.given(request.getSession()).willReturn(session);
        ServletContext context = Mockito.mock(ServletContext.class);
        BDDMockito.given(session.getServletContext()).willReturn(context);
        @SuppressWarnings("unchecked")
        StorageService<String, Object> storageService = Mockito.mock(StorageService.class);
        BDDMockito.given(context.getAttribute(Mockito.anyString())).willReturn(storageService);
        LoginContextEntry loginContextEntry = Mockito.mock(LoginContextEntry.class);
        BDDMockito.given(loginContextEntry.isExpired()).willReturn(false);
        LoginContext loginContext = Mockito.mock(LoginContext.class);
        BDDMockito.given(loginContext.getRelyingPartyId()).willReturn("dummyPartyId");
        BDDMockito.given(loginContextEntry.getLoginContext()).willReturn(loginContext);
        BDDMockito.given(storageService.get(Mockito.anyString(), Mockito.anyString())).willReturn(loginContextEntry);
        Cookie cookie = Mockito.mock(Cookie.class);
        BDDMockito.given(cookie.getName()).willReturn("_idp_authn_lc_key");
        BDDMockito.given(cookie.getValue()).willReturn("anythingNotNull");
        Cookie[] requestCookies = { cookie };
        BDDMockito.given(request.getCookies()).willReturn(requestCookies);
    }

    private Reader getReader(final String file) {
        return new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(file));
    }

    @Test
    public void testCustomConfigCallsCasCorrectly() throws IOException {
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("customProps.properties"), "customProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "renew=true", argument.getValue());

        argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).encodeRedirectURL(argument.capture());
        Assert.assertEquals("Incorrect URL built",
                "hhttttppss://casserv:8443/CAS/login?renew=true&entityId=dummyPartyId&service=sptth://idpserv:9443/pdi/my/Casback",
                argument.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingCasServerConfig() {
        new CasLoginHandler(getReader("missingCasServerProp.properties"), "missingCasServerProp.properties", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingIDPServerConfig() {
        new CasLoginHandler(getReader("missingIDPServerProp.properties"), "missingIDPServerProp.properties", "");
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoPropertiesFile() throws FileNotFoundException {
        new CasLoginHandler("notAValidPathToPropertiesFile.txt", "");
    }

    @Test
    public void testSimpleConfigCallsCasCorrectly() throws IOException {
        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        // Check nothing set in session that shouldn't be there
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertTrue(StringUtils.isEmpty(argument.getValue()));

        argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).encodeRedirectURL(argument.capture());
        Assert.assertEquals("Incorrect URL built",
                "https://localhost:443/cas/login?entityId=dummyPartyId&service=https://localhost:443/idp/Authn/Cas",
                argument.getValue());
    }

    /**
     * https://github.com/Unicon/shib-cas-authn2/issues/6
     * EntityIdParameterBuilder is in the Login Handler by default. When it is explicitly added, it should not end up in the map 
     * twice (or more).
     */
    @Test
    public void testSimpleConfigWithHandlerAddedTwiceDoesNotAddEntityIdTwice() throws IOException {
        // Add the same builder a couple of extra times.
        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties",
                "net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder, net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder");
        handler.login(request, response);

        // Check nothing set in session that shouldn't be there
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertTrue(StringUtils.isEmpty(argument.getValue()));

        argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).encodeRedirectURL(argument.capture());
        Assert.assertEquals("Incorrect URL built",
                "https://localhost:443/cas/login?entityId=dummyPartyId&service=https://localhost:443/idp/Authn/Cas",
                argument.getValue());
    }

    @Test
    public void testSimpleConfigWithGateway() {
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "gateway=true", argument.getValue());
    }

    @Test
    public void testSimpleConfigWithRenew() {
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "renew=true", argument.getValue());

        argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).encodeRedirectURL(argument.capture());
        Assert.assertEquals("Incorrect URL built",
                "https://localhost:443/cas/login?renew=true&entityId=dummyPartyId&service=https://localhost:443/idp/Authn/Cas",
                argument.getValue());
    }

    @Test
    public void testSimpleConfigWithRenewAndGateway() {
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "renew=true&gateway=true", argument.getValue());
    }
}
