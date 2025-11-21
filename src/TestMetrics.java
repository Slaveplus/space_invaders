public class TestMetrics {

    public int testMethod(int a, int b) {
        if (a > 10) { // +1
            if (b > 5) { // +1
                return 1;
            } else {
                return 2;
            }
        }

        for (int i = 0; i < a; i++) { // +1
            System.out.println(i);
        }

        return 0;
    } // 총 v(G) = 4
}