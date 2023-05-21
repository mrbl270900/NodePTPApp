package dk.stbn.p2peksperiment;


import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Global data
    private final int PORT = 4444;
    int clientNumber = 0;
    // UI-elements
    private Button startClient, submitIP;
    private TextView serverInfoTv, clientInfoTv;
    private EditText ipInputField;
    // Logging/status messages
    private String serverinfo = "SERVER LOG:";
    private String clientinfo = "CLIENT LOG: ";
    private String THIS_IP_ADDRESS = "";
    private String REMOTE_IP_ADDRESS = "";
    private Thread serverThread = new Thread(new MyServerThread());
    private Thread clientThread = new Thread(new MyClientThread());
    private String command = "getId";
    // Some state
    private String nodeCommand;
    private boolean ip_submitted = false;
    private boolean serverCarryOn = true;
    private String nodeIp;
    private boolean clientStarted = false;
    private String dataFromOtherNode;
    private Node node;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //UI boilerplate
        startClient = findViewById(R.id.button);
        serverInfoTv = findViewById(R.id.serveroutput);
        clientInfoTv = findViewById(R.id.clientoutput);
        submitIP = findViewById(R.id.sendclient);
        ipInputField = findViewById(R.id.clientmessagefield);

        //Setting click-listeners on buttons
        startClient.setOnClickListener(this);
        submitIP.setOnClickListener(this);

        //Setting some UI state
        ipInputField.setHint("Submit IP-address");
        startClient.setEnabled(false); //deactivates the button

        //Getting the IP address of the device
        THIS_IP_ADDRESS = getLocalIpAddress();
        sUpdate("This IP is " + THIS_IP_ADDRESS);

        //setting up a node
        List<String> nodesLeft = new ArrayList<>();
        nodesLeft.add(THIS_IP_ADDRESS);
        nodesLeft.add(THIS_IP_ADDRESS);
        nodesLeft.add(THIS_IP_ADDRESS);
        List<String> nodesRight = new ArrayList<>();
        nodesRight.add(THIS_IP_ADDRESS);
        nodesRight.add(THIS_IP_ADDRESS);
        nodesRight.add(THIS_IP_ADDRESS);
        node = new Node(THIS_IP_ADDRESS, nodesLeft, nodesRight);


        //Starting the server thread
        serverThread.start();
        serverinfo += "- - - SERVER STARTED - - -\n";

    }

    @Override
    public void onClick(View view) {

        if (view == startClient) {
            if (!clientStarted) {
                clientStarted = true;
                command = HandleApi.createHttpRequest("Get", "getId", "empty");
                System.out.println(command);
                clientThread.start();
                clientinfo += "- - - CLIENT STARTED - - - \n";
                startClient.setText("Resend");
            } else {
                if (!ipInputField.getText().toString().equals(REMOTE_IP_ADDRESS)) {
                    String newCommand = ipInputField.getText().toString();
                    String[] newCommandList = newCommand.split(",");
                    command = HandleApi.createHttpRequest(newCommandList[0], newCommandList[1], newCommandList[2]);
                } else {
                    command = HandleApi.createHttpRequest("Get", "getId", "empty");
                    System.out.println(command);
                }
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
            }
        } else if (view == submitIP) {
            if (!ip_submitted) {
                ip_submitted = true;
                REMOTE_IP_ADDRESS = ipInputField.getText().toString();
                startClient.setEnabled(true);
                submitIP.setEnabled(false);
            }
        }

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

        //Server update TexView
        private void sUpdate(String message) {
            //Run this code on UI-thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    serverinfo = message + "\n" + serverinfo;
                    serverInfoTv.setText(serverinfo);
                }
            });

        }

        //Client update TextView
        private void cUpdate(String message) {
            System.out.println(message);

            //Run this code on UI-thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    clientinfo = message + "\n" + clientinfo;
                    clientInfoTv.setText(clientinfo);
                }
            });
        }

    class MyServerThread implements Runnable {
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
                        sUpdate("SERVER: start listening..");
                        Socket clientSocket = serverSocket.accept();
                        sUpdate("SERVER connection accepted");
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

                            sUpdate("Client says: " + str);
                            System.out.println("client to server " + str);
                            //logic to handle things
                            if (input.path.equalsIgnoreCase("getId")) {
                                //run with getId
                                response = node.getId();
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
                        sUpdate(jsonString);
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
                sUpdate("SERVER: inputstream closed");
                client.shutdownOutput();
                sUpdate("SERVER: outputstream closed");
                client.close();
                sUpdate("SERVER: Client socket closed");
            } catch (IOException e) {
                sUpdate("oops!!");
                throw new RuntimeException(e);
            }
        }
    }

        class MyClientThread implements Runnable {
            @Override
            public void run() {

                try {
                    cUpdate("CLIENT: starting client socket ");
                    Socket connectionToServer = new Socket();
                    SocketAddress socketAddress = new InetSocketAddress(REMOTE_IP_ADDRESS, 4444);
                    connectionToServer.connect(socketAddress, 10000);
                    cUpdate("CLIENT: client connected ");

                    DataInputStream inClientStream = new DataInputStream(connectionToServer.getInputStream());
                    DataOutputStream outClientStream = new DataOutputStream(connectionToServer.getOutputStream());
                    String messageFromServer;
                    outClientStream.writeUTF(command);
                    outClientStream.flush();
                    cUpdate("I said:      " + command);
                    messageFromServer = inClientStream.readUTF();
                    Response response = HandleApi.readHttpResponse(messageFromServer);
                    cUpdate("Server says: " + response);
                    waitABit();
                    connectionToServer.shutdownInput();
                    cUpdate("CLIENT: closed inputstream");
                    connectionToServer.shutdownOutput();
                    cUpdate("CLIENT: closed outputstream");
                    connectionToServer.close();
                    cUpdate("CLIENT: closed socket");

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

            }//run()
        } //class MyClientThread
    }