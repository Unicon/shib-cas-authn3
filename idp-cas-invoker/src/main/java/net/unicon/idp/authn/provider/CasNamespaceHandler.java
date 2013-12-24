package net.unicon.idp.authn.provider;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

public class CasNamespaceHandler extends BaseSpringNamespaceHandler {
    /** Namespace URI. */
    public static final String NAMESPACE = "http://unicon.net/shib-cas/authn";

    /**
     * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
     */
    @Override
    public void init() {
        registerBeanDefinitionParser(CasBeanDefinitionParser.SCHEMA_TYPE, new CasBeanDefinitionParser());
    }
}
