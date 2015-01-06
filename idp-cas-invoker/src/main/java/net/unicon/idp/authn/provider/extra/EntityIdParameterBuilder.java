package net.unicon.idp.authn.provider.extra;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.ExternalAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a querystring parameter containing the entityId
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
public class EntityIdParameterBuilder implements IParameterBuilder {
    private Logger logger = LoggerFactory.getLogger(EntityIdParameterBuilder.class);

    /**
     * Create the param=value pair of entityId=[Shib relaying party id].
     * @param request The original servlet request.
     * @see net.unicon.idp.authn.provider.extra.IParameterBuilder#getParameterString(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public String getParameterString(final HttpServletRequest request) {
        final String relayingPartyId = request.getAttribute(ExternalAuthentication.RELYING_PARTY_PARAM).toString();
        String rpId = "error-encoding-rpid";

        try {
            rpId = URLEncoder.encode(relayingPartyId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding the relying party id.", e);
        }

        return "&entityId=" + rpId;
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
