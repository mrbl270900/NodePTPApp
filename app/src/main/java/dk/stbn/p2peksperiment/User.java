package dk.stbn.p2peksperiment;

public class User {
        private String username;

        private String ip;

        public String getIp(){
            return ip;
        }

        public String getUsername(){
            return username;
        }

        public User(String inputIp, String inputUsername){
            username = inputUsername;
            ip = inputIp;
        }
}
