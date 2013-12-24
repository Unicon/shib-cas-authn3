package net.unicon.idp.authn.provider;

import edu.internet2.middleware.shibboleth.idp.config.profile.authn.AbstractLoginHandlerFactoryBean;

public class CasLoginHandlerFactoryBean extends AbstractLoginHandlerFactoryBean {
    private String casResourceUrl;

    @Override
    protected Object createInstance() throws Exception {
        CasLoginHandler handler = new CasLoginHandler(casResourceUrl);
        populateHandler(handler);
        return handler;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getObjectType() {
        return CasLoginHandler.class;
    }

    public String getCasResourceUrl() {
        return casResourceUrl;
    }

    public void setCasResourceUrl(String casLoginUrl) {
        this.casResourceUrl = casLoginUrl;
    }

}
