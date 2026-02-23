package com.keza.admin.domain.port.out;

import com.keza.admin.domain.model.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Page<Announcement> findByPublishedTrueAndExpiresAtIsNullOrPublishedTrueAndExpiresAtAfterOrderByPublishedAtDesc(
            Instant now, Pageable pageable);

    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
