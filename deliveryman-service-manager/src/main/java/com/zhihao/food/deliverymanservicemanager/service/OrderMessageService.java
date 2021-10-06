package com.zhihao.food.deliverymanservicemanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.zhihao.food.deliverymanservicemanager.dao.DeliverymanDao;
import com.zhihao.food.deliverymanservicemanager.dto.OrderMessageDTO;
import com.zhihao.food.deliverymanservicemanager.enummeration.DeliverymanStatus;
import com.zhihao.food.deliverymanservicemanager.po.DeliverymanPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * 消息处理相关业务
 */
@Slf4j
@Service
public class OrderMessageService {

    @Autowired
    DeliverymanDao deliverymanDao;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start linstening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare("exchange.order.deliveryman",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);
            channel.queueDeclare("queue.deliveryman",
                    true,
                    false,
                    false,
                    null);
            channel.queueBind("queue.deliveryman",
                    "exchange.order.deliveryman",
                    "key.deliveryman");
            channel.basicConsume("queue.deliveryman", true, deliverCallback, consumerTag -> {
            });
            while (true) {
                Thread.sleep(10000);
            }

        }
    }

    DeliverCallback deliverCallback = ((consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            List<DeliverymanPO> deliverymanPOS = deliverymanDao.selectAvaliableDeliveryman(DeliverymanStatus.AVALIABLE);
            orderMessageDTO.setDeliverymanId(deliverymanPOS.get(0).getId());
            log.info("onMessage:restaurantOrderMessageDTO:{}", orderMessageDTO);
            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                String messageTosend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish("exchange.order.restaurant", "key.order", null, messageTosend.getBytes());
            }

        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    });
}
