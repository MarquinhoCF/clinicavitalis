package com.clinicavitalis.backend.controller;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicavitalis.backend.login.LoginRequestDTO;
import com.clinicavitalis.backend.login.LoginResponseDTO;
import com.clinicavitalis.backend.user.UserRepository;
import com.clinicavitalis.backend.user.User;

@RestController
@RequestMapping("/api/authenticate")
public class AuthenticateController {

    private final JwtEncoder jwtEncoder;
    private final UserRepository doctorRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticateController(JwtEncoder jwtEncoder, UserRepository doctorRepository, BCryptPasswordEncoder passwordEncoder) {
        this.jwtEncoder = jwtEncoder;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        Optional<User> user = doctorRepository.findByCpf(loginRequest.cpf());

        if (user.isPresent()) {
            throw new BadCredentialsException("O usuário não foi encontrado.");
        } else if (user.get().isLoginCorrect(loginRequest, passwordEncoder)) {
            throw new BadCredentialsException("A senha está incorreta.");
        }

        Instant now = Instant.now();
        Long expiresIn = 300L;

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("backend")
            .subject(user.get().getId().toString())
            .expiresAt(now.plusSeconds(expiresIn))
            .issuedAt(now)
            .build();

        String jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return ResponseEntity.ok(new LoginResponseDTO(jwtValue, expiresIn));
    }
}
