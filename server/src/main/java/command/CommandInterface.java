package command;

import models.Organization;
import network.Response;

/**
 * Интерфейс команд.
 */
public interface CommandInterface {
    /**
     * Выполняет команду
     *
     * @param args               аргументы
     * @param organizationObject
     */
    Response execute(String[] args, Organization organizationObject);
}
