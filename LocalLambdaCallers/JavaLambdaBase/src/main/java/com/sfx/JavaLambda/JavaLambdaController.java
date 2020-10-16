package com.sfx.JavaLambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.*;


/*
import brave.sampler.Sampler;
import brave.SpanCustomizer;
import brave.Tracer;
*/

@Controller
public class JavaLambdaController {
	/*
	 * // set up AutoWired sleuth for APM
	 * 
	 * @Autowired Tracer tracer;
	 * 
	 * @Autowired SpanCustomizer span;
	 */

	// setting up some fields for span.tags
	// private String environment = "Retail_Demo"; // Tag Used to set up APM environement.
	// private String version = "1.1"; // example fields that will be passed as tags

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	@Autowired
	RestTemplate restTemplate;

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@GetMapping("/order")
	public String orderForm(Model model) {
		// Set up inital values for html form
		model.addAttribute("order", new Order());
		return "home"; // view
	}

	@PostMapping("/order")
	public String orderSubmit(@ModelAttribute Order Order, Model model) {
		LOG.info("Inside OrderSubmit");
        // span.tag ("environment", sEnvironment);  // this tag is used by signalFX to place this in teh right cncepty in the ui - can be set by ENV variable or the agent
		// span.tag("Version", sVersion); // sending tag along in the span. usefull for development
		// replace url with proper URl of you Order Lambda

		LOG.info("Order:");
		LOG.info("phone   : " + Order.getPhoneType());
		LOG.info("Quanity : " + Order.getQuantity());
		LOG.info("Customer:"  + Order.getCustomerType());
		
		String url = "https://ckfnajft3i.execute-api.eu-west-1.amazonaws.com/default/RetailOrder";
		// create headers
		HttpHeaders headers = new HttpHeaders();
		// set `content-type` header
		headers.setContentType(MediaType.APPLICATION_JSON);
		// set `accept` header
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		// create a map for post parameters
		Map<String, Object> map = new HashMap<>();
		map.put("ProductName", "Order.phoneType");
		map.put("Quantity", "Order.quantity");
		map.put("CustomerType", "Order.customerType");
		// build the request
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);
		// send POST request
		ResponseEntity<Order> responseOrder = this.restTemplate.postForEntity(url, entity, Order.class);
		LOG.info("The response recieved by the remote call is " + responseOrder.toString());
		Order.setPrice(1111.1);
		Order.setTransaction("8072washere");
		model.addAttribute("order", Order);
		LOG.info("Leaving OrderSubmit");	
		return "result";
	}

}