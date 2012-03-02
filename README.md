# Shibboleth IDP External Authentication via CAS plugin

This is a Shibboleth IDP external authentication plugin that delegates the authentication to the Central Authentication Server. The biggest advantage of using this component over the plain `REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range of native CAS protocol features such as `renew` and `gateway`

The plugin consists of 2 components:

* A web resource protected by CAS and acting as an *authentication facade*
* Shibboleth IDP Servlets acting as a bridge between CAS and IDP  

This project uses [Gradle](http://gradle.org) build system
	
To build
--------
Simply run `./gradlew`

Configure build and deploy cas-authentication-facade resource
-------------------------------------------------------------
* Configure CAS filters in `cas-authentication-facade/src/main/webapp/WEB-INF/web.xml` suitable for your CAS installation

Example web.xml:

```xml
...
<filter>
		<filter-name>CAS Authentication Filter (Renew)</filter-name>
		<filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
		<init-param>
			<param-name>casServerLoginUrl</param-name>
			<param-value>https://dima767.example.org:9443/cas/login</param-value>
		</init-param>
		<init-param>
			<param-name>serverName</param-name>
			<param-value>https://dima767.example.org:9443</param-value>
		</init-param>
		<init-param>
			<param-name>renew</param-name>
			<param-value>true</param-value>
		</init-param>
	</filter>
    <filter-mapping>
		<filter-name>CAS Authentication Filter (Renew)</filter-name>
		<url-pattern>/facade/renew/*</url-pattern>
	</filter-mapping>

    <filter>
        <filter-name>CAS Authentication Filter (No Renew)</filter-name>
        <filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
        <init-param>
            <param-name>casServerLoginUrl</param-name>
            <param-value>https://dima767.example.org:9443/cas/login</param-value>
        </init-param>
        <init-param>
            <param-name>serverName</param-name>
            <param-value>https://dima767.example.org:9443</param-value>
        </init-param>
        <init-param>
            <param-name>renew</param-name>
            <param-value>false</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>CAS Authentication Filter (No Renew)</filter-name>
        <url-pattern>/facade/norenew/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>CAS Authentication Filter (Renew Gateway)</filter-name>
        <filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
        <init-param>
            <param-name>casServerLoginUrl</param-name>
            <param-value>https://dima767.example.org:9443/cas/login</param-value>
        </init-param>
        <init-param>
            <param-name>serverName</param-name>
            <param-value>https://dima767.example.org:9443</param-value>
        </init-param>
        <init-param>
            <param-name>renew</param-name>
            <param-value>true</param-value>
        </init-param>
        <init-param>
            <param-name>gateway</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>CAS Authentication Filter (Renew Gateway)</filter-name>
        <url-pattern>/facade/renewgateway/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>CAS Authentication Filter (No Renew Gateway)</filter-name>
        <filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
        <init-param>
            <param-name>casServerLoginUrl</param-name>
            <param-value>https://dima767.example.org:9443/cas/login</param-value>
        </init-param>
        <init-param>
            <param-name>serverName</param-name>
            <param-value>https://dima767.example.org:9443</param-value>
        </init-param>
        <init-param>
            <param-name>renew</param-name>
            <param-value>false</param-value>
        </init-param>
        <init-param>
            <param-name>gateway</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>CAS Authentication Filter (No Renew Gateway)</filter-name>
        <url-pattern>/facade/norenewgateway/*</url-pattern>
    </filter-mapping>

    <filter>
		<filter-name>CAS Validation Filter</filter-name>
		<filter-class>org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter</filter-class>
		<init-param>
			<param-name>casServerUrlPrefix</param-name>
			<param-value>https://dima767.example.org:9443/cas</param-value>
		</init-param>
		<init-param>
			<param-name>serverName</param-name>
			<param-value>https://dima767.example.org:9443</param-value>
		</init-param>
		<init-param>
			<param-name>redirectAfterValidation</param-name>
			<param-value>true</param-value>
		</init-param>
	</filter>
    <filter-mapping>
		<filter-name>CAS Validation Filter</filter-name>
		<url-pattern>/facade/renew/*</url-pattern>
	</filter-mapping>
    <filter-mapping>
        <filter-name>CAS Validation Filter</filter-name>
        <url-pattern>/facade/norenew/*</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>CAS Validation Filter</filter-name>
        <url-pattern>/facade/renewgateway/*</url-pattern>
    </filter-mapping>
    <filter-mapping>
        <filter-name>CAS Validation Filter</filter-name>
        <url-pattern>/facade/norenewgateway/*</url-pattern>
    </filter-mapping>
...
```

* Build the web archive: `gradlew`
* Deploy the web archive `cas-authentication-facade/build/libs/casauth.war` to a Servlet container

Configure build and deploy IDP external authentication plugin
-------------------------------------------------------------

* Make sure that IDP is deployed and war is exploded as `$CATALINA_HOME/webapps/idp`
* Configure IDP External Login Handler in `$IDP_HOME/conf/handler.xml`

Example:

```xml
...

<ph:LoginHandler xsi:type="ph:ExternalAuthn"
                 externalAuthnPath="/authn/external"
                 supportsForcedAuthentication="true" >
    <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</ph:AuthenticationMethod>
</ph:LoginHandler>

...
```

* Add the IDP External Auth Servlet entries in `$CATALINA_HOME/webapps/idp/WEB-INF/web.xml`

Example:

```xml
	<!-- Servlet for invoking external CAS authentication -->
	   <servlet>
	       <servlet-name>External Authn</servlet-name>
	       <servlet-class>net.unicon.idp.externalauth.CasInvokerServlet</servlet-class>

	       <!-- A URL for CAS-protected resource endpoint -->
	       <init-param>
	           <param-name>casProtectedResource</param-name>
	           <param-value>https://dima767.example.org:9443/casauth/facade</param-value>
	       </init-param>
	       <!-- Am IdP URL for the external CAS-protected resource to callback to -->
	       <init-param>
	           <param-name>postAuthnCallbackUrl</param-name>
	           <param-value>https://idp.example.org:8443/idp/externalAuthnCallback</param-value>
	       </init-param>

	       <load-on-startup>2</load-on-startup>
	   </servlet>

	   <servlet-mapping>
	       <servlet-name>External Authn</servlet-name>
	       <url-pattern>/authn/external</url-pattern>
	   </servlet-mapping>

	   <!-- Servlet for receiving a callback from an external authentication system and continuing the IdP login flow -->
	   <servlet>
	       <servlet-name>External Authn Callback</servlet-name>
	       <servlet-class>net.unicon.idp.externalauth.CasCallbackServlet</servlet-class>

	       <load-on-startup>2</load-on-startup>
	   </servlet>

	   <servlet-mapping>
	       <servlet-name>External Authn Callback</servlet-name>
	       <url-pattern>/externalAuthnCallback</url-pattern>
	   </servlet-mapping>
```

* Build the jar containing the Servlets: `gradlew`
* Copy `idp-cas-invoker/build/libs/idp-cas-invoker-x.x.jar` to `$CATALINA_HOME/webapps/idp/WEB-INF/lib`