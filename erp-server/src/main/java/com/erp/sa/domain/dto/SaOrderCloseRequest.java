package com.erp.sa.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaOrderCloseRequest {

    @Size(max = 255, message = "关闭原因长度不能超过255")
    private String closeReason;
}








