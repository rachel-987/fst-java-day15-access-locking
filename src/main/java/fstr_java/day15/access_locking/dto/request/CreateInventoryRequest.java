package fstr_java.day15.access_locking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(
        @NotBlank String productName,
        @NotNull @Min(0) Integer quantity
) {
}