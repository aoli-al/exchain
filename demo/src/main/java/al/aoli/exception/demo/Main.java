package al.aoli.exception.demo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.zip.DataFormatException;

public class Main {
    public static void main(String[] args) throws DataFormatException, InterruptedException, RemoteException {
        ControlFlowTest test = new ControlFlowTest();
        test.scene2();
    }
}