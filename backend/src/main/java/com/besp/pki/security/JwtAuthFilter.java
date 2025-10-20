 package com.besp.pki.security;
import com.besp.pki.repository.RevokedTokenRepository;
import com.besp.pki.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, RevokedTokenRepository revokedTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validate(token)) {
                    // Check if token is revoked (blacklist check)
                    String jti = jwtUtil.getJti(token);
                    if (revokedTokenRepository.existsByJti(jti)) {
                        // Token is revoked, reject request
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Token has been revoked");
                        return;
                    }
                    
                    Claims claims = jwtUtil.getAllClaims(token);
                    String subject = claims.getSubject();

                    // Accept several claim shapes: roles: ["ROLE_ADMIN"] | role: "ROLE_ADMIN" | authority: "ROLE_ADMIN"
                    List<String> roleStrings = new ArrayList<>();
                    Object rolesObj = claims.get("roles");
                    if (rolesObj instanceof List<?> list) {
                        list.forEach(v -> roleStrings.add(String.valueOf(v)));
                    }
                    if (claims.get("role") != null) roleStrings.add(String.valueOf(claims.get("role")));
                    if (claims.get("authority") != null) roleStrings.add(String.valueOf(claims.get("authority")));

                    List<GrantedAuthority> authorities = roleStrings.stream()
                            .map(String::valueOf)
                            .distinct()
                            .map(SimpleGrantedAuthority::new)
                            .map(a -> (GrantedAuthority) a)
                            .toList();

                    var auth = new UsernamePasswordAuthenticationToken(subject, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // invalid token -> leave anonymous
            }
        }
        chain.doFilter(request, response);
    }
}
