package br.gov.alvara.service;

import br.gov.alvara.dto.request.LoginRequest;
import br.gov.alvara.dto.response.AuthResponse;
import br.gov.alvara.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.gerarToken(userDetails);
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        log.info("Login realizado com sucesso para: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .email(userDetails.getUsername())
                .role(role)
                .expiracaoMs(jwtUtil.getExpiration())
                .build();
    }
}
