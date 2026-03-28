package com.zhouri.farmshop.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhouri.farmshop.domain.Review;
import com.zhouri.farmshop.domain.Role;
import com.zhouri.farmshop.domain.User;
import com.zhouri.farmshop.security.AuthenticatedUser;
import com.zhouri.farmshop.service.CatalogService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StorefrontControllerReviewSecurityTest {

    @Mock
    private CatalogService catalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StorefrontController(catalogService)).build();
    }

    @Test
    void sameAuthenticatedPrincipalCanBrowseAndSubmitReview() throws Exception {
        var principal = new AuthenticatedUser(1L, "consumer", "普通消费者", Role.CONSUMER);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null);
        when(catalogService.addReview(eq(1L), eq(1L), eq(11L), eq(5), eq("很好"))).thenReturn(Review.builder()
                .id(9L)
                .user(User.builder().id(1L).fullName("普通消费者").role(Role.CONSUMER).build())
                .rating(5)
                .content("很好")
                .createdAt(LocalDateTime.of(2026, 3, 28, 9, 0))
                .build());

        mockMvc.perform(post("/api/catalog/products/1/browse").principal(authentication))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/catalog/products/1/reviews")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"orderItemId":11,"rating":5,"content":"很好"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.content").value("很好"));
    }
}
