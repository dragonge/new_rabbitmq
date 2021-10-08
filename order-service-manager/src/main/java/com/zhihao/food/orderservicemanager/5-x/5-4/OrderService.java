
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihao.food.orderservicemanager.dao.OrderDetailDao;
import com.zhihao.food.orderservicemanager.dto.OrderMessageDTO;
import com.zhihao.food.orderservicemanager.enummeration.OrderStatus;
import com.zhihao.food.orderservicemanager.po.OrderDetailPO;
import com.zhihao.food.orderservicemanager.vo.OrderCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

@Slf4j
//@Service
public class OrderService {

    @Autowired
    private OrderDetailDao orderDetailDao;

    @Autowired
    RabbitTemplate rabbitTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException, InterruptedException {
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

        String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setExpiration("15000");
        Message message = new Message(messageToSend.getBytes(), messageProperties);
        CorrelationData correlationData = new CorrelationData();
        correlationData.setId(orderPO.getId().toString());
        rabbitTemplate.send(
                "exchange.order.restaurant",
                "key.restaurant",
                message, correlationData
        );

        rabbitTemplate.convertAndSend(
                "exchange.order.restaurant",
                "key.restaurant",
                messageToSend,correlationData
        );
        //        rabbitTemplate.execute(channel -> {
        //            channel.abort();
        //            return null;
        //        });

        log.info("message sent");

        Thread.sleep(1000);
    }
}