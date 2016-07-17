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

## Known issues

Note, you'll see an issue relating the Principal in the DEBUG logs, this is a known issue, and a solution will be added here in time. For now though, at least you can log in and the integration works with minimal effort.

Also, use Chrome or Firefox, you'll run into issues with Safari. More on this in time.
you can log in.  
