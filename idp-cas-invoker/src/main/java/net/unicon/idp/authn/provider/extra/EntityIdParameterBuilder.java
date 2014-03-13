package net.unicon.idp.authn.provider.extra;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;

public class EntityIdParameterBuilder implements IParameterBuilder {

    /**
     * Create the param=value pair of entityId=[Shib relaying party id].
     * @param request The original servlet request.
     * @see net.unicon.idp.authn.provider.extra.IParameterBuilder#getParameterString(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public String getParameterString(final HttpServletRequest request) {
        ServletContext servletContext = request.getSession().getServletContext();
        LoginContext loginContext = HttpServletHelper.getLoginContext(
                HttpServletHelper.getStorageService(servletContext), servletContext, request);
        String relayingPartyId = loginContext.getRelyingPartyId();
        return "&entityId=" + relayingPartyId;
    }

}
