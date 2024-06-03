package manager;

import command.*;
import models.Organization;
import network.Request;
import network.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Server {
    private InetSocketAddress address;
    private Selector selector;
    private Response response;
    private Request request;
    private Logger logger;
    private CollectionManager collectionManager;
    private CommandManager commandManager;

    public Server(InetSocketAddress address) {
        this.address = address;
        logger = LogManager.getLogger(Server.class);
        this.collectionManager = new CollectionManager();
        this.commandManager = new CommandManager();
    }

    public void run(String[] args) {

        FileManager fileManager = null;
        try {
            var pathToCollection = args[0]; //"data.xml"
            fileManager = new FileManager(pathToCollection, collectionManager);

            commandManager.addCommand("help", new HelpCommand(commandManager));
            commandManager.addCommand("info", new InfoCommand(collectionManager));
            commandManager.addCommand("show", new ShowCommand(collectionManager));
            commandManager.addCommand("add", new AddCommand(collectionManager));
            commandManager.addCommand("update", new UpdateCommand(collectionManager));
            commandManager.addCommand("remove_by_id", new RemoveByIdCommand(collectionManager));
            commandManager.addCommand("clear", new ClearCommand(collectionManager));
            commandManager.addCommand("save", new SaveCommand(fileManager));
            commandManager.addCommand("add_if_min", new AddIfMinCommand(collectionManager));
            commandManager.addCommand("remove_greater", new RemoveGreaterCommand(collectionManager));
            commandManager.addCommand("remove_lower", new RemoveLowerCommand(collectionManager));
            commandManager.addCommand("min_by_full_name", new MinByFullNameCommand(collectionManager));
            commandManager.addCommand("filter_contains_name", new FilterContainsNameCommand(collectionManager));
            commandManager.addCommand("filter_less_than_postal_address", new FilterLessThanPostalAddressCommand(collectionManager));

            fileManager.fillCollection();
            logger.info("Коллекция загружена");

            selector = Selector.open();
            logger.info("Селектор открыт");

            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(address);
            serverChannel.configureBlocking(false);

            logger.info("Канал сервера готов к работе");
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                logger.info("Итератор по ключам селектора успешно получен");

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    logger.info("Обработка ключа началась");

                    try {
                        if (key.isValid()) {
                            if (key.isAcceptable()) {
                                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                                SocketChannel clientChannel = serverSocketChannel.accept();

                                logger.info("Установлено соединение с клиентом " + clientChannel.socket().toString());

                                clientChannel.configureBlocking(false);
                                clientChannel.register(selector, SelectionKey.OP_READ);
                            }

                            if (key.isReadable()) {
                                SocketChannel clientChannel = (SocketChannel) key.channel();
                                clientChannel.configureBlocking(false);

                                ByteBuffer clientData = ByteBuffer.allocate(2048);

                                logger.info(clientChannel.read(clientData) + " байт пришло от клиента");

                                try (ObjectInputStream clientDataIn = new ObjectInputStream(new ByteArrayInputStream(clientData.array()))) {
                                    request = (Request) clientDataIn.readObject();
                                } catch (StreamCorruptedException e) {
                                    key.cancel();
                                }


                                if (request == null) {
                                    response = new Response("Клиент " + clientChannel.socket().toString() + " отключился((", " ");
                                    continue;
                                }

                                var commandName = request.getCommandName();
                                var commandStrArg = request.getCommandStrArg();
                                var commandObjArg = (Organization) request.getCommandObjArg();

                                System.out.println(commandName + commandStrArg + commandObjArg);
                                response = commandManager.executeCommand(commandName, commandStrArg, commandObjArg);
                                if (response == null) {
                                    response = new Response("Команда не найдена", "");
                                }

                                logger.info("Запрос:\n" + commandName + "\n" + commandStrArg + "\n" + commandObjArg + "\nУспешно обработан");
                                clientChannel.register(selector, SelectionKey.OP_WRITE);
                            }

                            if (key.isWritable()) {
                                SocketChannel clientChannel = (SocketChannel) key.channel();
                                clientChannel.configureBlocking(false);

                                try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                     ObjectOutputStream clientDataOut = new ObjectOutputStream(bytes)) {
                                    clientDataOut.writeObject(response);

                                    ByteBuffer clientData = ByteBuffer.wrap(bytes.toByteArray());
                                    ByteBuffer dataLength = ByteBuffer.allocate(32).putInt(clientData.limit());
                                    dataLength.flip();

                                    clientChannel.write(dataLength); // пишем длину ответа клиенту
                                    logger.info("Длинна ответа (" + dataLength + ") отправлена клиенту");
                                    clientChannel.write(clientData); // шлём клиенту ответ
                                    logger.info("Ответ отправлен клиенту");
                                    clientData.clear();
                                }

                                clientChannel.register(selector, SelectionKey.OP_READ);
                            }
                        }
                    } catch (SocketException | CancelledKeyException e) {
                        logger.info("Клиент " + key.channel().toString() + " отключился");
                        fileManager.saveCollection();
                        key.cancel();
                    }
                    keys.remove();
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        } catch (NoSuchElementException e) {
            System.out.println(1);
            logger.error("Остановка сервера через консоль");
            fileManager.saveCollection();
            System.exit(1);
        } catch (IOException e) {
            logger.error("Ошибка ввода/вывода");
        } catch (ClassNotFoundException e) {
            logger.error("Несоответствующие классы" + e.getStackTrace());
        } catch (Exception e) {
            logger.error("Ошибка сервера: " + e.getMessage());
        }
    }
}