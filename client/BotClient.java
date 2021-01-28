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
                    "Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды."
            );
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            //выводим в чат бота сообщение пользователя
            ConsoleHelper.writeMessage(message);
            if(message == null | !message.contains(": ")) return;
            String[] array = message.split(": ");
            //создаем карту ключ - слово которое введит пользователь
            //значение - паттерн для SimpleDateFormat
            HashMap<String, String> map = new HashMap<>();
            map.put("дата", "d.MM.YYYY");
            map.put("день", "d");
            map.put("месяц", "MMMM");
            map.put("год", "YYYY");
            map.put("время", "H:mm:ss");
            map.put("час", "H");
            map.put("минуты", "m");
            map.put("секунды", "s");
            //получаем значение из map в соответсвии со словом введенным пользователем
            String pattern = map.get(array[1]);
            if(pattern == null) return;
            String answer = new SimpleDateFormat(pattern).format(Calendar.getInstance().getTime());
            //отправляем сообщение - ответ
            sendTextMessage(String.format("Информация для %s: %s", array[0], answer));
        }
    }
}

