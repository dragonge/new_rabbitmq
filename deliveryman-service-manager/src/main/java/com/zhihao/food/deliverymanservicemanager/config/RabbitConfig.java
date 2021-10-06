package com.zhihao.food.deliverymanservicemanager.config;

import com.zhihao.food.deliverymanservicemanager.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Author zhihao.cai
 * Created by 2021/10/6.
 */
@Slf4j
@Configuration
public class RabbitConfig {
    @Autowired
    private OrderMessageService orderMessageService;

    @Autowired
    public void startListenMessage() throws InterruptedException, TimeoutException, IOException {
        orderMessageService.handleMessage();
    }
}
