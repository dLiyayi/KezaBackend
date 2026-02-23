package com.keza.admin.domain.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface AdminKycRepository {

    Page<Map<String, Object>> findKycDocuments(String status, String documentType, String search, Pageable pageable);
}
