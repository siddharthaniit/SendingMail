package com.example.controller;

import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.model.User;
import com.example.service.EmailService;
import com.example.service.UserService;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

@RestController
public class RegisterController {
	
	@RequestMapping(value = "login")
	public String m1()
	{
		return "hii welcome";
	}
	
	
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	private UserService userService;
	private EmailService emailService;
	
	@Autowired
    public RegisterController( UserService userService, EmailService emailService) {
      
     // this.bCryptPasswordEncoder = bCryptPasswordEncoder;
      this.userService = userService;
      this.emailService = emailService;
    }
 

	@RequestMapping(value="/register", method = RequestMethod.GET)
	public ModelAndView showRegisterationPage(ModelAndView modelAndView , User user)
	{
		modelAndView.addObject("user",user);
		modelAndView.setViewName("register");
		return modelAndView;
		
	}
			
	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public ModelAndView processRegistrationForm(ModelAndView modelAndView, @Valid User user, BindingResult bindingResult, HttpServletRequest request) {
				
     User userExists = userService.findByEmail(user.getEmail()); 
     if (userExists != null) {
			modelAndView.addObject("alreadyRegisteredMessage", "Oops!  There is already a user registered with the email provided.");
			modelAndView.setViewName("register");
			bindingResult.reject("email");
		}
     if (bindingResult.hasErrors()) { 
			modelAndView.setViewName("register");		
		} else { 
					
			// Disable user until they click on confirmation link in email
		    user.setEnabled(false);
		      
		    // Generate random 36-character string token for confirmation link
		    user.setConfirmationToken(UUID.randomUUID().toString());
		        
		    userService.save(user);
				
			String appUrl = request.getScheme() + "://" + request.getServerName();
			
			SimpleMailMessage registrationEmail = new SimpleMailMessage();
			registrationEmail.setTo(user.getEmail());
			registrationEmail.setSubject("Registration Confirmation");
			registrationEmail.setText("To confirm your e-mail address, please click the link below:\n"
					+ appUrl + ":8080/confirm?token=" + user.getConfirmationToken());
			registrationEmail.setFrom("kodamsiddhartha@gmail.com");
			emailService.sendEmail(registrationEmail);
			modelAndView.addObject("confirmationMessage", "A confirmation e-mail has been sent to " + user.getEmail());
			modelAndView.setViewName("register");
		}
			
		return modelAndView;
	}
		
	@RequestMapping(value="/confirm", method = RequestMethod.GET)
	public ModelAndView showConfirmationPage(ModelAndView modelAndView, @RequestParam("token") String token) {
		
		User user = userService.findByConfirmationToken(token);
		if (user == null) { // No token found in DB
			modelAndView.addObject("invalidToken", "Oops!  This is an invalid confirmation link.");
		} else { // Token found
			modelAndView.addObject("confirmationToken", user.getConfirmationToken());
		}	
		modelAndView.setViewName("confirm");
		return modelAndView;		
	}
	
	// Process confirmation link
	    
		@RequestMapping(value="/confirm", method = RequestMethod.POST)
		public ModelAndView processConfirmationForm(ModelAndView modelAndView, BindingResult bindingResult, @RequestParam Map requestParams, RedirectAttributes redir) {
					
			modelAndView.setViewName("confirm");
			
			Zxcvbn passwordCheck = new Zxcvbn();
			
			Strength strength = passwordCheck.measure((String) requestParams.get("password"));
			
			if (strength.getScore() < 3) {
				bindingResult.reject("password");
				
				redir.addFlashAttribute("errorMessage", "Your password is too weak.  Choose a stronger one.");

				modelAndView.setViewName("redirect:confirm?token=" + requestParams.get("token"));
				System.out.println(requestParams.get("token"));
				return modelAndView;
			}
			// Find the user associated with the reset token
			User user = userService.findByConfirmationToken((String) requestParams.get("token"));

			// Set new password
		//	user.setPassword(bCryptPasswordEncoder.encode((CharSequence) requestParams.get("password")));
			user.setPassword((String) requestParams.get("password"));

			// Set user to enabled
			user.setEnabled(true);
			
			// Save user
			userService.save(user);
			
			modelAndView.addObject("successMessage", "Your password has been set!");
			return modelAndView;		
		}

}
