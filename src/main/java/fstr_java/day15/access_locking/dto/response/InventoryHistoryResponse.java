package fstr_java.day15.access_locking.dto.response;

import java.time.Instant;

// dùng record vì đây là DTO chỉ chứa dữ liệu, không có business logic
public record InventoryHistoryResponse(
        Number revision,
        Instant changedAt,
        String action,
        Long productId,
        String productName,
        Integer quantity
) {
}