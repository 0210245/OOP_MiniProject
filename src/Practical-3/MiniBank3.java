//package Practical3;
public class MiniBank3{
    static class Customer implements Cloneable {
        private String name;
        private String email;
        private String mobile;
        private final String customerId;
        private Address address;

        private static long customerCounter = 100;

        private static String generateCustomerId(){
            customerCounter ++;
            return "Cust" + customerCounter;
        }

        Customer(String name , String email , String mobile){
            this.name = name;
            this.email = email;
            this.mobile = mobile;
            this.customerId = generateCustomerId();
        }
        public String getName (){
            return name;
        }

        public String getEmail(){
            return email;
        }
        public String getMobile(){
            return mobile;
        }
        public String getCustomerId(){
            return customerId;
        }
        public Address getAddress(){
            return address;
        }

        public static class Address{
            private String line;
            private String city;
            private String pincode;

            public Address(String line,String city,String pincode){
                this.line = line;
                this.city = city;
                this.pincode = pincode;
            }

            public String getLine(){
                return line;
            }
            public String getCity(){
                return city;
            }
            public String getPincode(){
                return pincode;
            }
        }

        public Customer clone () {
            try {
                return(Customer)super.clone();
            }catch (CloneNotSupportedException e){
                throw new RuntimeException(e);
            }
        }
    }

    static class Account {
        private final String accountNumber;
        private String ownerName;
        private long balance;
        private boolean active;

        private static long accountCounter = 0;

        private static String generateAccountNumber (){
            accountCounter++;
            return String.format("AC%04d", accountCounter);
        }

        Account(String ownerName , long balance){
            this.accountNumber = generateAccountNumber();
            this.ownerName = ownerName;
            this.balance = balance;
            this.active = true;
        }

        Account (String ownerName){
            this(ownerName,0);
        }
        public void deposit(long amount){
            balance = balance + amount;
        }
        public  boolean withdraw(long amount){
            if (balance >= amount){
                balance = balance - amount;
                return true;
            }
            return false;
        }

        public String getAccountNumber (){
            return accountNumber;
        }

        public String getOwnerName(){
            return ownerName;
        }

        public long getBalance(){
            return balance;
        }

        public boolean isActive(){
            return active;
        }

        @Override 
        public String toString(){
            return accountNumber + "-" + ownerName + "- Balance: " + balance;
        }

        @Override 
        public boolean equals(Object o){
            if (this == o){
                return true;
            }
            if (!(o instanceof Account)){
                return false;
            }
            Account account = (Account) o;
            return accountNumber.equals(account.accountNumber);
        }

        @Override 
        public int hashCode (){
            return accountNumber.hashCode();
        }
    }

    public static void main(String[] args) {
        Account [] accounts = new Account[3];

        accounts[0] = new Account("Nandani",1000);
        accounts[1] = new Account("Hitanshi");
        accounts[2] = new Account("Priya",5000);

        accounts[0].deposit(500);
        accounts[0].withdraw(200);

        accounts[1].deposit(2000);
        accounts[1].withdraw(500);

        accounts[2].withdraw(1000);
        accounts[2].deposit(500);

        for (Account account : accounts) {
            System.out.println(account);
        }

        System.out.println("\nAccount comparison:");

        System.out.println(accounts[0].equals(accounts[1]));

        System.out.println("\ninstanceof:");

        if (accounts[0] instanceof Account) {
            System.out.println("accounts[0] is an Account");
        }

        Customer customer = new Customer("Nandani", "nandani@gmail.com", "9876543210");

        Customer.Address address = new Customer.Address("ABC Road", "Anand", "388001");

        System.out.println("\nCustomer Address:");
        System.out.println(address.getLine());
        System.out.println(address.getCity());
        System.out.println(address.getPincode());

        Customer copy = customer.clone();

        System.out.println("\nCustomer ID: " + customer.getCustomerId());
        System.out.println("Cloned Customer ID: " + copy.getCustomerId());

    }
}