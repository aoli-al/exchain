package al.aoli.exchain.demo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ObjectDemo extends Object {

    private List synchedList;

    public ObjectDemo() {
        // create a new synchronized list to be used
        synchedList = Collections.synchronizedList(new LinkedList());
    }

    // method used to remove an element from the list
    public String removeElement() throws InterruptedException {
        synchronized (this) {
            wait(0);
        }
        return "?";
    }

    public static void main(String[] args) {}
}
