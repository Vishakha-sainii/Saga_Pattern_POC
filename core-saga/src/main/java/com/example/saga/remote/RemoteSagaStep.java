package com.example.saga.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.example.saga.core.SagaStep;

public class RemoteSagaStep implements SagaStep {

	private static final Logger log = LoggerFactory.getLogger(RemoteSagaStep.class);

	private final String service;
	private final String method;
	private final String url;

	public RemoteSagaStep(String service, String method, String url) {
		this.service = service;
		this.method = method.toUpperCase();
		this.url = url;
	}

	@Override
	public void compensate() {
		RestTemplate rest = new RestTemplate();
		log.info("Starting remote rollback | service={} | method={} | url={}", service, method, url);

		try {
			switch (method) {
			case "DELETE" -> {rest.delete(url);}
			case "POST" -> {rest.postForObject(url, null, Void.class);}
			case "PUT" -> {rest.put(url, null);}
			case "PATCH" -> {rest.patchForObject(url, null, Void.class);}
			default -> throw new RuntimeException("Unsupported rollback method: " + method);
			}
			log.info("Remote rollback success | service={}", service);

		} catch (Exception ex) {
			log.error("Remote rollback failed | service={} | reason={}", service, ex.getMessage());
			throw new RuntimeException(ex);
		}
	}
}