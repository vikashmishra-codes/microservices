package com.vikash.InventoryService.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class InventoryResponse {
    private String skuCode;
    private boolean isInStock;
}