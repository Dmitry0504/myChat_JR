package client;

import server.Connection;
import server.ConsoleHelper;
import server.Message;
import server.MessageType;

import java.io.IOException;
import java.net.Socket;

/*
������, � ������ ����� ������, ������ ��������� � ������������ ����� � ���� �������,
�������������� � ���������� ������, �������� ������ ����� �� �������, �������� ���
� ������������, ��������� ��� ������������ �������, ��������� �������� ����� ��������.
����� ����� ������ ����� ������������ ���������� ����������� � ��������.
����� ����������� ����� ����������� � ���� ����������� ���������� �������.
���� ����� ���������� ������� �� ������� � ��������� ������������ �������, � ������
����� ����� �������� ������ �� ������� � �������� �� � �������.
 */
public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("������� ����� �������(localhost)");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("������� ����� ����� �������");
        return ConsoleHelper.readInt();
    }

    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    //������ ������ ��� ������������
    protected String getUserName(){
        ConsoleHelper.writeMessage("������� ��� ������������");
        return ConsoleHelper.readString();
    }

    //���������� ���������
    protected void sendTextMessage(String text){
        try {
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            ConsoleHelper.writeMessage("������ ��� �������� ���������");
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
                ConsoleHelper.writeMessage("������ ��� ��������");
            }

        }
        while (clientConnected){
            String message = ConsoleHelper.readString();
            if(message.equals("exit")){
                ConsoleHelper.writeMessage("�� �������� ���");
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

    //�������� �� �����, ��������������� �������� ���������� � �������� ��������� �������
    public class SocketThread extends Thread{

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " ������������� � ����");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " ������� ���");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        //���������� � ��������
        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                //�������� ���������
                Message message = connection.receive();
                //���� ��� ������ �����, ���������� � ������������ ��� � ����������
                if(message.getType() == MessageType.NAME_REQUEST) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                    continue;
                }
                //���� ��� ������������ ��������� �����
                if(message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                }
                //���� ������ ���������� ���-�� �� �� ������ ����������
                if(message.getType() != MessageType.NAME_REQUEST |
                        message.getType() != MessageType.NAME_ACCEPTED)
                    throw new IOException("Unexpected MessageType");
            }
        }

        //�������� ����� ����������� ��������� �� �������
        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                //� ����������� �� ���� ��������� ���������� ��� � �������������� �����
                if(message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                }
                else if(message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                }
                else if(message.getType() == MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                }
                //���� ���-�� �� ��� ������ ����������
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
