package dk.stbn.p2peksperiment;


import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private RecyclerView postView;
    private String THIS_IP_ADDRESS = "";
    private String REMOTE_IP_ADDRESS = "";
    private Thread serverThread = new Thread(new MyServerThread());
    private String command = "getId";
    private boolean ip_submitted = false;
    private boolean serverCarryOn = true;
    private boolean serverStarted = false;
    private boolean serverRunning = false;
    private Network network;

//should only be one user is 2 in testing that is useres
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
        postView = findViewById(R.id.recyclerView);

        postView.setLayoutManager(new LinearLayoutManager(this));

        //Setting click-listeners on buttons
        startServer.setOnClickListener(this);
        submitIP.setOnClickListener(this);

        //Setting some UI state
        ipInputField.setHint("Input Network Ip");

        //Getting the IP address of the device
        THIS_IP_ADDRESS = getLocalIpAddress();
    }

    @Override
    public void onClick(View view) {

        if (view == startServer) {
            if(ip_submitted){
                REMOTE_IP_ADDRESS = "";
                startServer.setText("Start Server");
                network = new Network(THIS_IP_ADDRESS);
                submitIP.setText("Join network");
                ipInputField.setEnabled(true);
                ipInputField.setInputType(InputType.TYPE_CLASS_TEXT);
                ip_submitted = false;
                command = HandleApi.createHttpRequest("leaveNetwork", clientUser.getUsername());
                System.out.println(command);
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
            }
            else if (!serverStarted) {
                serverStarted = true;
                serverRunning = true;
                serverThread.start();

                //setteing up network and its test data
                serverUser = new User(THIS_IP_ADDRESS ,"server owner");
                network = new Network(THIS_IP_ADDRESS);

                network.addPost(new Post(serverUser.getUsername(), "dette er et test post 1", network.getPostList().size(), "dette er noget inhold"));
                network.addPost(new Post(serverUser.getUsername(), "dette er et test post 2", network.getPostList().size(), "dette er noget inhold"));
                network.addPost(new Post(serverUser.getUsername(), "dette er et test post 3", network.getPostList().size(), "dette er noget inhold"));

                startServer.setText("Stop Server");
                postView.setAdapter(new CustomAdapter(this, network.getPostList()));
                postView.addItemDecoration(new DividerItemDecoration(this,
                        LinearLayoutManager.VERTICAL));

            } else {
                serverRunning = false;
                serverStarted = false;
                serverCarryOn = false;
                network = new Network(THIS_IP_ADDRESS);
                startServer.setText("Start server");
                serverThread = new Thread(new MyServerThread());
                postView.setAdapter(new CustomAdapter(this, network.getPostList()));
                postView.addItemDecoration(new DividerItemDecoration(this,
                        LinearLayoutManager.VERTICAL));
            }
        } else if (view == submitIP) {
            if (!ip_submitted) {
                ip_submitted = true;
                clientUser = new User("1", "User01");
                REMOTE_IP_ADDRESS = ipInputField.getText().toString();
                command = HandleApi.createHttpRequest("newpeer", clientUser.getUsername());
                System.out.println(command);
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
                submitIP.setText("Opdate data");
                startServer.setText("End connection");
                ipInputField.setEnabled(false);
                ipInputField.setInputType(InputType.TYPE_NULL);
            }else{
                command = HandleApi.createHttpRequest("getdata", clientUser.getUsername());
                System.out.println(command);
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
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

    class MyServerThread extends Thread implements Runnable {
        @SuppressLint("SuspiciousIndentation")
        @Override
        public void run() {
            //Always be ready for next client
            try {
                ServerSocket serverSocket = new ServerSocket();
                while (serverRunning) {
                    serverSocket = new ServerSocket(4444);
                    //Always be ready for next client
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        clientNumber++;
                        new RemoteClient(clientSocket, clientNumber).start();

                    }//while listening for clients
                }
                serverSocket.close();
                } catch(IOException e){
                    throw new RuntimeException(e);
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