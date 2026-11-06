package com.neumatica.embudo.whatsap.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neumatica.embudo.whatsap.dto.push.PushSubscriptionRequest;
import com.neumatica.embudo.whatsap.entitys.PushSubscription;
import com.neumatica.embudo.whatsap.repository.PushSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;

    @Transactional
    public PushSubscription registerSubscription(
            UUID userId,
            PushSubscriptionRequest request) {

        PushSubscription subscription = repository
                .findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);

        subscription.setUserId(userId);
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getP256dh());
        subscription.setAuth(request.getAuth());

        if (subscription.getId() == null) {
            subscription.setCreatedAt(LocalDateTime.now());
        }

        subscription.setUpdatedAt(LocalDateTime.now());

        return repository.save(subscription);
    }
    
    public PushSubscription getFirstSubscription(UUID userId) {

        return repository.findFirstByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "El vendedor no tiene suscripciones Push"
                        )
                );
    }
    
    public List<PushSubscription> getfindAll() {

        return repository.findAll();
    }
}

