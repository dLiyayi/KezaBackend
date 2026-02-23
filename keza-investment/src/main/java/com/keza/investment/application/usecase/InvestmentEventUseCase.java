package com.keza.investment.application.usecase;

import com.keza.investment.application.dto.InvestmentEventResponse;
import com.keza.investment.domain.model.InvestmentEvent;
import com.keza.investment.domain.port.out.InvestmentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentEventUseCase {

    private final InvestmentEventRepository investmentEventRepository;

    @Async
    @Transactional
    public void recordEvent(UUID investmentId, UUID userId, String eventType, String description, String metadata) {
        InvestmentEvent event = InvestmentEvent.builder()
                .investmentId(investmentId)
                .userId(userId)
                .eventType(eventType)
                .description(description)
                .metadata(metadata)
                .build();
        investmentEventRepository.save(event);
        log.debug("Recorded event {} for investment {}", eventType, investmentId);
    }

    @Transactional(readOnly = true)
    public List<InvestmentEventResponse> getInvestmentTimeline(UUID investmentId) {
        return investmentEventRepository.findByInvestmentIdOrderByCreatedAtAsc(investmentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InvestmentEventResponse> getUserActivityFeed(UUID userId, Pageable pageable) {
        return investmentEventRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    private InvestmentEventResponse mapToResponse(InvestmentEvent event) {
        return InvestmentEventResponse.builder()
                .id(event.getId())
                .investmentId(event.getInvestmentId())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .description(event.getDescription())
                .metadata(event.getMetadata())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
