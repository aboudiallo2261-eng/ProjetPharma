package com.pharmacie;
import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pharmacie_vet_db?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false", "root", "");
            ResultSet rs = conn.createStatement().executeQuery("DESCRIBE ajustements_stock");
            while(rs.next()) {
                System.out.println(rs.getString("Field"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
