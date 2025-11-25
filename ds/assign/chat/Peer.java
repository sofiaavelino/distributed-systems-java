package ds.assign.chat;

import poisson.*;
import java.io.*;
import java.util.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.logging.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class Message implements Comparable<Message>, Serializable {
	int ts;
	String payload;
	int peer;
    boolean ack;
	
	public Message (int ts, String payload, int peer, boolean ack) {
		this.ts = ts;
		this.payload = payload;
		this.peer = peer;
        this.ack = ack;
	}	
		
		
	@Override
	public int compareTo(Message m2) {
		if(this.ts != m2.ts) {
			return this.ts - m2.ts;
		}
		return this.peer - m2.peer;
	}
	
}

class LamportClock {

    public static void LCsend(String payload, boolean isAck) throws Exception {

        int ts = Peer.clock.incrementAndGet();
        Message m = new Message(ts, payload, Peer.port, isAck);


        for (int peer : Peer.mFromPeer.keySet()) {
            //System.out.println(peer + " " + payload);
            try {
                Socket messageSocket  = new Socket(InetAddress.getLoopbackAddress(), peer); 
                ObjectOutputStream out = new ObjectOutputStream(messageSocket.getOutputStream());

                out.writeObject(m);
                messageSocket.close();
            }
            catch (IOException e) {
                System.out.println("send failed");
                e.printStackTrace();
            }
        }

    }

    public static void LCreceive(Message m) throws Exception {
        int ts = m.ts;
        Peer.clock.set(Math.max(Peer.clock.get(),ts) + 1);
    }

    public static void LCdeliver(Message m) throws Exception {
        System.out.println(m.payload);
        Peer.clock.incrementAndGet();
    }
}

public class Peer {
    String host;
    Logger logger;
    static int port;

    static HashMap<Integer,Integer> mFromPeer = new HashMap<Integer,Integer>(); //contains no. of messages in message queue from each peer

	static ConcurrentSkipListSet<Message> messages = new ConcurrentSkipListSet<Message>(); //queue of messages received
	static AtomicInteger clock = new AtomicInteger(0); //keeps track of the current lamport clock value

    static ArrayList<String> wordList;


    public Peer(String hostname) {
        host = hostname;
        logger = Logger.getLogger("logfile");
        
        try {
            FileHandler handler = new FileHandler("./" + hostname + "_peer.log", true);
            logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();	
            handler.setFormatter(formatter);	
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //iniciates the peers with 0 messages in queue each
    private static void setPeers(String[] args) {
        mFromPeer.put(Integer.parseInt(args[0]),0);

        for (int i=2; i<args.length; i++) {
            mFromPeer.put(Integer.parseInt(args[i]),0);
        }
    }


    private static void setWords() throws Exception {
        File words = new File("ds/assign/chat/words.txt");
		Scanner readin = new Scanner(words);
        wordList = new ArrayList<String>();
        
        while (readin.hasNext()) {
			wordList.add(readin.nextLine());
	    }

        readin.close();
    }


    public static void main(String[] args) throws Exception {
        Peer peer = new Peer(args[1]);
        port = Integer.parseInt(args[0]);
	    System.out.printf("new peer @ host=%s\n", port);

        setPeers(args);
        setWords();

        //awaits instruction to start (given by Starter.java once all peers are running)
        ServerSocket server = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());
        Socket start = server.accept();
        server.close();

        new Thread(new Client(wordList)).start();

        Thread.sleep(500);

        new Thread(new Server(args[1], port, peer.logger)).start();

    }

}

class Server implements Runnable{
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

				new Thread(new TOMreceive(clientAddress, client, logger, port)).start();
			}
		    catch (Exception e) {
		      e.printStackTrace();
		    }
			
	}
    }
	catch (Exception e) {
		      e.printStackTrace();
		    }
	}

}

class TOMreceive implements Runnable {
    String       clientAddress;
    Socket 		 client;
    Logger       logger;
	int 		 port;
    
    public TOMreceive(String clientAddress, Socket client, Logger logger, int port) throws Exception {
        this.client   = client;
        this.clientAddress   = clientAddress;
        this.logger = logger;
        this.port = port;
    }


    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            Message m = (Message) in.readObject();

            client.close();

            LamportClock.LCreceive(m);

            if (!m.ack) {
                LamportClock.LCsend("ack", true);
            }

            Peer.messages.add(m);

            synchronized(Peer.mFromPeer) {
                Peer.mFromPeer.put(m.peer, Peer.mFromPeer.get(m.peer) + 1);
            }

            TOMdeliver();
        }
        catch (Exception e) {
		      e.printStackTrace();
        }
    }

    private static void TOMdeliver() {

        try {
			Thread.sleep(10000);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

        if (!Peer.mFromPeer.containsValue(0)) {
            synchronized (Peer.mFromPeer) {
                Message m = Peer.messages.pollFirst();
                int peer = m.peer;

                Peer.mFromPeer.put(m.peer, Peer.mFromPeer.get(m.peer) - 1);

                if (!m.ack) {
                    try {
                        LamportClock.LCdeliver(m);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
		    }
        }
    }
}


class Client implements Runnable {
    int LAMBDA = 60; //lambda value for poisson process
    int secondsPerMinute = 60; //conversion value
    int secondsToMs = 1000; //conversion value

    ArrayList<String> wordList;

    public Client (ArrayList<String> wordList) {
        this.wordList = wordList;
    }

    @Override
	public void run() {
        PoissonProcess pp = new PoissonProcess(LAMBDA, new Random(0));
		while (true) {
		  double t = pp.timeForNextEvent() * secondsPerMinute * secondsToMs;
		  try {
			  Thread.sleep((int) t);
		  }
          catch(InterruptedException e) {
			  System.out.println("Thread interrupted");
		  }
        String payload = generatePayload(wordList);

        try {
            LamportClock.LCsend(payload, false);
        }
        catch (Exception e) {
            System.out.println("error here");
            e.printStackTrace();
        }
        }
    }

    private String generatePayload(ArrayList<String> words) {
        
        Random generator = new Random();
		int offset = generator.nextInt(wordList.size());

        return wordList.get(offset);
    }
}

