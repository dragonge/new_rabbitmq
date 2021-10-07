//设置队列TTL
 Map<String, Object> args=new HashMap<String, Object>();
        args.put("x-message-ttl",10000);
        channel.queueDeclear("exchang",true,false,false,args);


//设置单条消息TTL
        AMQP.BasicProperties properties=new AMQP.BasicProperties.Builder().expiration("100000").build();

        channel.basicPublish("exchange.order.restaurant", "key.order", properties, messageToSend.getBytes());
