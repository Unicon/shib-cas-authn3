# What is this?

Version 1.3.0.1 of `shib-cas-authenticator` is a critical security fix.

Existing `shib-cas-authenticator` adopters can patch their existing deployed usages following these instructions.


#  What to do

## 1. Configure IdP Tomcat (6.x)


* Enable Tomcat's *crossContext* in `$CATALINA_HOME/conf/context.xml`

```xml
<Context crossContext="true">
     ...
</Context>
```

* Enable Tomcat's SSL Connector's *emptySessionPath* in `$CATALINA_HOME/conf/server.xml`

```xml
 <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true" emptySessionPath="true" .../>
```


## 2. Replace `CasAuthenticatorResource.class`

Install the new `CasAuthenticatorResource.class` in `$CATALINA_HOME/webapps/casauth/WEB-INF/classes/net/unicon/idp/casauth/`, replacing the old one.

    cp build/classes/main/net/unicon/idp/casauth/CasAuthenticatorResource.class /opt/idp-tomcat-6.0.36/webapps/casauth/WEB-INF/classes/net/unicon/idp/casauth/CasAuthenticatorResource.class


## 3. Remove `idp-cas-invoker-1.3.jar`

Remove `/opt/shibboleth-identityprovider-2.4.0/lib/idp-cas-invoker-1.3.jar`


## 4. Install `idp-cas-invoker-1.3.0.1.jar`

Copy `idp-cas-invoker-1.3.0.1.jar` to  `/opt/shibboleth-identityprovider-2.4.0/lib`


## 5. Run the Shibboleth IdP installer

Run `/opt/shibboleth-identityprovider-2.4.0/install.sh` preserving configuration.


## 6. Restart Tomcat


# How to get help

If these instructions prove problematic or you otherwise seek assistance applying this security patch, please contact Unicon *privately.*

http://www.unicon.net/contact

