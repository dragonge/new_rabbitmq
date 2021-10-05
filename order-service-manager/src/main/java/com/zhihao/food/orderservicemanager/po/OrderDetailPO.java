package com.zhihao.food.orderservicemanager.po;

import com.zhihao.food.orderservicemanager.enummeration.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class OrderDetailPO {
    private Integer id;
    private OrderStatus status;
    private String address;
    private Integer deliverymanId;
    private Integer productId;
    private Integer accountId;
    private Integer settlementId;
    private Integer rewardId;
    private BigDecimal price;
    private Date date;
}
