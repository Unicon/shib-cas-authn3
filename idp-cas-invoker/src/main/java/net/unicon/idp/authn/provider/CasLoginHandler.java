package net.unicon.idp.authn.provider;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.internet2.middleware.shibboleth.idp.authn.provider.AbstractLoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.provider.ExternalAuthnSystemLoginHandler;

/**
 * CasLoginHandler replaces the {@link CasInvokerServlet} AND {@link CasAuthenticatorResource} (facade). Allows simplification
 * of the SHIB-CAS authenticator by removing the need to configure and deploy a separate war. The configuration is moved/handled 
 * in the SHIB idp web.xml and the idp handler.xml files.
 * @author chasegawa@unicon.net
 */
public class CasLoginHandler extends AbstractLoginHandler {
    private String casLoginUrl;

    /**
     * All attributes/parameters required
     * @param postAuthnCallbackUrl
     * @param casResourceUrl
     */
    public CasLoginHandler(String casResourceUrl) {
        if (isEmpty(casResourceUrl)) {
            throw new IllegalArgumentException(
                    "CasLoginHandler missing casResourceUrl attribute in handler configuration.");
        }
        this.casLoginUrl = casResourceUrl;
    }

    /**
     * Essentially StringUtils.isEmpty, but put this here to avoid another jar/dependency
     * @param string
     * @return
     */
    private boolean isEmpty(String string) {
        return null == string || "".equals(string.trim());
    }

    /**
     * @see edu.internet2.middleware.shibboleth.idp.authn.LoginHandler#login(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void login(HttpServletRequest request, HttpServletResponse response) {
        Boolean force = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.FORCE_AUTHN_PARAM);
        if (null == force) {
            force = Boolean.FALSE;
        }
        setSupportsForceAuthentication(force);
        String authnType = (force) ? "/renew" : "/norenew";

        Boolean passive = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM);
        if (null != passive) {
            setSupportsPassive(passive);
            if (passive) {
                authnType += "gateway";
            }
        }

        try {
            response.sendRedirect(response.encodeRedirectURL(this.casLoginUrl + authnType));
        } catch (IOException e) {
            // log this and then what?
        }
    }
}
