package org.plishka.backend.controller;

import java.util.List;
import org.plishka.backend.config.TimeConfig;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.security.CustomUserDetailsService;
import org.plishka.backend.service.auth.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Import({TimeConfig.class, BaseControllerTest.AuthenticationPrincipalTestConfig.class})
public abstract class BaseControllerTest {
    private static final String AUTHENTICATED_USER_PRINCIPAL_ATTRIBUTE = "authenticatedUserPrincipal";

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;

    protected static RequestPostProcessor authenticatedUser(AuthenticatedUserPrincipal principal) {
        return request -> {
            request.setAttribute(AUTHENTICATED_USER_PRINCIPAL_ATTRIBUTE, principal);
            return request;
        };
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new TestAuthenticationPrincipalArgumentResolver());
        }
    }

    private static class TestAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && AuthenticatedUserPrincipal.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            Object principal = webRequest.getAttribute(
                    AUTHENTICATED_USER_PRINCIPAL_ATTRIBUTE,
                    RequestAttributes.SCOPE_REQUEST
            );
            if (principal == null) {
                throw new IllegalStateException(
                        "Missing AuthenticatedUserPrincipal in test request. Use .with(authenticatedUser(...))"
                );
            }
            return principal;
        }
    }
}
