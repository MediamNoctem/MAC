package test;

import java.util.ArrayList;
import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        ArrayList<Record> a = new ArrayList<>();
        Record r = new Record("Булгур", "Крупа", 100);
        a.add(r);
        a.get(0).count = 5;
        System.out.println(a.get(0).count);
    }
}
