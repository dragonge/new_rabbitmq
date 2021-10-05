package com.zhihao.food.orderservicemanager.dto;

import com.zhihao.food.orderservicemanager.enummeration.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Setter
@Getter
@ToString
public class OrderMessageDTO {
    private Integer orderId;
    private OrderStatus orderStatus;
    private BigDecimal price;
    private Integer deliverymanId;
    private Integer productId;
    private Integer accountId;
    private Integer settlementId;
    private Integer rewardId;
    private BigDecimal rewardAmount;
    private Boolean confirmed;
}
