package com._ithon.speeksee.global.auth.jwt;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com._ithon.speeksee.domain.attendance.service.AttendanceService;
import com._ithon.speeksee.domain.member.dto.response.MemberInfoResponseDto;
import com._ithon.speeksee.domain.member.entity.Member;
import com._ithon.speeksee.global.auth.dto.request.LoginRequestDto;
import com._ithon.speeksee.global.auth.dto.response.LoginResponseDto;
import com._ithon.speeksee.global.auth.entity.RefreshToken;
import com._ithon.speeksee.global.auth.model.CustomUserDetails;
import com._ithon.speeksee.global.auth.repository.RefreshTokenRepository;
import com._ithon.speeksee.global.infra.exception.code.ErrorCode;
import com._ithon.speeksee.global.infra.exception.response.ApiRes;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;
	private final AttendanceService attendanceService;

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
		throws AuthenticationException {
		try {
			LoginRequestDto loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequestDto.class);

			UsernamePasswordAuthenticationToken token =
				new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());

			return authenticationManager.authenticate(token); // 실제 인증 수행
		} catch (IOException e) {
			throw new RuntimeException("로그인 요청 처리 실패", e);
		}
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
		FilterChain chain, Authentication authResult) throws IOException {

		CustomUserDetails userDetails = (CustomUserDetails)authResult.getPrincipal();
		Member member = userDetails.getMember();
		String email = userDetails.getUsername(); // 이메일값임

		// 토큰 생성
		String accessToken = jwtTokenProvider.generateAccessToken(email);
		String refreshToken = jwtTokenProvider.generateRefreshToken(email);
		long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs(); // 예: 3600초

		// 리프레쉬 토큰 저장
		RefreshToken refreshTokenEntity = buildRefreshTokenEntity(refreshToken, member);
		refreshTokenRepository.save(refreshTokenEntity);

		// 응답 DTO
		MemberInfoResponseDto memberInfo = buildMemberInfoDto(member);
		LoginResponseDto loginResponse = buildLoginResponseDto(accessToken, refreshToken, expiresIn,
			memberInfo);
		ApiRes<LoginResponseDto> apiResponse = ApiRes.success(loginResponse, "로그인 성공");

		// 응답 전송
		writeJsonResponse(response, apiResponse);
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException failed) throws IOException {

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 401

		// 모두 AUTH_FAIL 코드 사용
		String message;

		if (failed instanceof BadCredentialsException) {
			message = "아이디 또는 비밀번호가 올바르지 않습니다.";
		} else if (failed instanceof DisabledException) {
			message = "비활성화된 계정입니다.";
		} else {
			message = "로그인에 실패했습니다.";
		}

		ApiRes<Object> errorResponse = ApiRes.failure(HttpStatus.UNAUTHORIZED, message, ErrorCode.AUTH_FAIL);

		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}

	private RefreshToken buildRefreshTokenEntity(String token, Member member) {
		LocalDateTime expiryDate = LocalDateTime.now()
			.plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpirationMs()));
		return RefreshToken.builder()
			.member(member)
			.token(token)
			.used(false)
			.expiryDate(expiryDate)
			.build();
	}

	private MemberInfoResponseDto buildMemberInfoDto(Member member) {
		return MemberInfoResponseDto.builder()
			.userId(member.getId())
			.email(member.getEmail())
			.username(member.getUsername())
			.currentLevel(member.getCurrentLevel())
			.consecutiveDays(member.getConsecutiveDays())
			.totalExp(member.getTotalExp())
			.createdAt(member.getCreatedAt())
			.build();
	}

	private LoginResponseDto buildLoginResponseDto(String accessToken, String refreshToken,
		long expiresIn, MemberInfoResponseDto memberInfo) {
		return LoginResponseDto.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.expiresIn(expiresIn)
			.memberInfo(memberInfo)
			.build();
	}

	private void writeJsonResponse(HttpServletResponse response, Object body) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json; charset=UTF-8");
		objectMapper.writeValue(response.getWriter(), body);
	}

}
