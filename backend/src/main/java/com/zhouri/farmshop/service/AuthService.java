package com.zhouri.farmshop.service;

import com.zhouri.farmshop.domain.Role;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import com.zhouri.farmshop.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Path avatarUploadDir = Path.of("uploads", "avatars").toAbsolutePath().normalize();

    public AuthResult login(String username, String password) {
        var user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new AuthResult(jwtService.generateToken(user), user);
    }

    @Transactional(readOnly = true)
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    @Transactional
    public User updateProfile(Long userId, ProfileCommand command, MultipartFile avatarFile) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        String normalizedEmail = command.email().trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已存在");
                });

        user.setFullName(command.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(command.phone().trim());
        user.setAddress(command.address().trim());
        user.setAvatarColor(normalizeAvatarColor(command.avatarColor()));

        if (avatarFile != null && !avatarFile.isEmpty()) {
            user.setAvatarImageUrl(storeAvatar(avatarFile));
        }

        return userRepository.save(user);
    }

    public byte[] readAvatar(String filename) {
        Path filePath = avatarUploadDir.resolve(filename).toAbsolutePath().normalize();
        if (!filePath.startsWith(avatarUploadDir) || !Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "头像不存在");
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取头像失败");
        }
    }

    private String storeAvatar(MultipartFile avatarFile) {
        String originalFilename = avatarFile.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像仅支持 jpg、jpeg、png、webp");
        }
        try {
            Files.createDirectories(avatarUploadDir);
            String filename = UUID.randomUUID() + extension;
            Files.copy(avatarFile.getInputStream(), avatarUploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/api/auth/avatar/" + filename;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "保存头像失败");
        }
    }

    private String normalizeAvatarColor(String avatarColor) {
        if (!StringUtils.hasText(avatarColor)) {
            return null;
        }
        return avatarColor.trim();
    }

    @Transactional
    public AuthResult register(RegisterCommand command) {
        if (userRepository.existsByUsernameIgnoreCase(command.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmailIgnoreCase(command.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已存在");
        }

        Role role = command.role() == Role.FARM_ADMIN ? Role.FARM_ADMIN : Role.CONSUMER;
        var user = userRepository.save(User.builder()
                .username(command.username().trim())
                .passwordHash(passwordEncoder.encode(command.password()))
                .fullName(command.fullName().trim())
                .email(command.email().trim().toLowerCase())
                .phone(command.phone())
                .address(command.address())
                .avatarColor(command.avatarColor())
                .role(role)
                .build());
        return new AuthResult(jwtService.generateToken(user), user);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该邮箱对应账号"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public record RegisterCommand(
            String username,
            String password,
            String fullName,
            String email,
            String phone,
            String address,
            String avatarColor,
            Role role
    ) {
    }

    public record ProfileCommand(
            String fullName,
            String email,
            String phone,
            String address,
            String avatarColor
    ) {
    }

    public record AuthResult(String token, User user) {
    }
}
