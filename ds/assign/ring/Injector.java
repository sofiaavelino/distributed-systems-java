package ds.assign.ring;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Injector {
    public static void main(String[] args) {
        System.out.println("injection started");
        try {
            String server  = "localhost";
            Socket socket = new Socket(InetAddress.getByName(server), Integer.parseInt(args[0]));
            PrintWriter   out = new PrintWriter(socket.getOutputStream(), true); 
            /*
            * send command
            */
            out.println("token");
            out.flush();	    
            /*
            * close connection
            */
            socket.close();
        }
        catch(Exception e) {
		    e.printStackTrace();
	    }
		System.out.println("initial token sent");
    }
}