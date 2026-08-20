package com.community.board.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuthRedirectProperties.class)
public class SecurityConfig {

    @Bean
    OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GoogleOidcUserService googleOidcUserService,
            OidcLoginSuccessHandler successHandler,
            OidcLoginFailureHandler failureHandler,
            SecurityErrorResponseWriter errorResponseWriter,
            SecurityContextRepository securityContextRepository
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/members/signup")
                        .hasRole("SIGNUP_REQUIRED")
                        .requestMatchers(HttpMethod.GET, "/api/members/me").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/reviews/**")
                        .permitAll()
                        .requestMatchers("/api/reviews/**", "/api/comments/**").hasRole("MEMBER")
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errorResponseWriter.write(
                                        response,
                                        401,
                                        "AUTHENTICATION_REQUIRED",
                                        "인증이 필요합니다."
                                ))
                        .accessDeniedHandler((request, response, exception) ->
                                errorResponseWriter.write(
                                        response,
                                        403,
                                        "ACCESS_DENIED",
                                        "접근 권한이 없습니다."
                                ))
                )
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204))
                );

        return http.build();
    }
}
