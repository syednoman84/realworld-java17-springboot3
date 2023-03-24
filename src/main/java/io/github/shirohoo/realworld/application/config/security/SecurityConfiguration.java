package io.github.shirohoo.realworld.application.config.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.shirohoo.realworld.application.config.web.ExceptionResolveFilter;
import io.github.shirohoo.realworld.domain.user.UserRepository;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ExceptionResolveFilter exceptionResolveFilter,
            UsernamePasswordAuthenticationProcessingFilter usernamePasswordAuthenticationProcessingFilter)
            throws Exception {
        return http.httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests.requestMatchers(HttpMethod.POST, "/api/users")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
                .exceptionHandling(
                        handler -> handler.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                                .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .addFilterBefore(
                        usernamePasswordAuthenticationProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(exceptionResolveFilter, UsernamePasswordAuthenticationProcessingFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(List.of(authenticationProvider));
    }

    @Bean
    AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) {
        return new UsernamePasswordAuthenticationProvider(passwordEncoder, userDetailsService);
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository) {
        return new UsernamePasswordUserDetailsService(userRepository);
    }

    @Bean
    UsernamePasswordAuthenticationProcessingFilter usernamePasswordAuthenticationProcessingFilter(
            ObjectMapper objectMapper,
            AuthenticationManager authenticationManager,
            UsernamePasswordAuthenticationSuccessHandler successHandler) {
        UsernamePasswordAuthenticationProcessingFilter filter =
                new UsernamePasswordAuthenticationProcessingFilter(objectMapper);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(successHandler);
        return filter;
    }

    @Bean
    UsernamePasswordAuthenticationSuccessHandler usernamePasswordAuthenticationSuccessHandler(
            ObjectMapper objectMapper, JwtProvider jwtProvider) {
        return new UsernamePasswordAuthenticationSuccessHandler(objectMapper, jwtProvider);
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.key.public}") RSAPublicKey rsaPublicKey) {
        return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }

    @Bean
    JwtEncoder jwtEncoder(
            @Value("${security.key.public}") RSAPublicKey rsaPublicKey,
            @Value("${security.key.private}") RSAPrivateKey rsaPrivateKey) {
        JWK jwk = new RSAKey.Builder(rsaPublicKey).privateKey(rsaPrivateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}