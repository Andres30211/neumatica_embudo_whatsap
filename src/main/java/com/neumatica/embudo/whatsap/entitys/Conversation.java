package com.neumatica.embudo.whatsap.entitys;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "conversation")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Conversation {

	@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;
	
	@OneToMany(mappedBy = "conversation",
	           cascade = CascadeType.ALL,
	           fetch = FetchType.LAZY)
	private List<Message> messages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ConversationStatus status;

    /**
     * BOT
     * HUMAN
     */
    private String assignedTo;

    private LocalDateTime startedAt;

    private LocalDateTime lastMessageAt;

    private LocalDateTime closedAt;

}
