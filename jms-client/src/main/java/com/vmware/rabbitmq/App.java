package com.vmware.rabbitmq;

import com.rabbitmq.jms.admin.RMQConnectionFactory;

import javax.jms.*;
import java.security.NoSuchAlgorithmException;

public class App {
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private boolean secure;
    private String token;
    private int qbrMax;
    private String queueName;
    private String message = "hello world";
    private String hostname;

    public App(String hostname, String token, String queueName, boolean secure) {
        this.hostname = hostname;
        this.queueName = queueName;
        this.secure = secure;
        this.token = token;
        this.qbrMax = 0;
    }

    public static void main( String[] args ) throws JMSException {
        new App(hostname(), token(), queueName(), isSecure()).run();
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

            sender.close();
            session.close();
        }finally {
            conn.stop();
        }
    }


    private Message textMessage(QueueSession session) throws JMSException {
        return session.createTextMessage(message);

    }
    public void run() throws JMSException {
        sendQueueMessage();
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
