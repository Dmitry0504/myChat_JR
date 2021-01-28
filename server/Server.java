package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    //отправляем сообщение всем пользователям
    public static void sendBroadcastMessage(Message message){
        try {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                entry.getValue().send(message);
            }
        }catch (IOException e){
            System.out.println("Ошибка при отправке сообщения");
        }
    }

    private static class Handler extends Thread{
        private Socket socket;

        public Handler(Socket socket){
            this.socket = socket;
        }

        //пытаемся добавить нового пользователя
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            //вечный цикд для проверки условий
            while (true){
                //просим ввести имя пользователя
                connection.send(new Message(MessageType.NAME_REQUEST));
                //принимам ответ
                Message message = connection.receive();
                //если в ответ получили не имя возвращаемся к началу цикла
                if(message.getType() != MessageType.USER_NAME) continue;
                //если в ответ получили пустое имя или такое имя уже зарезервировано -> в начало
                if(message.getData().isEmpty() | connectionMap.containsKey(message.getData())) continue;
                //извлекаем из полученного сообщения имя пользователя
                String name = message.getData();
                //помещаем его в карту соединений
                connectionMap.put(name, connection);
                //отправляем подтверждение о добавлении пользователя
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                ConsoleHelper.writeMessage(name + " accepted!");
                //выходим из цикла
                return name;
            }
        }

        //говорим новому пользователю об остальных участниках беседы
        private void notifyUsers(Connection connection, String userName) throws IOException {
            for(Map.Entry<String, Connection> entry: connectionMap.entrySet()){
                if(entry.getKey().equals(userName)) continue;
                connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
            }
        }

        //принимаем сообщения от пользователя
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true){
                //получаем сообщение
                Message message = connection.receive();
                //проверяем текст ли это
                if(message.getType() == MessageType.TEXT){
                    //отправляем полученное сообщение остальным членам чата
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                }
                //если нет отправляем сообщение об ошибке
                if(message.getType() != MessageType.TEXT){
                    ConsoleHelper.writeMessage("Произошла ошибка!");
                }
            }
        }

        @Override
        public void run() {
            //получаем адресс удаленного подключения
            SocketAddress address = socket.getRemoteSocketAddress();
            ConsoleHelper.writeMessage("Соединение с " + address + " установлено!");
            try(Connection connection = new Connection(socket)) {
                //здороваемся с новым пользователем
                String name = serverHandshake(connection);
                //уведомляем остальных о его подключении
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                //говорим новому пользователю кто тут есть
                notifyUsers(connection, name);
                //запускаем цикл приема сообщений от пользователей
                serverMainLoop(connection, name);
                //удаляем пользователя из чата и уведомляем остальных участников беседы
                connectionMap.remove(name);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, name));
                ConsoleHelper.writeMessage("Соединение с " + address + " закрыто.");
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Соединение с " + address + " потеряно...");
            }
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите номер порта который будет использоваться");
        try(ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())){
            System.out.println("Сервер запущен");
            while (true){
                new Handler(serverSocket.accept()).start();
            }
        }catch (IOException e){
            System.out.println("IO exception");
        }
    }

}
