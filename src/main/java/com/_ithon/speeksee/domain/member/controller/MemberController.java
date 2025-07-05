package com._ithon.speeksee.domain.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._ithon.speeksee.domain.member.dto.request.SignUpRequestDto;
import com._ithon.speeksee.domain.member.dto.response.MemberInfoResponseDto;
import com._ithon.speeksee.domain.member.entity.Member;
import com._ithon.speeksee.domain.member.service.MemberService;
import com._ithon.speeksee.global.auth.model.CustomUserDetails;
import com._ithon.speeksee.global.infra.exception.response.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Member", description = "유저 관련 API")
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "내 정보 조회",
		description = "인증된 사용자의 정보를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
	})
	@GetMapping("/me")
	public ResponseEntity<ApiRes<MemberInfoResponseDto>> getMyInfo(
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		String username = userDetails.getUsername(); // 또는 email
		Member member = memberService.findByEmail(username); // DB에서 조회
		MemberInfoResponseDto response = memberService.getMyInfo(member);
		return ResponseEntity.ok(ApiRes.success(response));
	}
}
