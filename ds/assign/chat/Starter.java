package ds.assign.chat;

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

public class Starter {
    public static void main(String[] args) {
        try {
            for (int i=0; i<args.length; i++) {
                String server  = "localhost";
                Socket socket = new Socket(InetAddress.getByName(server), Integer.parseInt(args[i]));
                PrintWriter   out = new PrintWriter(socket.getOutputStream(), true); 
                /*
                * send command
                */
                out.println("start");
                out.flush();	    
                /*
                * close connection
                */
                socket.close();
                }
            System.out.println("All machines started");
        }
        catch(Exception e) {
		    e.printStackTrace();
	    }
    }
}