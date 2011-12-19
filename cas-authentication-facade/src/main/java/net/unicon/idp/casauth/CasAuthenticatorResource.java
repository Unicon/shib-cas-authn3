package net.unicon.idp.casauth;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * An HTTP resource protected by CAS and acting as an external authentication facade used by Shibboleth IdP external authentication LoginHandler
 *
 * @author Dmitriy Kopylenko
 */

@Component
@Path("/facade")
public class CasAuthenticatorResource {

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    private static final String CAS_ASSERTION_KEY = "_const_cas_assertion_";

    @GET
    @Path("renew")
    public Response renewAuth(@QueryParam("idp") final String u) throws IOException {
        return redirectBackToIdp(u);
    }

    @GET
    @Path("norenew")
    public Response noRenewAuth(@QueryParam("idp") final String u) throws IOException {
        return redirectBackToIdp(u);
    }

    private Response redirectBackToIdp(String idpCallbackUrl) throws IOException {
        Assertion casAssertion = (Assertion) this.request.getSession(false).getAttribute(CAS_ASSERTION_KEY);
        this.response.sendRedirect(this.response.encodeRedirectURL(idpCallbackUrl + "?p=" + casAssertion.getPrincipal().getName()));
        //HTTP 204
        return Response.noContent().build();
    }
}
