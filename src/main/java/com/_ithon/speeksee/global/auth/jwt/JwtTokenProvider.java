package com._ithon.speeksee.global.auth.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com._ithon.speeksee.global.infra.exception.auth.SpeekseeAuthException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

// JWT 생성, 파싱, 유효성 검사
@Component
public class JwtTokenProvider {

	@Value("${jwt.secret}"
	)
	private String secretKey;
	@Value("${jwt.access-token-expiration}")
	private long accessTokenValidTime;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenValidTime;

	private SecretKey key;
	private JwtParser jwtParser;

	@PostConstruct
	public void init() {    // Bean 초기화 시, secret key를 Base64 인코딩하고 Key 객체로 변환
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.jwtParser = Jwts.parser().verifyWith(key).build();
	}

	// Access Token 생성(권한 포함)
	public String generateAccessToken(String email) {
		return generateToken(email, accessTokenValidTime, "access");
	}

	// Refresh Token 생성(권한 포함X)
	public String generateRefreshToken(String email) {
		return generateToken(email, refreshTokenValidTime, "refresh");
	}

	private String generateToken(String email, long validTime, String type) {
		Date now = new Date();
		Date expirationTime = new Date(now.getTime() + validTime);

		return Jwts.builder()
			.subject(email.toString())
			.claim("email", email)
			.claim("type", type)    // access, refresh
			.issuedAt(now)
			.expiration(expirationTime)
			.signWith(key, Jwts.SIG.HS256)
			.compact();
	}

	// 토큰 유효성 검증(서명, 만료 체크)
	public boolean validateToken(String token) {
		try {
			Claims claims = jwtParser.parseSignedClaims(token).getPayload();

			if (claims.getExpiration().before(new Date())) {
				throw new SpeekseeAuthException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
			}
			return true;
		} catch (JwtException e) {
			throw new SpeekseeAuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
		}
	}

	// 파싱
	public Long getUserIdFromToken(String token) {
		Claims claims = jwtParser.parseSignedClaims(token).getPayload();
		return Long.valueOf(claims.getSubject());
	}

	public String getEmailFromToken(String token) {
		return jwtParser.parseSignedClaims(token).getPayload().getSubject();
	}

	public String getTokenType(String token) {
		return jwtParser.parseSignedClaims(token).getPayload().get("type", String.class);
	}

	public long getAccessTokenExpirationMs() {
		return accessTokenValidTime; // application.yml 에서 주입받은 값
	}

	public long getRefreshTokenExpirationMs() {
		return refreshTokenValidTime; // application.yml 에서 주입받은 값
	}

	// 쿠키에서 refreshToken을 꺼내오는 메서드
	public String resolveRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refreshToken".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

}
