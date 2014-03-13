## Shibboleth IdP External Authentication via CAS plugin

This is a Shibboleth IDP external authentication plugin that delegates the authentication to the 
Central Authentication Server. The biggest advantage of using this component over the plain 
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range 
of native CAS protocol features such as `renew` and `gateway`.

The plugin consists of 2 components:
* A custom Shibboleth `LoginHandler` to delegate to CAS
* Shibboleth IDP Servlet acting as a bridge between CAS and IDP

Strategy for sharing state between CASified resource and IdP
-------------------------------------------------------------
This project provides a custom Shibboleth LoginHandler and servlet. The handler prepares the redirect to CAS and the servlet 
handles pulling out the authenticated username and passing that back to Shibboleth.

Build Status
-------------------------------------------------------------
Travis-CI: ![Travis-CI build status](https://travis-ci.org/UniconLabs/shib-cas-authn2.png)

Software Requirements
-------------------------------------------------------------

* This plugin will require Shibboleth Identity Provider v2.4.0 and above.

Configure, build, and deploy IdP external authentication plugin
---------------------------------------------------------------
The first step is to update your Shib idp deployment with the `CasCallbackServlet`. This can be done prior to building/deploying the idp war file or
if preferred, after the build, the files can be modified/updated in the war file before deploying to Tomcat. Previous instructions
were based on the idea that the files would be modified post-deployment. The recommended installation/deployment of the Shib idp suggest 
not exploding the Shib war, so these instructions assume you will modify the files ahead of time. 

### Overview

1. Update the Shib idb `web.xml` (adding the `CasCallbackServlet`). 
2. Configure the Shib idb `CasCallBackServlet` in the properties file
3. Update/configure the `handler.xml` file by adding the Cas `LoginHandler`
4. Build this project
5. Copy the resulting jar artifact to the idp library
6. Copy the cas client jar artifact to the idp library


### Changes to web.xml
Add the IDP External Authn Callback Servlet entry in `idp/WEB-INF/web.xml`

The servlet needs to be configured with either the init-param: `casCallbackServletPropertiesFile` (indicating the path and filename 
of an external properties file containing the name value parameters needed)

Example `web.xml`:

```xml
<!-- Servlet for receiving a callback from an external authentication system and continuing the IdP login flow -->
<servlet>
    <servlet-name>External Authn Callback</servlet-name>
    <servlet-class>net.unicon.idp.externalauth.CasCallbackServlet</servlet-class>
    <!--
        Parameters:
        **cas.server** is required. **cas.server.protocol** and **cas.server.prefix** are optional and default to "https" and "/cas".
        **idp.server** is required. **idp.server.protocol** is optional and defaults to "https".
        **artifact.parameter.name** is optional and defaults to "ticket"

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
    <load-on-startup>2</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>External Authn Callback</servlet-name>
    <url-pattern>/Authn/Cas/*</url-pattern>
</servlet-mapping>
...
```

### Changes to handler.xml

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
    <!-- propertiesFile attribute is optional - default value show here -->
    <ph:LoginHandler xsi:type="shib-cas:CasLoginHandler" 
                     propertiesFile="/opt/shibboleth-idp/conf/cas-shib.properties">
        <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</ph:AuthenticationMethod>
        <!-- There may be 0..N paramBuilder entries. Each must list the fully qualified name of the class to be added. -->
        <shib-cas:paramBuilder class="net.unicon.idp.authn.provider.extra.EntityIdParameterBuilder" />
    </ph:LoginHandler>

...
```

### Configure cas-shib.properties file

Configure the parameters for the properties file. [See the `cas-shib.properties.sample` file](https://github.com/UniconLabs/shib-cas-authn2/blob/master/cas-shib.properties.sample)
in this project for the full list. We suggest using this sample file as your template. Because the login handler and servlet share a set of properties we recommend using the externalized properties file for all your configuration needs.

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

New Features
-----------------------------
* Externalized settings allow for setting the configuration of the callback servlet and login handler outside of the deployed IDP application.
* Default settings for as many of the parameters as possible has reduced the amount of items that have to be configured.
* Architecture now allows for pluggin of additional parameter builders. These builders can be added to send additional parameter information to CAS (such as the parameter in the form of the "entityId" param (relaying party id from Shib)).
