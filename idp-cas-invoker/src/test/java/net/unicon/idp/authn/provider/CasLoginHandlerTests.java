package net.unicon.idp.authn.provider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.provider.ExternalAuthnSystemLoginHandler;

/**
 * Validates the CasLoginHandler code
 * @author chasegawa@unicon.net
 */
public class CasLoginHandlerTests {
    private Reader getReader(final String file) {
        return new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(file));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoPropertiesFile() throws FileNotFoundException {
        new CasLoginHandler("notAValidPathToPropertiesFile.txt", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingCasServerConfig() {
        new CasLoginHandler(getReader("missingCasServerProp.properties"), "missingCasServerProp.properties", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingIDPServerConfig() {
        new CasLoginHandler(getReader("missingIDPServerProp.properties"), "missingIDPServerProp.properties", "");
    }

    @Test
    public void testSimpleConfigCallsCasCorrectly() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        BDDMockito.given(request.getSession()).willReturn(session);

        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        // Check nothing set in session that shouldn't be there
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertTrue(StringUtils.isEmpty(argument.getValue()));

        argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).encodeRedirectURL(argument.capture());
        Assert.assertEquals("Incorrect URL built",
                "https://localhost:443/cas/login?service=https://localhost:443/idp/Authn/Cas", argument.getValue());
    }

    @Test
    public void testCustomConfigCallsCasCorrectly() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        BDDMockito.given(request.getSession()).willReturn(session);
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("customProps.properties"), "customProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "&renew=true", argument.getValue());

        argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).encodeRedirectURL(argument.capture());
        Assert.assertEquals("Incorrect URL built",
                "hhttttppss://casserv:8443/CAS/login?service=sptth://idpserv:9443/pdi/my/Casback&renew=true",
                argument.getValue());
    }

    @Test
    public void testSimpleConfigWithRenew() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        BDDMockito.given(request.getSession()).willReturn(session);
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "&renew=true", argument.getValue());
    }

    @Test
    public void testSimpleConfigWithGateway() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        BDDMockito.given(request.getSession()).willReturn(session);
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "&gateway=true", argument.getValue());
    }

    @Test
    public void testSimpleConfigWithRenewAndGateway() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        BDDMockito.given(request.getSession()).willReturn(session);
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);
        BDDMockito.given(request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM)).willReturn(
                Boolean.TRUE);

        LoginHandler handler = new CasLoginHandler(getReader("simpleProps.properties"), "simpleProps.properties", "");
        handler.login(request, response);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).setAttribute(Mockito.anyString(), argument.capture());
        Assert.assertEquals("Value set in session was incorrect", "&renew=true", argument.getValue());
    }
}
