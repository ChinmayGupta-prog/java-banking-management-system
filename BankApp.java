import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class BankApp {
    private static final String DATA_FILE = "bank.dat"; // serialized List<Customer>
    private static final String PASSWORD = "matrix";
    private static final Scanner sc = new Scanner(System.in);

    // ----- Model -----
    private static class Customer implements Serializable {
        int acno;
        String name;
        char type; // 'S' or 'C'
        double bal;

        @Override
        public String toString() {
            return String.format("%-6d %-20s %-1s %10.2f", acno, name, String.valueOf(type), bal);
        }
    }

    // ----- Persistence -----
    @SuppressWarnings("unchecked")
    private static List<Customer> loadCustomers() {
        if (!Files.exists(Path.of(DATA_FILE))) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            Object obj = ois.readObject();
            if (obj instanceof List<?> list) {
                // Filter out non-Customer entries defensively
                return (List<Customer>) list.stream()
                        .filter(Customer.class::isInstance)
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        } catch (EOFException eof) {
        // empty file - treat as no data
        } catch (Exception e) {
            System.out.println("Data load error: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private static void saveCustomers(List<Customer> customers) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(customers);
        } catch (Exception e) {
            System.out.println("Data save error: " + e.getMessage());
        }
    }

    // ----- Utilities -----
    private static void pause() {
        System.out.println("\nPress ENTER to continue...");
        try { System.in.read(); } catch (IOException ignored) {}
    }

    private static void printlnHeader(String title) {
        System.out.println("\n==================== " + title + " ====================");
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {
                System.out.println("Enter a valid integer.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    private static char readAccountType() {
        while (true) {
            String t = readLine("Enter S for Saving or C for Current: ").toUpperCase();
            if (t.length() == 1 && (t.charAt(0) == 'S' || t.charAt(0) == 'C')) return t.charAt(0);
            System.out.println("Invalid input. Try again.");
        }
    }

    private static Customer findByAcno(List<Customer> list, int acno) {
        for (Customer c : list) if (c.acno == acno) return c;
        return null;
    }

    private static int nextAccountNumber(List<Customer> list) {
        int max = 0;
        for (Customer c : list) max = Math.max(max, c.acno);
        return max + 1;
    }

    private static double minBalance(char type) {
        return (type == 'S') ? 5000.0 : 10000.0;
    }

    // ----- Features -----
    private static void openAccount(List<Customer> customers) {
        printlnHeader("Open New Account");
        Customer c = new Customer();
        c.acno = nextAccountNumber(customers);
        c.name = readLine("Enter name: ");
        c.type = readAccountType();
        double min = minBalance(c.type);
        double init = readDouble("Enter initial deposit (min " + (int)min + "): ");
        if (init < min) {
            System.out.println("Insufficient amount to open account.");
            return;
        }
        c.bal = init;
        customers.add(c);
        saveCustomers(customers);
        System.out.println("Account created. Your account number is " + c.acno);
    }

    private static void deposit(List<Customer> customers) {
        printlnHeader("Deposit");
        int acno = readInt("Enter account number: ");
        Customer c = findByAcno(customers, acno);
        if (c == null) { System.out.println("Invalid account number."); return; }
        double amt = readDouble("Enter amount to deposit: ");
        if (amt <= 0) { System.out.println("Amount must be positive."); return; }
        c.bal += amt;
        saveCustomers(customers);
        System.out.printf("Deposited. New balance: %.2f%n", c.bal);
    }

    private static void withdraw(List<Customer> customers) {
        printlnHeader("Withdraw");
        int acno = readInt("Enter account number: ");
        Customer c = findByAcno(customers, acno);
        if (c == null) { System.out.println("Invalid account number."); return; }
        double amt = readDouble("Enter amount to withdraw: ");
        if (amt <= 0) { System.out.println("Amount must be positive."); return; }
        double min = minBalance(c.type);
        if (c.bal - amt < min) {
            System.out.println("Insufficient funds to maintain minimum balance (" + (int)min + ").");
            return;
        }
        c.bal -= amt;
        saveCustomers(customers);
        System.out.printf("Withdrawn. New balance: %.2f%n", c.bal);
    }

    private static void showBalance(List<Customer> customers) {
        printlnHeader("Show Balance");
        int acno = readInt("Enter account number: ");
        Customer c = findByAcno(customers, acno);
        if (c == null) { System.out.println("Invalid account number."); return; }
        System.out.printf("Balance for A/C %d (%s): %.2f%n", c.acno, c.name, c.bal);
    }

    private static void showAll(List<Customer> customers) {
        printlnHeader("All Accounts");
        if (customers.isEmpty()) { System.out.println("No accounts found."); return; }
        System.out.printf("%-6s %-20s %-1s %10s%n", "Acno", "Name", "T", "Balance");
        for (Customer c : customers) System.out.println(c);
    }

    private static void modifyAccount(List<Customer> customers) {
        printlnHeader("Modify Account");
        int acno = readInt("Enter account number: ");
        Customer c = findByAcno(customers, acno);
        if (c == null) { System.out.println("Invalid account number."); return; }
        String newName = readLine("Enter new name (leave blank to keep '" + c.name + "'): ");
        if (!newName.isEmpty()) c.name = newName;
        saveCustomers(customers);
        System.out.println("Account updated.");
    }

    private static void closeAccount(List<Customer> customers) {
        printlnHeader("Close Account");
        int acno = readInt("Enter account number: ");
        Customer c = findByAcno(customers, acno);
        if (c == null) { System.out.println("Invalid account number."); return; }
        customers.remove(c);
        saveCustomers(customers);
        System.out.println("Account closed successfully.");
    }

    // ----- Auth -----
    private static boolean authenticate() {
        Console console = System.console();
    	if (console == null) {
        	System.out.println("No console available. Please run from a system terminal.");
        	return false;
    	}
    	char[] passwordChars = console.readPassword("Enter your password: ");
    	String input = new String(passwordChars);
    	return PASSWORD.equals(input.trim());
    }

    // ----- Main Menu -----
    public static void main(String[] args) {
        System.out.println("\n*** WELCOME TO THE BANKING MANAGEMENT SYSTEM ***\n");
        if (!authenticate()) {
            System.out.println("You are not an authorized user.");
            return;
        }
        System.out.println("\n                   Welcome to Bank Management System\n");

        List<Customer> customers = loadCustomers();

        int choice;
        do {
            System.out.println("\nMenu");
            System.out.println("1. Open New Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Show Balance");
            System.out.println("5. Show All");
            System.out.println("6. Modify Account");
            System.out.println("7. Close Account");
            System.out.println("8. Exit");
            choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1 -> openAccount(customers);
                case 2 -> deposit(customers);
                case 3 -> withdraw(customers);
                case 4 -> showBalance(customers);
                case 5 -> showAll(customers);
                case 6 -> modifyAccount(customers);
                case 7 -> closeAccount(customers);
                case 8 -> System.out.println("Goodbye!");
                default -> System.out.println("Incorrect input.");
            }
            if (choice != 8) pause();
        } while (choice != 8);
    }
}
