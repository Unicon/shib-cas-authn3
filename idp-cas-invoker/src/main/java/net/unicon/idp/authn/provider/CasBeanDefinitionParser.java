package net.unicon.idp.authn.provider;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
        String propertiesFile = safeTrim(config.getAttributeNS(null, "propertiesFile"));
        if (null != propertiesFile) {
            builder.addPropertyValue("propertiesFile", propertiesFile);
        }
        NodeList nodeList = config.getChildNodes();
        StringBuilder paramNames = new StringBuilder();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if ("paramBuilder".equals(nodeList.item(i).getLocalName())) {
            String className = nodeList.item(i).getAttributes().getNamedItem("class").getNodeValue();
            if (StringUtils.isNotEmpty(className)) {
                paramNames.append(className);
                paramNames.append(",");
                }
            }
        }
        String result = paramNames.toString();
        if (result.endsWith(",")) {
            result = result.substring(0, result.lastIndexOf(","));
        }
        builder.addPropertyValue("paramBuilderNames", result);
    }

    private String safeTrim(String s) {
        if (s != null) {
            return s.trim();
        }
        return null;
    }
}
