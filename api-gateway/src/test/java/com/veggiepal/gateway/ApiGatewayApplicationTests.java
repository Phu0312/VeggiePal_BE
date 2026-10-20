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

		new CorsConfig().corsFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:5173");
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
	}

	@Test
	void gatewayDoesNotOwnMultipartParsingLimits() {
		assertThat(environment.getProperty("spring.servlet.multipart.max-file-size")).isNull();
		assertThat(environment.getProperty("spring.servlet.multipart.max-request-size")).isNull();
	}

}
