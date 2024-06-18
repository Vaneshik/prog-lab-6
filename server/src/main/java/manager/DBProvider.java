package manager;

import models.Coordinates;
import models.Location;
import models.Organization;
import models.OrganizationType;
import network.User;

import java.sql.*;
import java.util.Date;


public class DBProvider {
    private static Connection connection;

    public static void establishConnection(String url, String user, String password) {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, password);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkUserExistence(String username) {

        String query = "SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)";

        try (PreparedStatement p = connection.prepareStatement(query)) {

            p.setString(1, username);
            ResultSet res = p.executeQuery();
            if (res.next()) {
                return res.getBoolean(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean checkUserPassword(User user) {
        var username = user.getUsername();
        var hashedPassword = user.getPassword();

        String query = "SELECT hashedpassword FROM users WHERE username = ?";

        try (PreparedStatement p = connection.prepareStatement(query)) {

            p.setString(1, username);
            ResultSet res = p.executeQuery();

            if (res.next()) {
                String storedHashedPassword = res.getString("hashedpassword");
                return storedHashedPassword.equals(hashedPassword);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static void addUser(User user) {
        var username = user.getUsername();
        var hashedPassword = user.getPassword();

        String query = "INSERT INTO users (username, hashedpassword) VALUES (?, ?)";

        try (PreparedStatement p = connection.prepareStatement(query)) {

            p.setString(1, username);
            p.setString(2, hashedPassword);
            p.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void load(CollectionManager collectionManager) {

        String query = "SELECT organizations.id, organizations.name, organizations.x, organizations.y, " +
                "organizations.creationDate, organizations.annualTurnover, organizations.fullName, organizations.employeesCount, "
                + "organizations.organizationType, organizations.addressName, organizations.locationX, organizations.locationY, organizations.locationName, organizations.creatorID FROM organizations JOIN users ON users.id = organizations.creatorid";

        try (PreparedStatement p = connection.prepareStatement(query)) {
            ResultSet res = p.executeQuery();

            while (res.next()) {
                try {
                    var element = new Organization(
                            res.getLong(1),
                            res.getString(2),
                            new Coordinates(res.getDouble(3), res.getFloat(4)),
                            res.getDate(5),
                            res.getDouble(6),
                            res.getString(7),
                            res.getInt(8),
                            OrganizationType.valueOf(res.getString(9)),
                            new models.Address(res.getString(10), new Location(res.getLong(11), res.getDouble(12), res.getString(13))),
                            res.getString(14)
                    );
                    if (checkUserExistence(element.getCreator())) {
                        collectionManager.add(element);
                    }

                } catch (IllegalArgumentException e) {
                    Server.logger.error("Повреждённый атрибут type у элемента с id " + res.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean addOrganization(Organization organization) {

        String query = "INSERT INTO organizations (name, x, y, creationDate, annualTurnover, fullName, employeesCount, organizationType, addressName, locationX, locationY, locationName, creatorID)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, (SELECT id FROM users WHERE username = ?))";

        try (PreparedStatement p = connection.prepareStatement(query)) {
            p.setString(1, organization.getName());
            p.setDouble(2, organization.getCoordinates().getX());
            p.setFloat(3, organization.getCoordinates().getY());

            long dateInMilliseconds = new Date().getTime();
            p.setTimestamp(4, new Timestamp(dateInMilliseconds));

            p.setDouble(5, organization.getAnnualTurnover());
            p.setString(6, organization.getFullName());
            p.setInt(7, organization.getEmployeesCount());
            p.setString(8, organization.getType().getClass().getName());
            p.setString(9, organization.getPostalAddress().getZipCode());
            p.setString(10, organization.getPostalAddress().getZipCode());
            p.setLong(11, organization.getPostalAddress().getTown().getX());
            p.setDouble(12, organization.getPostalAddress().getTown().getY());
            p.setString(13, organization.getPostalAddress().getTown().getName());

            p.setString(14, organization.getCreator());

            p.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateOrganization(User user, long id, Organization organization) {

        String query = "UPDATE vehicles SET name = ?, x = ?, y = ?, creationDate = ?, annualTurnover = ?, fullName = ?, employeesCount = ?, organizationType = ?, addressName = ?, locationX = ?, locationY = ?, locationName = ? WHERE (id = ? AND creatorid IN (SELECT id FROM users WHERE username = ?))";

        try (PreparedStatement p = connection.prepareStatement(query)) {
            p.setString(1, organization.getName());
            p.setDouble(2, organization.getCoordinates().getX());
            p.setFloat(3, organization.getCoordinates().getY());

            long dateInMilliseconds = new Date().getTime();
            p.setTimestamp(4, new Timestamp(dateInMilliseconds));

            p.setDouble(5, organization.getAnnualTurnover());
            p.setString(6, organization.getFullName());
            p.setInt(7, organization.getEmployeesCount());
            p.setString(8, organization.getType().getClass().getName());
            p.setString(9, organization.getPostalAddress().getZipCode());
            p.setString(10, organization.getPostalAddress().getZipCode());
            p.setLong(11, organization.getPostalAddress().getTown().getX());
            p.setDouble(12, organization.getPostalAddress().getTown().getY());
            p.setString(13, organization.getPostalAddress().getTown().getName());

            p.setString(14, organization.getCreator());

            p.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeOrganizationById(long id) {

        String query = "DELETE FROM organizations WHERE id = ?";

        try (PreparedStatement p = connection.prepareStatement(query)) {
            p.setLong(1, id);
            p.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeOrganizationsGreaterThanId(User user, long id) {

        String query = "DELETE FROM organizations WHERE (id > ? AND creatorID IN (SELECT id FROM users WHERE username = ?))";

        try (PreparedStatement p = connection.prepareStatement(query)) {
            p.setLong(1, id);
            p.setString(2, user.getUsername());
            p.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeOrganizationsByType(User user, OrganizationType type) {

        String query = "DELETE FROM organizations WHERE (organizationType = ? AND creatorId IN (SELECT id FROM users WHERE username = ?))";

        try (PreparedStatement p = connection.prepareStatement(query)) {
            p.setString(1, type.getClass().getName());
            p.setString(2, user.getUsername());
            p.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean clearOrganizaitons(User user) {

        String query = "DELETE FROM organizations WHERE creatorid IN (SELECT id FROM users WHERE username = ?)";

        try (PreparedStatement p = connection.prepareStatement(query)) {
            p.setString(1, user.getUsername());
            p.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

//    public static long loadElCount() {
//        String query = " select last_value from vehicles_id_seq";
//
//        try (PreparedStatement p = connection.prepareStatement(query)) {
//            ResultSet res = p.executeQuery();
//
//            if (res.next()) {
//                return res.getLong(1);
//            }
//            return -1;
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return -1;
//        }
//    }
}
