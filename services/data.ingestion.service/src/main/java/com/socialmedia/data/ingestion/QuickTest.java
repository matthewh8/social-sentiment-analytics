// Create: test/java/com/socialmedia/data/ingestion/QuickTest.java

package com.socialmedia.data.ingestion;

import com.socialmedia.data.ingestion.dto.SocialPostDto;
import com.socialmedia.data.ingestion.model.Platform;
import java.time.LocalDateTime;

public class QuickTest {
    
    public static void main(String[] args) {
        System.out.println("🚀 Quick Hour 4 Test...");
        
        try {
            // Test DTO creation
            SocialPostDto post = SocialPostDto.builder()
                    .externalId("quick_test_123")
                    .platform(Platform.REDDIT)
                    .content("Quick test of Hour 4 components")
                    .author("tester")
                    .createdAt(LocalDateTime.now())
                    .upvotes(42L)
                    .likeCount(15L)
                    .build();
            
            System.out.println("✅ SocialPostDto created successfully!");
            System.out.println("   Post ID: " + post.getExternalId());
            System.out.println("   Platform: " + post.getPlatform());
            System.out.println("   Upvotes: " + post.getUpvotes());
            System.out.println("   Likes: " + post.getLikeCount());
            
            System.out.println("🎉 Hour 4 DTOs are working!");
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}