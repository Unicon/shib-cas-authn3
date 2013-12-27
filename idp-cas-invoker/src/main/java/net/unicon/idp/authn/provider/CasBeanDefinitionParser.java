package net.unicon.idp.authn.provider;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.idp.config.profile.authn.AbstractLoginHandlerBeanDefinitionParser;

public class CasBeanDefinitionParser extends AbstractLoginHandlerBeanDefinitionParser {
    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(CasNamespaceHandler.NAMESPACE, "CasLoginHandler");

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Class getBeanClass(Element element) {
        return CasLoginHandlerFactoryBean.class;
    }

    /** {@inheritDoc} */
    @Override
    protected void doParse(Element config, BeanDefinitionBuilder builder) {
        super.doParse(config, builder);
        builder.addPropertyValue("casLoginUrl", safeTrim(config.getAttributeNS(null, "casLoginUrl")));
        builder.addPropertyValue("callbackUrl", safeTrim(config.getAttributeNS(null, "callbackUrl")));
    }

    private String safeTrim(String s) {
        if (s != null) {
            return s.trim();
        }
        return null;
    }
}
