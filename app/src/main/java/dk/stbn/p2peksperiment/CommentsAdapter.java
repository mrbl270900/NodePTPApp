package dk.stbn.p2peksperiment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentsViewHolder> {
    private Context context;

    private List<Post.Comment> commentList;

    public CommentsAdapter(Context context, List<Post.Comment> comments) {
        this.commentList = comments;
        this.context = context;
    }

    @NonNull
    @Override
    public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.comment_layout, parent, false);

        return new CommentsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentsAdapter.CommentsViewHolder holder, int position) {
        Post.Comment comment = commentList.get(position);

        holder.commentSubjektTextView.setText(comment.getOwner());
        holder.commentContensTextView.setText(comment.getContens());
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class CommentsViewHolder extends RecyclerView.ViewHolder {
        private TextView commentContensTextView, commentSubjektTextView;

        public CommentsViewHolder(@NonNull View itemView) {
            super(itemView);

            commentSubjektTextView = itemView.findViewById(R.id.CommentSubjektTextView);
            commentContensTextView = itemView.findViewById(R.id.CommentContensTextView);
        }
    }
}
