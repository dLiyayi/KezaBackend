package com.keza.user.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Pattern(regexp = "^\\+254[17]\\d{8}$", message = "Phone must be a valid Kenyan number")
    private String phone;

    private String bio;
    private LocalDate dateOfBirth;
    private String nationalId;
    private String kraPin;
    private BigDecimal annualIncome;

    @Size(max = 20)
    private String gender;

    @Size(max = 100)
    private String countryOfResidence;

    @Size(max = 100)
    private String citizenship;

    private BigDecimal netWorth;
}
