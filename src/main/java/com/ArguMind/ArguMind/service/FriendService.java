package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.FriendSummaryDto;
import com.ArguMind.ArguMind.model.Friendship;
import com.ArguMind.ArguMind.model.FriendshipStatus;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.FriendshipRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    public enum FriendRelation {
        SELF, NONE, FRIENDS, PENDING_OUT, PENDING_IN
    }

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public java.util.Optional<Friendship> findFriendshipBetween(Long userA, Long userB) {
        return friendshipRepository.findBetweenUsers(userA, userB);
    }

    @Transactional(readOnly = true)
    public FriendRelation getRelation(Long viewerId, Long profileUserId) {
        if (viewerId == null || profileUserId == null) {
            return FriendRelation.NONE;
        }
        if (viewerId.equals(profileUserId)) {
            return FriendRelation.SELF;
        }
        return friendshipRepository.findBetweenUsers(viewerId, profileUserId)
                .map(f -> {
                    if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                        return FriendRelation.FRIENDS;
                    }
                    if (f.getRequester().getId().equals(viewerId)) {
                        return FriendRelation.PENDING_OUT;
                    }
                    return FriendRelation.PENDING_IN;
                })
                .orElse(FriendRelation.NONE);
    }

    @Transactional
    public void sendFriendRequest(Long requesterId, String targetUsername) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizator invalid."));
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Jucătorul nu există."));

        if (requester.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Nu poți adăuga propriul cont.");
        }

        if (friendshipRepository.findBetweenUsers(requester.getId(), target.getId()).isPresent()) {
            throw new IllegalArgumentException("Există deja o relație sau o cerere în așteptare.");
        }

        friendshipRepository.save(Friendship.builder()
                .requester(requester)
                .addressee(target)
                .status(FriendshipStatus.PENDING)
                .build());
    }

    @Transactional
    public void acceptFriendRequest(Long friendshipId, Long userId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Cerere invalidă."));
        if (!f.getAddressee().getId().equals(userId)) {
            throw new IllegalArgumentException("Nu poți accepta această cerere.");
        }
        if (f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Cererea nu mai este activă.");
        }
        f.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(f);
    }

    @Transactional(readOnly = true)
    public List<FriendSummaryDto> listFriends(Long userId) {
        return mapFriends(friendshipRepository.findAllByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED), userId);
    }

    @Transactional(readOnly = true)
    public List<FriendSummaryDto> listPendingIncoming(Long userId) {
        List<FriendSummaryDto> result = new ArrayList<>();
        for (Friendship f : friendshipRepository.findPendingIncoming(userId)) {
            User other = f.getRequester();
            result.add(FriendSummaryDto.builder()
                    .friendshipId(f.getId())
                    .userId(other.getId())
                    .username(other.getUsername())
                    .eloRating(other.getEloRating())
                    .rankTitle(other.getRankTitle())
                    .build());
        }
        return result;
    }

    private List<FriendSummaryDto> mapFriends(List<Friendship> friendships, Long userId) {
        List<FriendSummaryDto> result = new ArrayList<>();
        for (Friendship f : friendships) {
            User other = f.getRequester().getId().equals(userId) ? f.getAddressee() : f.getRequester();
            result.add(FriendSummaryDto.builder()
                    .friendshipId(f.getId())
                    .userId(other.getId())
                    .username(other.getUsername())
                    .eloRating(other.getEloRating())
                    .rankTitle(other.getRankTitle())
                    .build());
        }
        return result;
    }
}
