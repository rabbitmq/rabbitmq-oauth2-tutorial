package com.vmware.rabbitmq;

import com.rabbitmq.jms.admin.RMQConnectionFactory;

import javax.jms.*;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Semaphore;

public class App {
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private final Command command;
    private boolean secure;
    private String token;
    private int qbrMax;
    private String queueName;
    private String message = "hello world";
    private String hostname;

    enum Command { pub, sub }

    public App(Command command, String hostname, String token, String queueName, boolean secure) {
        this.command = command;
        this.hostname = hostname;
        this.queueName = queueName;
        this.secure = secure;
        this.token = token;
        this.qbrMax = 0;
    }

    public static void main( String[] args ) throws JMSException, InterruptedException {
        if (args.length < 1) {
            throw new RuntimeException("Missing 1st parameter [pub or sub]");
        }
        new App(Command.valueOf(args[0]), hostname(), token(), queueName(), isSecure()).run();
    }

    private static String queueName() {
        String queue = System.getenv("QUEUE");
        return queue != null ? queue : "jms-client.test";
    }

    private static String token() {
        return System.getenv("TOKEN");
    }

    private static String hostname() {
        String hostname = System.getenv("HOSTNAME");
        return hostname != null ? hostname : "localhost";
    }
    private static boolean isSecure() {
        String secureOption = System.getenv("SECURE");
        return secureOption != null && Boolean.parseBoolean(secureOption);
    }

    private void sendQueueMessage() throws JMSException {
        QueueConnection conn = getQueueConnectionFactory().createQueueConnection("", token);
        try {
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);

            QueueSender sender = session.createSender(queue);
            sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            sender.send(textMessage(session));
            System.out.println("Sent message");
            sender.close();
            session.close();
        }finally {
            conn.stop();
        }
    }


    private Message textMessage(QueueSession session) throws JMSException {
        return session.createTextMessage(message);

    }
    public void run() throws JMSException, InterruptedException {
        System.out.printf("Running command %s\n", command);

        switch(command) {
            case pub:
                sendQueueMessage();
                break;
            case sub:
                subscribeQueue();
                break;
            }
    }

    private void subscribeQueue() throws JMSException, InterruptedException {
        QueueConnection conn = getQueueConnectionFactory().createQueueConnection("", token);
        try {
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);
            QueueReceiver receiver = session.createReceiver(queue);

            final Semaphore sem = new Semaphore(0);
            receiver.setMessageListener(message -> {
                System.out.println("Received message");
                if (message instanceof TextMessage) {
                    try {
                        String msgBody = ((TextMessage) message).getText();
                        if (msgBody.equals("exit")) sem.release();
                    } catch (JMSException e) {

                    }
                }
            });
            sem.acquire();
            session.close();
        }finally {
            conn.stop();
        }
    }

    private QueueConnectionFactory getQueueConnectionFactory(){
        return (QueueConnectionFactory)getConnectionFactory();
    }
    private ConnectionFactory getConnectionFactory() {
        RMQConnectionFactory rmqCF = new RMQConnectionFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public Connection createConnection(String userName, String password) throws JMSException {
                if (!secure) {
                    this.setPort(RABBIT_PORT);
                } else {
                    this.setPort(RABBIT_TLS_PORT);
                }
                return super.createConnection(userName, password);
            }
        };
        if (secure) {
            try {
                rmqCF.useSslProtocol();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        rmqCF.setHost(hostname);
        rmqCF.setQueueBrowserReadMax(qbrMax);
        return rmqCF;
    }

}
