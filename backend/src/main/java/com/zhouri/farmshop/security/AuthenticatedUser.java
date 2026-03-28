package com.zhouri.farmshop.security;

import com.zhouri.farmshop.domain.Role;

public record AuthenticatedUser(Long id, String username, String fullName, Role role) {
}
