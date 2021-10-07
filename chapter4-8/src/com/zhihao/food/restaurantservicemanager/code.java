// 声明死信交换机
        channel.exchangeDeclare(
                "exchange.dlx",
                BuiltinExchangeType.TOPIC,
                true,
                false,
                null);
// 声明死信队列
                channel.queueDeclare(
                "queue.dlx",
                true,
                false,
                false,
                null);
// 声明死信绑定
                channel.queueBind(
                "queue.dlx",
                "exchange.dlx",
                "#");

//声明队列时，携带以下参数：

        Map<String, Object> args=new HashMap<>(16);
        //设置死信队列
        args.put("x-dead-letter-exchange","exchange.dlx");
        // 设置队列最大消息数
        args.put("x-max-length",10);
        channel.queueDeclare("queue.xxx",
        true,
        false,
        false,
        args);