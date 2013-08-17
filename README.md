# Shibboleth IdP External Authentication via CAS plugin

This is a Shibboleth IDP external authentication plugin that delegates the authentication to the 
Central Authentication Server. The biggest advantage of using this component over the plain 
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range 
of native CAS protocol features such as `renew` and `gateway`.

The plugin consists of 2 components:

* A web resource protected by CAS and acting as an *authentication facade*
* Shibboleth IDP Servlets acting as a bridge between CAS and IDP


Strategy for sharing state between CASified resource and IdP
-------------------------------------------------------------
The CASified resource uses the Java CAS Client to participate in the CAS protocol to determine the authenticated username.  It then publishes this username into the IdP's `ServletContext` using a cross-context access to put an attribute into that `ServletContext` keyed by the end user session identifier (with a namespacing prefix).  The IdP and the CASified resource have the same session identifier for the user session thanks to the configuration described below.


Software Requirements
-------------------------------------------------------------

* This plugin will require Shibboleth Identity Provider v2.4.0 and above.

* The Shibboleth IdP and the web resource protected by CAS (`/casauth`) *must* be deployed alongside one another in the same servlet container
* The servlet container *must* be configured such that `casauth` is able to do a cross-context request to access the IdP's `ServletContext`. Detailed following.
* The servlet container *must* be configured such that `/casauth` and the IDP (`/idp` ?) share session identifiers.  This is the `emptySessionPath="true"` tomcat feature.  Detailed following.

Servlet Container Configuration
-------------------------------------------------------------

Here's how you do the cross-context enablement in Tomcat:

* Enable Tomcat's *crosscontext* in `$CATALINA_HOME/conf/context.xml`

```xml
<Context crossContext="true">
	...
</Context>
```

Here's how you do the empty session path in Tomcat:

* Enable Tomcat's SSL Connector's *emptySessionPath* in `$CATALINA_HOME/conf/server.xml`

```xml
 <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true" emptySessionPath="true" .../>
```



Configure build and deploy cas-authentication-facade resource
-------------------------------------------------------------

* Configure a `context-param` to tell the facade the path to the IdP

In `cas-authentication-facade/src/main/webapp/WEB-INF/web.xml` :

    <context-param>
        <param-name>idPContextName</param-name>
        <param-value>/idp</param-value>
    </context-param>

If you've deployed your IdP as `/samlThing`, then the param-value should be

    <param-value>/samlThing</param-value>

* Configure CAS filters in `cas-authentication-facade/src/main/webapp/WEB-INF/web.xml` 
suitable for your CAS installation.

Example `web.xml`:

```xml
...
<filter>
	<filter-name>CAS Authentication Filter (Renew)</filter-name>
	<filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
	<init-param>
		<param-name>casServerLoginUrl</param-name>
		<param-value>https://sso.server.edu/cas/login</param-value>
	</init-param>
	<!--
		The value of the serverName parameter should be the FQDN of the application server (tomcat)
		where the casauth.war application is deployed.
		
		This configuration assumes that the WAR file is lives inside the same application instance
		in which CAS itself is deployed. If your configuration has deployed the WAR file alongside
		the IdP server, the value should then be changed to be the FQDN of the IdP tomcat server.
		
		Note: Use the standard IdP operating port and not the SOAP endpoint.
	-->
	<init-param>
		<param-name>serverName</param-name>
		<param-value>https://sso.server.edu</param-value>
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
        <param-value>https://sso.server.edu/cas/login</param-value>
    </init-param>
    <!--
      The value of the serverName parameter should be the FQDN of the application server (tomcat)
      where the casauth.war application is deployed.
      
      This configuration assumes that the WAR file is lives inside the same application instance
      in which CAS itself is deployed. If your configuration has deployed the WAR file alongside
      the IdP server, the value should then be changed to be the FQDN of the IdP tomcat server.
      
      Note: Use the standard IdP operating port and not the SOAP endpoint.
    -->
    <init-param>
      <param-name>serverName</param-name>
      <param-value>https://sso.server.edu</param-value>
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
      <param-value>https://sso.server.edu/cas/login</param-value>
    </init-param>
		<!--
			The value of the serverName parameter should be the FQDN of the application server (tomcat)
			where the casauth.war application is deployed.
			
			This configuration assumes that the WAR file is lives inside the same application instance
			in which CAS itself is deployed. If your configuration has deployed the WAR file alongside
			the IdP server, the value should then be changed to be the FQDN of the IdP tomcat server.
			
			Note: Use the standard IdP operating port and not the SOAP endpoint.
		-->
		<init-param>
			<param-name>serverName</param-name>
			<param-value>https://sso.server.edu</param-value>
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
        <param-value>https://sso.server.edu/cas/login</param-value>
    </init-param>
    <!--
      The value of the serverName parameter should be the FQDN of the application server (tomcat)
      where the casauth.war application is deployed.
      
      This configuration assumes that the WAR file is lives inside the same application instance
      in which CAS itself is deployed. If your configuration has deployed the WAR file alongside
      the IdP server, the value should then be changed to be the FQDN of the IdP tomcat server.
      
      Note: Use the standard IdP operating port and not the SOAP endpoint.
    -->
    <init-param>
      <param-name>serverName</param-name>
      <param-value>https://sso.server.edu</param-value>
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
		<param-value>https://sso.server.edu/cas</param-value>
	</init-param>
	<!--
		The value of the serverName parameter should be the FQDN of the application server (tomcat)
		where the casauth.war application is deployed.
		
		This configuration assumes that the WAR file is lives inside the same application instance
		in which CAS itself is deployed. If your configuration has deployed the WAR file alongside
		the IdP server, the value should then be changed to be the FQDN of the IdP tomcat server.
		
		Note: Use the standard IdP operating port and not the SOAP endpoint.
	-->
	<init-param>
		<param-name>serverName</param-name>
		<param-value>https://sso.server.edu</param-value>
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

Configure build and deploy IdP external authentication plugin
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

	       <!-- 
            A URL for CAS-protected resource endpoint  
            The value of the casProtectedResource parameter should be the FQDN of the application server (tomcat)
            where the casauth.war application is deployed.
            
            This configuration assumes that the WAR file is lives inside the same application instance
            in which CAS itself is deployed. If your configuration has deployed the WAR file alongside
            the IdP server, the value should then be changed to be the FQDN of the IdP tomcat server.
            
            Note: Use the standard IdP operating port and not the SOAP endpoint.
          -->
	       <init-param>
	           <param-name>casProtectedResource</param-name>
	           <param-value>https://sso.server.edu/casauth/facade</param-value>
	       </init-param>
	       <!-- Am IdP URL for the external CAS-protected resource to callback to -->
	       <init-param>
	           <param-name>postAuthnCallbackUrl</param-name>
	           <param-value>https://shibidp.server.edu/idp/externalAuthnCallback</param-value>
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

To Build
--------

This project uses [Gradle](http://gradle.org) build system.

* In [`gradle.properties`](https://github.com/Unicon/shib-cas-authenticator/blob/master/gradle.properties), adjust
the property settings for the IdP path, version and Shibboleth common JAR file dependency version:

```properties
shibIdpVersion=2.3.8
shibCommonVersion=1.3.7
shibIdpPath=c:/portal/shibboleth-identityprovider-2.3.8
```

* From the root directory, simply run `./gradlew`

* Copy `cas-authentication-facade/build/libs/casauth.war` to `$CATALINA_HOME/webapps`
* Copy `idp-cas-invoker/build/libs/idp-cas-invoker-x.x.jar` to `$CATALINA_HOME/webapps/idp/WEB-INF/lib`

Shibboleth IdP Upgrades
-------------------------------------------------------------

In order to properly protect the changes to the `web.xml` file of the Shibboleth IdP between upgrades, 
copy the changed version to the `conf` directory of the main Shib IdP directory (e.g. usually `/opt/shibboleth-idp/conf`).
Then, rebuild and redeploy the IdP as usual.

See the following links for additional info:
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPEnableECP
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPInstall [section: `Using a customized web.xml`)

