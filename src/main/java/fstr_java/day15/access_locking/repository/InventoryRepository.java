package fstr_java.day15.access_locking.repository;

import fstr_java.day15.access_locking.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    //Khi gọi phương thức này trong transaction
    // => Hibernate sẽ yêu cầu PostgreSQL khóa bản ghi (thường bằng SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.productId = :productId
            """)
    Optional<Inventory> findByProductIdForUpdate(
            @Param("productId") Long productId
    );
}