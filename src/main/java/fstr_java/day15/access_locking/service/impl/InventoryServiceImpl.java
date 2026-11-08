package fstr_java.day15.access_locking.service.impl;

import fstr_java.day15.access_locking.dto.response.InventoryHistoryResponse;
import fstr_java.day15.access_locking.entity.Inventory;
import fstr_java.day15.access_locking.repository.InventoryRepository;
import fstr_java.day15.access_locking.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.RevisionType;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    @PersistenceContext
    private EntityManager entityManager;

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public Inventory createInventory(String productName, Integer quantity) {
        Inventory inventory = new Inventory();

        inventory.setProductName(productName);
        inventory.setQuantity(quantity);

        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory stockIn(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory not found"
                ));

        // Hibernate Dirty Checking sẽ phát hiện thay đổi & tự flush SQL UPDATE trước/trong quá trình commit.
        // !!!Entity có @Version => Hibernate có thể kiểm tra và tăng version khi stock-in.
        // ** Điều này không mâu thuẫn với Pessimistic Locking
        // cơ chế bảo vệ chính của luồng nhập kho vẫn là row lock
        inventory.setQuantity(
                Math.addExact(inventory.getQuantity(), quantity)
        );

        return inventory;
    }



    @Override
    @Transactional
    public Inventory updateInventory(
            Long productId,
            String productName,
            Long version
    ) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory not found"
                ));

        /*
            Tại sao vừa kiểm tra version thủ công, vừa dùng @Version?
            --- Đây là hai lớp bảo vệ:
                1. Kiểm tra version từ client:
                    -> Phát hiện client đang sửa dữ liệu đã cũ.
                2. Hibernate @Version:
                    -> Phát hiện transaction khác cập nhật bản ghi trong khoảng thời gian từ lúc Service đọc đến lúc ghi database.
         */
        if (!inventory.getVersion().equals(version)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory has been modified by another transaction"
            );
        }

        inventory.setProductName(productName);

        /*
            flush() giúp Hibernate thực thi UPDATE ngay trong method,
            => để lỗi optimistic locking có thể xuất hiện trước khi method trả về.
            Transaction vẫn chỉ commit khi kết thúc.
         */
        inventoryRepository.flush();

        return inventory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryHistoryResponse> getHistory(Long productId) {

        AuditReader reader = AuditReaderFactory.get(entityManager);

        List<Number> revisions =
                reader.getRevisions(Inventory.class, productId);

        return revisions.stream()
                .map(revision -> {
                    Inventory inventory = reader.find(Inventory.class, productId, revision);
                    Instant changedAt = reader.getRevisionDate(revision).toInstant();
                    return new InventoryHistoryResponse(
                            revision,
                            changedAt,
                            "REVISION",
                            inventory.getProductId(),
                            inventory.getProductName(),
                            inventory.getQuantity()
                    );
                })
                .toList();
    }

}