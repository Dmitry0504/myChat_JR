package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    //���������� ��������� ���� �������������
    public static void sendBroadcastMessage(Message message){
        try {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                entry.getValue().send(message);
            }
        }catch (IOException e){
            System.out.println("������ ��� �������� ���������");
        }
    }

    private static class Handler extends Thread{
        private Socket socket;

        public Handler(Socket socket){
            this.socket = socket;
        }

        //�������� �������� ������ ������������
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            //������ ���� ��� �������� �������
            while (true){
                //������ ������ ��� ������������
                connection.send(new Message(MessageType.NAME_REQUEST));
                //�������� �����
                Message message = connection.receive();
                //���� � ����� �������� �� ��� ������������ � ������ �����
                if(message.getType() != MessageType.USER_NAME) continue;
                //���� � ����� �������� ������ ��� ��� ����� ��� ��� ��������������� -> � ������
                if(message.getData().isEmpty() | connectionMap.containsKey(message.getData())) continue;
                //��������� �� ����������� ��������� ��� ������������
                String name = message.getData();
                //�������� ��� � ����� ����������
                connectionMap.put(name, connection);
                //���������� ������������� � ���������� ������������
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                ConsoleHelper.writeMessage(name + " accepted!");
                //������� �� �����
                return name;
            }
        }

        //������� ������ ������������ �� ��������� ���������� ������
        private void notifyUsers(Connection connection, String userName) throws IOException {
            for(Map.Entry<String, Connection> entry: connectionMap.entrySet()){
                if(entry.getKey().equals(userName)) continue;
                connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
            }
        }

        //��������� ��������� �� ������������
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true){
                //�������� ���������
                Message message = connection.receive();
                //��������� ����� �� ���
                if(message.getType() == MessageType.TEXT){
                    //���������� ���������� ��������� ��������� ������ ����
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                }
                //���� ��� ���������� ��������� �� ������
                if(message.getType() != MessageType.TEXT){
                    ConsoleHelper.writeMessage("��������� ������!");
                }
            }
        }

        @Override
        public void run() {
            //�������� ������ ���������� �����������
            SocketAddress address = socket.getRemoteSocketAddress();
            ConsoleHelper.writeMessage("���������� � " + address + " �����������!");
            try(Connection connection = new Connection(socket)) {
                //����������� � ����� �������������
                String name = serverHandshake(connection);
                //���������� ��������� � ��� �����������
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                //������� ������ ������������ ��� ��� ����
                notifyUsers(connection, name);
                //��������� ���� ������ ��������� �� �������������
                serverMainLoop(connection, name);
                //������� ������������ �� ���� � ���������� ��������� ���������� ������
                connectionMap.remove(name);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, name));
                ConsoleHelper.writeMessage("���������� � " + address + " �������.");
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("���������� � " + address + " ��������...");
            }
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("������� ����� ����� ������� ����� ��������������");
        try(ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())){
            System.out.println("������ �������");
            while (true){
                new Handler(serverSocket.accept()).start();
            }
        }catch (IOException e){
            System.out.println("IO exception");
        }
    }

}
