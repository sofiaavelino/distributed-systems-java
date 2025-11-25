package ds.assign.entropy;

import poisson.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.io.File; 
import java.io.FileNotFoundException; 
import java.util.logging.*;
import java.util.concurrent.ThreadLocalRandom;

class WordGenerator implements Runnable {
    int LAMBDA = 6; //lambda value for poisson process ~ 1 generation per 10 seconds
    int secondsPerMinute = 60; //conversion value
    int secondsToMs = 1000; //conversion value

    @Override
	public void run() {

		PoissonProcess pp = new PoissonProcess(LAMBDA, new Random(0));

		while (true) {

            double t = pp.timeForNextEvent() * secondsPerMinute * secondsToMs;

            Random generator = new Random();
            int offset = (int) (generator.nextDouble() * Peer.wordLookup.size()); //generate offset for word lookup

            String data = Peer.wordLookup.get(offset);
            
            Peer.wordList.add(data);

            System.out.println("Current list: " + Peer.wordList.toString() + "\n");

            try {
                Thread.sleep((int) t);
            }
            catch(InterruptedException e) {
                System.out.println("Thread interrupted");
            }

		}
	}
}

public class Peer {
    String host;
    Logger logger;

    static int[] peerList; //keeps list of neighbout peers
    static ArrayList<String> wordLookup; //keeps all the possible words for generation
	static ArrayList<String> wordList = new ArrayList<String>(); //keeps current list of words

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

    public static void getWords() throws Exception {
        File words = new File("ds/assign/entropy/words.txt");
        Scanner readin = new Scanner(words);

        wordLookup = new ArrayList<String>();
        while (readin.hasNextLine()) {
            wordLookup.add(readin.nextLine());
        }

        readin.close();
    }

    public static void setPeers(String[] args) {
        peerList = new int[args.length-2];

        //adding all neighbour peers to list
        System.out.println("Neighbour peers:");
        for (int i=2; i<args.length; i++) {
            peerList[i-2] = Integer.parseInt(args[i]);
            System.out.println(peerList[i-2]);
        }
        System.out.println("\n");
    }
    
    public static void main(String[] args) throws Exception {

        Peer peer = new Peer(args[1]);

        int port = Integer.parseInt(args[0]);
        System.out.printf("new peer @ host=%s\n", port);

        setPeers(args); //add neighbour peers to list
        getWords(); //save words for generation in list

        //waiting to receive ¨go" message from Starter when all machines are ready
        ServerSocket server = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());
        Socket start = server.accept();
        server.close();

        new Thread(new Server(args[1], port, peer.logger)).start();
        new Thread(new WordGenerator()).start();

        //gives time for word generation to start before requesting push-pull operations
        Thread.sleep(1000);
        new Thread(new Client(port, peer.logger)).start();
    }
}

class Server implements Runnable {
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    
    public Server(String host, int port, Logger logger) throws Exception {
	this.host   = host;
	this.port   = port;
	this.logger = logger;
    server = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());
    }

    @Override
    public void run() {
	try {
	    while(true) {
		try {
		    Socket client = server.accept();
		    String clientAddress = client.getInetAddress().getHostAddress();

		    new Thread(new PushPull(client, 0, logger, port)).start(); //thread to handle push-pull request

		}catch(Exception e) {
		    e.printStackTrace();
		}    
	}
	} catch (Exception e) {
	     e.printStackTrace();
	}
    }
}


class Client implements Runnable {
    int port;
    Logger logger;

    int LAMBDA = 1; //lambda value for poisson process
    int secondsPerMinute = 60; //conversion value
    int secondsToMs = 1000; //conversion value

	public Client (int port, Logger logger) {
        this.port = port;
        this.logger = logger;
	}

	@Override
	public void run() {

        try {
			Thread.sleep(port%100 * 500); //just to try to not get all push-pull requests at once
		}
		catch(Exception e) {
			e.printStackTrace();
		}

        PoissonProcess pp = new PoissonProcess(LAMBDA, new Random(0));
		while (true) {
				double t = pp.timeForNextEvent() * secondsPerMinute * secondsToMs;

                Random rand = new Random();
                int offset = rand.nextInt(Peer.peerList.length); //randomly choose peer to send push-pull request to
                int peer = Peer.peerList[offset];

                new Thread(new PushPull(null, peer, logger, port)).start(); //generate push-pull request

                try {
                    Thread.sleep((int) t);
                }
                catch(InterruptedException e) {
                    System.out.println("Thread interrupted");
                }
		}
	}
}

class PushPull implements Runnable {
    Socket client;
    int peer;
    static Logger logger;
    static int port;

	public PushPull (Socket client, int peer, Logger logger, int port) {
        this.client = client;
        this.peer = peer;
        this.logger = logger;
        this.port = port;
	}

	@Override
	public void run() {
        if (peer!=0) {
            sendRequest(peer);
        }
        else {
            receiveRequest(client);
        }
	}

    private static void receiveRequest(Socket client) {
        try {
            String clientAddress = client.getInetAddress().getHostAddress();

            //receive push-pull request + list
            ObjectInputStream objIn = new ObjectInputStream(client.getInputStream());  
            ArrayList<String> incomingList = (ArrayList<String>) objIn.readObject();

            int neighbour = Integer.parseInt(incomingList.get(incomingList.size() - 1));
            incomingList.remove(incomingList.get(incomingList.size() - 1));

            System.out.println("Incoming push-pull request from " + neighbour); //push-pull request arrives when other peer sends their list

            //send back own list
            ObjectOutputStream objOut = new ObjectOutputStream(client.getOutputStream());
            objOut.writeObject(Peer.wordList);
            objOut.flush();

            //join the lists together
            PushPull.joinLists(incomingList, neighbour); 
        
            client.close();
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendRequest(int peer) {
        try {
            Socket pushPullSocket  = new Socket(InetAddress.getLoopbackAddress(), peer); //create socket to peer for push-pull request

            Peer.wordList.add(port + ""); //adding port to list to let receiver know who request was sent from

            //send push-pull request
            ObjectOutputStream objOut = new ObjectOutputStream(pushPullSocket.getOutputStream());
            objOut.writeObject(Peer.wordList);
            objOut.flush();

            Peer.wordList.remove(port + "");

            System.out.println("Push-pull request sent to " + peer + "\n");

            //receiving list from peer
            ObjectInputStream objIn = new ObjectInputStream(pushPullSocket.getInputStream());
            ArrayList<String> incomingList = (ArrayList<String>) objIn.readObject();

            System.out.println("List received from " + peer);

            joinLists(incomingList, peer);

            pushPullSocket.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void joinLists(ArrayList<String> incomingList, int peer) {
        synchronized(Peer.wordList) {
            int countChanges = Peer.wordList.size();

            incomingList.forEach(e -> {
                if(!Peer.wordList.contains(e)) {
                    Peer.wordList.add(e);
                }
            });

            countChanges = Peer.wordList.size() - countChanges;

            if (countChanges != 0) {
                System.out.println("Current list after sync with " + peer + " = " + Peer.wordList.toString() + "\n"); 
            }
            else {
                System.out.println("No changes to commit \n"); 
            }
        }
    }
}