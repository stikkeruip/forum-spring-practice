package com.uipko.forumbackend.domain.dto;

import java.util.List;

public class OnlineUserDto {
    private String username;
    private String createdDate;
    private List<ProfilePostDto> posts;
    private List<ProfilePostDto> deletedPosts;

    public OnlineUserDto() {}

    public OnlineUserDto(String username, String createdDate, List<ProfilePostDto> posts, List<ProfilePostDto> deletedPosts) {
        this.username = username;
        this.createdDate = createdDate;
        this.posts = posts;
        this.deletedPosts = deletedPosts;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public List<ProfilePostDto> getPosts() {
        return posts;
    }

    public void setPosts(List<ProfilePostDto> posts) {
        this.posts = posts;
    }

    public List<ProfilePostDto> getDeletedPosts() {
        return deletedPosts;
    }

    public void setDeletedPosts(List<ProfilePostDto> deletedPosts) {
        this.deletedPosts = deletedPosts;
    }
}
