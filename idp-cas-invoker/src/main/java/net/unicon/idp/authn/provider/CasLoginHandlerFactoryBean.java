package net.unicon.idp.authn.provider;

import edu.internet2.middleware.shibboleth.idp.config.profile.authn.AbstractLoginHandlerFactoryBean;

public class CasLoginHandlerFactoryBean extends AbstractLoginHandlerFactoryBean {
    private String callbackUrl;
    private String casLoginUrl;

    @Override
    protected Object createInstance() throws Exception {
        CasLoginHandler handler = new CasLoginHandler(casLoginUrl, callbackUrl);
        populateHandler(handler);
        return handler;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getCasLoginUrl() {
        return casLoginUrl;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getObjectType() {
        return CasLoginHandler.class;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setCasLoginUrl(String casLoginUrl) {
        this.casLoginUrl = casLoginUrl;
    }

}
