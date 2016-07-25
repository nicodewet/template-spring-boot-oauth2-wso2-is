# template-spring-boot-oauth2-wso2-is
Template Spring Boot project with OAuth2 proven to work with WSO2 IS 5.1.0

We use the simplest possible security setup meaning that getting served anything from the web application requires authentication. This approach
may also be the easiest to maintain in the long run.

## Setup
On your local, download and unzip wso2is-5.1.0, then cd wso2is-5.1.0 and $ ./bin/wso2server.sh

Now, once WSO2 IS is up and running, navigate to https://localhost:9443/carbon/, login with admin/admin and set up a service provider. Just call it 
localhost, then move onto Inbound Authentication Configuration and move down to OAuth/OpenID Connect Configuration. An OAuth Client Key and an OAuth
Client Secret will get generated for you, then as callback URL enter http://localhost:8080/login

At this stage you should be good to go appart from having to import the WSO2 IS self-signed certificate into your JVM truststore (to establish a chain 
of trust). If you don't do this your SSL handshake will fail when "backchannel" (web service) requests are made to WSO2 IS endpoints. See the section 
*Adding WSO2 IS public certificate to Java Certificate Store* on how to do this.

Once the WSO2 IS self-signed certificate import has been done you can right click on Application in your IDE and select Run As, then Java Application. Just
make sure your JVM is that you'll run with is the one with the self-signed certs added to its trust store.

Just use admin/admin to log in and you should be presented with a Hello World message.

In terms of setup, in my experience at some stage you may be banging your head against a *why-is-this-not-working* wall when it comes to OAuth 2 / OpenID Connect. In this
case, its useful to have clear visibility over all traffic when in *debug* mode. In this case, the use of mitmproxy becomes a useful, if not essential tool for 
HTTPS traffic interception.

### Adding WSO2 IS public certificate to Java Certificate Store
As stated above this part is required for the web service calls to WSO2 IS in the [OAuth2](https://tools.ietf.org/html/rfc6749) Authorization Code Flow.

[Adding WSO2 IS public certificate to Java Certificate Store](https://nadeesha678.wordpress.com/2015/09/21/adding-wso2-public-certificate-to-java-certificate-store/)

    Nicos-Air:wso2is-5.1.0 nico$ cd repository/resources/security/
    Nicos-Air:security nico$ keytool -export -keystore wso2carbon.jks -alias wso2carbon -file wso2PubCert.cer

**default password is wso2carbon**
    Nicos-Air:security nico$ keytool -import -keystore cacerts -file wso2PubCert.cer

**default password is changeit**
    Nicos-Air:security nico$ keytool -list -keystore cacerts

At this stage, if you change all the port references in application.yml to 9443 you should be able to authenticate using WSO2 IS when using the credentials admin/admin.

### Import mitmproxy ca cert
    Nicos-Air:security nico$ keytool -import -trustcacerts -alias mitmproxy-ca -file ~/.mitmproxy/mitmproxy-ca-cert.cer -keystore cacerts -storepass changeit

At this stage, you'll be able to run mitmproxy as a man-in-the-middle proxy for debugging purposes. 

Note that even if your OAuth 2 / OpenID Connect integration is working perfectly this is a useful tool to have in your arsenal, for example when introducing a new team 
member to a project so you can step through the message flows.

### mitmproxy
This is a setup to run WSO2 IS 5.1.0, mitmproxy in reverse proxy mode and then also the Spring Boot application.

### cacerts sanity check
Always a good idea to sanity check your cacert file. In my case, I had deleted an old file (forgot the password) and realized that I needed to do this:

    $ mv cacerts /Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/security/

#### Part 1: run mitmproxy from terminal
    Nicos-Air:mitmproxy-0.17.1-osx nico$ ./mitmproxy -p 9444 -R https://localhost:9443
    
Remember to quit mitmproxy simply press q and also that you must select an entry and then press Enter to see the HTML message detail (headers, request and response bodies).

#### Part 2: run wso2 is from terminal
    Nicos-Air:wso2is-5.1.0 nico$ ./bin/wso2server.sh

#### Part 3: run Spring Boot application from your IDE
Run the Spring Boot Application as Java Application and be sure to keep a *ssl debug* Run Configuration handy which entails for example using this option as a VM Argument: *-Djavax.net.debug=all*

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

In terms of endpoints that you want to pass through mitmproxy, the **accessTokenUri** and **userInfoUri** are the most important since you'll be able to 
see pertinent *userAuthorizationUri* traffic in your browser debug console. That said, the mitmproxy view keeps all the traffic in one window with messages
in sequence and so it's easiest to also pass **userAuthorizationUri** traffic through mitmproxy. 

#### Part 4: initiate login using browser
Using Chrome for example, enter http://localhost:8080 to initiate the [OAuth2](https://tools.ietf.org/html/rfc6749) Authorization Code Flow.

## Known issues

### Principal issue
Note, you'll see an issue relating the Principal in the DEBUG logs with the Principal appearing as unknown.

    SecurityContext 'org.springframework.security.core.context.SecurityContextImpl@fbdb8794: Authentication: org.springframework.security.oauth2.provider.OAuth2Authentication@fbdb8794: Principal: unknown; Credentials: [PROTECTED]; Authenticated: true; Details: remoteAddress=0:0:0:0:0:0:0:1, sessionId=<SESSION>, tokenType=BearertokenValue=<TOKEN>; Granted Authorities: ROLE_USER' stored to HttpSession: 'org.apache.catalina.session.StandardSessionFacade@2f54bded

This is a known issue that stems from a WSO2 IS bug and a solution will be added here in time. The bug relates to the response to the HTTP GET request to https://localhost:9444/oauth2/userinfo?schema=openid appearing as follows in the response body.

    {"sub":"nicodewet@carbon.super"}

For now though, at least you can log in and know that the integration partially works with minimal effort.

To add to the effect of this issue, note that when using [Thymeleaf's Spring Security Extras](https://github.com/thymeleaf/thymeleaf-extras-springsecurity) integration module the value of the "name" property of the authentication object shows up as *unknown* out of the box. To see this in action log in and take note of the html depicted below.

    <p>
    Logged user: <span sec:authentication="name">Bob</span>
    </p>

### Browser issue

Use Chrome or Firefox, you'll run into issues with Safari. More on this in time.
