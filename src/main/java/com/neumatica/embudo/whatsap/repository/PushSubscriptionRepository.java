package com.neumatica.embudo.whatsap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neumatica.embudo.whatsap.entitys.PushSubscription;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    List<PushSubscription> findAll();

    //List<PushSubscription> findAllByUserIdIn(List<UUID> userIds);
    
    Optional<PushSubscription> findByEndpoint(String endpoint);
    
    Optional<PushSubscription> findFirstByUserId(UUID userId);

}

