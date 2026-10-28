public class ThreadDemo9 {
    public static void main(String[] args) throws InterruptedException {
        MiniBank9.Account account = new MiniBank9.SavingsAccount("Test User", 0, 0);

        int threadCount = 10;
        int times = 1000;
        long amount = 1000;

        Thread[] threads = new Thread[threadCount];

        System.out.println("Starting threads...");

        for (int i = 0; i < threadCount; i++) {
            MiniBank9.AccountWorker worker =
                    new MiniBank9.AccountWorker(account, times, amount);
            threads[i] = new Thread(worker, "Worker-" + (i + 1));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("\nAll threads completed.");
        System.out.println("Expected Balance: 10000000");
        System.out.println("Actual Balance: " + account.getBalance());
    }
}