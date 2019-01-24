package net.unicon.idp.externalauth;

import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import org.apache.commons.lang.StringUtils;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.Cas30ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExternalAuthentication.class, Cas20ServiceTicketValidator.class})
public class ShibcasAuthServletTest {
    private String CONVERSATION = "conversation=e1s1";
    private String CONVERSATION_TICKET = "conversation=e1s1&ticket=ST-1234-123456789-a";
    private String CONVERSATION_TICKET_GATEWAY_ATTEMPTED = "conversation=e1s1&ticket=ST-1234-123456789-a&gatewayAttempted=true";
    private String CONVERSATION_TICKET_EMBEDDED_ENTITYID = "conversation=e1s1&ticket=ST-1234-123456789-a&entityId=http://test.edu/sp";
    private String E1S1 = "E1S1";
    private String JDOE = "jdoe";
    private String TICKET = "ST-1234-123456789-a";
    private String URL_WITH_CONVERSATION = "https://shibserver.example.edu/idp/Authn/ExtCas?conversation=e1s1";
    private String URL_WITH_CONVERSATION_GATEWAY_ATTEMPTED = "https://shibserver.example.edu/idp/Authn/ExtCas?conversation=e1s1&gatewayAttempted=true";
    private String URL_WITH_CONVERSATION_EMBEDDED_ENTITYID = "https://shibserver.example.edu/idp/Authn/ExtCas?conversation=e1s1&entityId=http%3A%2F%2Ftest.edu%2Fsp";

    @Test
    public void testDoGetStandard() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest(CONVERSATION_TICKET, TICKET, null);
        final HttpServletResponse response = createMockHttpServletResponse();
        final Assertion assertion = createMockAssertion();

        final Cas20ServiceTicketValidator ticketValidator = PowerMockito.mock(Cas20ServiceTicketValidator.class);
        PowerMockito.when(ticketValidator.validate(TICKET, URL_WITH_CONVERSATION)).thenReturn(assertion);

        PowerMockito.mockStatic(ExternalAuthentication.class);
        BDDMockito.given(ExternalAuthentication.startExternalAuthentication(request)).willReturn(E1S1);

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();

        //Override the internal Cas20TicketValidator because we don't want it to call a real server
        MemberModifier.field(ShibcasAuthServlet.class, "ticketValidator").set(shibcasAuthServlet, ticketValidator);

        //Standard request/response
        BDDMockito.given(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM)).willReturn("false");
        BDDMockito.given(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM)).willReturn("false");
        shibcasAuthServlet.doGet(request, response);

        //Verify
        verify(request).setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, JDOE);
    }


    @Test
    public void testDoGetBadTicket() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest(CONVERSATION_TICKET, TICKET, "false");
        final HttpServletResponse response = createMockHttpServletResponse();
        final Cas20ServiceTicketValidator ticketValidator = PowerMockito.mock(Cas20ServiceTicketValidator.class);
        PowerMockito.when(ticketValidator.validate(TICKET, URL_WITH_CONVERSATION)).thenThrow(new TicketValidationException("Invalid Ticket"));

        PowerMockito.mockStatic(ExternalAuthentication.class);
        BDDMockito.given(ExternalAuthentication.startExternalAuthentication(request)).willThrow(new ExternalAuthenticationException());

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();

        //Override the internal Cas20TicketValidator because we don't want it to call a real server
        MemberModifier.field(ShibcasAuthServlet.class, "ticketValidator").set(shibcasAuthServlet, ticketValidator);

        //Standard request/response - bad ticket
        BDDMockito.given(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM)).willReturn("false");
        BDDMockito.given(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM)).willReturn("false");
        shibcasAuthServlet.doGet(request, response);

        //Verify
        verify(request).getRequestDispatcher("/no-conversation-state.jsp");
        verify(response).setStatus(404);
    }


    @Test
    public void testDoGetPassiveAuthenticated() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest(CONVERSATION_TICKET + "&gatewayAttempted=true", TICKET, "true");
        final HttpServletResponse response = createMockHttpServletResponse();
        final Assertion assertion = createMockAssertion();

        final Cas20ServiceTicketValidator ticketValidator = PowerMockito.mock(Cas20ServiceTicketValidator.class);
        PowerMockito.when(ticketValidator.validate(TICKET, URL_WITH_CONVERSATION_GATEWAY_ATTEMPTED)).thenReturn(assertion);

        PowerMockito.mockStatic(ExternalAuthentication.class);
        BDDMockito.given(ExternalAuthentication.startExternalAuthentication(request)).willReturn(E1S1);

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();

        //Override the internal Cas20TicketValidator because we don't want it to call a real server
        MemberModifier.field(ShibcasAuthServlet.class, "ticketValidator").set(shibcasAuthServlet, ticketValidator);

        //Passive request/response with authenticated user
        BDDMockito.given(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM)).willReturn("false");
        BDDMockito.given(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM)).willReturn("true");
        shibcasAuthServlet.doGet(request, response);

        //Verify
        verify(request).setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, JDOE);
    }

    @Test
    public void testDoGetPassiveNotAuthenticated() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest("conversation=e1s1&gatewayAttempted=true", null, "true");
        final HttpServletResponse response = createMockHttpServletResponse();

        final Cas20ServiceTicketValidator ticketValidator = PowerMockito.mock(Cas20ServiceTicketValidator.class);

        PowerMockito.mockStatic(ExternalAuthentication.class);
        BDDMockito.given(ExternalAuthentication.startExternalAuthentication(request)).willReturn(E1S1);

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();

        //Override the internal Cas20TicketValidator because we don't want it to call a real server
        MemberModifier.field(ShibcasAuthServlet.class, "ticketValidator").set(shibcasAuthServlet, ticketValidator);

        //Passive request/response with no user
        BDDMockito.given(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM)).willReturn("false");
        BDDMockito.given(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM)).willReturn("true");
        shibcasAuthServlet.doGet(request, response);

        //Verify
        verify(request, never()).setAttribute(eq(ExternalAuthentication.PRINCIPAL_NAME_KEY), any());
        verify(request).setAttribute(ExternalAuthentication.AUTHENTICATION_ERROR_KEY, "NoPassive");
        verify(ticketValidator, never()).validate(anyString(), anyString());
    }

    @Test
    public void testDoGetForced() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest(CONVERSATION_TICKET, TICKET, null);
        final HttpServletResponse response = createMockHttpServletResponse();
        final Assertion assertion = createMockAssertion();

        final Cas20ServiceTicketValidator ticketValidator = PowerMockito.mock(Cas20ServiceTicketValidator.class);
        PowerMockito.when(ticketValidator.validate(TICKET, URL_WITH_CONVERSATION)).thenReturn(assertion);

        PowerMockito.mockStatic(ExternalAuthentication.class);
        BDDMockito.given(ExternalAuthentication.startExternalAuthentication(request)).willReturn(E1S1);

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();

        //Override the internal Cas20TicketValidator because we don't want it to call a real server
        MemberModifier.field(ShibcasAuthServlet.class, "ticketValidator").set(shibcasAuthServlet, ticketValidator);

        //Forced request/response
        BDDMockito.given(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM)).willReturn("true");
        BDDMockito.given(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM)).willReturn("false");
        shibcasAuthServlet.doGet(request, response);

        //Verify
        verify(request).setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, JDOE);
    }

    @Test
    public void testDoGetPassiveAndForced() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest(CONVERSATION_TICKET_GATEWAY_ATTEMPTED, TICKET, "true");
        final HttpServletResponse response = createMockHttpServletResponse();
        final Assertion assertion = createMockAssertion();

        final Cas20ServiceTicketValidator ticketValidator = PowerMockito.mock(Cas30ServiceTicketValidator.class);
        PowerMockito.when(ticketValidator.validate(TICKET, URL_WITH_CONVERSATION_GATEWAY_ATTEMPTED)).thenReturn(assertion);

        PowerMockito.mockStatic(ExternalAuthentication.class);
        BDDMockito.given(ExternalAuthentication.startExternalAuthentication(request)).willReturn(E1S1);

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();

        //Override the internal Cas30TicketValidator because we don't want it to call a real server
        MemberModifier.field(ShibcasAuthServlet.class, "ticketValidator").set(shibcasAuthServlet, ticketValidator);

        //Passive and forced request/response
        BDDMockito.given(request.getAttribute(ExternalAuthentication.FORCE_AUTHN_PARAM)).willReturn("true");
        BDDMockito.given(request.getAttribute(ExternalAuthentication.PASSIVE_AUTHN_PARAM)).willReturn("true");
        shibcasAuthServlet.doGet(request, response);

        //Verify
        verify(request).setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, JDOE);
    }


    @Test
    public void testConstructServiceUrlAppend() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest(CONVERSATION_TICKET, TICKET, "false");
        final HttpServletResponse response = createMockHttpServletResponse();

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();

        final String result = shibcasAuthServlet.constructServiceUrl(request, response);

        assertEquals("https://shibserver.example.edu/idp/Authn/ExtCas?conversation=e1s1", result);
    }

    @Test
    public void testConstructServiceUrlEmbed() throws Exception {
        //Mock some objects.
        final HttpServletRequest request = createDoGetHttpServletRequest(CONVERSATION_TICKET + "&entityId=http://test.edu/sp", TICKET, "false");
        final HttpServletResponse response = createMockHttpServletResponse();

        //Prep our object
        final ShibcasAuthServlet shibcasAuthServlet = createShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig("embed"));

        final String result = shibcasAuthServlet.constructServiceUrl(request, response, true);

        assertEquals("https://shibserver.example.edu/idp/Authn/ExtCas?conversation=e1s1&entityId=http%3A%2F%2Ftest.edu%2Fsp", result);
    }


    @Test
    public void testStartLoginRequestStandard() throws Exception {
        final HttpServletRequest request = createMockHttpServletRequest();
        BDDMockito.given(request.getQueryString()).willReturn(CONVERSATION);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION)).willReturn(URL_WITH_CONVERSATION);

        final ShibcasAuthServlet shibcasAuthServlet = new ShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig());

        shibcasAuthServlet.startLoginRequest(request, response, false, false, "");
        verify(response).sendRedirect("https://cassserver.example.edu/cas/login?service=https%3A%2F%2Fshibserver.example.edu%2Fidp%2FAuthn%2FExtCas%3Fconversation%3De1s1&entityId=http%3A%2F%2Ftest.edu%2Fsp");
    }

    @Test
    public void testStartLoginRequestEmbeddedEntityId() throws Exception {
        final HttpServletRequest request = createMockHttpServletRequest();
        BDDMockito.given(request.getQueryString()).willReturn(CONVERSATION);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION)).willReturn(URL_WITH_CONVERSATION);

        final ShibcasAuthServlet shibcasAuthServlet = new ShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig("embed"));

        shibcasAuthServlet.startLoginRequest(request, response, false, false, "");
        verify(response).sendRedirect("https://cassserver.example.edu/cas/login?service=https%3A%2F%2Fshibserver.example.edu%2Fidp%2FAuthn%2FExtCas%3Fconversation%3De1s1%26entityId%3Dhttp%3A%2F%2Ftest.edu%2Fsp");
    }

    @Test
    public void testStartLoginRequestAppendedEntityId() throws Exception {
        final HttpServletRequest request = createMockHttpServletRequest();
        BDDMockito.given(request.getQueryString()).willReturn(CONVERSATION);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION)).willReturn(URL_WITH_CONVERSATION);

        final ShibcasAuthServlet shibcasAuthServlet = new ShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig("append"));

        shibcasAuthServlet.startLoginRequest(request, response, false, false, "");
        verify(response).sendRedirect("https://cassserver.example.edu/cas/login?service=https%3A%2F%2Fshibserver.example.edu%2Fidp%2FAuthn%2FExtCas%3Fconversation%3De1s1&entityId=http%3A%2F%2Ftest.edu%2Fsp");
    }

    @Test
    public void testStartLoginRequestPassive() throws Exception {
        final HttpServletRequest request = createMockHttpServletRequest();
        BDDMockito.given(request.getQueryString()).willReturn(CONVERSATION);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION)).willReturn(URL_WITH_CONVERSATION);

        final ShibcasAuthServlet shibcasAuthServlet = new ShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig());

        //Passive
        shibcasAuthServlet.startLoginRequest(request, response, false, true, "");
        verify(response).sendRedirect("https://cassserver.example.edu/cas/login?service=https%3A%2F%2Fshibserver.example.edu%2Fidp%2FAuthn%2FExtCas%3Fconversation%3De1s1%26gatewayAttempted%3Dtrue&gateway=true&entityId=http%3A%2F%2Ftest.edu%2Fsp");
    }

    @Test
    public void testStartLoginRequestForced() throws Exception {
        final HttpServletRequest request = createMockHttpServletRequest();
        BDDMockito.given(request.getQueryString()).willReturn(CONVERSATION);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION)).willReturn(URL_WITH_CONVERSATION);

        final ShibcasAuthServlet shibcasAuthServlet = new ShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig());

        //Forced
        shibcasAuthServlet.startLoginRequest(request, response, true, false, "");
        verify(response).sendRedirect("https://cassserver.example.edu/cas/login?service=https%3A%2F%2Fshibserver.example.edu%2Fidp%2FAuthn%2FExtCas%3Fconversation%3De1s1&renew=true&entityId=http%3A%2F%2Ftest.edu%2Fsp");
       }

    @Test
    public void testStartLoginRequestPassiveAndForced() throws Exception {
        final HttpServletRequest request = createMockHttpServletRequest();
        BDDMockito.given(request.getQueryString()).willReturn(CONVERSATION);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION)).willReturn(URL_WITH_CONVERSATION);

        final ShibcasAuthServlet shibcasAuthServlet = new ShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig());

        //Passive and Forced
        shibcasAuthServlet.startLoginRequest(request, response, true, true, "");
        verify(response).sendRedirect("https://cassserver.example.edu/cas/login?service=https%3A%2F%2Fshibserver.example.edu%2Fidp%2FAuthn%2FExtCas%3Fconversation%3De1s1%26gatewayAttempted%3Dtrue&renew=true&gateway=true&entityId=http%3A%2F%2Ftest.edu%2Fsp");
    }

    private HttpServletRequest createDoGetHttpServletRequest(final String queryString, final String ticket, final String gatewayAttempted) {
        final HttpServletRequest request = createMockHttpServletRequest();

        BDDMockito.given(request.getQueryString()).willReturn(queryString);
        BDDMockito.given(request.getParameter("ticket")).willReturn(ticket);
        BDDMockito.given(request.getParameter("gatewayAttempted")).willReturn(gatewayAttempted);

        return request;
    }

    private Assertion createMockAssertion() {
        final Assertion assertion = Mockito.mock(Assertion.class);
        final AttributePrincipal attributePrincipal = Mockito.mock(AttributePrincipal.class);

        BDDMockito.given(attributePrincipal.getName()).willReturn(JDOE);
        BDDMockito.given(assertion.getPrincipal()).willReturn(attributePrincipal);

        return assertion;
    }


    private ServletConfig createMockServletConfig() {
        return createMockServletConfig("");
    }

    private ServletConfig createMockServletConfig(final String entityIdLocation) {
        final ServletConfig config = Mockito.mock(ServletConfig.class);
        final ServletContext servletContext = Mockito.mock(ServletContext.class);
        final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        final Environment environment = Mockito.mock(Environment.class);
        
        BDDMockito.given(config.getServletContext()).willReturn(servletContext);
        BDDMockito.given(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).willReturn(applicationContext);
        BDDMockito.given(applicationContext.getEnvironment()).willReturn(environment);
        BDDMockito.given(environment.getRequiredProperty("shibcas.casServerUrlPrefix")).willReturn("https://cassserver.example.edu/cas");
        BDDMockito.given(environment.getRequiredProperty("shibcas.casServerLoginUrl")).willReturn("https://cassserver.example.edu/cas/login");
        BDDMockito.given(environment.getRequiredProperty("shibcas.serverName")).willReturn("https://shibserver.example.edu");
        BDDMockito.given(environment.getRequiredProperty("shibcas.casToShibTranslators")).willReturn(null);
        BDDMockito.given(environment.getProperty("shibcas.ticketValidatorName", "cas30")).willReturn("cas30");

        if (StringUtils.isNotEmpty(entityIdLocation)) {
            BDDMockito.given(environment.getProperty("shibcas.entityIdLocation", "append")).willReturn(entityIdLocation);
        } else {
            BDDMockito.given(environment.getProperty("shibcas.entityIdLocation", "append")).willReturn("append");
        }

        return config;
    }

    private HttpServletRequest createMockHttpServletRequest() {
        try {
            final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

            BDDMockito.given(request.getScheme()).willReturn("http");
            BDDMockito.given(request.getMethod()).willReturn("GET");
            BDDMockito.given(request.isSecure()).willReturn(true);
            BDDMockito.given(request.getHeader("Host")).willReturn("shibserver.example.edu");
            //BDDMockito.given(request.getHeader("X-Forwarded-Host")).willReturn();
            BDDMockito.given(request.getServerPort()).willReturn(443);
            BDDMockito.given(request.getRequestURI()).willReturn("/idp/Authn/ExtCas");
            BDDMockito.given(request.getRequestURL()).willReturn(new StringBuffer("/idp/Authn/ExtCas"));
            BDDMockito.given(request.getAttribute(ExternalAuthentication.RELYING_PARTY_PARAM)).willReturn("http://test.edu/sp");

            return request;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpServletResponse createMockHttpServletResponse() {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION)).willReturn(URL_WITH_CONVERSATION);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION_GATEWAY_ATTEMPTED)).willReturn(URL_WITH_CONVERSATION_GATEWAY_ATTEMPTED);
        BDDMockito.given(response.encodeURL(URL_WITH_CONVERSATION_EMBEDDED_ENTITYID)).willReturn(URL_WITH_CONVERSATION_EMBEDDED_ENTITYID);

        return response;
    }

    private ShibcasAuthServlet createShibcasAuthServlet() throws ServletException {
        final ShibcasAuthServlet shibcasAuthServlet;
        shibcasAuthServlet = new ShibcasAuthServlet();
        shibcasAuthServlet.init(createMockServletConfig());

        return shibcasAuthServlet;
    }

}
