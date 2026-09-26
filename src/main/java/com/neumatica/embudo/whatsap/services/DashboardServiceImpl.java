package com.neumatica.embudo.whatsap.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.neumatica.embudo.whatsap.dto.dashborad.ContactQualityDto;
import com.neumatica.embudo.whatsap.dto.dashborad.ContactSummaryDto;
import com.neumatica.embudo.whatsap.dto.dashborad.ConversationSummaryDto;
import com.neumatica.embudo.whatsap.dto.dashborad.DailyContactActivityDto;
import com.neumatica.embudo.whatsap.dto.dashborad.DailyMessageActivityDto;
import com.neumatica.embudo.whatsap.dto.dashborad.DashboardSummaryDto;
import com.neumatica.embudo.whatsap.dto.dashborad.MessageSummaryDto;
import com.neumatica.embudo.whatsap.dto.dashborad.MetricValueDto;
import com.neumatica.embudo.whatsap.enums.ConversationStatus;
import com.neumatica.embudo.whatsap.enums.Direction;
import com.neumatica.embudo.whatsap.interfaces.DailyContactActivityProjection;
import com.neumatica.embudo.whatsap.interfaces.DailyMessageActivityProjection;
import com.neumatica.embudo.whatsap.repository.ContactRepository;
import com.neumatica.embudo.whatsap.repository.ConversationRepository;
import com.neumatica.embudo.whatsap.repository.DashboardServiceRepository;
import com.neumatica.embudo.whatsap.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardServiceRepository {

    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public DashboardSummaryDto getSummary(
            LocalDate from,
            LocalDate to
    ) {

        LocalDateTime fromDateTime =
                from.atStartOfDay();

        LocalDateTime toExclusive =
                to.plusDays(1).atStartOfDay();


        // =====================================================
        // CONTACTS
        // =====================================================

        long totalContacts =
                contactRepository.count();

        long newContacts =
                contactRepository
                        .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                fromDateTime,
                                toExclusive
                        );

        long activeContacts =
                contactRepository
                        .countByLastInteractionGreaterThanEqualAndLastInteractionLessThan(
                                fromDateTime,
                                toExclusive
                        );

        double activePercentage =
                calculatePercentage(
                        activeContacts,
                        totalContacts
                );
        
        long contactsWithPhone =
                contactRepository.countContactsWithPhone();

        long contactsWithName =
                contactRepository.countContactsWithName();

        long contactsWithEmail =
                contactRepository.countContactsWithEmail();

        long contactsWithCompany =
                contactRepository.countContactsWithCompany();

        ContactSummaryDto contactSummary =
                ContactSummaryDto.builder()
                        .totalContacts(totalContacts)
                        .newContacts(newContacts)
                        .activeContacts(activeContacts)
                        .activePercentage(activePercentage)
                        .build();


        // =====================================================
        // MESSAGES
        // =====================================================

        long totalMessages =
                messageRepository
                        .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                fromDateTime,
                                toExclusive
                        );

        long receivedMessages =
                messageRepository
                        .countByDirectionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                Direction.INCOMING,
                                fromDateTime,
                                toExclusive
                        );

        long sentMessages =
                messageRepository
                        .countByDirectionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                Direction.OUTGOING,
                                fromDateTime,
                                toExclusive
                        );

        long manualSentMessages =
                messageRepository
                        .countByDirectionAndSenderUserIdIsNotNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                Direction.INCOMING,
                                fromDateTime,
                                toExclusive
                        );

        long uniqueContactsWhoWrote =
                messageRepository
                        .countDistinctContactsByDirectionAndPeriod(
                                Direction.INCOMING,
                                fromDateTime,
                                toExclusive
                        );

        MessageSummaryDto messageSummary =
                MessageSummaryDto.builder()
                        .totalMessages(totalMessages)
                        .receivedMessages(receivedMessages)
                        .sentMessages(sentMessages)
                        .manualSentMessages(manualSentMessages)
                        .uniqueContactsWhoWrote(uniqueContactsWhoWrote)
                        .build();
        
        
        // =====================================================
        // MessageActivity
        // =====================================================
        List<DailyMessageActivityProjection> rows =
                messageRepository.findDailyMessageActivity(
                        fromDateTime,
                        toExclusive
                );
        
        Map<LocalDate, DailyMessageActivityDto> activityMap =
                new LinkedHashMap<>();
        
        LocalDate currentDate = from;

        while (!currentDate.isAfter(to)) {

            activityMap.put(
                    currentDate,
                    DailyMessageActivityDto.builder()
                            .date(currentDate)
                            .received(0)
                            .sent(0)
                            .total(0)
                            .build()
            );

            currentDate = currentDate.plusDays(1);
        }
        
        for (DailyMessageActivityProjection row : rows) {

            DailyMessageActivityDto activity =
                    activityMap.get(row.getDate());

            if (activity == null) {
                continue;
            }

            long count =
                    row.getTotal() != null
                            ? row.getTotal()
                            : 0L;

            Direction direction = row.getDirection();

            if (direction == Direction.INCOMING) {

                activity.setReceived(count);

            } else if (direction == Direction.OUTGOING) {

                activity.setSent(count);
            }

            activity.setTotal(
                    activity.getReceived()
                            + activity.getSent()
            );
        }
        
        List<DailyMessageActivityDto> messageActivity =
                new ArrayList<>(activityMap.values());
        
        ContactQualityDto contactQuality =
                ContactQualityDto.builder()

                        .phone(
                                buildMetricValue(
                                        contactsWithPhone,
                                        totalContacts
                                )
                        )

                        .name(
                                buildMetricValue(
                                        contactsWithName,
                                        totalContacts
                                )
                        )

                        .email(
                                buildMetricValue(
                                        contactsWithEmail,
                                        totalContacts
                                )
                        )

                        .company(
                                buildMetricValue(
                                        contactsWithCompany,
                                        totalContacts
                                )
                        )

                        .build();
        
     // =====================================================
     // CONVERSATIONS
     // =====================================================

     long totalConversations =
             conversationRepository.count();


     long startedConversations =
             conversationRepository
                     .countByStartedAtGreaterThanEqualAndStartedAtLessThan(
                             fromDateTime,
                             toExclusive
                     );


     long botConversations =
             conversationRepository.countByStatus(
                     ConversationStatus.BOT
             );


     long humanConversations =
             conversationRepository.countByStatus(
                     ConversationStatus.HUMAN
             );


     long closedConversations =
             conversationRepository.countByStatus(
                     ConversationStatus.CLOSED
             );


     ConversationSummaryDto conversationSummary =
             ConversationSummaryDto.builder()
                     .totalConversations(totalConversations)
                     .startedConversations(startedConversations)
                     .botConversations(botConversations)
                     .humanConversations(humanConversations)
                     .closedConversations(closedConversations)
                     .build();
     
  // =========================================================
  // DAILY CONTACT ACTIVITY
  // =========================================================

  List<DailyContactActivityProjection> contactRows =
      contactRepository.findDailyContactActivity(
          fromDateTime,
          toExclusive
      );


  // ---------------------------------------------------------
  // INITIALIZE ALL DAYS WITH ZERO
  // ---------------------------------------------------------

  Map<LocalDate, DailyContactActivityDto> contactActivityMap =
      new LinkedHashMap<>();


  LocalDate contactDate = from;


  while (!contactDate.isAfter(to)) {

      contactActivityMap.put(
          contactDate,

          DailyContactActivityDto.builder()
              .date(contactDate)
              .newContacts(0)
              .build()
      );


      contactDate =
          contactDate.plusDays(1);

  }


  // ---------------------------------------------------------
  // MERGE DATABASE RESULTS
  // ---------------------------------------------------------

  for (
      DailyContactActivityProjection row :
      contactRows
  ) {

      DailyContactActivityDto activity =
          contactActivityMap.get(
              row.getDate()
          );


      if (activity == null) {
          continue;
      }


      long count =
          row.getTotal() != null
              ? row.getTotal()
              : 0L;


      activity.setNewContacts(
          count
      );

  }


  // ---------------------------------------------------------
  // MAP -> LIST
  // ---------------------------------------------------------

  List<DailyContactActivityDto> contactActivity =
      new ArrayList<>(
          contactActivityMap.values()
      );


        // =====================================================
        // DASHBOARD
        // =====================================================

        return DashboardSummaryDto.builder()
                .contacts(contactSummary)
                .messages(messageSummary)
                .messageActivity(messageActivity)
                .contactQuality(contactQuality)
                .conversations(conversationSummary)
                .contactActivity(contactActivity)
                .build();
    }
    
    private MetricValueDto buildMetricValue(
            long count,
            long total
    ) {

        return MetricValueDto.builder()
                .count(count)
                .percentage(
                        calculatePercentage(
                                count,
                                total
                        )
                )
                .build();
    }


    private double calculatePercentage(
            long value,
            long total
    ) {

        if (total == 0) {
            return 0.0;
        }

        return (value * 100.0) / total;
    }
}