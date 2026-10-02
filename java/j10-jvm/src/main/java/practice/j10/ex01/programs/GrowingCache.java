package practice.j10.ex01.programs;

import java.util.ArrayList;
import java.util.List;

/** Builds 1,000 reports of 1 MB each and keeps every one in a cache, in case someone asks for it again. */
public final class GrowingCache {

    private static final List<byte[]> CACHE = new ArrayList<>();

    private GrowingCache() {
    }

    public static void main(String[] args) {
        for (int i = 0; i < 1_000; i++) {
            byte[] report = new byte[1_000_000]; // 1 MB
            report[i] = 1;
            CACHE.add(report);
        }
        System.out.println(CACHE.size() + " reports cached");
    }
}
