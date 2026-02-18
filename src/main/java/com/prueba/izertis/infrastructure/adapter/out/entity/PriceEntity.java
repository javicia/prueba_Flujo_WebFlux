package com.prueba.izertis.infrastructure.adapter.out.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("prices")
@Data
public class PriceEntity {

    @Id
    private Long id;

    @Column("brand_id")
    Integer brandId;

    @Column("start_date")
    private LocalDateTime startDate;

    @Column("end_date")
    private  LocalDateTime endDate;

    @Column("price_list")
    private Integer priceList;

    @Column("product_id")
    private Long productId;

    @Column("priority")
    private Integer priority;

    @Column("price")
    private BigDecimal price;

    @Column("currency")
    private String currency;
}
