package dk.stbn.p2peksperiment;


import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private Button startServer, submitIP, addPost;
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
    private User user;

    public static void runClientCommand(String newCommand, String ip){
        try {
            Socket connectionToServer = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(ip, 4444);
            connectionToServer.connect(socketAddress, 10000);

            DataInputStream inClientStream = new DataInputStream(connectionToServer.getInputStream());
            DataOutputStream outClientStream = new DataOutputStream(connectionToServer.getOutputStream());
            String messageFromServer;
            System.out.println(newCommand);
            outClientStream.writeUTF(newCommand);
            outClientStream.flush();
            messageFromServer = inClientStream.readUTF();
            Response response = HandleApi.readHttpResponse(messageFromServer);
            System.out.println(response);
            connectionToServer.shutdownInput();
            connectionToServer.shutdownOutput();
            connectionToServer.close();
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public void updatePosts(){
        postView.setAdapter(new CustomAdapter(this, network, user));
        postView.addItemDecoration(new DividerItemDecoration(this,
                LinearLayoutManager.VERTICAL));
    }

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
        addPost = findViewById(R.id.AddPostButton);

        postView.setLayoutManager(new LinearLayoutManager(this));

        DefaultItemAnimator animator = (DefaultItemAnimator) postView.getItemAnimator();
        animator.setSupportsChangeAnimations(false);

        //Setting click-listeners on buttons
        startServer.setOnClickListener(this);
        submitIP.setOnClickListener(this);
        addPost.setOnClickListener(this);

        addPost.setEnabled(false);
        addPost.setVisibility(View.INVISIBLE);

        //Setting some UI state
        ipInputField.setHint("Input Username");
        submitIP.setText("Set Username");

        //Getting the IP address of the device
        THIS_IP_ADDRESS = getLocalIpAddress();

        startServer.setEnabled(false);

        network = new Network(THIS_IP_ADDRESS);

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
                command = HandleApi.createHttpRequest("leaveNetwork", user.getUsername());
                System.out.println(command);
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();

                ipInputField.setText("");
                ipInputField.setEnabled(true);
            }
            else if (!serverStarted) {
                serverStarted = true;
                serverRunning = true;
                serverThread.start();

                addPost.setEnabled(true);
                addPost.setVisibility(View.VISIBLE);

                submitIP.setEnabled(false);
                submitIP.setVisibility(View.INVISIBLE);

                ipInputField.setText(THIS_IP_ADDRESS);
                ipInputField.setEnabled(false);


                //setteing up network and its test data
                network = new Network(THIS_IP_ADDRESS);

                network.addPost(new Post(user.getUsername(), "dette er et test post 1", network.getPostList().size(), "dette er noget inhold"));
                network.addPost(new Post(user.getUsername(), "dette er et test post 2", network.getPostList().size(), "dette er noget inhold"));
                network.addPost(new Post(user.getUsername(), "dette er et test post 3", network.getPostList().size(), "dette er noget inhold"));

                network.getPostList().get(0).addComment("TestBruger", "god test commentar");
                network.getPostList().get(0).addComment("TestBruger2", "god test commentar2");
                network.getPostList().get(1).addComment("TestBruger", "god test commentar");
                network.getPostList().get(0).addLike("TestBruger");

                startServer.setText("Stop Server");
                postView.setAdapter(new CustomAdapter(this, network, user));
                postView.addItemDecoration(new DividerItemDecoration(this,
                        LinearLayoutManager.VERTICAL));

            } else {
                submitIP.setEnabled(true);
                submitIP.setVisibility(View.VISIBLE);

                addPost.setEnabled(false);
                addPost.setVisibility(View.INVISIBLE);

                ipInputField.setText("");
                ipInputField.setEnabled(true);

                serverRunning = false;
                serverStarted = false;
                serverCarryOn = false;
                network = new Network(THIS_IP_ADDRESS);
                startServer.setText("Start server");
                serverThread = new Thread(new MyServerThread());
                postView.setAdapter(new CustomAdapter(this, network, user));
                postView.addItemDecoration(new DividerItemDecoration(this,
                        LinearLayoutManager.VERTICAL));
            }
        } else if (view == submitIP) {
            if(user == null){
                user = new User(THIS_IP_ADDRESS ,ipInputField.getText().toString());
                ipInputField.setText("");
                ipInputField.setHint("Input Network Ip");
                submitIP.setText("Join Network");
                startServer.setEnabled(true);
                ipInputField.clearFocus();
                InputMethodManager imm = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            }else if (!ip_submitted) {
                ip_submitted = true;
                REMOTE_IP_ADDRESS = ipInputField.getText().toString();

                network = new Network(REMOTE_IP_ADDRESS);

                ipInputField.setText(REMOTE_IP_ADDRESS);
                ipInputField.setEnabled(false);

                command = HandleApi.createHttpRequest("newpeer", user.getUsername());
                System.out.println(command);
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
                submitIP.setText("Opdate data");
                startServer.setText("End connection");
                ipInputField.setEnabled(false);
                ipInputField.setInputType(InputType.TYPE_NULL);
            }else{
                command = HandleApi.createHttpRequest("getdata", user.getUsername());
                System.out.println(command);
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
            }
        } else if(view == addPost) {
            //addpost logic
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
                            }else if(input.method.equalsIgnoreCase("getdata")){
                                String out = network.getPostListString();
                                System.out.println(out);
                                response = out;
                                status = "200 ok";
                            }else if(input.method.equalsIgnoreCase("newdata")){
                                network.setPostList(network.getPostListFromString(input.body));
                                System.out.println(network.getPostList());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updatePosts();
                                    }
                                });
                            }else if(input.method.equalsIgnoreCase("leavenetwork")){
                                String peerIp = client.getRemoteSocketAddress().toString();
                                network.removePeer(peerIp.substring(1, peerIp.length()-6), input.body);
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

                    if(HandleApi.readHttpRequest(command).method.equalsIgnoreCase("getdata")){
                        System.out.println(response.body);
                        network.setPostList(network.getPostListFromString(response.body));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updatePosts();
                            }
                        });
                    }

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