# Shibboleth IdP External Authentication via CAS plugin

This is a Shibboleth IDP external authentication plugin that delegates the authentication to the 
Central Authentication Server. The biggest advantage of using this component over the plain 
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range 
of native CAS protocol features such as `renew` and `gateway`.

The plugin consists of 2 components:
* A custom Shibboleth LoginHandler to delegate to CAS
* Shibboleth IDP Servlet acting as a bridge between CAS and IDP


Strategy for sharing state between CASified resource and IdP
-------------------------------------------------------------
This project provides a custom Shibboleth LoginHandler and servlet. The handler prepares the redirect to CAS and the servlet handles pulling out the authenticated username and passing that back to Shibboleth.

Software Requirements
-------------------------------------------------------------

* This plugin will require Shibboleth Identity Provider v2.4.0 and above.


Configure build and deploy IdP external authentication plugin
-------------------------------------------------------------
* Add the CAS filters and the IDP External Auth Callback Servlet entry in `$CATALINA_HOME/webapps/idp/WEB-INF/web.xml`

Example `web.xml`:

```xml
<!-- CAS Authentication Filter -->
<filter>
    <filter-name>CAS Authentication Filter</filter-name>
    <filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
    <init-param>
        <param-name>casServerLoginUrl</param-name>
        <param-value>https://sso.server.edu/cas/login</param-value>
    </init-param>
    <init-param>
        <param-name>serverName</param-name>
        <param-value>https://sso.server.edu</param-value>
    </init-param>
    <init-param>
        <param-name>renew</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>
 
<!-- CAS Validation Filter -->
<filter>
    <filter-name>CAS Validation Filter</filter-name>
    <filter-class>org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter</filter-class>
    <init-param>
        <param-name>casServerUrlPrefix</param-name>
        <param-value>https://sso.server.edu/cas</param-value>
    </init-param>
    <init-param>
        <param-name>serverName</param-name>
        <param-value>https://sso.server.edu</param-value>
    </init-param>
    <init-param>
        <param-name>useSession</param-name>
        <param-value>false</param-value>
    </init-param>
    <init-param>
        <param-name>redirectAfterValidation</param-name>
        <param-value>false</param-value>
    </init-param>
</filter>
 
<!-- CAS Authentication Filter - forceAuthn  -->
<filter>
    <filter-name>CAS Authentication Filter - forceAuthn</filter-name>
    <filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
    <init-param>
        <param-name>casServerLoginUrl</param-name>
        <param-value>https://sso.server.edu/cas/login</param-value>
    </init-param>
    <init-param>
        <param-name>serverName</param-name>
        <param-value>https://sso.server.edu</param-value>
    </init-param>
    <init-param>
        <param-name>renew</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>
 
<!-- CAS Validation Filter - forceAuthn -->
<filter>
    <filter-name>CAS Validation Filter - forceAuthn</filter-name>
    <filter-class>org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter</filter-class>
    <init-param>
        <param-name>casServerUrlPrefix</param-name>
        <param-value>https://sso.server.edu/cas</param-value>
    </init-param>
    <init-param>
        <param-name>serverName</param-name>
        <param-value>https://sso.server.edu</param-value>
    </init-param>
    <init-param>
        <param-name>useSession</param-name>
        <param-value>false</param-value>
    </init-param>
    <init-param>
        <param-name>redirectAfterValidation</param-name>
        <param-value>false</param-value>
    </init-param>
</filter>
 
<!-- CAS HttpServletRequest Wrapper Filter -->
<filter>
    <filter-name>CAS HttpServletRequest Wrapper Filter</filter-name>
    <filter-class>org.jasig.cas.client.util.HttpServletRequestWrapperFilter</filter-class>
</filter>
 
<!-- CAS Assertion Thread Local Filter -->
<filter>
    <filter-name>CAS Assertion Thread Local Filter</filter-name>
    <filter-class>org.jasig.cas.client.util.AssertionThreadLocalFilter</filter-class>
</filter>
 
<!-- CAS Filters Mappings -->
<filter-mapping>
    <filter-name>CAS Authentication Filter</filter-name>
    <url-pattern>/Authn/Cas/norenew/*</url-pattern>
</filter-mapping>
 
<filter-mapping>
    <filter-name>CAS Validation Filter</filter-name>
    <url-pattern>/Authn/Cas/norenew/*</url-pattern>
</filter-mapping>

<filter-mapping>
    <filter-name>CAS Authentication Filter - forceAuthn</filter-name>
    <url-pattern>/Authn/Cas/renew/*</url-pattern>
</filter-mapping>
 
<filter-mapping>
    <filter-name>CAS Validation Filter - forceAuthn</filter-name>
    <url-pattern>/Authn/Cas/renew/*</url-pattern>
</filter-mapping>
  
<filter-mapping>
    <filter-name>CAS HttpServletRequest Wrapper Filter</filter-name> 
    <url-pattern>/Authn/Cas/*</url-pattern>
</filter-mapping>

<filter-mapping>
    <filter-name>CAS Assertion Thread Local Filter</filter-name>
    <url-pattern>/Authn/Cas/*</url-pattern>
</filter-mapping>

...

<!-- Servlet for receiving a callback from an external authentication system and continuing the IdP login flow -->
<servlet>
    <servlet-name>External Authn Callback</servlet-name>
    <servlet-class>net.unicon.idp.externalauth.CasCallbackServlet</servlet-class>

    <load-on-startup>2</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>External Authn Callback</servlet-name>
    <url-pattern>/Authn/Cas/*</url-pattern>
</servlet-mapping>
...
```

Configure build and deploy IdP external authentication plugin
-------------------------------------------------------------

* Make sure that IDP is deployed and war is exploded as `$CATALINA_HOME/webapps/idp`
* Add the namespace and XSD path to `$IDP_HOME/conf/handler.xml`
* Configure IDP External Login Handler in `$IDP_HOME/conf/handler.xml`

Example:

```xml
<ph:ProfileHandlerGroup xmlns:ph="urn:mace:shibboleth:2.0:idp:profile-handler" 
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                        xmlns:shib-cas="http://unicon.net/shib-cas/authn"
                        xsi:schemaLocation="urn:mace:shibboleth:2.0:idp:profile-handler 
                        classpath:/schema/shibboleth-2.0-idp-profile-handler.xsd
                        http://unicon.net/shib-cas/authn classpath:/schema/casLoginHandler.xsd">

...

    <ph:LoginHandler xsi:type="shib-cas:CasLoginHandler" 
                     casResourceUrl="https://sso.server.edu/idp/Authn/Cas">
        <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</ph:AuthenticationMethod>
    </ph:LoginHandler>

...
```

To Build
--------

This project uses [Gradle](http://gradle.org) build system.

* In [`gradle.properties`](https://github.com/Unicon/shib-cas-authenticator/blob/master/gradle.properties), adjust
the property settings for the IdP path, version and Shibboleth common JAR file dependency version:

```properties
shibIdpVersion=2.4.0
shibCommonVersion=1.4.0
shibIdpPath=/opt/shibboleth-idp
```

* From the root directory, simply run `./gradlew`
* Copy `idp-cas-invoker/build/libs/idp-cas-invoker-x.x.jar` to `$CATALINA_HOME/webapps/idp/WEB-INF/lib`
* Copy FROM CAS DEPLOYED WAR: `$CATALINA_HOME/webapps/idp/WEB-INF/lib/cas-client-core-[x.x.x].jar` to `$CATALINA_HOME/webapps/idp/WEB-INF/lib`

To Build in IntelliJ IDE
-------------------------

The IntelliJ metadata files included declare the Shibboleth IdP .jar dependency as version 2.4.0 obtained from `/opt/shibboleth-idp`.  If you first install the IdP there, then IntelliJ should find the Shibboleth IdP .jar dependency and be able to build in the IDE.  If you want to use a different IdP version or a different IdP location, you'll have some IntelliJ library configuration to do.


Shibboleth IdP Upgrades
-------------------------------------------------------------

In order to properly protect the changes to the `web.xml` file of the Shibboleth IdP between upgrades, 
copy the changed version to the `conf` directory of the main Shib IdP directory (e.g. usually `/opt/shibboleth-idp/conf`).
Then, rebuild and redeploy the IdP as usual.

See the following links for additional info:
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPEnableECP
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPInstall [section: `Using a customized web.xml`)

