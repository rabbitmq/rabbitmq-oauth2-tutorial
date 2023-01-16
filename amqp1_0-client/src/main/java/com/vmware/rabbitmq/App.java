package com.vmware.rabbitmq;


import jakarta.jms.*;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Unit test for simple App.
 */
public class App {
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private final Command command;
    private boolean secure;
    private int qbrMax;
    private String username;
    private String password;
    private String message = "hello world";

    enum Command { pub, sub }

    public App(Command command, String username, String password, boolean secure) {
        this.command = command;
        this.username = username;
        this.password = password;
        this.secure = secure;
        this.qbrMax = 0;
    }

    public static void main( String[] args ) {
        if (args.length < 1) {
            throw new RuntimeException("Missing 1st parameter [pub or sub]");
        }
        new App(Command.valueOf(args[0]), username(), password(), isSecure()).run();
    }

    private static String username() {
        return System.getenv("USERNAME");
    }
    private static String password() {
        return System.getenv("PASSWORD");
    }
    private static boolean isSecure() {
        String secureOption = System.getenv("SECURE");
        return secureOption != null && Boolean.parseBoolean(secureOption);
    }
    public void run() {
        System.out.printf("Running command %s\n", command);

        switch(command) {
            case pub:
                sendMessage();
                break;
            case sub:
                receivedMessage();
                break;
        }
    }

    private void receivedMessage() {
        try {
            // The configuration for the Qpid InitialContextFactory has been supplied in
            // a jndi.properties file in the classpath, which results in it being picked
            // up automatically by the InitialContext constructor.
            Context context = new InitialContext();

            ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
            Destination queue = (Destination) context.lookup("myQueueLookup");

            Connection connection = factory.createConnection(username, password);
            connection.setExceptionListener(new MyExceptionListener());
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageConsumer messageConsumer = session.createConsumer(queue);

            int timeout = 1000;
            Message message = messageConsumer.receive(timeout);
            if (message == null) {
                System.out.println("Message not received within timeout, stopping.");
                return;
            }
            System.out.println("Received message");

            connection.close();
        } catch (Exception exp) {
            System.out.println("Caught exception, exiting.");
            exp.printStackTrace(System.out);
            System.exit(1);
        }
    }
    private void sendMessage() {
        try {
            // The configuration for the Qpid InitialContextFactory has been supplied in
            // a jndi.properties file in the classpath, which results in it being picked
            // up automatically by the InitialContext constructor.
            Context context = new InitialContext();

            ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
            Destination queue = (Destination) context.lookup("myQueueLookup");

            Connection connection = factory.createConnection(username, password);
            connection.setExceptionListener(new MyExceptionListener());
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer messageProducer = session.createProducer(queue);

            TextMessage textMessage = session.createTextMessage(message);
            messageProducer.send(textMessage, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY,
                    Message.DEFAULT_TIME_TO_LIVE);

            connection.close();
        } catch (Exception exp) {
            System.out.println("Caught exception, exiting.");
            exp.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static class MyExceptionListener implements ExceptionListener {

        public void onException(JMSException exception) {
            System.out.println("Connection ExceptionListener fired, exiting.");
            exception.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
