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

@Slf4j
@Service
public class OrderMessageService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProductDao productDao;
    @Autowired
    RestaurantDao restaurantDao;

    @Autowired
    Channel channel;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start linstening message");

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

        channel.basicQos(2);
        this.channel.basicConsume("queue.restaurant", false, deliverCallback, consumerTag -> {
        });
        while (true) {
            Thread.sleep(100000);
        }
    }



    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);

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
            //AutoClosable


//                channel.addReturnListener(new ReturnListener() {
//                    @Override
//                    public void handleReturn(int replyCode,
//                                             String replyText,
//                                             String exchange,
//                                             String routingKey,
//                                             AMQP.BasicProperties properties,
//                                             byte[] body
//                    ) throws IOException {
//                        log.info("Message Return: " +
//                                "replyCode:{}, replyText:{}, exchange:{}, routingKey:{}, properties:{}, body:{}",
//                                replyCode, replyText, exchange, routingKey, properties, new String(body));
//                        //除了打印log，可以加别的业务操作
//                    }
//                });

            channel.addReturnListener(new ReturnCallback() {
                @Override
                public void handle(Return returnMessage) {
                    log.info("Message Return: returnMessage{}", returnMessage);

                    //除了打印log，可以加别的业务操作
                }
            });
            if (message.getEnvelope().getDeliveryTag()%10 == 0){
                channel.basicAck(message.getEnvelope().getDeliveryTag(),true);}

            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.basicPublish("exchange.order.restaurant", "key.order",true, null, messageToSend.getBytes());
            Thread.sleep(1000);

        } catch (JsonProcessingException  | InterruptedException e) {
            e.printStackTrace();
        }
    };
}

