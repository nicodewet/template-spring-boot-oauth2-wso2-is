# template-spring-boot-oauth2-wso2-is
Template Spring Boot project with OAuth2 proven to work with WSO2 IS 5.1.0

I use the simplest possible security setup which naturally inludes login and also local logout.

## Setup
On your local, download and unzip wso2is-5.1.0, then start it up as follows: 

    $ cd wso2is-5.1.0
    $ ./bin/wso2server.sh

Now, once WSO2 IS is up and running, navigate to https://localhost:9443/carbon/, login with admin/admin and set up a Service Provider. Just call it 
localhost, then move onto Inbound Authentication Configuration and move down to OAuth/OpenID Connect Configuration. An OAuth Client Key and an OAuth
Client Secret will get generated for you, then as callback URL enter http://localhost:8080/login

As an aside, you may wonder, looking at the code, and AppController in particular where on earth **login** comes from. This path
comes from auto-configuration magic (where exactly I cannot recall at the time of writing) and Spring Security looks out for requests to it in it's plethora of HTTP filters.

At this stage you should be good to go appart from having to import the WSO2 IS self-signed certificate into your JVM truststore (to establish a chain 
of trust). If you don't do this your SSL handshake will fail when "backchannel" (web service) requests are made to WSO2 IS endpoints. See the section 
*Adding WSO2 IS public certificate to Java Certificate Store* on how to do this.

Once the WSO2 IS self-signed certificate import has been done you can right click on Application in your IDE and select Run As, then Java Application. Just
make sure your JVM that you'll run with is the one with the self-signed certs added to its trust store.

Just use admin/admin to log in and you should be presented with a Hello World message.

In terms of setup, in my experience at some stage you may be banging your head against a *why-is-this-not-working* wall when it comes to OAuth 2 / OpenID Connect. To prevent the said head banging, its useful to have clear visibility over all traffic when in *debug* mode. In a way this goes without saying but it's easy to forget once you venture outside the familiar bounds on your IDE. When it comes to visiblity over all traffic, the use of mitmproxy becomes a useful and perhaps an essential tool for HTTPS traffic interception.

### Adding WSO2 IS public certificate to Java Certificate Store
As stated above this part is required for the web service calls to WSO2 IS in the [OAuth2](https://tools.ietf.org/html/rfc6749) Authorization Code Flow.

[Adding WSO2 IS public certificate to Java Certificate Store](https://nadeesha678.wordpress.com/2015/09/21/adding-wso2-public-certificate-to-java-certificate-store/)

    Nicos-Air:wso2is-5.1.0 nico$ cd repository/resources/security/
    Nicos-Air:security nico$ keytool -export -keystore wso2carbon.jks -alias wso2carbon -file wso2PubCert.cer

**default password is wso2carbon**

    Nicos-Air:security nico$ sudo keytool -import -keystore $JAVA_HOME/jre/lib/security/cacerts -file wso2PubCert.cer

**default password is changeit**

    Nicos-Air:security nico$ sudo keytool -list -keystore $JAVA_HOME/jre/lib/security/cacerts

At this stage, if you change all the port references in application.yml to 9443 you should be able to authenticate using WSO2 IS when using the credentials admin/admin.

### Import mitmproxy ca cert
    Nicos-Air:security nico$ sudo keytool -import -trustcacerts -alias mitmproxy-ca -file ~/.mitmproxy/mitmproxy-ca-cert.cer -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit

At this stage, you'll be able to run mitmproxy as a man-in-the-middle proxy for debugging purposes. 

Note that even if your OAuth 2 / OpenID Connect integration is working perfectly this is a useful tool to have in your arsenal, for example when introducing a new team 
member to a project so you can step through the message flows.

### mitmproxy
This is a setup to run WSO2 IS 5.1.0, mitmproxy in reverse proxy mode and then also the Spring Boot application.

### cacerts sanity check
Given that we've specified $JAVA_HOME/jre/lib/security/cacerts as our cacerts file and also that you'll be running the Spring Boot app from your IDE, the first thing to do is 
to make sure that you are using the same JVM.

In addition, you can always grep for the newly added cacerts as follows:

    $ sudo keytool -list -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit | grep mitmproxy

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
    wso2SessionTerminateUrl: https://localhost:9443/commonauth?commonAuthLogout=true&type=oid&commonAuthCallerPath=http://localhost:8080/logout&relyingParty=localhost

In terms of endpoints that you want to pass through mitmproxy, the **accessTokenUri** and **userInfoUri** are the most important since you'll be able to 
see pertinent *userAuthorizationUri* traffic in your browser debug console. That said, the mitmproxy view keeps all the traffic in one window with messages
in sequence and so it's easiest to also pass **userAuthorizationUri** traffic through mitmproxy. 

#### Part 4: initiate login using browser
Using Chrome for example, enter http://localhost:8080 to initiate the [OAuth2](https://tools.ietf.org/html/rfc6749) Authorization Code Flow.

## Known issues and fixes

### Principal issue
Note, you'll see an issue relating the Principal in the DEBUG logs with the Principal appearing as unknown.

    SecurityContext 'org.springframework.security.core.context.SecurityContextImpl@fbdb8794: Authentication: org.springframework.security.oauth2.provider.OAuth2Authentication@fbdb8794: Principal: unknown; Credentials: [PROTECTED]; Authenticated: true; Details: remoteAddress=0:0:0:0:0:0:0:1, sessionId=<SESSION>, tokenType=BearertokenValue=<TOKEN>; Granted Authorities: ROLE_USER' stored to HttpSession: 'org.apache.catalina.session.StandardSessionFacade@2f54bded

This is a known issue that stems from a [WSO2 IS 5.1.0 bug](https://wso2.org/jira/browse/IDENTITY-4250) and a workaround is included in AppUserInfoTokenservices.java. The bug relates to the response to the HTTP GET request to https://localhost:9444/oauth2/userinfo?schema=openid appearing as follows in the response body.

    {"sub":"nicodewet@carbon.super"}

Without the workaround in place, at this stage at least you can log in and know that the integration partially works with minimal effort.

To add to the effect of this issue, note that when using [Thymeleaf's Spring Security Extras](https://github.com/thymeleaf/thymeleaf-extras-springsecurity) integration module the value of the "name" property of the authentication object shows up as *unknown* out of the box. To see this in action, introduce a regression by removing the "sub" key from the PRINCIPAL_KEYS array in AppUserInfoTokenServices, log in and take note of the html depicted below.

    <p>
    Logged user: <span sec:authentication="name">Bob</span>
    </p>

#### Principal issue fix
The fix is surprisingly easy, but thats said in hindsight. All you have to do is to copy class UserInfoTokenServices from GitHub, then add "sub" to the PRINCIPAL_KEYS array. See class AppUserInfoTokenServices.java. With this fix in place, when 
you log in your'll see:

    Hello, World!

    Logged user: admin@carbon.super

    This content is only shown to users with role ROLE_USER.

### Local logout issue

Once a stock standard Spring Security logout process has been put in place with a POST to /logout and hidden CSRF fields you'd
expect that you're all done.

In fact, you'll soon notice that once you have logged out as far as Spring Security is concerned this is short lived. Taking
a close look at your mitmproxy traffic with repeated Login / Logout clicks will show you why or at least indicate why.

First of all, each time you click Login, you'll notice the typical OAuth2 / OpenID Connect *dance* with a sequence of three HTTP request / response pairs:

    GET https://localhost:9443/oauth2/authorize?client_id=LXphgIFoHC5qltrSrYzGwoGUs90a&redirect_uri=http:/
       /localhost:8080/login&response_type=code&scope=openid&state=bYvf1G
       ← 302 [no content] 77ms
    POST https://localhost:9443/oauth2/token
        ← 200 application/json 700B 29ms
    GET https://localhost:9443/oauth2/userinfo?schema=openid
       ← 200 application/json 28B 45ms

At the end of the third exchange your user is logged in again, this is no good since it means you don't have local logout.

So, in terms of *why*, the astute reader may already start suspecting a server-side cookie sent from the client to the server in the first request / response pair, and thats exactly what is happening:

    Cookie: JSESSIONID=72C10AE56C8EC753B9C08FD98E5A6F9C76712DACF97411B17A466F3473BE526E
            B8C848BDC652175F1B033A428C38C685824FDBAABDE550720B81DDA69C3F7FBEF873E865492
            52C9FFDE368D290E416FD1E5F2097AD9EFD4C1A30EFA47F9D29954E1DF188C11DA8A2A475B7
            6ABFD3A43C7D258F67C1DEACF9C44DAC4382F5919D; menuPanel=visible;             
            menuPanelType=main; commonAuthId=d8023506-d236-48d6-a3cd-b0e1edec99fd;     
            JSESSIONID=C65F528E895D110B3A208EB88D1B4148

The *commonAuthId* cookie is the culprit here. According to [this article](http://xacmlinfo.org/2015/10/15/how-to-configure-session-time-out-in-wso2-identity-server-wso2is/) WSO2 IS creates an SSO session for each end user and a *commonAuthId* cookie is associated with the said session. 

Now, please refer to the referenced article for concerns other than logging out, I'll just focus on this immediate issue.

At the time of writing the accepted answer to [How to destroy authentication session in WSO2 Identity Server?](http://stackoverflow.com/questions/29963787/how-to-destroy-authentication-session-in-wso2-identity-server) is to:

* send a request to the /commonauth WSO2 IS endpoint with query parameter **commonAuthLogout=true**

As per [this article](http://xacmlinfo.org/2015/01/08/openid-connect-identity-server/) here is a full example that is specific to WSO2 IS 5.1.0 and our localhost setup:

    https://localhost:9443/commonauth?commonAuthLogout=true&type=oidc&commonAuthCallerPath=http://localhost:8080/login&relyingParty=localhost

* **commonAuthCallerPath** is the redirection url
* **relyingParty** is registered Service Provider application name which is registered in the WSO2 IS 5.1.0

Now, in terms of implementing the WSO2 IS *commonAuth* logout as well as plain vanilla Spring Security logout, this has been detailed in the next section which came about after much experimentation (and school boy errors which I won't mention here).

#### Combining WSO2 IS commonAuth and Spring Security Logout

The design is as follows. 

Given that the commonAuthId cookie is associated with the WSO2 IS server, the index.html Javascript will not have access to it, so, I have added a **LOGOUT** hyperlink to index.html that will only show when our thymeleaf-extras-springsecurity4 config says its ok to do so (sec:authorize="isAuthenticated()"). The above leaves a question, and that is how will the stock standard Spring Security logout process happen? The default way to do this is to add a form with hidden csrf attributes. The answer that I've come up with is that the **commonAuthCallerPath** dictates where the browser will be directed to (HTTP 302 response by WSO2 IS), so one can simply redirect to a Controller method that is only available to authenticated users. This method, called *logout* in 
AppController.java performs server-side logout and redirects to index.html where the stock standard thymeleaf-extras-springsecurity4 config will let the user know that they are logged out.

### Browser issue

Use Chrome or Firefox, you'll run into issues with Safari. More on this in time.

## Adding some spit and polish with a health endpoint

In AppHealthIndicator.java I've added Socket (TCP) level health checks for each external endpoint configured in application.yml

The idea here is to get a health indicator rapidly, you don't want to waste time trawling log files to get such basic information. Just start the application and point your browser to:

    http://localhost:8080/health
    
Note, critically, that you don't need to be authenticated to hit the health endpoint and that as per the Spring Boot Actuator docs
that the endpoint has denial-of-service protection built in (as a challenge read up how this works).

## Technical References

* [Thymeleaf + Spring Security integration basics](http://www.thymeleaf.org/doc/articles/springsecurity.html) - José Miguel Samper 
* [Spring Security Documentation](http://static.springsource.org/spring-security/site/reference.html)
