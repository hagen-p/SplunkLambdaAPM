package com.sfx.JavaLambdaStarter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import brave.sampler.Sampler;
import brave.SpanCustomizer;
import brave.Tracer;

@SpringBootApplication
public class JavaLambdaStarterApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(JavaLambdaStarterApplication.class, args);
	}

@Bean
	public Sampler defaultSampler() {
		return Sampler.ALWAYS_SAMPLE;
	}
}
@RestController
class JavaLambdaStarterController {

    @Autowired	
	Tracer tracer;

	@Autowired 
	SpanCustomizer span;	
	
	@Autowired
	RestTemplate restTemplate;

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}


	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	// setting up some fields for span.tags
	private String sVersion =  "1.1";   	 // example fields that will be passed as tags
    private String sCustomerType =  "Gold";  // example fields that will be passed as tags
	private String sEnvironment = "Retail_Demo";	
	@GetMapping(value = "/JavaLambdaStarter")
	public String remotecall() {
		LOG.info("Inside remotecall");	
		span.tag("Version", sVersion);  // sending tag along in the span. usefull for development
		span.tag ("environment", sEnvironment);
		
		String url = "https://mc9va52sy5.execute-api.eu-west-1.amazonaws.com/default/RetailParentFunction";
		// create headers
   		HttpHeaders headers = new HttpHeaders();
    	// set `content-type` header
		headers.setContentType(MediaType.APPLICATION_JSON);
    	// set `accept` header
    	headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    	// create a map for post parameters
    	Map<String, Object> map = new HashMap<>();
    	map.put("ProductName", "sony S8");
		map.put("Quantity", 4);
		map.put("CustomerType", sCustomerType);
        // build the request
    	HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);
  		// send POST request
  		ResponseEntity <String> response = this.restTemplate.postForEntity(url, entity, String.class);
		
		//String response = (String) restTemplate.exchange(baseUrl, HttpMethod.GET,null, String.class).getBody();
		LOG.info("The response recieved by the remote call is " + response.toString());
	//	span.tag("Response",response); //capture the response in a tag as well
		return response.toString();
	}
	 

}
