package dk.stbn.p2peksperiment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Network {
    private List<Post> postList = new ArrayList<Post>();

    private List<User> peerList = new ArrayList<User>();

    public String networkCode;

    public Network(String inputNetworkCode){
        networkCode = inputNetworkCode;
    }

    public void addPost(Post inputPost){
        postList.add(inputPost);
    }

    public List<Post> getPostList(){return postList;}

    public String getPostListString(){
        Type listType = new TypeToken<List<Post>>(){}.getType();

        return new Gson().toJson(postList, listType);
    }

    public List<Post> getPostListFromString(String input){
        Type listType = new TypeToken<List<Post>>(){}.getType();

        return new Gson().fromJson(input, listType);
    }

    public void setPostList(List<Post> input){
        postList = input;
    }

    public void addPeer(String inputIp, String inputUsername){peerList.add(new User(inputIp, inputUsername));}
    public void addPeer(User inputUser){peerList.add(inputUser);}

    public void removePeer(String inputIp, String inputUsername){
        peerList.remove(new User(inputIp, inputUsername));
    }

    public void KickUser(String usermame){

    }
}
