package client;

import server.Connection;
import server.ConsoleHelper;
import server.Message;
import server.MessageType;

import java.io.IOException;
import java.net.Socket;

/*
Клиент, в начале своей работы, должен запросить у пользователя адрес и порт сервера,
подсоединиться к указанному адресу, получить запрос имени от сервера, спросить имя
у пользователя, отправить имя пользователя серверу, дождаться принятия имени сервером.
После этого клиент может обмениваться текстовыми сообщениями с сервером.
Обмен сообщениями будет происходить в двух параллельно работающих потоках.
Один будет заниматься чтением из консоли и отправкой прочитанного серверу, а второй
поток будет получать данные от сервера и выводить их в консоль.
 */
public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адрес сервера(localhost)");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Введите номер порта сервера");
        return ConsoleHelper.readInt();
    }

    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    //просим ввести имя пользователя
    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите имя пользователя");
        return ConsoleHelper.readString();
    }

    //отправляем сообщение
    protected void sendTextMessage(String text){
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Ошибка при отправке сообщения");
            clientConnected = false;
        }
    }

    public void run(){
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this){
            try {
                this.wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Ошибка при ожидании");
            }

        }
        while (clientConnected){
            String message = ConsoleHelper.readString();
            if(message.equals("exit")){
                ConsoleHelper.writeMessage("Вы покинули чат");
                clientConnected = false;
                break;
            }
            if(shouldSendTextFromConsole()){
                sendTextMessage(message);
            }

        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    //отвечает за поток, устанавливающий сокетное соединение и читающий сообщения сервера
    public class SocketThread extends Thread{

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        //знакомимся с сервером
        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                //получаем сообщение
                Message message = connection.receive();
                //если это запрос имени, спрашиваем у пользователя имя и отправляем
                if(message.getType() == MessageType.NAME_REQUEST) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                    continue;
                }
                //если имя подтверждено завершаем метод
                if(message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                }
                //если сервер спрашивает что-то не то кидаем исключение
                if(message.getType() != MessageType.NAME_REQUEST |
                        message.getType() != MessageType.NAME_ACCEPTED)
                    throw new IOException("Unexpected MessageType");
            }
        }

        //основной метод принимающий сообщения от сервера
        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                //в зависимости от типа сообщения отправляем его в соответсвующий метод
                if(message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                }
                else if(message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                }
                else if(message.getType() == MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                }
                //если что-то не так кидаем исключение
                else if(message.getType() != MessageType.TEXT |
                        message.getType() != MessageType.USER_ADDED |
                        message.getType() != MessageType.USER_REMOVED){
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }
}
