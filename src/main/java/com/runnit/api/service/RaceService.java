package com.runnit.api.service;

import com.runnit.api.dto.RaceRequest;
import com.runnit.api.dto.RaceResponse;
import com.runnit.api.model.Race;
import com.runnit.api.model.RaceInterest;
import com.runnit.api.model.User;
import com.runnit.api.repository.RaceInterestRepository;
import com.runnit.api.repository.RaceRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RaceService {

    private final RaceRepository raceRepository;
    private final RaceInterestRepository interestRepository;
    private final UserRepository userRepository;

    @Transactional
    public RaceResponse createRace(Long userId, RaceRequest request) {
        User organizer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Race race = Race.builder()
                .name(request.getName())
                .description(request.getDescription())
                .raceType(request.getRaceType())
                .sportType(request.getSportType())
                .raceDate(request.getRaceDate())
                .locationName(request.getLocationName())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry() != null ? request.getCountry() : "US")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .distanceMeters(request.getDistanceMeters())
                .registrationUrl(request.getRegistrationUrl())
                .organizerName(request.getOrganizerName())
                .organizerUser(organizer)
                .coverImageUrl(request.getCoverImageUrl())
                .priceCents(request.getPriceCents())
                .isFeatured(false)
                .build();

        race = raceRepository.save(race);
        return toRaceResponse(race, userId);
    }

    public Page<RaceResponse> getUpcomingRaces(Long currentUserId, String city, int page, int size) {
        LocalDate today = LocalDate.now();
        Page<Race> races;
        if (city != null && !city.isBlank()) {
            races = raceRepository.findByDateAndCity(today, city, PageRequest.of(page, size));
        } else {
            races = raceRepository.findByRaceDateGreaterThanEqualOrderByRaceDateAsc(today, PageRequest.of(page, size));
        }
        return races.map(r -> toRaceResponse(r, currentUserId));
    }

    public Page<RaceResponse> getFeaturedRaces(Long currentUserId, int page, int size) {
        return raceRepository.findByIsFeaturedTrueAndRaceDateGreaterThanEqualOrderByRaceDateAsc(
                LocalDate.now(), PageRequest.of(page, size))
                .map(r -> toRaceResponse(r, currentUserId));
    }

    public RaceResponse getRaceById(Long raceId, Long currentUserId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new RuntimeException("Race not found"));
        return toRaceResponse(race, currentUserId);
    }

    public List<RaceResponse> getMyRaces(Long userId) {
        return interestRepository.findByUserId(userId).stream()
                .map(ri -> toRaceResponse(ri.getRace(), userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public RaceResponse markInterest(Long raceId, Long userId, String status) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new RuntimeException("Race not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RaceInterest interest = interestRepository.findByRaceIdAndUserId(raceId, userId)
                .orElse(RaceInterest.builder().race(race).user(user).build());
        interest.setStatus(status);
        interestRepository.save(interest);
        return toRaceResponse(race, userId);
    }

    @Transactional
    public void removeInterest(Long raceId, Long userId) {
        interestRepository.findByRaceIdAndUserId(raceId, userId)
                .ifPresent(interestRepository::delete);
    }

    private RaceResponse toRaceResponse(Race race, Long currentUserId) {
        long interestedCount = interestRepository.countByRaceId(race.getId());
        String currentUserStatus = interestRepository.findByRaceIdAndUserId(race.getId(), currentUserId)
                .map(RaceInterest::getStatus).orElse(null);

        return RaceResponse.builder()
                .id(race.getId())
                .name(race.getName())
                .description(race.getDescription())
                .raceType(race.getRaceType())
                .sportType(race.getSportType())
                .raceDate(race.getRaceDate())
                .locationName(race.getLocationName())
                .city(race.getCity())
                .state(race.getState())
                .country(race.getCountry())
                .latitude(race.getLatitude())
                .longitude(race.getLongitude())
                .distanceMeters(race.getDistanceMeters())
                .registrationUrl(race.getRegistrationUrl())
                .organizerName(race.getOrganizerName())
                .organizerUserId(race.getOrganizerUser() != null ? race.getOrganizerUser().getId() : null)
                .coverImageUrl(race.getCoverImageUrl())
                .priceCents(race.getPriceCents())
                .isFeatured(race.isFeatured())
                .interestedCount(interestedCount)
                .currentUserStatus(currentUserStatus)
                .createdAt(race.getCreatedAt())
                .build();
    }
}
