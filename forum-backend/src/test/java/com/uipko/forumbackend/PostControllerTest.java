package com.uipko.forumbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipko.forumbackend.domain.dto.PostCreateDto;
import com.uipko.forumbackend.domain.dto.UserLoginDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=mySecretKeyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"
})
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;
    private Long postId;

    @BeforeEach
    public void setup() throws Exception {
        // Register a regular user
        UserLoginDto userDto = new UserLoginDto("testuser_" + System.currentTimeMillis(), "password123");

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk());

        // Login as the regular user
        MvcResult userLoginResult = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andReturn();

        userToken = userLoginResult.getResponse().getContentAsString();

        // Create a post as the regular user
        PostCreateDto postDto = new PostCreateDto("Test Post", "This is a test post content");

        MvcResult postResult = mockMvc.perform(post("/posts")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> postResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(), 
                Map.class
        );
        postId = ((Number) postResponse.get("id")).longValue();

        // Login as admin
        UserLoginDto adminDto = new UserLoginDto("admin", "password123");

        MvcResult adminLoginResult = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminDto)))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = adminLoginResult.getResponse().getContentAsString();
    }

    @Test
    public void testAdminCanDeleteOtherUsersPosts() throws Exception {
        // Admin should be able to delete the post created by another user
        mockMvc.perform(delete("/posts/" + postId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify the post is soft deleted (admin can still see it)
        mockMvc.perform(get("/posts/" + postId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testUserCannotDeleteOtherUsersPosts() throws Exception {
        // Create another user
        UserLoginDto otherUserDto = new UserLoginDto("otheruser_" + System.currentTimeMillis(), "password123");

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherUserDto)))
                .andExpect(status().isOk());

        // Login as the other user
        MvcResult otherUserLoginResult = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherUserDto)))
                .andExpect(status().isOk())
                .andReturn();

        String otherUserToken = otherUserLoginResult.getResponse().getContentAsString();

        // Other user should not be able to delete the post
        mockMvc.perform(delete("/posts/" + postId)
                .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUserCanDeleteOwnPost() throws Exception {
        // User should be able to delete their own post
        mockMvc.perform(delete("/posts/" + postId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }
}