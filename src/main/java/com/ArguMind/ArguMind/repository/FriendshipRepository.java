package com.ArguMind.ArguMind.repository;

import com.ArguMind.ArguMind.model.Friendship;
import com.ArguMind.ArguMind.model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    @Query("""
            SELECT f FROM Friendship f
            WHERE f.status = :status
              AND (f.requester.id = :userId OR f.addressee.id = :userId)
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    @Query("""
            SELECT f FROM Friendship f
            WHERE f.status = 'PENDING' AND f.addressee.id = :userId
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findPendingIncoming(@Param("userId") Long userId);

    @Query("""
            SELECT f FROM Friendship f
            WHERE ((f.requester.id = :a AND f.addressee.id = :b) OR (f.requester.id = :b AND f.addressee.id = :a))
            """)
    Optional<Friendship> findBetweenUsers(@Param("a") Long userIdA, @Param("b") Long userIdB);
}
