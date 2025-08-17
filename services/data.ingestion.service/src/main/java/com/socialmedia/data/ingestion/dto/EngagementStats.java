package com.socialmedia.data.ingestion.dto;

public class EngagementStats {
    
    // Overall engagement statistics
    private Double averageEngagementScore;
    private Double medianEngagementScore;
    private Double maxEngagementScore;
    private Double minEngagementScore;
    private Double standardDeviation;
    
    // Like/upvote statistics
    private Long totalLikes;
    private Long totalUpvotes;
    private Long totalDownvotes;
    private Double averageLikes;
    private Double averageUpvotes;
    
    // Comment and share statistics
    private Long totalComments;
    private Long totalShares;
    private Double averageComments;
    private Double averageShares;
    
    // View statistics (YouTube specific)
    private Long totalViews;
    private Double averageViews;
    
    // Engagement rate calculations
    private Double likesToViewsRatio;
    private Double commentsToLikesRatio;
    private Double sharesToLikesRatio;
    
    // Distribution percentiles
    private Double engagementScore25thPercentile;
    private Double engagementScore75thPercentile;
    private Double engagementScore90thPercentile;
    
    // Constructors
    public EngagementStats() {}
    
    public EngagementStats(Double averageEngagementScore, Long totalLikes, Long totalComments) {
        this.averageEngagementScore = averageEngagementScore;
        this.totalLikes = totalLikes;
        this.totalComments = totalComments;
    }
    
    // Helper methods for calculations
    public Double calculateEngagementRate() {
        if (totalViews != null && totalViews > 0 && totalLikes != null) {
            return (totalLikes.doubleValue() / totalViews.doubleValue()) * 100;
        }
        return 0.0;
    }
    
    public Double calculateInteractionRate() {
        if (totalLikes != null && totalComments != null && totalShares != null) {
            return (double)(totalLikes + totalComments + totalShares);
        }
        return 0.0;
    }
    
    // Getters and Setters
    public Double getAverageEngagementScore() { return averageEngagementScore; }
    public void setAverageEngagementScore(Double averageEngagementScore) { this.averageEngagementScore = averageEngagementScore; }
    
    public Double getMedianEngagementScore() { return medianEngagementScore; }
    public void setMedianEngagementScore(Double medianEngagementScore) { this.medianEngagementScore = medianEngagementScore; }
    
    public Double getMaxEngagementScore() { return maxEngagementScore; }
    public void setMaxEngagementScore(Double maxEngagementScore) { this.maxEngagementScore = maxEngagementScore; }
    
    public Double getMinEngagementScore() { return minEngagementScore; }
    public void setMinEngagementScore(Double minEngagementScore) { this.minEngagementScore = minEngagementScore; }
    
    public Double getStandardDeviation() { return standardDeviation; }
    public void setStandardDeviation(Double standardDeviation) { this.standardDeviation = standardDeviation; }
    
    public Long getTotalLikes() { return totalLikes; }
    public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }
    
    public Long getTotalUpvotes() { return totalUpvotes; }
    public void setTotalUpvotes(Long totalUpvotes) { this.totalUpvotes = totalUpvotes; }
    
    public Long getTotalDownvotes() { return totalDownvotes; }
    public void setTotalDownvotes(Long totalDownvotes) { this.totalDownvotes = totalDownvotes; }
    
    public Double getAverageLikes() { return averageLikes; }
    public void setAverageLikes(Double averageLikes) { this.averageLikes = averageLikes; }
    
    public Double getAverageUpvotes() { return averageUpvotes; }
    public void setAverageUpvotes(Double averageUpvotes) { this.averageUpvotes = averageUpvotes; }
    
    public Long getTotalComments() { return totalComments; }
    public void setTotalComments(Long totalComments) { this.totalComments = totalComments; }
    
    public Long getTotalShares() { return totalShares; }
    public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
    
    public Double getAverageComments() { return averageComments; }
    public void setAverageComments(Double averageComments) { this.averageComments = averageComments; }
    
    public Double getAverageShares() { return averageShares; }
    public void setAverageShares(Double averageShares) { this.averageShares = averageShares; }
    
    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }
    
    public Double getAverageViews() { return averageViews; }
    public void setAverageViews(Double averageViews) { this.averageViews = averageViews; }
    
    public Double getLikesToViewsRatio() { return likesToViewsRatio; }
    public void setLikesToViewsRatio(Double likesToViewsRatio) { this.likesToViewsRatio = likesToViewsRatio; }
    
    public Double getCommentsToLikesRatio() { return commentsToLikesRatio; }
    public void setCommentsToLikesRatio(Double commentsToLikesRatio) { this.commentsToLikesRatio = commentsToLikesRatio; }
    
    public Double getSharesToLikesRatio() { return sharesToLikesRatio; }
    public void setSharesToLikesRatio(Double sharesToLikesRatio) { this.sharesToLikesRatio = sharesToLikesRatio; }
    
    public Double getEngagementScore25thPercentile() { return engagementScore25thPercentile; }
    public void setEngagementScore25thPercentile(Double engagementScore25thPercentile) { this.engagementScore25thPercentile = engagementScore25thPercentile; }
    
    public Double getEngagementScore75thPercentile() { return engagementScore75thPercentile; }
    public void setEngagementScore75thPercentile(Double engagementScore75thPercentile) { this.engagementScore75thPercentile = engagementScore75thPercentile; }
    
    public Double getEngagementScore90thPercentile() { return engagementScore90thPercentile; }
    public void setEngagementScore90thPercentile(Double engagementScore90thPercentile) { this.engagementScore90thPercentile = engagementScore90thPercentile; }
}
    