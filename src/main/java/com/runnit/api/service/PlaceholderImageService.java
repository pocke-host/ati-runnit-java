// ========== PlaceholderImageService.java ==========
package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.Activity.SportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceholderImageService {

    private final S3Service s3Service;
    
    private static final int IMAGE_WIDTH = 800;
    private static final int IMAGE_HEIGHT = 1000;

    /**
     * Generate a placeholder image for an activity when no photo is provided
     */
    public String generateActivityPlaceholder(Activity activity) {
        log.info("Generating placeholder image for activity: {}", activity.getId());
        
        try {
            BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Set background gradient based on sport type
            Color startColor = getSportColorStart(activity.getSportType());
            Color endColor = getSportColorEnd(activity.getSportType());
            
            GradientPaint gradient = new GradientPaint(
                0, 0, startColor,
                0, IMAGE_HEIGHT, endColor
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            
            // Draw sport emoji/icon
            String emoji = getSportEmoji(activity.getSportType());
            Font emojiFont = new Font("Apple Color Emoji", Font.PLAIN, 200);
            g2d.setFont(emojiFont);
            g2d.setColor(new Color(255, 255, 255, 180));
            
            FontMetrics fm = g2d.getFontMetrics();
            int emojiX = (IMAGE_WIDTH - fm.stringWidth(emoji)) / 2;
            int emojiY = (IMAGE_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(emoji, emojiX, emojiY - 100);
            
            // Draw activity stats
            g2d.setFont(new Font("Futura", Font.BOLD, 48));
            g2d.setColor(Color.WHITE);
            
            String distance = activity.getDistanceMeters() != null 
                ? String.format("%.2f km", activity.getDistanceMeters() / 1000.0)
                : "Activity";
            String duration = formatDuration(activity.getDurationSeconds());
            
            drawCenteredString(g2d, distance, IMAGE_WIDTH, emojiY + 100);
            
            g2d.setFont(new Font("Futura", Font.PLAIN, 36));
            drawCenteredString(g2d, duration, IMAGE_WIDTH, emojiY + 160);
            
            // Draw RUNNIT watermark
            g2d.setFont(new Font("Futura", Font.BOLD, 24));
            g2d.setColor(new Color(255, 255, 255, 120));
            drawCenteredString(g2d, "RUNNIT", IMAGE_WIDTH, IMAGE_HEIGHT - 50);
            
            g2d.dispose();
            
            // Convert to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Upload to S3
            String filename = "activity-placeholders/" + UUID.randomUUID() + ".png";
            return s3Service.uploadFile(imageBytes, filename, "image/png");
            
        } catch (IOException e) {
            log.error("Failed to generate placeholder image", e);
            return null;
        }
    }
    
    private Color getSportColorStart(SportType sportType) {
        return switch (sportType) {
            case RUN -> new Color(196, 106, 42); // Orange
            case BIKE -> new Color(90, 107, 78);  // Olive
            case SWIM -> new Color(74, 158, 204); // Blue
            case HIKE -> new Color(139, 115, 85); // Brown
            case WALK -> new Color(163, 166, 159); // Gray
            default -> new Color(107, 91, 149);   // Purple
        };
    }
    
    private Color getSportColorEnd(SportType sportType) {
        Color start = getSportColorStart(sportType);
        return new Color(
            Math.max(0, start.getRed() - 40),
            Math.max(0, start.getGreen() - 40),
            Math.max(0, start.getBlue() - 40)
        );
    }
    
    private String getSportEmoji(SportType sportType) {
        return switch (sportType) {
            case RUN -> "🏃";
            case BIKE -> "🚴";
            case SWIM -> "🏊";
            case HIKE -> "🥾";
            case WALK -> "🚶";
            default -> "🏋️";
        };
    }
    
    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }
    
    private void drawCenteredString(Graphics2D g2d, String text, int width, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }
}