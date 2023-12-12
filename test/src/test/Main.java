package test;

import java.util.ArrayList;
import java.util.HashSet;

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
        HashSet<String> hs = new HashSet<>();
        hs.add("aaa");
        hs.add("bbb");
        hs.add("ccc");
        hs.add("aaa");
        hs.add("bbbb");
        String[] hss = hs.toArray(new String[0]);
        for (int i = 0; i < hss.length; i++) {
            System.out.println(hss[i]);
        }
    }
}
