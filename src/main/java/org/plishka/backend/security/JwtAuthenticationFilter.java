package org.plishka.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.service.auth.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
        Optional<Claims> claims = jwtService.parseClaims(accessToken);

        if (claims.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(claims.get());
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!email.equals(userDetails.getUsername())) {
                log.debug(
                        "JWT subject does not match loaded user: tokenEmail={}, loadedUsername={}",
                        email,
                        userDetails.getUsername()
                );
                filterChain.doFilter(request, response);
                return;
            }

            if (!isAuthenticationAllowed(userDetails)) {
                log.debug(
                        "JWT authentication rejected due to user state: username={}, "
                                + "enabled={}, accountNonLocked={}",
                        userDetails.getUsername(),
                        userDetails.isEnabled(),
                        userDetails.isAccountNonLocked()
                );
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException exception) {
            log.debug("JWT authentication skipped because user was not found");
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticationAllowed(UserDetails userDetails) {
        return userDetails.isEnabled() && userDetails.isAccountNonLocked();
    }
}
