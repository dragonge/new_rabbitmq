package com.zhihao.food.orderservicemanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.zhihao.food.orderservicemanager.dao.OrderDetailDao;
import com.zhihao.food.orderservicemanager.dto.OrderMessageDTO;
import com.zhihao.food.orderservicemanager.enummeration.OrderStatus;
import com.zhihao.food.orderservicemanager.po.OrderDetailPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 消息处理相关业务
 */
@Slf4j
@Service
public class OrderMessageService {
    @Resource
    private OrderDetailDao orderDetailDao;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start listening message in order message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare("exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null
            );
            channel.queueDeclare(
                    "queue.order",
                    true,
                    false,
                    false,
                    null
            );
            channel.queueBind("queue.order",
                    "exchange.order.restaurant",
                    "key.order"
            );
            /*---------------------deliveryman---------------------*/
            channel.exchangeDeclare(
                    "exchange.order.deliveryman",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);


            channel.queueBind(
                    "queue.order",
                    "exchange.order.deliveryman",
                    "key.order");

            /*---------------------settlement---------------------*/
            channel.exchangeDeclare("exchange.settlement.order",
                    BuiltinExchangeType.FANOUT,
                    true,
                    false,
                    null);

            channel.queueBind("queue.order",
                    "exchange.settlement.order",
                    "key.order");

            /*---------------------reward---------------------*/

            channel.exchangeDeclare(
                    "exchange.order.reward",
                    BuiltinExchangeType.TOPIC,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.reward",
                    "key.order");


            channel.basicConsume("queue.order", true, deliverCallback, consumerTag -> {
            });
            while (true) {
                Thread.sleep(100000);
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
            OrderDetailPO orderPO = orderDetailDao.selectOrder(orderMessageDTO.getOrderId());
            switch (orderPO.getStatus()) {
                case ORDER_CREATING:
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        orderPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderPO.setPrice(orderMessageDTO.getPrice());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.deliveryman", "key.deliveryman", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    if (null != orderMessageDTO.getDeliverymanId()) {
                        orderPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        orderPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.settlement", "key.settlement", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    if (null != orderMessageDTO.getSettlementId()) {
                        orderPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderPO.setSettlementId(orderMessageDTO.getSettlementId());
                        orderDetailDao.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.reward", "key.reward.food", null, messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    if (null != orderMessageDTO.getRewardId()) {
                        orderPO.setStatus(OrderStatus.ORDER_CREATED);
                        orderPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailDao.update(orderPO);
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailDao.update(orderPO);
                    }
                    break;
                default:
                    log.info("order status is error, {}", orderPO.getStatus());
                    break;
            }
        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    });
}
