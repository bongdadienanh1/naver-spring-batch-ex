package com.naver.spring.batch.extension.item.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class AbstractAsyncRestItemReader<T> implements ItemReader<T>, InitializingBean {
	private Logger log = LoggerFactory.getLogger(AbstractAsyncRestItemReader.class);

	private AsyncRestTemplate asyncRestTemplate;
	private Map<Map<String, ?>, ListenableFuture<ResponseEntity<String>>> responseMap = null;
	private Iterator<T> itemIterator = null;
	private Iterator<Map<String, ?>> uriVariablesIterator;

	private String apiUri;

	private List<Map<String, ?>> uriVariables;

	private AsyncListenableTaskExecutor asyncListenableTaskExecutor;

	private HttpHeaders headers;

	private int responseTimeout = 30;

	public void setApiUri(String apiUri) {
		this.apiUri = apiUri;
	}

	public void setUriVariables(List<Map<String, ?>> uriVariables) {
		this.uriVariables = uriVariables;
	}

	public void setAsyncListenableTaskExecutor(AsyncListenableTaskExecutor asyncListenableTaskExecutor) {
		this.asyncListenableTaskExecutor = asyncListenableTaskExecutor;
	}

	public void setHeaders(HttpHeaders headers) {
		this.headers = headers;
	}

	public void setResponseTimeout(int responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	abstract protected List<T> convertResponse(ResponseEntity<String> responseEntity, Map<String, ?> uriVariable);

	@Override
	public T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		requestAsyncRestApis();

		if (itemIterator == null || !itemIterator.hasNext()) {

			while (this.uriVariablesIterator.hasNext()) {
				Map<String, ?> uriVariable = this.uriVariablesIterator.next();
				ListenableFuture<ResponseEntity<String>> listenableFuture = responseMap.get(uriVariable);
				ResponseEntity<String> responseEntity = null;

				try {
					responseEntity = listenableFuture.get(this.responseTimeout, TimeUnit.SECONDS);
				} catch (Exception e) {
					log.warn("retrieving response error", e);
				}

				List<T> results = convertResponse(responseEntity, uriVariable);

				if (results != null && !results.isEmpty()) {
					this.itemIterator = results.iterator();
					break;
				}
			}
		}

		return (itemIterator != null && itemIterator.hasNext()) ? itemIterator.next() : null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(apiUri, "'apiUri' is required");

		if (asyncListenableTaskExecutor != null) {
			asyncRestTemplate = new AsyncRestTemplate(asyncListenableTaskExecutor);
		} else {
			asyncRestTemplate = new AsyncRestTemplate();
		}
		asyncRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
	}

	private void requestAsyncRestApis() {
		if (this.responseMap != null) {
			return;
		}

		if (this.uriVariables == null || this.uriVariables.isEmpty()) {
			this.uriVariables = Collections.singletonList(Collections.emptyMap());
		}

		this.uriVariablesIterator = this.uriVariables.iterator();
		this.responseMap = new HashMap<>();

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(apiUri);

		if (this.headers == null) {
			this.headers = new HttpHeaders();
		}

		for (Map<String, ?> uriVariable : this.uriVariables) {
			URI uri = uriBuilder.buildAndExpand(uriVariable).toUri();

			if (log.isDebugEnabled()) {
				log.debug("request uri: {}", uri.toString());
			}

			ListenableFuture<ResponseEntity<String>> resFuture =
					asyncRestTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(this.headers), String.class);

			this.responseMap.put(uriVariable, resFuture);
		}
	}
}