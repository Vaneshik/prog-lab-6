package command;

import manager.CommandManager;
import models.Organization;
import network.Response;

/**
 * Команда "help".
 * Описание команды: вывести справку по доступным командам.
 */
public class HelpCommand implements CommandInterface {
    CommandManager manager;

    public HelpCommand(CommandManager manager) {
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
            System.err.println("Команда не принимает аргументы!");
        }

        StringBuilder help = new StringBuilder();

        help.append("Доступные команды:\n");
        manager.getCommands().forEach((name, command) -> help.append(name + command.toString()+'\n'));
        return new Response(help.toString(), "");
    }

    @Override
    public String toString() {
        return ": вывести справку по доступным командам";
    }
}
