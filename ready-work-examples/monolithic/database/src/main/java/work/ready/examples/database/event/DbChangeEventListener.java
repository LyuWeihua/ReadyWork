package work.ready.examples.database.event;

import work.ready.core.database.DbChangeEvent;
import work.ready.core.database.DbChangeListener;

import java.util.Arrays;

public class DbChangeEventListener implements DbChangeListener {
    @Override
    public void onInserted(DbChangeEvent event) {
        System.out.println("DbChangeEvent: ==> " + event.getTable() + " | " + event.getType() + " | " + Arrays.toString(event.getId()));
    }

    @Override
    public void onUpdated(DbChangeEvent event) {
        System.out.println("DbChangeEvent: ==> " + event.getTable() + " | " + event.getType() + " | " + Arrays.toString(event.getId()));
    }

    @Override
    public void onDeleted(DbChangeEvent event) {
        System.out.println("DbChangeEvent: ==> " + event.getTable() + " | " + event.getType() + " | " + Arrays.toString(event.getId()));
    }

    @Override
    public void onReplaced(DbChangeEvent event) {
        System.out.println("DbChangeEvent: ==> " + event.getTable() + " | " + event.getType() + " | " + Arrays.toString(event.getId()));
    }

    @Override
    public void onMerged(DbChangeEvent event) {
        System.out.println("DbChangeEvent: ==> " + event.getTable() + " | " + event.getType() + " | " + Arrays.toString(event.getId()));
    }
}
