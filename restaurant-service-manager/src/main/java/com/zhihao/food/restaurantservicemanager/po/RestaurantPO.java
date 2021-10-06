package com.zhihao.food.restaurantservicemanager.po;

import com.zhihao.food.restaurantservicemanager.enummeration.RestaurantStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @Author zhihao.cai
 * Created by 2021/10/6.
 */
@Getter
@Setter
@ToString
public class RestaurantPO {
    private Integer id;
    private String name;
    private String address;
    private RestaurantStatus status;
    private Date date;
}
