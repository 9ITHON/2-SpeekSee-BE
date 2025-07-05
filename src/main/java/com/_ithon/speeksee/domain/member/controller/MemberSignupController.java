package com._ithon.speeksee.domain.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._ithon.speeksee.domain.member.dto.request.SignUpRequestDto;
import com._ithon.speeksee.domain.member.dto.response.SignUpResponseDto;
import com._ithon.speeksee.domain.member.entity.Member;
import com._ithon.speeksee.domain.member.service.MemberSignupService;
import com._ithon.speeksee.global.infra.exception.response.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "일반 회원가입", description = "회원가입")
public class MemberSignupController {

	private final MemberSignupService memberSignupService;

	public MemberSignupController(MemberSignupService memberSignupService) {
		this.memberSignupService = memberSignupService;
	}

	@PostMapping("/signup")
	@Operation(
		summary = "회원가입",
		description = "새로운 회원을 등록합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "회원가입 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "회원가입 성공 예시",
					value = """
						{
						    "success": true,
						    "data": {
						        "userId": 3,
						        "email": "user1@example.com",
						        "username": "홍길동1"
						    },
						    "message": "성공",
						    "status": 200,
						    "code": 0,
						    "time": "2025-07-06T03:19:03.1233907"
						}
						"""
				)
			)
		)
	})
	public ResponseEntity<ApiRes<SignUpResponseDto>> signup(@RequestBody @Valid SignUpRequestDto signUpRequestDto) {
		Member member = memberSignupService.signUp(signUpRequestDto); // ✅ 결과 받기

		SignUpResponseDto response = SignUpResponseDto.builder()
			.userId(member.getId())
			.email(member.getEmail())
			.username(member.getUsername())
			.build();

		return ResponseEntity.ok(ApiRes.success(response));
	}

}
