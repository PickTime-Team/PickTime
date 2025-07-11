package com.b101.pickTime.common.auth;

import com.b101.pickTime.common.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

@RequiredArgsConstructor
//@AllArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String username = obtainUsername(request);
        String password = obtainPassword(request);


        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

        return authenticationManager.authenticate(authToken);
    }

    @Override
    // 성공시 실행
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();
        Integer userId = customUserDetails.getUserId();

        // 토큰 생성하기
        String access = jwtUtil.createJwt("access", userId, username, role, JWTUtil.ACCESS_TOKEN_VALIDITY_TIME);
        String refresh = jwtUtil.createJwt("refresh", userId, username, role, JWTUtil.REFRESH_TOKEN_VALIDITY_TIME);

        // 응답 설정
        response.setHeader("Authorization", JWTUtil.BEARER_PREFIX +access);
        response.addCookie(createCookie("refresh", refresh));
        responseWrite("login success", HttpStatus.OK, response);
    }

    @Override
    // 실패시 실행
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        responseWrite("login failed", HttpStatus.UNAUTHORIZED, response);
    }

    // 쿠키 생성 메서드
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        cookie.setHttpOnly(true);

        return cookie;
    }

    private void responseWrite(String message,  HttpStatus status, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println(message);
        response.setStatus(status.value());
    }
}
