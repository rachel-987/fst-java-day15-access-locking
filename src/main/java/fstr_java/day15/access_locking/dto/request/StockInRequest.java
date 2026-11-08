package fstr_java.day15.access_locking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockInRequest(
        @NotNull
        @Min(1)
        Integer quantity
) {
}