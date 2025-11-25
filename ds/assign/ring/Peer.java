package ds.assign.ring;

import poisson.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.ThreadLocalRandom;

public class Peer {
    String host;
    Logger logger;
	static Queue<String> commandlist = new LinkedList<String>();

    public Peer(String hostname) {
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
		Peer peer = new Peer(args[1]);

		int port = Integer.parseInt(args[0]);
		System.out.printf("new peer @ host=%s\n", port);

		int server = Integer.parseInt(args[3]);
		int nextPeer = Integer.parseInt(args[2]);
		
		//start exchanging token and generating commands
		new Thread(new ContactServer(args[1], port, nextPeer, server, peer.logger)).start();
		new Thread(new OpGenerator()).start();
    }
}

class ContactServer implements Runnable {
    String       host;
    int          port;
    int          nextPeer;
	int serverport;
    ServerSocket server;
    Logger       logger;
    
    public ContactServer(String host, int port, int nextPeer, int serverport, Logger logger) throws Exception {
	this.host   = host;
	this.port   = port;
    this.nextPeer = nextPeer;
	this.serverport = serverport;
	this.logger = logger;
    server = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());
    }

    @Override
    public void run() {
	    while(true) {
			try {
				Socket client = server.accept(); //socket listening to receive token
				String clientAddress = client.getInetAddress().getHostAddress();

				BufferedReader inToken = new BufferedReader(new InputStreamReader(client.getInputStream()));    
				String token = inToken.readLine(); //token received

				client.close();

			}
			catch(Exception e) {
				e.printStackTrace();
			}    

			try {
				//send command requests to sever while holding token
				while (!Peer.commandlist.isEmpty()) {
					System.out.println("Token received \n"); //we only print this message when peer has commands for server so as to not overload

					Socket serversocket  = new Socket(InetAddress.getLoopbackAddress(), serverport); //create socket to server

					//send command
					PrintWriter out = new PrintWriter(serversocket.getOutputStream(), true);
					String c = Peer.commandlist.remove();
					out.println(c + " " + port);
					out.flush();	

					System.out.println("Command sent: " + c);

					//receive result
					BufferedReader in = new BufferedReader(new InputStreamReader(serversocket.getInputStream()));   
					String result = in.readLine();
					System.out.println("Raw server response: " + result);
					System.out.printf("Result = %f\n", Double.parseDouble(result));
					System.out.println();

					in.close();
					out.close();
					serversocket.close();
				}

			}
			catch(Exception e) {
				e.printStackTrace();
			}    

								
			try{
				//send token to next peer in ring
				Socket tokenSocket  = new Socket(InetAddress.getLoopbackAddress(), nextPeer);
				PrintWriter outToken = new PrintWriter(tokenSocket.getOutputStream(), true);
	
				outToken.println("token");
				outToken.flush();	    
			
				tokenSocket.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
		}
    }
}

class OpGenerator implements Runnable {

	int LAMBDA = 4; //lambda value for poisson process 
    int secondsPerMinute = 60; //conversion value
    int secondsToMs = 1000; //conversion value


	public OpGenerator () {
		;
	}

	@Override
	public void run() {
		PoissonProcess pp = new PoissonProcess(LAMBDA, new Random(0));
		while (true) {
				double t = pp.timeForNextEvent() * secondsPerMinute * secondsToMs;

				//choose request type from 4 possibilities
				Random rand = new Random();
				int request = rand.nextInt(4);

				//create command for server
				String command = "";
				if (request == 0) {command = "add " + ThreadLocalRandom.current().nextFloat() + " " + ThreadLocalRandom.current().nextFloat();}
				if (request == 1) {command = "sub " + ThreadLocalRandom.current().nextFloat() + " " + ThreadLocalRandom.current().nextFloat();}
				if (request == 2) {command = "mul " + ThreadLocalRandom.current().nextFloat() + " " + ThreadLocalRandom.current().nextFloat();}
				if (request == 3) {command = "div " + ThreadLocalRandom.current().nextFloat() + " " + ThreadLocalRandom.current().nextFloat();}

				//add command to queue
				Peer.commandlist.add(command);

				try {
					Thread.sleep((int) t);
				}
				catch(InterruptedException e) {
					System.out.println("Thread interrupted");
				}
		}
	}
}
