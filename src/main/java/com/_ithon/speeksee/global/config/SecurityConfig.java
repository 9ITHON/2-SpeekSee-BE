package com._ithon.speeksee.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import com._ithon.speeksee.domain.attendance.service.AttendanceService;
import com._ithon.speeksee.global.auth.jwt.JwtAuthenticationFilter;
import com._ithon.speeksee.global.auth.jwt.JwtTokenProvider;
import com._ithon.speeksee.global.auth.jwt.LoginFilter;
import com._ithon.speeksee.global.auth.repository.RefreshTokenRepository;
import com._ithon.speeksee.global.auth.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RefreshTokenRepository refreshTokenRepository;

	//    private final JWTUtil jwtUtil;
	private final ObjectMapper objectMapper;
	//    private final RefreshTokenService refreshTokenService;
	//    private final BlacklistService blacklistService;
	private final CustomUserDetailsService customUserDetailsService;
	private final JwtTokenProvider jwtTokenProvider;
	private final AttendanceService attendanceService;
	//    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	//    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
	//    private final CustomOAuth2UserService customOAuth2UserService;

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authManager) throws
		Exception {

		LoginFilter loginFilter = new LoginFilter(refreshTokenRepository, authManager, jwtTokenProvider, objectMapper, attendanceService);
		loginFilter.setFilterProcessesUrl("/api/auth/login");

		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)

			.cors(cors -> cors.configurationSource(request -> {
				CorsConfiguration config = new CorsConfiguration();
				config.setAllowedOriginPatterns(List.of("*"));
				config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
				config.setAllowCredentials(true);
				config.setAllowedHeaders(List.of("*"));
				config.setExposedHeaders(List.of("Authorization"));
				config.setMaxAge(3600L);
				return config;
			}))

			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			.authorizeHttpRequests(auth -> auth
				.anyRequest().permitAll())

			.exceptionHandling(except -> except
				.authenticationEntryPoint((request, response, authException) ->
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")));

		http.addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
			UsernamePasswordAuthenticationFilter.class);

		//        http.oauth2Login(oauth2 -> oauth2
		//                .successHandler(oAuth2LoginSuccessHandler)
		//                .failureHandler(oAuth2LoginFailureHandler)
		//                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
		//        );

		//        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);
		//        http.addFilterBefore(
		//                new JwtAuthenticationFilter(jwtUtil, blacklistService, customUserDetailsService),
		//                UsernamePasswordAuthenticationFilter.class
		//        );

		return http.build();
	}
}
