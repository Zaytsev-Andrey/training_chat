package client.storages;

import java.util.List;

public interface Storage {
    void open();

    void close();

    void write(String str);

    List<String> nextHistoryList();
}
