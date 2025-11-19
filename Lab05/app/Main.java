import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

public class Main {

    private static String masterUrl;
    private static String replicaUrl;
    private static String user;
    private static String pass;

    public static void main(String[] args) throws Exception {

        loadConfig();

        createCategory("NewCategory");
        readCategories();
        updateCategory(1, "UpdatedName");
        deleteCategory(2);
        readCategoriesFromReplica();
    }

    private static void loadConfig() throws Exception {
        Properties p = new Properties();
        p.load(new FileInputStream("config.properties"));

        masterUrl = p.getProperty("master.url");
        replicaUrl = p.getProperty("replica.url");
        user = p.getProperty("db.user");
        pass = p.getProperty("db.pass");
    }

    private static void createCategory(String name) {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            PreparedStatement ps =
                conn.prepareStatement("INSERT INTO categories (name) VALUES (?)");
            ps.setString(1, name);
            ps.executeUpdate();
            System.out.println("CREATE OK");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void readCategories() {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            ResultSet rs = conn.prepareStatement("SELECT * FROM categories").executeQuery();
            System.out.println("READ from MASTER:");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " + rs.getString("name"));
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private static void updateCategory(int id, String newName) {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            PreparedStatement ps =
                conn.prepareStatement("UPDATE categories SET name=? WHERE id=?");
            ps.setString(1, newName);
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("UPDATE OK");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void deleteCategory(int id) {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            PreparedStatement ps =
                conn.prepareStatement("DELETE FROM categories WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("DELETE OK");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void readCategoriesFromReplica() {
        try (Connection conn = DriverManager.getConnection(replicaUrl, user, pass)) {
            ResultSet rs = conn.prepareStatement("SELECT * FROM categories").executeQuery();
            System.out.println("READ from REPLICA:");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " + rs.getString("name"));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
