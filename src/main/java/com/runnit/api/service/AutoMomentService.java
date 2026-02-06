// ========== AutoMomentService.java ==========
package com.runnit.service;

import com.runnit.entity.Activity;
import com.runnit.entity.Moment;
import com.runnit.entity.User;
import com.runnit.repository.MomentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoMomentService {

    private final MomentRepository momentRepository;
    private final RouteSnapshotService routeSnapshotService;
    private final SpotifyService spotifyService;
    private final PlaceholderImageService placeholderImageService;

    // Default workout songs for auto-generation
    private static final List<SongSuggestion> DEFAULT_SONGS = List.of(
        new SongSuggestion("Eye of the Tiger", "Survivor", "https://open.spotify.com/track/2KH16WveTQWT6KOG9Rg6e2"),
        new SongSuggestion("Lose Yourself", "Eminem", "https://open.spotify.com/track/5Z01UMMf7V1o0MzF86s6WJ"),
        new SongSuggestion("Stronger", "Kanye West", "https://open.spotify.com/track/4fzsfWzRhPawzqhX8Qt9F3"),
        new SongSuggestion("Till I Collapse", "Eminem", "https://open.spotify.com/track/4xkOaSrkexMciUUogZKVTS"),
        new SongSuggestion("Can't Hold Us", "Macklemore", "https://open.spotify.com/track/3XVBdLihbNbxUwZosxcGuJ"),
        new SongSuggestion("Remember the Name", "Fort Minor", "https://open.spotify.com/track/1lgN0A2Vki2FTON5PYq42P"),
        new SongSuggestion("POWER", "Kanye West", "https://open.spotify.com/track/2mxHmbKHbKnKdT8iRzo0AW"),
        new SongSuggestion("Thunderstruck", "AC/DC", "https://open.spotify.com/track/57bgtoPSgt236HzfBOd8kj")
    );

    @Transactional
    public Moment autoGenerateMomentFromActivity(Activity activity) {
        log.info("Auto-generating moment for activity: {}", activity.getId());
        
        User user = activity.getUser();
        
        // Check if user has auto-moment generation enabled (optional feature flag)
        if (!shouldAutoGenerateMoment(user)) {
            log.info("Auto-moment generation disabled for user: {}", user.getId());
            return null;
        }
        
        Moment moment = new Moment();
        moment.setUser(user);
        moment.setActivity(activity);
        
        // Generate or use placeholder photo
        String photoUrl = generateActivityPhoto(activity);
        moment.setPhotoUrl(photoUrl);
        
        // Generate route snapshot if GPS data available
        if (activity.getRoutePolyline() != null && !activity.getRoutePolyline().isEmpty()) {
            String routeSnapshot = routeSnapshotService.generateRouteSnapshot(activity.getRoutePolyline());
            moment.setRouteSnapshotUrl(routeSnapshot);
        }
        
        // Get song - try Spotify first, fallback to default
        SongSuggestion song = getSongForActivity(activity, user);
        moment.setSongTitle(song.getTitle());
        moment.setSongArtist(song.getArtist());
        moment.setSongLink(song.getLink());
        
        Moment savedMoment = momentRepository.save(moment);
        log.info("Auto-generated moment {} for activity {}", savedMoment.getId(), activity.getId());
        
        return savedMoment;
    }
    
    private boolean shouldAutoGenerateMoment(User user) {
        // TODO: Add user preference field for auto-moment generation
        // For now, default to true for all users
        return true;
    }
    
    private String generateActivityPhoto(Activity activity) {
        // If user uploaded a photo during activity, use that
        // Otherwise, generate a placeholder based on sport type
        return placeholderImageService.generateActivityPlaceholder(activity);
    }
    
    private SongSuggestion getSongForActivity(Activity activity, User user) {
        // Try to get user's recently played Spotify track during workout time
        try {
            if (user.getSpotifyAccessToken() != null) {
                SongSuggestion spotifySong = spotifyService.getRecentTrackDuringActivity(user, activity);
                if (spotifySong != null) {
                    return spotifySong;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Spotify track for user {}: {}", user.getId(), e.getMessage());
        }
        
        // Fallback to random motivational song
        Random random = new Random();
        return DEFAULT_SONGS.get(random.nextInt(DEFAULT_SONGS.size()));
    }
    
    public record SongSuggestion(String title, String artist, String link) {}
}