package test;

import java.util.ArrayList;
import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        ArrayList<String> a1 = new ArrayList<>();
        ArrayList<String> a2 = new ArrayList<>();
        ArrayList<String> a3 = new ArrayList<>();
        ArrayList<ArrayList<String>> a = new ArrayList<>();
        a1.add("milk1");
        a1.add("m1");
        a2.add("milk");
        a2.add("m2");
        a3.add("milk1");
        a3.add("m1");
        a.add(a1);
        a.add(a2);
        System.out.println(a.indexOf(a3));
    }
}
