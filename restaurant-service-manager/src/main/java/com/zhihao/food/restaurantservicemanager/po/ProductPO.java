package com.zhihao.food.restaurantservicemanager.po;

import com.zhihao.food.restaurantservicemanager.enummeration.ProductStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author zhihao.cai
 * Created by 2021/10/6.
 */
@Getter
@Setter
@ToString
public class ProductPO {
    private Integer id;
    private String name;
    private BigDecimal price;
    private Integer restaurantId;
    private ProductStatus status;
    private Date date;
}
