package com.neumatica.embudo.whatsap.entitys;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "conversation")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Conversation {

	@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

	@JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;
	
	@OneToMany(mappedBy = "conversation",
	           cascade = CascadeType.ALL,
	           fetch = FetchType.LAZY,
	           orphanRemoval = true)
	private List<Message> messages;

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

    public void addMessage(Message message){
        messages.add(message);
        message.setConversation(this);
    }
}
