package net.unicon.idp.authn.provider.extra;

import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.authn.ExternalAuthentication;

/**
 * Generates a querystring parameter containing the entityId
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
public class EntityIdParameterBuilder implements IParameterBuilder {

    /**
     * Create the param=value pair of entityId=[Shib relaying party id].
     * @param request The original servlet request.
     * @see net.unicon.idp.authn.provider.extra.IParameterBuilder#getParameterString(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public String getParameterString(final HttpServletRequest request) {
        String relayingPartyId = request.getAttribute(ExternalAuthentication.RELYING_PARTY_PARAM).toString();
        return "&entityId=" + relayingPartyId;
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof EntityIdParameterBuilder;
    }

}
