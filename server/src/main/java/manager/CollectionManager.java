package manager;

import lombok.Getter;
import lombok.Setter;
import models.Address;
import models.Organization;
import network.User;

import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Класс для управления коллекцией.
 */
@Getter
@Setter
public class CollectionManager {
    /**
     * Время инициализации коллекции.
     */
    public static Date initializationTime = new Date();

    /**
     * Коллекция.
     */
    private TreeSet<Organization> collection = new TreeSet<Organization>();

    private final ReadWriteLock locker = new ReentrantReadWriteLock();

    /**
     * Добавить элемент в коллекцию.
     *
     * @param o элемент
     */
    public void add(Organization o, boolean flag) {
        locker.readLock().lock();

        if (flag) {
            var id = DBProvider.addOrganization(o);
            o.setId(id);
        }
        collection.add(o);

        locker.readLock().unlock();
    }

    /**
     * Обновить элемент коллекции по id.
     *
     * @param id id элемента
     * @param o  новый элемент
     */
    public void update(long id, Organization o) {
        locker.readLock().lock();
        if (DBProvider.updateOrganization(id, o)) {
            collection.stream().filter(organization -> organization.getId() == id).forEach(organization -> {
                organization.setName(o.getName());
                organization.setCoordinates(o.getCoordinates());
                organization.setCreationDate(o.getCreationDate());
                organization.setAnnualTurnover(o.getAnnualTurnover());
                organization.setFullName(o.getFullName());
                organization.setEmployeesCount(o.getEmployeesCount());
                organization.setType(o.getType());
                organization.setPostalAddress(o.getPostalAddress());
                System.out.println("Элемент обновлен!");
            });
        } else {
            System.out.println("Ошибка обновления элемента((");
        }
        locker.readLock().unlock();
    }

    /**
     * Удалить элемент по id.
     *
     * @param id id элемента
     */
    public void removeById(long id) {
        locker.readLock().lock();
        if (DBProvider.removeOrganizationById(id)) {
            collection.removeIf(o -> o.getId() == id);
        }
        locker.readLock().unlock();
    }

    /**
     * Очистить коллекцию.
     */
    public void clear(User user) {
        locker.readLock().lock();
        if (DBProvider.clearOrganizations(user)) {
            collection.clear();
            DBProvider.load(this);
        }
        initializationTime = new Date();
        locker.readLock().unlock();
    }

    /**
     * Вернуть первый(минимальный) элемент коллекции.
     *
     * @return первый элемент коллекции
     */
    public String minByFullName() {
        return collection.stream().map(Organization::getFullName).min(String::compareTo).orElse(null);
    }

    /**
     * Добавить элемент в коллекцию, если он меньше минимального.
     *
     * @param o элемент
     */
    public void addIfMin(Organization o) {
        locker.readLock().lock();
        if (collection.isEmpty() || collection.first().compareTo(o) > 0) {
            if (DBProvider.addOrganization(o) != -1) {
                collection.add(o);
            }
        }
        locker.readLock().unlock();
    }

    /**
     * Вернуть список элементов, значение поля name которых содержит заданную подстроку.
     *
     * @param name подстрока
     */
    public List<Organization> filterContainsName(String name) {
        return collection.stream().filter(o -> o.getName().contains(name)).toList();
    }

    /**
     * Вернуть список элементов, значение поля postalAddress которых меньше заданного.
     *
     * @param postalAddress индекс
     */
    public List<Organization> filterLessThanPostalAddress(Address postalAddress) {
        return collection.stream().filter(o -> o.getPostalAddress().compareTo(postalAddress) < 0).toList();
    }

    /**
     * Удалить элементы, которые больше заданного.
     *
     * @param o объект для сравнения
     */

    // TODO: removeGreater
    public void removeGreater(Organization o) {
        collection.removeIf(organization -> organization.compareTo(o) > 0);
    }

    /**
     * Удалить элементы, значение поля annualTurnover которых меньше заданного.
     *
     * @param o объект для сравнения
     */
    // TODO: removeLower
    public void removeLower(Organization o) {
        collection.removeIf(organization -> organization.compareTo(o) < 0);
    }

    /**
     * Вернуть элемент по id.
     *
     * @param id id элемента
     */
    public Organization getById(long id) {
        return collection.stream().filter(o -> o.getId() == id).findFirst().orElse(null);
    }
}
