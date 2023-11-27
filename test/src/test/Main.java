package test;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
//        ArrayList<Record> catalogue = new ArrayList<Record>();
//        catalogue.add(new Record("Испания", "A", 900, 4));
//        catalogue.add(new Record("Испания", "B",950, 5));
//
//        String str = "Испания;A;1000;3";
//        String[] arrayStr = str.split(";");
//
//        Record r;
//
//        for (int i = 0; i < catalogue.size(); i++) {
//            r = catalogue.get(i);
//
//        }
        ArrayList<String> str = new ArrayList<>();
        str.add("A");
        str.add("B");
        str.add("C");
        str.add("L");

        System.out.println(str.get(3).compareTo(str.get(0)));
    }
}
