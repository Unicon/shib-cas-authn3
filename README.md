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
This project provides a custom Shibboleth LoginHandler and servlet. The handler prepares the redirect to CAS and the servlet 
handles pulling out the authenticated username and passing that back to Shibboleth.

Software Requirements
-------------------------------------------------------------

* This plugin will require Shibboleth Identity Provider v2.4.0 and above.

Configure, build, and deploy IdP external authentication plugin
---------------------------------------------------------------
The first step to update your Shib idp deployment with the CasCallbackServlet. This can be done prior to building/deploying the idp war file or
if preferred, after the build, the files can be modify/update in the war file before deploying to Tomcat. Previous instructions
were based on the idea that the files would be modified post-deployment. The recommended installation/deployment of the Shib idp suggest 
not exploding the Shib war, so these instructions assume you will modify the files ahead of time. 

Overview of steps:
1. Update the Shib idb web.xml (adding the CasCallbackServlet). 
1a. Configure the Shib idb CasCallBackServlet either in the web.xml or in an external properties file (recommended).
2. Update/configure the handler.xml file by adding the Cas LoginHandler
3. Build this project
4. Copy the resulting jar artifact to the idp library
5. Copy the cas client jar artifact to the idp library

* Add the IDP External Authn Callback Servlet entry in `idp/WEB-INF/web.xml`

The servlet needs to be configured with either the init-param: casCallbackServletPropertiesFile (indicating the path and filename 
of an external properties file containing the name value parameters needed)

Example `web.xml`:

```xml
<!-- Servlet for receiving a callback from an external authentication system and continuing the IdP login flow -->
<servlet>
    <servlet-name>External Authn Callback</servlet-name>
    <servlet-class>net.unicon.idp.externalauth.CasCallbackServlet</servlet-class>
    <!--
        Parameters: **idp.server** and **cas.server.url.prefix** are required; **artifact.parameter.name** is OPTIONAL and defaults to "ticket"

        Use the casCallbackServletPropertiesFile param to externalize the properties. If this is not set, the servlet will look
        in the default location (described below) for the properties. If the file doesn't exist or is not readable, the servlet
        will attempt to initialize using defined init-params matching the desired properties.
    -->
    <init-param>
        <param-name>casCallbackServletPropertiesFile</param-name>
        <!-- 
            This can be any valid path and the name of the file can be whatever you prefer. Default value used if this parameter
            is not set is shown here.
        -->
        <param-value>/opt/shibboleth-idp/conf/cas-shib.properties</param-value>
    </init-param>
    
    <!-- These should be defined in an external properties file for maximum flexibility 
        <init-param>
            <param-name>idp.server</param-name>
            <param-value>https://idp.server.edu</param-value>
        </init-param>
        <init-param>
            <param-name>cas.server.url.prefix</param-name>
            <param-value>https://sso.server.edu/cas</param-value>
        </init-param>
        <init-param>
            <param-name>artifact.parameter.name</param-name>
            <param-value>ticket</param-value>
        </init-param>
    -->
    <load-on-startup>2</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>External Authn Callback</servlet-name>
    <url-pattern>/Authn/Cas/*</url-pattern>
</servlet-mapping>
...
```

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
                     propertiesFile="/opt/shibboleth-idp/conf/cas-shib.properties">
        <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</ph:AuthenticationMethod>
    </ph:LoginHandler>

...
```
* Configure the parameters for the properties file: **cas.login.url** and **idp.callback.url**
* Add the idp url to the CAS services configuration.

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
* Copy `idp-cas-invoker/build/libs/idp-cas-invoker-x.x.jar` to `idp/WEB-INF/lib`
* Copy FROM CAS DEPLOYED WAR: `$CATALINA_HOME/webapps/cas/WEB-INF/lib/cas-client-core-[x.x.x].jar` to `idp/WEB-INF/lib`


Shibboleth IdP Upgrades
-------------------------------------------------------------

In order to properly protect the changes to the `web.xml` file of the Shibboleth IdP between upgrades, 
copy the changed version to the `conf` directory of the main Shib IdP directory (e.g. usually `/opt/shibboleth-idp/conf`).
Then, rebuild and redeploy the IdP as usual.

See the following links for additional info:
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPEnableECP
* https://wiki.shibboleth.net/confluence/display/SHIB2/IdPInstall [section: `Using a customized web.xml`)

Additional Features
-----------------------------
* Externalize settings to allow for setting the configuration of the callback servlet outside of the deployed IDP application.
* Additionally, CAS is now sent the entityId param (relaying party id from Shib).