package com.gillsoft.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

public class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		String id = UUID.randomUUID().toString();
		logRequest(id, request, body);
		ClientHttpResponse response = new ClientHttpResponseWrapper(execution.execute(request, body));
		logResponse(id, response);
		return response;
	}

	private void logRequest(String id, HttpRequest request, byte[] body) throws IOException {
		LOGGER.info("==============request begin==============");
		LOGGER.info("Exchange id  : {}", id);
		LOGGER.info("URI          : {}", request.getURI());
		LOGGER.info("Method       : {}", request.getMethod());
		LOGGER.info("Headers      : {}", request.getHeaders());
		LOGGER.info("Request body : {}", new String(body, "UTF-8"));
		LOGGER.info("==============request end================");
	}

	private void logResponse(String id, ClientHttpResponse response) throws IOException {
		LOGGER.info("==============response begin=============");
		LOGGER.info("Exchange id  : {}", id);
		LOGGER.info("Status code  : {}", response.getStatusCode());
		LOGGER.info("Status text  : {}", response.getStatusText());
		LOGGER.info("Headers      : {}", response.getHeaders());
		LOGGER.info("Response body: {}", StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
		LOGGER.info("==============response end===============");
	}

}
