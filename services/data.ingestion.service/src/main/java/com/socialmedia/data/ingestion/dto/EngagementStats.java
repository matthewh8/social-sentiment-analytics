package com.socialmedia.data.ingestion.dto;

/**
 * Engagement statistics
 */
public class EngagementStats {
    
    // Core engagement statistics
    private Double averageEngagementScore;
    private Double maxEngagementScore;
    private Double minEngagementScore;
    
    // Basic counts
    private Long totalLikes;
    private Long totalUpvotes;
    private Long totalComments;
    private Long totalShares;
    private Long totalViews; // YouTube specific
    
    // Simple averages
    private Double averageLikes;
    private Double averageUpvotes;
    private Double averageComments;
    
    // Constructors
    public EngagementStats() {}
    
    public EngagementStats(Double averageEngagementScore, Long totalLikes, Long totalComments) {
        this.averageEngagementScore = averageEngagementScore;
        this.totalLikes = totalLikes;
        this.totalComments = totalComments;
    }
    
    // Helper methods for basic calculations
    public Double calculateBasicEngagementRate() {
        if (totalViews != null && totalViews > 0 && totalLikes != null) {
            return (totalLikes.doubleValue() / totalViews.doubleValue()) * 100;
        }
        return 0.0;
    }
    
    public Long getTotalInteractions() {
        long interactions = 0;
        if (totalLikes != null) interactions += totalLikes;
        if (totalComments != null) interactions += totalComments;
        if (totalShares != null) interactions += totalShares;
        return interactions;
    }
    
    // Getters and Setters
    public Double getAverageEngagementScore() { return averageEngagementScore; }
    public void setAverageEngagementScore(Double averageEngagementScore) { this.averageEngagementScore = averageEngagementScore; }
    
    public Double getMaxEngagementScore() { return maxEngagementScore; }
    public void setMaxEngagementScore(Double maxEngagementScore) { this.maxEngagementScore = maxEngagementScore; }
    
    public Double getMinEngagementScore() { return minEngagementScore; }
    public void setMinEngagementScore(Double minEngagementScore) { this.minEngagementScore = minEngagementScore; }
    
    public Long getTotalLikes() { return totalLikes; }
    public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }
    
    public Long getTotalUpvotes() { return totalUpvotes; }
    public void setTotalUpvotes(Long totalUpvotes) { this.totalUpvotes = totalUpvotes; }
    
    public Long getTotalComments() { return totalComments; }
    public void setTotalComments(Long totalComments) { this.totalComments = totalComments; }
    
    public Long getTotalShares() { return totalShares; }
    public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
    
    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }
    
    public Double getAverageLikes() { return averageLikes; }
    public void setAverageLikes(Double averageLikes) { this.averageLikes = averageLikes; }
    
    public Double getAverageUpvotes() { return averageUpvotes; }
    public void setAverageUpvotes(Double averageUpvotes) { this.averageUpvotes = averageUpvotes; }
    
    public Double getAverageComments() { return averageComments; }
    public void setAverageComments(Double averageComments) { this.averageComments = averageComments; }
}