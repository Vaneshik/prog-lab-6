package command;

import manager.FileManager;
import network.Response;

/**
 * Команда "save".
 * Описание команды: сохранить коллекцию в файл.
 */
public class SaveCommand implements CommandInterface {
    FileManager fileManager;

    public SaveCommand(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * Выполнение команды.
     *
     * @param args аргументы
     */
    @Override
    public Response execute(String[] args, models.Organization organizationObject) {
        if (args.length != 0) {
            return new Response("Команда не принимает аргументы!", " ");
        }

        try {
            fileManager.saveCollection();
        } catch (Exception e) {
            return new Response(e.getMessage(), " ");
        }

        return new Response("Коллекция сохранена в файл", " ");
    }

    @Override
    public String toString() {
        return ": сохранить коллекцию в файл";
    }
}
