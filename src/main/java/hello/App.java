package hello;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;

import org.apache.catalina.filters.RequestDumperFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateCustomizer;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@SpringBootApplication
@EnableOAuth2Sso
public class App extends WebSecurityConfigurerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(App.class);
	
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
    
    @Autowired
    private ResourceServerProperties sso;
    
    @Bean
    @Primary
    public ResourceServerTokenServices myUserInfoTokenServices() {
        return new AppUserInfoTokenServices(sso.getUserInfoUri(), sso.getClientId());
    }
    
    // see: https://spring.io/guides/tutorials/spring-boot-oauth2/#_social_login_logout
    @Override
	protected void configure(HttpSecurity http) throws Exception {
		http.antMatcher("/**")
			.authorizeRequests()
				.antMatchers("/", "/login**", "/webjars/**").permitAll()
				.anyRequest().authenticated()
			.and().logout().logoutSuccessUrl("/").permitAll()
			.and().csrf().csrfTokenRepository(new HttpSessionCsrfTokenRepository());
	}
    
//    @Bean
//    public FilterRegistrationBean requestDumperFilter() {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        Filter requestDumperFilter = new RequestDumperFilter();
//        registration.setFilter(requestDumperFilter);
//        registration.addUrlPatterns("/*");
//        return registration;
//    }
    
    @Bean
    public UserInfoRestTemplateCustomizer restTemplateCustomizer() {
    	return new UserInfoRestTemplateCustomizer() {

    		@Override
    		public void customize(OAuth2RestTemplate template) {
    			
    			// Give the RestTemplate a BufferingClientHttpRequestFactory so we can read the response twice
    			template.setRequestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    			
    			logger.info("####### Customize OAuth2RestTemplate");
    			ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {

    				private final Logger log = LoggerFactory.getLogger(getClass());
					@Override
					public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
							throws IOException {
						
						log.info("######### OAuth2RestTemplate HTTP REQUEST START");
						traceRequest(request, body);
						log.info("######### OAuth2RestTemplate HTTP REQUEST END");
						
						ClientHttpResponse response = execution.execute(request, body);
						traceResponse(response);
						log.info("######### OAuth2RestTemplate HTTP RESPONSE END");
						
						return response;
					}
    				
    			};
    			List<ClientHttpRequestInterceptor> ris = new ArrayList<ClientHttpRequestInterceptor>();
    			ris.add(interceptor);
    			template.setInterceptors(ris);
    			logger.info("####### Customize OAuth2RestTemplate");
    		}
    		
    		private void traceRequest(HttpRequest request, byte[] body) throws IOException {
    	        logger.info("===========================request begin================================================");
    	        logger.info("URI : " + request.getURI());
    	        logger.info("Method : " + request.getMethod());
    	        logger.info("Headers : " + request.getHeaders());
    	        logger.info("Request Body : " + new String(body, "UTF-8"));
    	        logger.info("==========================request end================================================");
    	    }

    	    private void traceResponse(ClientHttpResponse response) throws IOException {
    	        StringBuilder inputStringBuilder = new StringBuilder();
    	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody(), "UTF-8"));
    	        String line = bufferedReader.readLine();
    	        while (line != null) {
    	            inputStringBuilder.append(line);
    	            inputStringBuilder.append('\n');
    	            line = bufferedReader.readLine();
    	        }
    	        logger.info("============================response begin==========================================");
    	        logger.info("status code: " + response.getStatusCode());
    	        logger.info("status text: " + response.getStatusText());
    	        logger.info("Headers : " + response.getHeaders());
    	        logger.info("Response Body : " + inputStringBuilder.toString());
    	        logger.info("=======================response end=================================================");
    	    }
        	
        };
    }
    
}
