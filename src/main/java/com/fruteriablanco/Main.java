package com.fruteriablanco;

import com.fruteriablanco.util.Conexion;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        try (Connection c = Conexion.getConnection()) {
            System.out.println("Conexion exitosa: " + c.getMetaData().getURL());
        }
    }
}