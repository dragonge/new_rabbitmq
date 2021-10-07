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
            // 自动ACK表示队列是否自动确认签收消息并处理，当这条消息是未被签收状态 且消费端挂了，那么这条消息就会被重新放回ready队列等待消费
            // 这里注意需要修改是否自动确认ACK
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
                // 监听回调消息失败了 会调用
//                channel.addReturnListener(new ReturnListener() {
//                    @Override
//                    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                        log.info("Message Return: replyCode:{}, replyText:{}, exchange:{},routingKey:{}," +
//                        "properties:{}, " +
//                                "body:{}",
//                                replyCode, replyText, exchange, routingKey, properties, body);
//                    }
//                });
                channel.addReturnListener(new ReturnCallback() {
                    @Override
                    public void handle(Return returnMessage) {
                        log.info("Message Return: returnMessage:{}", returnMessage);
                    }
                });
                // 等待10条 ack一起确认完成
                if(message.getEnvelope().getDeliveryTag() % 10 == 0){
                    channel.basicAck(message.getEnvelope().getDeliveryTag(), true);
                }
                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish("exchange.order.restaurant", "key.order", null, messageToSend.getBytes());
            }
        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }


    };
}
