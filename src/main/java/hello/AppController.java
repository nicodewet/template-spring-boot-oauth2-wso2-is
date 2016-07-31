package hello;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AppController {
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String WSO2_SESSION_TERMINATE_URL = "https://localhost:9443/commonauth?commonAuthLogout=true&type=oid&commonAuthCallerPath=http://localhost:8080/logout&relyingParty=localhost";
	
    @RequestMapping("/")
    public String greeting(Model model) {
        model.addAttribute("wso2SessionTerminateUrl", WSO2_SESSION_TERMINATE_URL);
        return "index";
    }
    
    /**
     * NOTE that this method assumes that the user performing this action is authenticated.
     * This is implied by the implementation, noting it here in addition to this.
     */
    @RequestMapping(value="/logout", method = RequestMethod.GET)
    public String logoutPage (HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/";
    }
}
