package com.uipko.forumbackend.domain.dto;

public class ProfilePostDto {
    private Long postId;
    private String owner;
    private String title;
    private String content;
    private Long commentCount;
    private Long likeCount;
    private Long dislikeCount;
    private String userReaction; // "LIKE", "DISLIKE", or null
    private String createdDate;
    private String updatedDate;
    private String deletedDate;
    private String deletedBy;

    public ProfilePostDto() {}

    public ProfilePostDto(Long postId, String owner, String title, String content, 
                         Long commentCount, Long likeCount, Long dislikeCount, 
                         String userReaction, String createdDate, String updatedDate, 
                         String deletedDate, String deletedBy) {
        this.postId = postId;
        this.owner = owner;
        this.title = title;
        this.content = content;
        this.commentCount = commentCount;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.userReaction = userReaction;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.deletedDate = deletedDate;
        this.deletedBy = deletedBy;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Long getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(Long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public String getUserReaction() {
        return userReaction;
    }

    public void setUserReaction(String userReaction) {
        this.userReaction = userReaction;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(String deletedDate) {
        this.deletedDate = deletedDate;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }
}
