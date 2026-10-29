package com.veggiepal.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.veggiepal.gateway.configuration.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest
class ApiGatewayApplicationTests {

	@Autowired Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	void corsAllowsCredentialedFrontendPreflight() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/ai/videos");
		request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new CorsConfig(CorsConfig.DEFAULT_ALLOWED_ORIGINS).corsFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:5173");
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
	}

	@Test
	void corsRejectsUntrustedArbitraryOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/ai/videos");
		request.addHeader(HttpHeaders.ORIGIN, "https://evil-hacker.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new CorsConfig(CorsConfig.DEFAULT_ALLOWED_ORIGINS).corsFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
	}

	@Test
	void corsAllowsConfiguredProductionOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/ai/videos");
		request.addHeader(HttpHeaders.ORIGIN, "https://veggiepal.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new CorsConfig("https://veggiepal.com").corsFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://veggiepal.com");
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
	}

	@Test
	void clientIpForwardingFilter_replacesSpoofedHeaderWithRemoteAddr() {
		com.veggiepal.gateway.configuration.ClientIpForwardingFilter filter =
				new com.veggiepal.gateway.configuration.ClientIpForwardingFilter("");

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/ai/chat/guest");
		servletRequest.setRemoteAddr("203.0.113.195");
		org.springframework.web.servlet.function.ServerRequest serverRequest =
				org.springframework.web.servlet.function.ServerRequest.create(servletRequest, java.util.List.of());

		HttpHeaders incomingHeaders = new HttpHeaders();
		incomingHeaders.set("X-Forwarded-For", "8.8.8.8");

		HttpHeaders forwardedHeaders = filter.apply(incomingHeaders, serverRequest);

		assertThat(forwardedHeaders.getFirst("X-Forwarded-For")).isEqualTo("203.0.113.195");
		assertThat(forwardedHeaders.getFirst("X-Real-IP")).isEqualTo("203.0.113.195");
	}

	@Test
	void clientIpForwardingFilter_trustsUpstreamProxyWhenConfigured() {
		com.veggiepal.gateway.configuration.ClientIpForwardingFilter filter =
				new com.veggiepal.gateway.configuration.ClientIpForwardingFilter("10.0.0.1");

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/ai/chat/guest");
		servletRequest.setRemoteAddr("10.0.0.1");
		org.springframework.web.servlet.function.ServerRequest serverRequest =
				org.springframework.web.servlet.function.ServerRequest.create(servletRequest, java.util.List.of());

		HttpHeaders incomingHeaders = new HttpHeaders();
		incomingHeaders.set("X-Forwarded-For", "198.51.100.5");

		HttpHeaders forwardedHeaders = filter.apply(incomingHeaders, serverRequest);

		assertThat(forwardedHeaders.getFirst("X-Forwarded-For")).isEqualTo("198.51.100.5, 10.0.0.1");
	}

	@Test
	void gatewayDoesNotOwnMultipartParsingLimits() {
		assertThat(environment.getProperty("spring.servlet.multipart.max-file-size")).isNull();
		assertThat(environment.getProperty("spring.servlet.multipart.max-request-size")).isNull();
	}

	@Test
	void routesAdminPaymentsToPaymentService() {
		assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[21].predicates[0]"))
				.isEqualTo("Path=/api/admin/payments/**");
		assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[21].filters[0]"))
				.isEqualTo("StripPrefix=1");
	}

}
