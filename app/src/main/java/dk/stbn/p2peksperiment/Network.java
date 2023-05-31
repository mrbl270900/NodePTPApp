package dk.stbn.p2peksperiment;

import java.util.ArrayList;
import java.util.List;

public class Network {
    private List<Post> postList = new ArrayList<Post>();

    private List<User> peerList = new ArrayList<User>();

    private String networkCode;

    public Network(String inputNetworkCode){
        networkCode = inputNetworkCode;
    }

    public void addPost(Post inputPost){
        postList.add(inputPost);
    }

    public List<Post> getPostList(){return postList;}

    public void addPeer(String inputIp, String inputUsername){peerList.add(new User(inputIp, inputUsername));}
    public void addPeer(User inputUser){peerList.add(inputUser);}

    public void EndNetwork(){

    }

    public void KickUser(String usermame){

    }
}
