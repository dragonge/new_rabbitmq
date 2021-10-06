package com.zhihao.food.restaurantservicemanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.zhihao.food.restaurantservicemanager.dao.ProductDao;
import com.zhihao.food.restaurantservicemanager.dao.RestaurantDao;
import com.zhihao.food.restaurantservicemanager.dto.OrderMessageDTO;
import com.zhihao.food.restaurantservicemanager.enummeration.ProductStatus;
import com.zhihao.food.restaurantservicemanager.enummeration.RestaurantStatus;
import com.zhihao.food.restaurantservicemanager.po.ProductPO;
import com.zhihao.food.restaurantservicemanager.po.RestaurantPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Author zhihao.cai
 * Created by 2021/10/6.
 */
@Slf4j
@Service
public class OrderMessageService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProductDao productDao;
    @Autowired
    RestaurantDao restaurantDao;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start listening message in restaurant");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            channel.queueDeclare(
                    "queue.restaurant",
                    true,
                    false,
                    false,
                    null);

            channel.queueBind(
                    "queue.restaurant",
                    "exchange.order.restaurant",
                    "key.restaurant");


            channel.basicConsume("queue.restaurant", true, deliverCallback, consumerTag -> {
            });
            while (true) {
                Thread.sleep(100000);
            }
        }

    }

    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            ProductPO productPO = productDao.selsetProduct(orderMessageDTO.getProductId());
            log.info("onMessage:productPO:{}", productPO);
            RestaurantPO restaurantPO = restaurantDao.selsctRestaurant(productPO.getRestaurantId());
            log.info("onMessage:restaurantPO:{}", restaurantPO);
            if (ProductStatus.AVALIABLE == productPO.getStatus() && RestaurantStatus.OPEN == restaurantPO.getStatus()) {
                orderMessageDTO.setConfirmed(true);
                orderMessageDTO.setPrice(productPO.getPrice());
            } else {
                orderMessageDTO.setConfirmed(false);
            }
            log.info("sendMessage:restaurantOrderMessageDTO:{}", orderMessageDTO);

            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish("exchange.order.restaurant", "key.order", null, messageToSend.getBytes());
            }
        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }


    };
}
