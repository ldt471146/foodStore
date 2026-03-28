package com.zhouri.farmshop.controller;

import com.zhouri.farmshop.domain.Role;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.security.AuthenticatedUser;
import com.zhouri.farmshop.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(new AuthService.RegisterCommand(
                request.username(),
                request.password(),
                request.fullName(),
                request.email(),
                request.phone(),
                request.address(),
                request.avatarColor(),
                request.role()
        ));
        return new AuthResponse(result.token(), toUserSummary(result.user()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.username(), request.password());
        return new AuthResponse(result.token(), toUserSummary(result.user()));
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resetPassword(request.email(), request.newPassword());
        return new MessageResponse("密码已重置，请使用新密码登录");
    }

    @GetMapping("/me")
    public UserSummary me(Authentication authentication) {
        var principal = (AuthenticatedUser) authentication.getPrincipal();
        return toUserSummary(authService.getUserProfile(principal.id()));
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserSummary updateProfile(
            Authentication authentication,
            @RequestParam @NotBlank @Size(min = 2, max = 80) String fullName,
            @RequestParam @Email @NotBlank String email,
            @RequestParam @NotBlank String phone,
            @RequestParam @NotBlank String address,
            @RequestParam(required = false) String avatarColor,
            @RequestParam(required = false) MultipartFile avatarFile
    ) {
        var principal = (AuthenticatedUser) authentication.getPrincipal();
        return toUserSummary(authService.updateProfile(
                principal.id(),
                new AuthService.ProfileCommand(fullName, email, phone, address, avatarColor),
                avatarFile
        ));
    }

    @GetMapping("/avatar/{filename}")
    public ResponseEntity<byte[]> avatar(@PathVariable String filename) {
        byte[] file = authService.readAvatar(filename);
        String contentType = filename.endsWith(".png") ? MediaType.IMAGE_PNG_VALUE
                : filename.endsWith(".webp") ? "image/webp"
                : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(file);
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getAvatarColor(),
                user.getAvatarImageUrl(),
                user.getRole()
        );
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 40) String username,
            @NotBlank @Size(min = 6, max = 64) String password,
            @NotBlank @Size(min = 2, max = 80) String fullName,
            @Email @NotBlank String email,
            @NotBlank String phone,
            @NotBlank String address,
            String avatarColor,
            Role role
    ) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record ForgotPasswordRequest(@Email @NotBlank String email, @NotBlank @Size(min = 6, max = 64) String newPassword) {
    }

    public record AuthResponse(String token, UserSummary user) {
    }

    public record UserSummary(
            Long id,
            String username,
            String fullName,
            String email,
            String phone,
            String address,
            String avatarColor,
            String avatarImageUrl,
            Role role
    ) {
    }

    public record MessageResponse(String message) {
    }
}
