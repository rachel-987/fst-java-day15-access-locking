package fstr_java.day15.access_locking.service;

import fstr_java.day15.access_locking.dto.response.InventoryHistoryResponse;
import fstr_java.day15.access_locking.entity.Inventory;

import java.util.List;

public interface InventoryService {

    // Tạo sản phẩm
    Inventory createInventory(String productName, Integer quantity);

    //Nhập thêm hàng vào kho
    Inventory stockIn(Long productId, Integer quantity);

    Inventory updateInventory(Long productId, String productName, Long version);

    List<InventoryHistoryResponse> getHistory(Long productId);
}