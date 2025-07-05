package com._ithon.speeksee.global.auth.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._ithon.speeksee.global.auth.dto.request.LoginRequestDto;
import com._ithon.speeksee.global.auth.dto.request.RefreshTokenRequestDto;
import com._ithon.speeksee.global.auth.dto.response.AccessTokenResponseDto;
import com._ithon.speeksee.global.auth.dto.response.LoginResponseDto;
import com._ithon.speeksee.global.auth.jwt.JwtTokenProvider;
import com._ithon.speeksee.global.auth.dto.request.GoogleOAuthRequestDto;
import com._ithon.speeksee.global.auth.service.OAuth2LoginService;
import com._ithon.speeksee.global.auth.service.AuthService;
import com._ithon.speeksee.global.infra.exception.auth.SpeekseeAuthException;
import com._ithon.speeksee.global.infra.exception.code.ErrorCode;
import com._ithon.speeksee.global.infra.exception.response.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "기본 로그인/로그아웃", description = "인증 관련 API")
public class AuthController {

	private final AuthService authService;
	private final JwtTokenProvider jwtTokenProvider;

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고, accessToken을 반환합니다. refreshToken은 쿠키로 전달됩니다."
		+ "이 api는 스웨거용입니다 실제 로그인 api는 /api/auth/login 입니다")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그인 성공"),
	})
	@PostMapping("/loginTest")
	public ResponseEntity<ApiRes<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto,
		HttpServletResponse response) {
		LoginResponseDto loginResponse = authService.login(loginRequestDto);

		// HttpOnly 쿠키로 refreshToken 내려주기
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
			.httpOnly(true)
			.secure(false)
			.sameSite("Lax")
			.path("/")
			.maxAge(Duration.ofDays(14))
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

		return ResponseEntity.ok(ApiRes.success(loginResponse));
	}

	@Operation(summary = "액세스 토큰 재발급", description = "refreshToken을 이용해 새로운 accessToken을 발급받습니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "재발급 성공"),
	})
	@PostMapping("/refresh")
	public ResponseEntity<ApiRes<AccessTokenResponseDto>> refreshAccessToken(
		@RequestBody RefreshTokenRequestDto request) {

		String refreshToken = request.getRefreshToken();

		// 1. 새 Access Token 발급
		LoginResponseDto loginResponse = authService.refreshAccessToken(refreshToken);

		// 2. 응답 DTO 생성
		AccessTokenResponseDto response = new AccessTokenResponseDto(
			loginResponse.getAccessToken(),
			(int)loginResponse.getExpiresIn()
		);

		// 3. API 응답 래핑
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@Operation(summary = "로그아웃", description = "refreshToken 쿠키를 제거하여 로그아웃합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그아웃 성공",
			content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiRes<String>> logout(HttpServletResponse response) {
		ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.secure(false)
			.sameSite("Lax")
			.path("/")
			.maxAge(0)
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
		return ResponseEntity.ok(ApiRes.success("로그아웃 되었습니다."));
	}
}
