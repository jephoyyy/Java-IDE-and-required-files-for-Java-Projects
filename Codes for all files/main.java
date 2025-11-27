import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    private static Scanner sc = new Scanner(System.in);
    private static int currentCustomerID = -1;
    private static List<OrderItem> sessionOrders = new ArrayList<>();

    static class OrderItem { 
        String name; int qty; double total;
        OrderItem(String n,int q,double t){name=n;qty=q;total=t;}
    }

    public static void main(String[] args){
        while(true){
            System.out.println("=== CANTEEN ORDERING SYSTEM ===");
            System.out.println("[1] User Mode  \n[2] Admin Mode  \n[3] Exit");
            int mode = readInt("Choose mode: ", 1, 3);
            if(mode==1){ createCustomer(); userMenu(); }
            else if(mode==2){ adminLogin(); }
            else{ System.out.println("Goodbye!"); return; }
        }
    }

    private static void createCustomer(){
        try(Connection conn=Database.connect()){
            PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO Customers(customer_name,created_at) VALUES(?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,"Guest"); 
            ps.setString(2,LocalDateTime.now().toString());
            ps.executeUpdate();
            ResultSet rs=ps.getGeneratedKeys();
            if(rs.next()){
                currentCustomerID=rs.getInt(1); 
                System.out.println("\nYour Customer ID: "+currentCustomerID);
            }
        } catch(Exception e){ System.out.println("Error: "+e.getMessage()); }
    }

    private static void userMenu(){
        while(true){
            System.out.println("\n--- USER MENU ---");
            System.out.println("[1] Beverages  \n[2] Snacks  \n[3] Meals  \n[4] Exit  \n[5] Finish Ordering");
            int choice = readInt("Choose: ",1,5);
            switch(choice){
                case 1 -> showMenu("Beverages");
                case 2 -> showMenu("Snacks");
                case 3 -> showMenu("Meals");
                case 4 -> { return; }
                case 5 -> finishOrder();
            }
        }
    }

    private static void showMenu(String cat){
        try(Connection conn=Database.connect()){
            Statement st=conn.createStatement(); 
            ResultSet rs=st.executeQuery("SELECT * FROM "+cat);
            List<String> names = new ArrayList<>();
            List<Double> prices = new ArrayList<>();
            System.out.println("\n--- "+cat+" ---");
            while(rs.next()){
                System.out.printf("%d. %s - P%.2f\n", rs.getInt("id"), rs.getString("name"), rs.getDouble("price"));
                names.add(rs.getString("name")); prices.add(rs.getDouble("price"));
            }
            if(names.isEmpty()){ System.out.println("No items available."); return; }

            int n = readInt("Choose item number (0 to go back): ",0,names.size());
            if(n==0) return;
            int q = readInt("Quantity: ",1,1000);
            sessionOrders.add(new OrderItem(names.get(n-1),q,prices.get(n-1)*q));
            simulateTimer(names.get(n-1));
            System.out.println("Added to cart: "+q+" x "+names.get(n-1));

        } catch(Exception e){ System.out.println("Error: "+e.getMessage()); }
    }

    private static void simulateTimer(String item){
        System.out.println("\nPreparing "+item+"...");
        for(int i=3;i>=1;i--){
            System.out.println("Ready in "+i+"...");
            try{ Thread.sleep(500); } catch(Exception ignored){}
        }
        System.out.println("Order READY!\n");
    }

    private static void finishOrder(){
        if(sessionOrders.isEmpty()){ System.out.println("No orders yet!"); return;}
        double total=0;
        System.out.println("\n=== RECEIPT FOR CUSTOMER #"+currentCustomerID+" ===");
        for(OrderItem o:sessionOrders){
            System.out.printf("%d x %s .... P%.2f\n", o.qty,o.name,o.total);
            total+=o.total;
            saveOrder(o);
        }
        System.out.println("--------------------------");
        System.out.println("TOTAL: P"+total);
        sessionOrders.clear();
    }

    private static void saveOrder(OrderItem o){
        try(Connection conn=Database.connect()){
            PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO Orders(item_name,quantity,total,order_time,customer_id) VALUES(?,?,?,?,?)");
            ps.setString(1,o.name); ps.setInt(2,o.qty); ps.setDouble(3,o.total);
            ps.setString(4,LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.setInt(5,currentCustomerID); 
            ps.executeUpdate();
        }catch(Exception e){ System.out.println("Error: "+e.getMessage()); }
    }

    private static void adminLogin(){
        System.out.print("\nEnter admin password: ");
        if(sc.next().equals("pogiako123")) adminMenu();
        else System.out.println("Wrong password.");
    }

    private static void adminMenu(){
        while(true){
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("[1] View ALL Orders  \n[2] View Orders by Customer ID  \n[3] Back");
            int choice = readInt("Choose: ",1,3);
            switch(choice){
                case 1 -> showOrders("ALL",0);
                case 2 -> {
                    int custID = readInt("Customer ID: ",1,100000);
                    showOrders("CUSTOMER", custID);
                }
                case 3 -> { return; }
            }
        }
    }

    private static void showOrders(String type,int customerId){
        try(Connection conn=Database.connect()){
            PreparedStatement ps;
            if(type.equals("ALL")) ps=conn.prepareStatement("SELECT * FROM Orders");
            else { ps=conn.prepareStatement("SELECT * FROM Orders WHERE customer_id=?"); ps.setInt(1,customerId); }

            ResultSet rs=ps.executeQuery();
            System.out.println(type.equals("ALL")?"\n=== ALL ORDERS ===":"\n=== Orders for Customer #"+customerId+" ===");

            boolean hasOrders = false;
            while(rs.next()){
                hasOrders = true;
                System.out.printf("Order #%d | %s x%d | ₱%.2f | %s | Customer #%d\n",
                        rs.getInt("order_id"),rs.getString("item_name"),
                        rs.getInt("quantity"),rs.getDouble("total"),
                        rs.getString("order_time"),rs.getInt("customer_id"));
            }
            if(!hasOrders) System.out.println("No orders found.");
        }catch(Exception e){ System.out.println("Error: "+e.getMessage());}
    }

    // Helper method to read integers with range validation
    private static int readInt(String prompt, int min, int max){
        int num;
        while(true){
            System.out.print(prompt);
            if(!sc.hasNextInt()){
                System.out.println("Invalid input. Enter a number.");
                sc.next();
                continue;
            }
            num = sc.nextInt();
            if(num < min || num > max){
                System.out.println("Number out of range. Try again.");
                continue;
            }
            break;
        }
        return num;
    }
}
