package com.sfx.JavaLambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.*;
import java.io.IOException;

import brave.sampler.Sampler;
import brave.SpanCustomizer;
import brave.Tracer;


@Controller
public class JavaLambdaController {
	
	 // set up AutoWired sleuth for APM
	
	@Autowired Tracer tracer;
	 
	@Autowired SpanCustomizer span;
	

	// setting up some fields for span.tags
	private String environment = "Retail_Demo"; // Tag Used to set up APM environement.
	private String version = "1.1"; // example fields that will be passed as tags

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
		return "order"; // view
	}

	@PostMapping("/order")
	public String orderSubmit(@ModelAttribute Order Order, Model model) throws IOException  {
		LOG.info("Inside OrderSubmit");
         span.tag ("environment", environment);  // this tag is used by signalFX to place this in teh right cncepty in the ui - can be set by ENV variable or the agent
		 span.tag("Version", version); // sending tag along in the span. usefull for development
		
		LOG.info("Order:");
		LOG.info("phone   : " + Order.getPhoneType());
		LOG.info("Quanity : " + Order.getQuantity());
		LOG.info("Customer:"  + Order.getCustomerType());
		span.tag("phone",    Order.getPhoneType());
		span.tag("Quanity",  String.valueOf(Order.getQuantity()));
		span.tag("Customer", Order.getCustomerType());
		
		// replace url with proper URl of you Order Lambda
		//String url = "https://ckfnajft3i.execute-api.eu-west-1.amazonaws.com/default/RetailOrder";
		String url = "https://d1t91qsus2.execute-api.eu-west-1.amazonaws.com/default/RetailOrderAPM";
		// create headers
		HttpHeaders headers = new HttpHeaders();
		// set `content-type` header
		headers.setContentType(MediaType.APPLICATION_JSON);
		// set `accept` header
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		// create a map for post parameters
		Map<String, Object> map = new HashMap<>();
		map.put("ProductName", Order.getPhoneType());
		map.put("Quantity",  Order.getQuantity());
		map.put("CustomerType", Order.getCustomerType());
		// build the request
		HttpEntity<Map<String, Object>> orderRequest = new HttpEntity<>(map, headers);
		// send POST request
		ResponseEntity<String> returnedOrder = this.restTemplate.postForEntity(url, orderRequest, String.class);
        
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader objectReader = objectMapper.readerForUpdating(Order);
		
		Order newOrder = objectReader.readValue(returnedOrder.getBody());
		LOG.info("The response recieved by the remote call is " + returnedOrder.toString());
		//span.tag("Lambda Response",returnedOrder.toString());
		model.addAttribute("order", newOrder);
		LOG.info("Leaving OrderSubmit");	
		return "result";
	}

}