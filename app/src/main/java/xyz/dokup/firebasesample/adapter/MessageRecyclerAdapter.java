package xyz.dokup.firebasesample.adapter;

import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;

import xyz.dokup.firebasesample.R;
import xyz.dokup.firebasesample.databinding.ItemMessageBinding;
import xyz.dokup.firebasesample.model.FriendlyMessage;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by e10dokup on 2017/03/30
 **/
public class MessageRecyclerAdapter extends RecyclerView.Adapter<MessageRecyclerAdapter.BindingHolder> {
    private static final String TAG = MessageRecyclerAdapter.class.getSimpleName();

    private List<FriendlyMessage> mFriendlyMessageList;

    public MessageRecyclerAdapter(List<FriendlyMessage> friendlyMessageList) {
        mFriendlyMessageList = friendlyMessageList;
    }

    @Override
    public int getItemCount() {
        return mFriendlyMessageList.size();
    }

    @Override
    public MessageRecyclerAdapter.BindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new BindingHolder(v);
    }

    @Override
    public void onBindViewHolder(BindingHolder holder, int position) {
        holder.getBinding().setMessage(mFriendlyMessageList.get(position));
        FriendlyMessage message = mFriendlyMessageList.get(position);

        if (message.getText() != null) {
            holder.getBinding().messageTextView.setVisibility(VISIBLE);
            holder.getBinding().messageImageView.setVisibility(GONE);
        } else {
            String imageUrl = message.getImageUrl();
            if (imageUrl.startsWith("gs://")) {
                StorageReference storageReference = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl);
                storageReference.getDownloadUrl().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String downloadUrl = task.getResult().toString();
                                Picasso.with(get)
                            } else {
                                Log.w(TAG, "Getting download url was not successful.",
                                        task.getException());
                            }
                        });
            } else {
                Glide.with(viewHolder.messageImageView.getContext())
                        .load(friendlyMessage.getImageUrl())
                        .into(viewHolder.messageImageView);
            }
            viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
            viewHolder.messageTextView.setVisibility(TextView.GONE);
        }
    }



    static class BindingHolder extends RecyclerView.ViewHolder {
        private ItemMessageBinding binding;

        public BindingHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }

        public ItemMessageBinding getBinding() {
            return binding;
        }
    }
}