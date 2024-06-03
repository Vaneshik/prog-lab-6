package command;

import manager.CollectionManager;
import models.Organization;
import models.forms.OrganizationForm;
import network.Response;

/**
 * Команда "remove_lower".
 * Описание команды: удалить из коллекции все элементы, меньшие, чем заданный.
 */
public class RemoveLowerCommand implements CommandInterface {
    CollectionManager manager;

    public RemoveLowerCommand(CollectionManager manager) {
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

        var size = manager.getCollection().size();
        manager.removeLower(new OrganizationForm().build());
        return new Response("Удалено " + String.valueOf(size - manager.getCollection().size()) + " элементов, меньшие, чем заданный", " ");
    }

    @Override
    public String toString() {
        return ": удалить из коллекции все элементы, меньшие, чем заданный";
    }
}
