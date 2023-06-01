package dk.stbn.p2peksperiment;


import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    int clientNumber = 0;
    // UI-elements
    private Button startServer, submitIP;
    private EditText ipInputField;
    // Logging/status messages
    private String serverinfo = "SERVER LOG:";
    private String clientinfo = "CLIENT LOG: ";
    private String THIS_IP_ADDRESS = "";
    private String REMOTE_IP_ADDRESS = "";
    private final Thread serverThread = new Thread(new MyServerThread());
    private final Thread clientThread = new Thread(new MyClientThread());
    private String command = "getId";
    private boolean ip_submitted = false;
    private boolean serverCarryOn = true;
    private boolean serverStarted = false;
    private Network network;
    private User serverUser;
    private User clientUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //UI boilerplate
        startServer = findViewById(R.id.button);
        submitIP = findViewById(R.id.sendclient);
        ipInputField = findViewById(R.id.clientmessagefield);

        //Setting click-listeners on buttons
        startServer.setOnClickListener(this);
        submitIP.setOnClickListener(this);

        //Setting some UI state
        ipInputField.setHint("Submit IP-address");
        startServer.setEnabled(false); //deactivates the button

        //Getting the IP address of the device
        THIS_IP_ADDRESS = getLocalIpAddress();

        //setteing up network and its test data
        serverUser = new User(THIS_IP_ADDRESS ,"server owner");
        network = new Network(THIS_IP_ADDRESS);

        network.addPost(new Post(serverUser.getUsername(), "dette er et test post 1", network.getPostList().size()));
        network.addPost(new Post(serverUser.getUsername(), "dette er et test post 2", network.getPostList().size()));
        network.addPost(new Post(serverUser.getUsername(), "dette er et test post 3", network.getPostList().size()));

        clientUser = new User("1", "User01");
    }

    @Override
    public void onClick(View view) {

        if (view == startServer) {
            if (!serverStarted) {
                serverStarted = true;
                serverThread.start();
                serverinfo += "- - - SERVER STARTED - - -\n";
            } else {
                serverStarted = false;
                serverThread.stop();
            }
        } else if (view == submitIP) {
            if (!ip_submitted) {
                ip_submitted = true;
                REMOTE_IP_ADDRESS = ipInputField.getText().toString();
                command = HandleApi.createHttpRequest("newpeer", "mads");
                System.out.println(command);
                clientThread.start();
                clientinfo += "- - - CLIENT STARTED - - - \n";
                startServer.setText("Resend");
            }
        }
        //code to send command
        /*if (!ipInputField.getText().toString().equals(REMOTE_IP_ADDRESS)) {
            String newCommand = ipInputField.getText().toString();
            String[] newCommandList = newCommand.split(",");
            command = HandleApi.createHttpRequest(newCommandList[0], newCommandList[1]);
        } else {
            command = HandleApi.createHttpRequest("newpeer", "mads");
            System.out.println(command);
        }
        Thread clientThread = new Thread(new MyClientThread());
        clientThread.start();*/

    }//onclick

        // !!! Returns 0.0.0.0 on emulator
        //Modified from https://www.tutorialspoint.com/sending-and-receiving-data-with-sockets-in-android
        private String getLocalIpAddress() {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            assert wifiManager != null;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipInt = wifiInfo.getIpAddress();
            String address = null;
            try {
                address = InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            return address;
        }

        //Wait by setting the thread to sleep for 1,5 seconds
        private void waitABit() {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    class MyServerThread extends Thread implements Runnable {
        @SuppressLint("SuspiciousIndentation")
        @Override
        public void run() {
            //Always be ready for next client
            boolean running = true;
            while (true) {
                try {
                    ServerSocket serverSocket = new ServerSocket(4444);

                    //Always be ready for next client
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        clientNumber++;
                        new RemoteClient(clientSocket, clientNumber).start();

                    }//while listening for clients

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


        //The below two methods are for updating UI-elements on the main thread

    class RemoteClient extends Thread {
        private final Socket client;
        private int number;

        public RemoteClient(Socket clientSocket, int number) {
            this.client = clientSocket;
            this.number = number;
        }

        public void run() {
            try {
                DataInputStream inNodeStream = new DataInputStream(client.getInputStream());
                DataOutputStream outNodeStream = new DataOutputStream(client.getOutputStream());
                String str;
                String response = "ok";
                String status = "200 ok";
                serverCarryOn = true;
                //Start conversation
                while (serverCarryOn) {
                    try {
                        str = (String) inNodeStream.readUTF();
                        try {
                            Request input = HandleApi.readHttpRequest(str);

                            System.out.println("client to server " + str);
                            //logic to handle things
                            if (input.method.equalsIgnoreCase("newpeer")) {
                                //run with newpeer
                                String peerIp = client.getRemoteSocketAddress().toString();
                                network.addPeer(peerIp.substring(1, peerIp.length()-6), input.body);
                                response = "peer added";
                                status = "200 ok";
                            } else {
                                status = "400 bad rec";
                                response = "Fail";
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            System.out.println("server fail on command");
                            status = "400 bad rec";
                            response = "Fail";
                        }

                        String jsonString = HandleApi.createHttpResponse(response, status);
                        outNodeStream.writeUTF(jsonString);
                        outNodeStream.flush();
                        waitABit();
                        serverCarryOn = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }//serverCarryOn loop

                //Closing everything down
                client.shutdownInput();
                client.shutdownOutput();
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

        class MyClientThread implements Runnable {
            @Override
            public void run() {

                try {
                    Socket connectionToServer = new Socket();
                    SocketAddress socketAddress = new InetSocketAddress(REMOTE_IP_ADDRESS, 4444);
                    connectionToServer.connect(socketAddress, 10000);

                    DataInputStream inClientStream = new DataInputStream(connectionToServer.getInputStream());
                    DataOutputStream outClientStream = new DataOutputStream(connectionToServer.getOutputStream());
                    String messageFromServer;
                    outClientStream.writeUTF(command);
                    outClientStream.flush();
                    messageFromServer = inClientStream.readUTF();
                    Response response = HandleApi.readHttpResponse(messageFromServer);
                    waitABit();
                    connectionToServer.shutdownInput();
                    connectionToServer.shutdownOutput();
                    connectionToServer.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

            }//run()
        } //class MyClientThread
    }