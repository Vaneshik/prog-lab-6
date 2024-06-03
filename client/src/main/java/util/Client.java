package util;

import exceptions.ScriptRecursionException;
import models.Organization;
import models.forms.OrganizationForm;
import network.Request;
import network.Response;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.System.exit;

public class Client {
    private final Set<Path> scriptsNames = new TreeSet<>();
    private final InetAddress host;
    private final int port;
    private SocketChannel channel;

    public Client(InetAddress host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        try {
            SocketAddress address = new InetSocketAddress(host, port);
            channel = SocketChannel.open();
            channel.connect(address);

            PromptScan.setUserScanner(new Scanner(System.in));
            var scanner = PromptScan.getUserScanner();

            System.out.println("Клиентское приложение запущено. Для просмотра доступных команд введите 'help'");

            while (true) {
                System.out.print("> ");
                try {

                    while (scanner.hasNext()) {
                        var command = "";
                        var arguments = "";
                        String[] input = (scanner.nextLine() + " ").trim().split(" ");
                        command = input[0].trim();

                        processUserPrompt(command, Arrays.copyOfRange(input, 1, input.length));
                        System.out.print("> ");
                    }

                } catch (NoSuchElementException e) {
                    System.out.println("Остановка клиента через консоль");
                    exit(1);
                } catch (ClassNotFoundException e) {
                    System.out.println("Объект поступивший в ответ от сервера не найден");
                } catch (SocketException e) {
                    System.out.println("Сервер был остановлен во время обработки вашего запроса. Пожалуйста, повторите попытку позже");
                    exit(1);
                }
            }

        } catch (ConnectException e) {
            System.out.println("Сервер недоступен в данный момент. Пожалуйста, повторите попытку позже");
        } catch (IOException e) {
            System.out.println("Ошибка ввода/вывода");
        }
    }

    private void processUserPrompt(String command, String[] arguments) throws IOException, ClassNotFoundException {
        Request request;
        if (command.equalsIgnoreCase("add") || command.equalsIgnoreCase("update") || command.equalsIgnoreCase("remove_greater") || command.equalsIgnoreCase("add_if_min") || command.equalsIgnoreCase("remove_lower")) {
            Organization objArgument = new OrganizationForm().build();
            request = new Request(command, arguments, objArgument);
            sendAndReceive(request);
        } else if (command.equalsIgnoreCase("exit")) {
            System.out.println("Работа клиентского приложения завершена");
            exit(0);
        } else if (command.equalsIgnoreCase("execute_script")) {
            if (arguments.length == 0) {
                System.out.println("Неверные аргументы команды");
            } else if (arguments.length == 1) {
                executeScript(arguments[0]);
            } else {
                System.out.println("Неверное количество аргументов");
            }
        } else {
            request = new Request(command, arguments);
            sendAndReceive(request);
        }
    }

    private void sendAndReceive(Request request) throws IOException, ClassNotFoundException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {

            out.writeObject(request);
            ByteBuffer dataToSend = ByteBuffer.wrap(bytes.toByteArray());
            channel.write(dataToSend); // отправляем серверу запрос
            out.flush();
        }

        try {
            ByteBuffer dataToReceiveLength = ByteBuffer.allocate(32);
            channel.read(dataToReceiveLength); // читаем длину ответа от сервера
            dataToReceiveLength.flip();
            int responseLength = dataToReceiveLength.getInt(); // достаём её из буфера
            ByteBuffer dataToReceive = ByteBuffer.allocate(responseLength * 4); // создаем буфер нужной нам длины
            channel.read(dataToReceive); // получаем ответ от сервера

            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(dataToReceive.array()))) {
                Response response = (Response) in.readObject();

                System.out.println(response.getMessage());
                String collection = response.getCollectionToStr();
                if (!collection.isEmpty()) {
                    System.out.println(collection);
                }

            }
        } catch (Exception e) {
            System.out.println("Ошибка при получении ответа от сервера");
            exit(1);
        }

    }

    private void executeScript(String path) throws ClassNotFoundException {
        if (path.isBlank()) {
            System.out.println("Неверные аргументы команды");

        } else {
            try {
                Path pathToScript = Paths.get(path);

                PromptScan.setUserScanner(new Scanner(pathToScript));
                Scanner scriptScanner = PromptScan.getUserScanner();

                Path scriptFile = pathToScript.getFileName();

                if (!scriptScanner.hasNext()) throw new NoSuchElementException();

                scriptsNames.add(scriptFile);

                do {
                    var command = "";
                    String[] input = (scriptScanner.nextLine() + " ").trim().split(" ");
                    command = input[0].trim();

                    while (scriptScanner.hasNextLine() && command.isEmpty()) {
                        input = (scriptScanner.nextLine() + " ").trim().split(" ", 2);
                        command = input[0].trim();
                    }

                    if (command.equalsIgnoreCase("execute_script")) {
                        try {
                            Path scriptNameFromArgument = Paths.get(input[1]).getFileName();

                            if (scriptsNames.contains(scriptNameFromArgument)) {
                                throw new ScriptRecursionException("Один и тот же скрипт не может выполнятся рекурсивно");
                            }
                            executeScript(input[1]);

                        } catch (ScriptRecursionException e) {
                            System.out.println(e.getMessage());
                        }

                    } else {
                        processUserPrompt(command, Arrays.copyOfRange(input, 1, input.length));
                    }

                } while (scriptScanner.hasNextLine());

                scriptsNames.remove(scriptFile);
                PromptScan.setUserScanner(new Scanner(System.in));
                System.out.println("Скрипт " + scriptFile + " успешно выполнен");

            } catch (FileNotFoundException e) {
                System.out.println("Файл " + path + " не найден");
            } catch (NoSuchElementException e) {
                System.out.println("Файл " + path + " пуст");
            } catch (IllegalStateException e) {
                System.out.println("Непредвиденная ошибка");
            } catch (SecurityException e) {
                System.out.println("Недостаточно прав для чтения файла " + path);
            } catch (IOException e) {
                System.out.println("Ошибка ввода/вывода");
                System.out.println(e.getClass().getName());
            } catch (InvalidPathException e) {
                System.out.println("Проверьте путь к файлу. В нём не должно быть лишних символов");
            }
        }


    }
}