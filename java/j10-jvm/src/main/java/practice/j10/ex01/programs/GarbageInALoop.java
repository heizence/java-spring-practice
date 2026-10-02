package practice.j10.ex01.programs;

/** Builds 1,000 reports of 1 MB each, one after the other. Each report is used once and then dropped. */
public final class GarbageInALoop {

    private GarbageInALoop() {
    }

    public static void main(String[] args) {
        long totalBytes = 0;
        for (int i = 0; i < 1_000; i++) {
            byte[] report = new byte[1_000_000]; // 1 MB
            report[i] = 1;
            totalBytes += report.length;
        }
        System.out.println(totalBytes / 1_000_000 + " MB allocated in total");
    }
}
