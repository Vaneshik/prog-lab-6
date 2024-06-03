package manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import models.Organization;

import java.io.*;
import java.util.List;

/**
 * Менеджер файлов.
 */
public class FileManager {
    private final String inputFileName;
    private final CollectionManager collectionManager;

    public FileManager(String inputFileName, CollectionManager collectionManager) {
        this.inputFileName = inputFileName;
        this.collectionManager = collectionManager;
    }

    /**
     * Проверить, можно ли читать файл.
     *
     * @param file    файл
     * @return true, если можно читать файл, иначе false
     */
    public static boolean canRead(File file) {
        if (!file.exists()) {
            System.err.println("Файл не найден");
            return false;
        }

        if (!file.canRead()) {
            System.err.println("Нет прав на чтение файла");
            return false;
        }

        if (!file.isFile()) {
            System.err.println("Указанный путь не является файлом");
            return false;
        }

        return true;
    }

    /**
     * Сохранить коллекцию в файл.
     */
    public void saveCollection() {
        try {
            XmlMapper mapper = new XmlMapper();
            String xmlResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(collectionManager.getCollection());
            try (FileOutputStream fos = new FileOutputStream(inputFileName)) {
                fos.write(xmlResult.getBytes());
                fos.flush();
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения коллекции в файл");
        }
    }

    /**
     * Заполнить коллекцию из файла.
     */
    public void fillCollection() {
        File file = new File(inputFileName);
        if (!canRead(file)) {
            return;
        }

        try {
            XmlMapper xmlMapper = new XmlMapper();
            String xml = bufferedReaderToString(new BufferedReader(new FileReader(file)));
            List<Organization> to_check = xmlMapper.readValue(xml, new TypeReference<List<Organization>>() {
            });

            for (var organization : to_check) {
                if (ValidationManager.isValidOrganization(organization, collectionManager)) {
                    collectionManager.add(organization);
                } else {
                    System.err.println("Организация с id " + organization.getId() + " не прошла валидацию");

                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка чтения файла");
        }
    }

    /**
     * Преобразовать BufferedReader в строку.
     *
     * @param br BufferedReader
     * @return строка
     * @throws IOException ошибка ввода/вывода
     */
    public String bufferedReaderToString(BufferedReader br) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
}
