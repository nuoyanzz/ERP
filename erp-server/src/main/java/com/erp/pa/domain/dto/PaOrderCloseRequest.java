package com.erp.pa.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaOrderCloseRequest {

    @Size(max = 255, message = "关闭原因长度不能超过255")
    private String closeReason;
}








