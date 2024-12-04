package com.bmg.deliver.config.okta;

import com.bmg.deliver.utils.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final OAuth2AuthorizedClientService authorizedClientService;

	public CustomOAuth2AuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService) {
		this.authorizedClientService = authorizedClientService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
		OAuth2AuthorizedClient client = authorizedClientService
				.loadAuthorizedClient(AppConstants.CLIENT_REGISTRATION_ID, oidcUser.getName());

		String accessToken = client.getAccessToken().getTokenValue();

		HttpSession session = request.getSession();
		session.setAttribute("jwt", accessToken);

		super.onAuthenticationSuccess(request, response, authentication);

	}
}
