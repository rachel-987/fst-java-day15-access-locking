package fstr_java.day15.access_locking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
    Tại sao cần version trong request?
    ---- Vì nếu chỉ gửi productName, Service đọc bản ghi mới nhất từ database rồi cập nhật,
    => nó không biết client đang sửa dữ liệu cũ.
    ==> Client phải gửi version mà nó đã đọc trước đó.
 */
public record UpdateInventoryRequest(
        @NotBlank String productName,
        @NotNull Long version
) {
}