package hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * This class, as a basic starting point, will attempt to connect at TCP level to 
 * each endpoint that we need to integrate with. 
 * 
 * This provides a quick way, via the health endpoint, to figure out if something (firewall, 
 * system actually down etc etc) fundamental is affecting our application health.
 * 
 * @author nico
 */
@Component
public class AppHealthIndicator implements HealthIndicator {
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	@Value("${wso2SessionTerminateUrl}")
	private String strWso2SessionTerminateUrl;
	
	@Value("${security.oauth2.client.accessTokenUri}")
	private String accessTokenUri;
	
	@Value("${security.oauth2.client.userAuthorizationUri}")
	private String userAuthorizationUri;
	
	@Value("${security.oauth2.resource.userInfoUri}")
	private String userInfoUri;

	public static int DOWN = 1;
	public static int UP = 0;
	
	public Set<InetSocketAddress> endPoints = new HashSet<>();
	
	@PostConstruct
	public void init() {
		endPoints.add(toInetSocketAddress(strWso2SessionTerminateUrl));
		endPoints.add(toInetSocketAddress(accessTokenUri));
		endPoints.add(toInetSocketAddress(userAuthorizationUri));
		endPoints.add(toInetSocketAddress(userInfoUri));
	}
	
	InetSocketAddress toInetSocketAddress(String strUrl) {
		URL url;
		try {
			url = new URL(strUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return new InetSocketAddress(url.getHost(), url.getPort());
	}
	
	@Override
	public Health health() {
		int errorCode = appHealthCheck();
        if (errorCode != UP) {
            return Health.down().withDetail("Error Code", errorCode).build();
        }
        return Health.up().build();
	}
	
	/**
	 * Check whether the main integration points are up. 
	 * 
	 * When it comes to OAuth2 / OpenID Connect, these include:
	 * 
	 * accessTokenUri: e.g. https://localhost:9444/oauth2/token
	 * userAuthorizationUri: e.g. https://localhost:9444/oauth2/authorize
	 * userInfoUri: e.g. https://localhost:9444/oauth2/userinfo?schema=openid
	 * wso2SessionTerminateUrl: e.g. https://localhost:9443/commonauth?commonAuthLogout=true&type=oid&commonAuthCallerPath=http://localhost:8080/logout&relyingParty=localhost
	 * 
	 * If all our WSO2 IS endpoints are not responding, for whatever reason, then 
	 * conceivably as far as the user is concerned this application is down.
	 * 
	 * @return
	 */
	private int appHealthCheck() {
		
		boolean singleEndPointDown = false;
		for (InetSocketAddress inetSocketAddress : endPoints) {
			Socket socket = null;
			try {
				socket = new Socket();
				socket.connect(inetSocketAddress, 1000);
				if (socket.isConnected()) {
					logger.info("ENDPOINT UP: {}:{}", inetSocketAddress.getHostString(), inetSocketAddress.getPort());
					socket.close();
				}
			} catch (IOException e) {
				logger.info("END POINT DOWN: {}:{}",inetSocketAddress.getHostString(), inetSocketAddress.getPort());
				singleEndPointDown = true;
			} finally {
				if (socket != null && !socket.isClosed()) {
					try {
						socket.close();
					} catch (IOException e) {
						// Nothing we can do here.
						logger.info("IOException closing socket, nothing much to do from application layer. ", e);
					}
				}
			}
		}
		if (singleEndPointDown) {
			return DOWN;
		} else {
			return UP;
		}
		
	}
}
