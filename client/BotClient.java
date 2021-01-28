package client;

import server.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class BotClient extends Client {

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int)(Math.random()*100);
    }

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    public class BotSocketThread extends SocketThread{

        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            BotClient.this.sendTextMessage(
                    "������ ������. � ���. ������� �������: ����, ����, �����, ���, �����, ���, ������, �������."
            );
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            //������� � ��� ���� ��������� ������������
            ConsoleHelper.writeMessage(message);
            if(message == null | !message.contains(": ")) return;
            String[] array = message.split(": ");
            //������� ����� ���� - ����� ������� ������ ������������
            //�������� - ������� ��� SimpleDateFormat
            HashMap<String, String> map = new HashMap<>();
            map.put("����", "d.MM.YYYY");
            map.put("����", "d");
            map.put("�����", "MMMM");
            map.put("���", "YYYY");
            map.put("�����", "H:mm:ss");
            map.put("���", "H");
            map.put("������", "m");
            map.put("�������", "s");
            //�������� �������� �� map � ����������� �� ������ ��������� �������������
            String pattern = map.get(array[1]);
            if(pattern == null) return;
            String answer = new SimpleDateFormat(pattern).format(Calendar.getInstance().getTime());
            //���������� ��������� - �����
            sendTextMessage(String.format("���������� ��� %s: %s", array[0], answer));
        }
    }
}

