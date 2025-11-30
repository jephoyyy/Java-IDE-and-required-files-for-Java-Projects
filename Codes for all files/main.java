import java.sql.*; // Database related classes, functions
import java.util.*; // Utilities
import java.time.LocalDateTime; // Para makuha yung current date and time - Real Time
import java.time.format.DateTimeFormatter; // Format data into readable form

public class Main {
    private static Scanner sc = new Scanner(System.in); // Para makuha ang input mula sa users
    private static int currentCustomerID = -1; // Placeholder, para alam ng system saang number mag sstart ang incrementation.
    private static List<OrderItem> sessionOrders = new ArrayList<>(); // Array na unlimited ang size and no need na mag declare ng size, Ito yung nagsasave ng orders ng mga customers galing sa inputs nila.

    static class OrderItem { 
        String name; 
        int qty; 
        double total;
        
        OrderItem (String n, int q, double t) { // Statements, Parameter and Constructors
            name = n;
            qty = q;
            total = t;
        }
    }

    public static void main(String[] args){
        while(true){
            System.out.println("=== CANTEEN ORDERING SYSTEM ===");
            System.out.println("[1] User Mode  \n[2] Admin Mode  \n[3] Exit");
            int mode = readInt("Choose mode: ", 1, 3); // Para siguraduhing valid number sa range 1–3.
            if (mode==1) { // Start ng decision making sa menu
                createCustomer(); // Dito nagawa ng bagong customer at start ng incrementation ng ID
                userMenu(); 
            } else if (mode==2) { 
                adminLogin(); 
            } else { 
                System.out.println("Goodbye!"); 
                return; // Exit and terminate the Program
            }
        }
    }

    private static void createCustomer() { 
        try(Connection conn=Database.connect()) { // Temporary connection sa database, gagamitin lang sa current method
            PreparedStatement ps=conn.prepareStatement( // Method na nag bibigay ng way para makapag create ng sql commands sa main system
                "INSERT INTO Customers(customer_name, created_at) VALUES(?,?)", // "?" means empty slot sa mga values ng name at date.
                Statement.RETURN_GENERATED_KEYS); // Taga return ng ID na ginawa
            ps.setString(1,"Guest"); // Nagiinsert ng value sa unang slot
            ps.setString(2,LocalDateTime.now().toString()); // Date and Time dun sa pangalawang slot
            ps.executeUpdate(); // Taga save sa Database
            ResultSet rs=ps.getGeneratedKeys(); // Nakuha ng na nacreate bago mag order ng previous input
            if(rs.next()){ //Function for moving into next column
                currentCustomerID=rs.getInt(1); // Kaya naka 1 is gawa ID ang kukunin hindi yung created_at
                System.out.println("\nYour Customer ID: "+currentCustomerID); // Print if ano yung ID ni user
            }
        } catch(Exception e){ System.out.println("Error: "+e.getMessage()); } // Error handling if may errors sa database
    }

    private static void userMenu(){ // Method sa option 1 ng unang MENU
        while(true){
            System.out.println("\n--- USER MENU ---");
            System.out.println("[1] Beverages  \n[2] Snacks  \n[3] Meals  \n[4] Exit  \n[5] Finish Ordering");
            int choice = readInt("Choose: ",1,5); // Range ulit hangggang 1-5
            switch(choice){
                case 1 -> showMenu("Beverages");
                case 2 -> showMenu("Snacks");
                case 3 -> showMenu("Meals");
                case 4 -> { return; } // Exit
                case 5 -> finishOrder();
            }
        }
    }

    private static void showMenu(String cat){ // Method after mamili sa 1-3 option sa userMenu
        try(Connection conn=Database.connect()){ 
            Statement st=conn.createStatement();  // Gumagawa ng Statement object para makapag-run ng SQL query sa database
            ResultSet rs=st.executeQuery("SELECT * FROM "+cat); // I-run ang SQL query at ilagay ang result sa ResultSet para magamit sa program
            List<String> names = new ArrayList<>(); 
            List<Double> prices = new ArrayList<>();
            System.out.println("\n--- "+cat+" ---"); // Header
            while(rs.next()){
                System.out.printf("%d. %s - P%.2f\n", rs.getInt("id"), rs.getString("name"), rs.getDouble("price")); // Taga print ng names, id and price ng products
                names.add(rs.getString("name")); prices.add(rs.getDouble("price")); // I-store sa memory ang name at price ng product para magamit kapag pumili ang user
            }
            if(names.isEmpty()){ System.out.println("No items available."); return; } // if walang laman ang database

            int n = readInt("Choose item number (0 to go back): ",0,names.size()); // since varying ang size ng products, ito ay hindi naka fixed sa specific size
            if(n==0) return; // go back to previous menu
            int q = readInt("Quantity: ",1,1000); // maximum of 1000 ang pwedeng bilhin.
            sessionOrders.add(new OrderItem(names.get(n-1),q,prices.get(n-1)*q)); // sinasave ang newly added order sa session and this is temporary palang 
            simulateTimer(names.get(n-1)); // caller para sa method ng queue time based sa napiling name 
            System.out.println("Added to cart: "+q+" x "+names.get(n-1)); // all are n-1 cause ang inder ng array ay lagi nag sstart sa 0

        } catch(Exception e){ System.out.println("Error: "+e.getMessage()); } // Error handling ulit if may error sa buong method
    }

    private static void simulateTimer(String item){ // Queue Time/Timer para sa order ng user
        System.out.println("\nPreparing "+item+"...");
        for(int i=3;i>=1;i--){ // means 3 seconds ang time ng pag prepare
            System.out.println("Ready in "+i+"..."); // loop print
            try{ Thread.sleep(500); } catch(Exception ignored){} // 500 milliseconds ang counting kada print ng loop sa taas.
        }
        System.out.println("Order READY!\n");
    }

    private static void finishOrder(){ // For option 5
        if(sessionOrders.isEmpty()){ System.out.println("No orders yet!"); return;} // Check if may laman ang order
        double total=0;
        System.out.println("\n=== RECEIPT FOR CUSTOMER #"+currentCustomerID+" ==="); // Print header
        for(OrderItem o:sessionOrders){ // Gawa ng object na o for total
            System.out.printf("%d x %s .... P%.2f\n", o.qty,o.name,o.total); // Print overall nakuha and total
            total+=o.total; // add yung order mga nakuhang item at price sa buong total
            saveOrder(o); // method na nag sasave ng order sa database after clicking option 5
        }
        System.out.println("--------------------------");
        System.out.println("TOTAL: P" + total); // Total and Receipt ng user
        sessionOrders.clear(); // Para ma-reset yung ArrayList at hindi mag add yung next orders sa previous order
    }

    private static void saveOrder(OrderItem o){ // Method para msave ang order sa database and para maging possible ang option 5
        try(Connection conn=Database.connect()){ 
            PreparedStatement ps=conn.prepareStatement(
                "INSERT INTO Orders(item_name,quantity,total,order_time,customer_id) VALUES(?,?,?,?,?)"); // Ito yung magiging format pag nag view order ka sa admin view
            ps.setString(1,o.name); ps.setInt(2,o.qty); ps.setDouble(3,o.total);
            ps.setString(4,LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.setInt(5,currentCustomerID); 
            ps.executeUpdate(); // save sa database
        }catch(Exception e){ System.out.println("Error: "+e.getMessage()); }
    }

    private static void adminLogin(){ // method for option 1 sa pinaka first menu ng system
        System.out.print("\nEnter admin password: ");
        if(sc.next().equals("pogiako123")) // Dito pwede mag change ng password, and chinecheck if tama ba ang password na na-input
            adminMenu(); // Method para mashow ang admin menu after mag correct ng password
        else System.out.println("Wrong password.");
    }

    private static void adminMenu(){
        while(true){
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("[1] View ALL Orders  \n[2] View Orders by Customer ID  \n[3] Back");
            int choice = readInt("Choose: ",1,3);
            switch(choice){
                case 1 -> showOrders("ALL",0); // Filler value lang
                case 2 -> {
                    int custID = readInt("Customer ID: ",1,100000); // Pick customer id, ranging up to 1-100000
                    showOrders("CUSTOMER", custID);
                }
                case 3 -> { return; }
            }
        }
    }

    private static void showOrders(String type,int customerId){
        try(Connection conn=Database.connect()){
            PreparedStatement ps;
            if(type.equals("ALL")) ps=conn.prepareStatement("SELECT * FROM Orders"); //Generalized ang pag retrieve sa mga orders if mag pick ng option 1 sa menu ng admin
            else { ps=conn.prepareStatement("SELECT * FROM Orders WHERE customer_id=?"); ps.setInt(1,customerId); } // Isa isa based sa isang specific id

            ResultSet rs=ps.executeQuery(); // nagbabalik ng lahat ng rows na nakuha sa database.
            System.out.println(type.equals("ALL")?"\n=== ALL ORDERS ===":"\n=== Orders for Customer #"+customerId+" ===");

            boolean hasOrders = false; // False kasi hindi pa natin alam kung may makukuhang order sa database
            while(rs.next()){ // Naglilipat sa next row 
                hasOrders = true; // if meron laman print ang asa baba
                System.out.printf("Order #%d | %s x%d | ₱%.2f | %s | Customer #%d\n", // Print format sa view order sa all and specific id
                        rs.getInt("order_id"), rs.getString("item_name"), rs.getInt("quantity"), rs.getDouble("total"), rs.getString("order_time"), rs.getInt("customer_id"));
            }
            if(!hasOrders) System.out.println("No orders found."); // Check if null ang order
        }catch(Exception e){ System.out.println("Error: "+e.getMessage());} // Error handling
    }

    // Helper method to read integers with range validation
    private static int readInt(String prompt, int min, int max){
        int num;
        while(true){
            System.out.print(prompt); // Promt is yung mga "Choose", "Enter option" and etc
            if(!sc.hasNextInt()){ // Check if valid ang napiling option.
                System.out.println("Invalid input. Enter a number.");
                sc.next(); // nag sskip ng invalid input
                continue; // balik sa start ng while loop
            }
            num = sc.nextInt(); // Store ang valid int sa variable ng "num"
            if(num < min || num > max){ // statement na nag ccheck if asa range ba ang napiling int input
                System.out.println("Number out of range. Try again.");
                continue; // balik sa start ng while loop
            }
            break; // break pag nakakuha ng valid value
        }
        return num; // Ibinabalik ang number na valid at nasa range sa caller ng method.
    }
}
