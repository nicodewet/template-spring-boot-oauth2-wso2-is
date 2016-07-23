# template-spring-boot-oauth2-wso2-is
Template Spring Boot project with OAuth2 proven to work with WSO2 IS 5.1.0

We use the simplest possible security setup meaning that getting served anything from the web application requires authentication. This approach
may also be the easiest to maintain in the long run.

## Setup

On you local, download and unzip wso2is-5.1.0, then cd wso2is-5.1.0 and $ ./bin/wso2server.sh

Now, once WSO2 IS is up and running, navigate to https://localhost:9443/carbon/, login with admin/admin and set up a service provider. Just call it 
localhost, then move onto Inbound Authentication Configuration and move down to OAuth/OpenID Connect Configuration. An OAuth Client Key and an OAuth
Client Secret will get generated for you, then as callback URL enter http://localhost:8080/login

At this stage you should be good to go appart from having to add the WSO2 IS self-signed certificate to your JVM truststore (to establish a chain 
of trust). If you don't do this your SSL handshake will fail when "backchannel" requests are made to WSO2 IS endpoints.

Once the self-signed certificate bit above has been done you can right click on Application in your IDE and select Run As, then Java Application. Just
make sure your JVM is that you'll run with is the one with the self-signed certs added to its trust store.

Just use admin/admin to log in and you should be presented with a Hello World message.

In terms of setup, in my experience at some stage you may be banging your head against a *why-is-this-not-working* wall when it comes to OAuth 2 / OpenID Connect. In this
case, its useful to have clear visibility over all traffic when in *debug* mode. In this case, putting mitmproxy becomes a useful tool for HTTPS traffic inteception.

### Adding WSO2 IS public certificate to Java Certificate Store

As stated above this part is required for the web service calls to WSO2 IS in the [OAuth2](https://tools.ietf.org/html/rfc6749) Authorization Code Flow.

[Adding WSO2 IS public certificate to Java Certificate Store](https://nadeesha678.wordpress.com/2015/09/21/adding-wso2-public-certificate-to-java-certificate-store/)

    Nicos-Air:wso2is-5.1.0 nico$ cd repository/resources/security/
    Nicos-Air:security nico$ keytool -export -keystore wso2carbon.jks -alias wso2carbon -file wso2PubCert.cer

**default password is wso2carbon**

    Nicos-Air:security nico$ keytool -import -keystore cacerts -file wso2PubCert.cer

**default password is changeit**

    Nicos-Air:security nico$ keytool -list -keystore cacerts

### Import mitmproxy ca cert

    Nicos-Air:security nico$ keytool -import -trustcacerts -alias mitmproxy-ca -file ~/.mitmproxy/mitmproxy-ca-cert.cer -keystore cacerts -storepass changeit

### mitmproxy

This is a setup to run WSO2 IS 5.1.0, mitmproxy in reverse proxy mode and then also the Spring Boot application.

### cacerts sanity check

Always a good idea to sanity check your cacert file. In my case, I had deleted an old file (forgot the password) and realized that I needed to do this:

    $ mv cacerts /Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/security/

#### Terminal A: run mitmproxy
    Nicos-Air:mitmproxy-0.17.1-osx nico$ ./mitmproxy -p 9444 -R https://localhost:9443 
    
Remember to quit mitmproxy simply press q and also that you must select an entry and then press Enter to see the HTML message detail (headers, request and response bodies).

#### Terminal B: run wso2 is
    Nicos-Air:wso2is-5.1.0 nico$ ./bin/wso2server.sh

#### IDE
Run as Java Application and be sure to keep a *ssl debug* Run Configuration handy which entails for example using this option as a VM Argument: *-Djavax.net.debug=all*

Note the application.yml configuration:

    security:
      oauth2:
        client:
          accessTokenUri: https://localhost:9444/oauth2/token
          userAuthorizationUri: https://localhost:9444/oauth2/authorize
          clientId: LXphgIFoHC5qltrSrYzGwoGUs90a
          clientSecret: j6TlbQ3UXvONmIZMVql5fuaTN_Ua
          scope: openid
          clientAuthenticationScheme: header
          preEstablishedRedirectUri: http://localhost:8080/login
          useCurrentUri: false
        resource:
          userInfoUri: https://localhost:9444/oauth2/userinfo?schema=openid 

## Known issues

Note, you'll see an issue relating the Principal in the DEBUG logs, this is a known issue, and a solution will be added here in time. For now though, at least you can log in and the integration works with minimal effort.

Also, use Chrome or Firefox, you'll run into issues with Safari. More on this in time.
