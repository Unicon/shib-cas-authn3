package net.unicon.idp.authn.provider;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.internet2.middleware.shibboleth.idp.authn.provider.AbstractLoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.provider.ExternalAuthnSystemLoginHandler;

/**
 * CasLoginHandler replaces the {@link CasInvokerServlet} AND {@link CasAuthenticatorResource} (facade) from the earlier implementations. 
 * Allows simplification of the SHIB-CAS authenticator by removing the need to configure and deploy a separate war.
 * @author chasegawa@unicon.net
 */
public class CasLoginHandler extends AbstractLoginHandler {
    private String casLoginUrl;
    private String callbackUrl;

    /**
     * All attributes/parameters required
     * @param postAuthnCallbackUrl
     * @param casResourceUrl
     */
    public CasLoginHandler(String casLoginUrl, String callbackUrl) {
        if (isEmpty(casLoginUrl)) {
            throw new IllegalArgumentException(
                    "CasLoginHandler missing casLoginUrl attribute in handler configuration.");
        }
        this.casLoginUrl = casLoginUrl;
        if (isEmpty(callbackUrl)) {
            throw new IllegalArgumentException(
                    "CasLoginHandler missing callbackUrl attribute in handler configuration.");
        }
        this.callbackUrl = callbackUrl;
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
        String authnType = (force) ? "&renew=false" : "&renew=true";

        Boolean passive = (Boolean) request.getAttribute(ExternalAuthnSystemLoginHandler.PASSIVE_AUTHN_PARAM);
        if (null != passive) {
            setSupportsPassive(passive);
            if (passive) {
                authnType += "&gateway=true";
            }
        }

        try {
            response.sendRedirect(response.encodeRedirectURL(casLoginUrl + "?service=" + callbackUrl + authnType));
        } catch (IOException e) {
            // log this and then what?
        }
    }
}
