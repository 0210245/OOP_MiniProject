public class ThreadDemo {

    static class AccountWorker implements Runnable {
        private MiniBank10.Account account;
        private int times;
        private long amount;

        AccountWorker(MiniBank10.Account account, int times, long amount) {
            this.account = account;
            this.times = times;
            this.amount = amount;
        }

        @Override
        public void run() {
            for (int i = 0; i < times; i++) {
                try {
                    account.deposit(amount);
                } catch (MiniBank10.BankException e) {
                    System.out.println(Thread.currentThread().getName()
                            + ": " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        MiniBank10.Account account =
                new MiniBank10.SavingsAccount("Test User", 0, 0);

        int threadCount = 10;
        int times = 1000;
        long amount = 1000;

        Thread[] threads = new Thread[threadCount];

        System.out.println("Starting threads...");

        for (int i = 0; i < threadCount; i++) {
            AccountWorker worker = new AccountWorker(account, times, amount);

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