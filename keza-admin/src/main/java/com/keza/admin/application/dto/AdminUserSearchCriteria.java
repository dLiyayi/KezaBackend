package com.keza.admin.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSearchCriteria {

    private String kycStatus;
    private String userType;
    private Boolean active;
    private String search;
}
