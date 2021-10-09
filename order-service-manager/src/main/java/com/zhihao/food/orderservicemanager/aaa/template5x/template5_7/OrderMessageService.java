package com.zhihao.food.orderservicemanager.aaa.template5x.template5_7;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zhihao.food.orderservicemanager.dao.OrderDetailDao;
import com.zhihao.food.orderservicemanager.dto.OrderMessageDTO;
import com.zhihao.food.orderservicemanager.enummeration.OrderStatus;
import com.zhihao.food.orderservicemanager.po.OrderDetailPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
//@Service
public class OrderMessageService {
    @Autowired
    private OrderDetailDao orderDetailDao;
    ObjectMapper objectMapper = new ObjectMapper();


    public void handleMessage(OrderMessageDTO orderMessageDTO) throws IOException {
        log.info("handleMessage:messageBody:{}", orderMessageDTO);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
//            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
//                    OrderMessageDTO.class);
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
                            channel.basicPublish(
                                    "exchange.order.settlement",
                                    "key.settlement",
                                    null,
                                    messageToSend.getBytes()
                            );
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
                            channel.basicPublish(
                                    "exchange.order.reward",
                                    "key.reward",
                                    null,
                                    messageToSend.getBytes()
                            );
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
            }

        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    };
}
