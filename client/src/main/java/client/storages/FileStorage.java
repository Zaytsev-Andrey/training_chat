package client.storages;

import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileStorage implements Storage {
    private static final String path = "./client/storage/";
    private String historyFile;
    private FileWriter writer;
    private String login;
    private DateFormat dateFormat;
    private int bufferSize;
    private List<String> buffer;
    LinkedList<File> fileList;

    public static class tester {
        public static void main(String[] args) {
            FileStorage fileStorage = new FileStorage("qwe", 100);

            List<String> history;
            while ((history = fileStorage.nextHistoryList()) != null) {
                history.forEach(System.out::println);
                System.out.println("---");
            }
        }
    }

    public FileStorage(String login, int bufferSize) {
        this.login = login;
        this.bufferSize = bufferSize;
        dateFormat = new SimpleDateFormat("yyyyMMdd");
        buffer = new ArrayList<>();

        String fileName = String.format("history_%s_%s.txt", login, dateFormat.format(new Date()));
        historyFile = path + fileName;

        open();
        fillFileList();
    }

    @Override
    public void open() {
        try {
            writer = new FileWriter(historyFile, true);
            System.out.println("History file opened");
        } catch (IOException e) {
            System.out.println("History file was not opened");
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
            System.out.println("History file closed");
        } catch (IOException e) {
            System.out.println("History file was not closed");
        }
    }

    @Override
    public void write(String str) {
        try {
            writer.write(str);
            writer.flush();
        } catch (IOException e) {
            System.out.println("History file not available");
        }
    }

    @Override
    public List<String> nextHistoryList() {
        if (fileList != null && !fileList.isEmpty()) {
            fillBuffer();
        }

        if (buffer.isEmpty()) {
            return null;
        }

        List<String> result;

        if (buffer.size() < bufferSize) {
            result = new ArrayList<>(buffer);
            buffer.clear();
        } else {
            result = new ArrayList<>(buffer.subList(buffer.size() - bufferSize, buffer.size()));
            buffer.removeAll(result);
        }

        return result;
    }

    private void fillFileList() {
        File file = new File(path);

        File[] files = file.listFiles(pathname -> pathname.isFile()
                && pathname.getName().startsWith("history_" + login)
                && pathname.length() > 0);

        if (files == null || files.length == 0) {
            return;
        }

        fileList = new LinkedList<>(Arrays.asList(files));
        fileList.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));
    }

    private void fillBuffer() {
        while (buffer.size() < bufferSize && !fileList.isEmpty()) {
            File f = fileList.poll();
            try {
                buffer.addAll(0, Files.readAllLines(f.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
