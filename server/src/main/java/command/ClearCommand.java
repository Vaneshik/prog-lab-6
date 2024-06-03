package command;

import manager.CollectionManager;
import models.Organization;
import network.Response;

/**
 * Команда "clear".
 * Описание команды: очистить коллекцию.
 */
public class ClearCommand implements CommandInterface {
    CollectionManager manager;

    public ClearCommand(CollectionManager manager) {
        this.manager = manager;
    }

    /**
     * Выполнение команды.
     *
     * @param args               аргументы
     * @param organizationObject
     */
    @Override
    public Response execute(String[] args, Organization organizationObject) {
        if (args.length != 0) {
           return new Response("Команда не принимает аргументы!", " ");
        }

        manager.clear();
        return new Response("Коллекция очищена", " ");
    }

    @Override
    public String toString() {
        return ": очистить коллекцию";
    }
}
