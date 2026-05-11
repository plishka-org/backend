package org.plishka.backend.controller;

import org.plishka.backend.config.TimeConfig;
import org.plishka.backend.security.CustomUserDetailsService;
import org.plishka.backend.service.auth.JwtService;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TimeConfig.class) 
public abstract class BaseControllerTest {
    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;
}
