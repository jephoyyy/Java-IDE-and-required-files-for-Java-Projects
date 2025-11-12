import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    private static Scanner sc = new Scanner(System.in);
    private static int orderCount = 0; // Track number of processed orders
    private static List<OrderItem> sessionOrders = new ArrayList<>(); // Store orders in current session

    // Class para sa bawat order item
    static class OrderItem {
        String name;
        int qty;
        double total;

        OrderItem(String name, int qty, double total) {
            this.name = name;
            this.qty = qty;      // ✅ Fixed lossy conversion error
            this.total = total;
        }
    }

    public static void main(String[] args) {
        while(true) {
            System.out.println("=== CANTEEN ORDERING SYSTEM ===");
            System.out.println("[1] User Mode");
            System.out.println("[2] Admin Mode");
            System.out.println("[3] Exit");
            System.out.print("Choose mode: ");
            int mode = sc.nextInt();

            if(mode == 2) {
                adminLogin();
            } else if(mode == 1) {
                userMenu();
            } else {
                System.out.println("Exiting system. Goodbye!");
                return;
            }
        }
    }

    // ---------------- USER MODE ----------------
    private static void userMenu() {
        while(true) {
            System.out.println("\n[1] Beverages\n[2] Snacks\n[3] Meals\n[4] Exit\n[5] Finish Ordering");
            System.out.print("Choose a category: ");
            int choice = sc.nextInt();

            switch(choice) {
                case 1 -> showMenu("Beverages");
                case 2 -> showMenu("Snacks");
                case 3 -> showMenu("Meals");
                case 4 -> { 
                    System.out.println("Returning to main menu...");
                    return; 
                }
                case 5 -> finishOrdering();
                default -> System.out.println("Invalid choice, please try again.");
            }
        }
    }

    // Displays items from a chosen category
    private static void showMenu(String category) {
        try (Connection conn = Database.connect()) {
            String query = "SELECT * FROM " + category;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            System.out.println("\n--- " + category.toUpperCase() + " ---");
            List<String> names = new ArrayList<>();
            List<Double> prices = new ArrayList<>();

            while(rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                System.out.println(id + ". " + name + " - P" + price);
                names.add(name);
                prices.add(price);
            }

            System.out.print("\nEnter item number to order (0 to go back): ");
            int order = sc.nextInt();
            if(order == 0) return;

            System.out.print("Enter quantity: ");
            int qty = sc.nextInt();

            String itemName = names.get(order - 1);
            double total = prices.get(order - 1) * qty;

            orderCount++;
            simulateOrderTimer(orderCount, itemName);

            // Save sa session lang muna, hindi agad sa database
            sessionOrders.add(new OrderItem(itemName, qty, total));

            System.out.println("Added to your cart: " + qty + " x " + itemName + " (P" + total + ")");
        } catch(Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Simulate countdown for preparing the order
    private static void simulateOrderTimer(int orderNumber, String itemName) {
        System.out.println("\nOrder #" + orderNumber + " (" + itemName + ") is being prepared...");
        for(int i = 3; i >= 1; i--) {
            try {
                System.out.println("Order #" + orderNumber + " ready in " + i + "...");
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Order #" + orderNumber + " is READY!\n");
    }

    // ---------------- FINISH ORDERING ----------------
    private static void finishOrdering() {
        if(sessionOrders.isEmpty()) {
            System.out.println("You haven't ordered anything yet!");
            return;
        }

        double grandTotal = 0;
        System.out.println("\n=== SESSION RECEIPT ===");
        for(OrderItem item : sessionOrders) {
            System.out.printf("%d x %s ........ P%.2f\n", item.qty, item.name, item.total);
            grandTotal += item.total;
            saveOrder(item.name, item.qty, item.total); // Save sa database
        }
        System.out.println("-----------------------------");
        System.out.println("GRAND TOTAL: P" + grandTotal);
        System.out.println("Thank you for ordering!\n");

        sessionOrders.clear(); // Reset session para sa susunod na order
    }

    // Save completed order to database
    private static void saveOrder(String item, int qty, double total) {
        try(Connection conn = Database.connect()) {
            String query = "INSERT INTO Orders (item_name, quantity, total, order_time) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, item);
            pstmt.setInt(2, qty);
            pstmt.setDouble(3, total);
            pstmt.setString(4, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println("Error saving order: " + e.getMessage());
        }
    }

    // ---------------- ADMIN MODE ----------------
    private static void adminLogin() {
        System.out.print("Enter admin password: ");
        String pass = sc.next();

        if(pass.equals("pogiako123")) { // Change password kung gusto
            showAllOrders();
        } else {
            System.out.println("Incorrect password! Returning to main menu...");
        }
    }

    // View all orders in database
    private static void showAllOrders() {
        try(Connection conn = Database.connect()) {
            String query = "SELECT * FROM Orders";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            System.out.println("\n=== ALL ORDERS (ADMIN VIEW) ===");
            while(rs.next()) {
                int id = rs.getInt("order_id");
                String item = rs.getString("item_name");
                int qty = rs.getInt("quantity");
                double total = rs.getDouble("total");
                String time = rs.getString("order_time");

                System.out.printf("Order #%d | %s x%d | ₱%.2f | %s\n", id, item, qty, total, time);
            }
        } catch(SQLException e) {
            System.out.println("Error retrieving orders: " + e.getMessage());
        }
    }
}
