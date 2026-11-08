package fstr_java.day15.access_locking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "inventories")
@Audited    //Hibernate Envers: yêu cầu Envers lưu lại lịch sử thay đổi của Entity.
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Version
    @Column(name = "version")   //Optimistic Locking
    private Long version;
}