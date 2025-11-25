package ds.assign.ring;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.ThreadLocalRandom;

public class Server {
    String host;
    Logger logger;

    public Server(String hostname) {
		host   = hostname;
		logger = Logger.getLogger("logfile");

		try {
			FileHandler handler = new FileHandler("./" + hostname + "_peer.log", true);
			logger.addHandler(handler);
			SimpleFormatter formatter = new SimpleFormatter();	
			handler.setFormatter(formatter);	
		} catch ( Exception e ) {
			e.printStackTrace();
		}
    }
    
    public static void main(String[] args) throws Exception {
		Server peer = new Server(args[1]);
		System.out.printf("new peer @ host=%s\n", args[0]);

		//start thread that listens for incoming requests
		new Thread(new ReceiveRequest(args[1], Integer.parseInt(args[0]), peer.logger)).start();
    }
}

class ReceiveRequest implements Runnable {
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    
    public ReceiveRequest(String host, int port, Logger logger) throws Exception {
		this.host   = host;
		this.port   = port;
		this.logger = logger;
		server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

    @Override
    public void run() {
	    while(true) {
			try {
				//wait for request from peer
				Socket client = server.accept();
				String clientAddress = client.getInetAddress().getHostAddress();

				//when request is received solve request
				new Thread(new SolveRequest(clientAddress, client, logger)).start();
			}
			catch(Exception e) {
				e.printStackTrace();
			}    
	    }
    }
}

class SolveRequest implements Runnable {
    String clientAddress;
    Socket clientSocket;
    Logger logger;

    public SolveRequest(String clientAddress, Socket clientSocket, Logger logger) {
	this.clientAddress = clientAddress;
	this.clientSocket  = clientSocket;
	this.logger        = logger;
    }

    @Override
    public void run() {

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));    
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
		
			//receive command
			String command;
			command = in.readLine();
			
			//translate command into values and actions
			Scanner sc = new Scanner(command);
			String  op = sc.next();	    
			double  x  = Double.parseDouble(sc.next());
			double  y  = Double.parseDouble(sc.next());
			int port = Integer.parseInt(sc.next());
			double  result = 0.0; 

			logger.info("server: message from host " + port + "[command = " + command + "]");

			//get result for request
			switch(op) {
				case "add": result = x + y; break;
				case "sub": result = x - y; break;
				case "mul": result = x * y; break;
				case "div": result = x / y; break;
			}  
			
			//send result
			out.println(String.valueOf(result));
			out.flush();
			
			System.out.println("result sent \n");
			clientSocket.close();
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
    }
}