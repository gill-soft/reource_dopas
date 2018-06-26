package com.gillsoft.client;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import com.gillsoft.util.StringUtil;

public class RequestResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		String id = StringUtil.generateUUID();
		logRequest(id, request, body);
		ClientHttpResponse response = new ClientHttpResponseWrapper(execution.execute(request, body));
		logResponse(id, response);
		return response;
	}

	private void logRequest(String id, HttpRequest request, byte[] body) throws IOException {
		LOGGER.info(new StringBuilder().append("\n")
				.append("==============request begin==============").append("\n")
				.append("Exchange id  : ").append(id).append("\n")
				.append("URI          : ").append(request.getURI()).append("\n")
				.append("Method       : ").append(request.getMethod()).append("\n")
				.append("Headers      : ").append(request.getHeaders()).append("\n")
				.append("Request body : ").append(new String(body, Charset.forName("windows-1251"))).append("\n")
				.append("==============request end================").toString());
	}

	private void logResponse(String id, ClientHttpResponse response) throws IOException {
		LOGGER.info(new StringBuilder().append("\n")
				.append("==============response begin=============").append("\n")
				.append("Exchange id  : ").append(id).append("\n")
				.append("Status code  : ").append(response.getStatusCode()).append("\n")
				.append("Status text  : ").append(response.getStatusText()).append("\n")
				.append("Headers      : ").append(response.getHeaders()).append("\n")
				.append("Response body: ").append(
						StreamUtils.copyToString(response.getBody(), Charset.forName("windows-1251"))).append("\n")
				.append("==============response end===============").toString());
	}

}
