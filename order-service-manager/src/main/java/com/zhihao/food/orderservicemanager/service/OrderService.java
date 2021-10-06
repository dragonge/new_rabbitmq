package com.zhihao.food.orderservicemanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zhihao.food.orderservicemanager.dao.OrderDetailDao;
import com.zhihao.food.orderservicemanager.dto.OrderMessageDTO;
import com.zhihao.food.orderservicemanager.enummeration.OrderStatus;
import com.zhihao.food.orderservicemanager.po.OrderDetailPO;
import com.zhihao.food.orderservicemanager.vo.OrderCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * 处理用户关于订单的业务请求
 */
@Slf4j
@Service
public class OrderService {
    @Resource
    private OrderDetailDao orderDetailDao;

    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        log.info("createOrder:orderCreateVO:{}", orderCreateVO);
        OrderDetailPO orderPO = new OrderDetailPO();
        orderPO.setAddress(orderCreateVO.getAddress());
        orderPO.setAccountId(orderCreateVO.getAccountId());
        orderPO.setProductId(orderCreateVO.getProductId());
        orderPO.setStatus(OrderStatus.ORDER_CREATING);
        orderPO.setDate(new Date());
        orderDetailDao.insert(orderPO);

        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderPO.getId());
        orderMessageDTO.setProductId(orderPO.getProductId());
        orderMessageDTO.setAccountId(orderCreateVO.getAccountId());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.basicPublish("exchange.order.restaurant", "key.restaurant", null, messageToSend.getBytes());
        }
    }
}
