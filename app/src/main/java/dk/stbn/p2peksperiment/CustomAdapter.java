package dk.stbn.p2peksperiment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomHolder> {

    private Context context;
    private List<Post> Data;

    public CustomAdapter (Context context, List<Post> data) {
        this.context = context;
        this.Data = data;
    }


    @NonNull
    @Override
    public CustomHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.post_layout, parent, false);

        return new CustomHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomAdapter.CustomHolder holder, int position) {
        Post post = Data.get(position);
        holder.setDetails(post);
    }

    @Override
    public int getItemCount() {
        return Data.size();
    }
    class CustomHolder extends RecyclerView.ViewHolder{

        private TextView postSubjekt;

        public CustomHolder(@NonNull View itemView) {
            super(itemView);

            postSubjekt = itemView.findViewById(R.id.textView2);

        }

        void setDetails(Post input){
            postSubjekt.setText(input.subject);
        }
    }
}