package fstr_java.day15.access_locking.controller;

import fstr_java.day15.access_locking.dto.request.CreateInventoryRequest;
import fstr_java.day15.access_locking.dto.request.StockInRequest;
import fstr_java.day15.access_locking.dto.request.UpdateInventoryRequest;
import fstr_java.day15.access_locking.dto.response.InventoryHistoryResponse;
import fstr_java.day15.access_locking.entity.Inventory;
import fstr_java.day15.access_locking.service.InventoryService;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Inventory createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        return inventoryService.createInventory(
                request.productName(),
                request.quantity()
        );
    }

    @PostMapping("/{productId}/stock-in")
    public Inventory stockIn(@PathVariable Long productId, @Valid @RequestBody StockInRequest request) {
        return inventoryService.stockIn(
                productId,
                request.quantity()
        );
    }

    @PutMapping("/{productId}")
    public Inventory updateInventory(@PathVariable Long productId, @Valid @RequestBody UpdateInventoryRequest request) {
        return inventoryService.updateInventory(
                productId,
                request.productName(),
                request.version()
        );
    }

    @GetMapping("/{productId}/history")
    public List<InventoryHistoryResponse> getHistory(
            @PathVariable Long productId
    ) {
        return inventoryService.getHistory(productId);
    }


    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<String> handleOptimisticLock(
            Exception ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("Inventory has been modified by another transaction");
    }

}