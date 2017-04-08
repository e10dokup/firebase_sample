package xyz.dokup.firebasesample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import xyz.dokup.firebasesample.adapter.MessageRecyclerAdapter;
import xyz.dokup.firebasesample.databinding.ActivityMainBinding;
import xyz.dokup.firebasesample.handlers.MainActivityHandlers;
import xyz.dokup.firebasesample.model.FriendlyMessage;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, MainActivityHandlers {
    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mUserName;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;

    private ActivityMainBinding mBinding;
    private MessageRecyclerAdapter mAdapter;
    private List<FriendlyMessage> mFriendlyMessageList = new ArrayList<>();

    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setHandlers(this);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set default username is anonymous.
        mUserName = ANONYMOUS;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        mBinding.progressBar.setVisibility(View.INVISIBLE);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUserName = mFirebaseUser.getDisplayName();
            if(mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mFriendlyMessageList.clear();
                for (DataSnapshot messageSnapshot : dataSnapshot.child(MESSAGES_CHILD).getChildren()) {
                    String text = (String) messageSnapshot.child("text").getValue();
                    String name = (String) messageSnapshot.child("name").getValue();
                    String photoUrl = (String) messageSnapshot.child("photoUrl").getValue();
                    String imageUrl = (String) messageSnapshot.child("imageUrl").getValue();
                    FriendlyMessage message = new FriendlyMessage(text, name, photoUrl, imageUrl);
                    mFriendlyMessageList.add(message);
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mAdapter = new MessageRecyclerAdapter(mFriendlyMessageList);

        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                int count = mAdapter.getItemCount();
                int lastVisiblePosition = mBinding.messageRecyclerView.getVerticalScrollbarPosition();
                if (lastVisiblePosition == -1 || (positionStart >= (count - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mBinding.messageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mBinding.messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.messageRecyclerView.setAdapter(mAdapter);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUserName = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessageTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().trim().length() > 0) {
            mBinding.sendButton.setEnabled(true);
        } else {
            mBinding.sendButton.setEnabled(false);
        }
    }

    @Override
    public void onClickSendButton(View view) {
        FriendlyMessage message = new FriendlyMessage(mBinding.messageEditText.getText().toString(), mUserName, mPhotoUrl, null);
        mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(message);
        mBinding.messageEditText.setText("");
    }

    @Override
    public void onClickAddMessageImageView(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    FriendlyMessage tempMessage = new FriendlyMessage(null, mUserName, mPhotoUrl, LOADING_IMAGE_URL);
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(tempMessage, (databaseError, databaseReference) -> {
                        if (databaseError == null) {
                            String key = databaseReference.getKey();
                            StorageReference storageReference = FirebaseStorage.getInstance()
                                    .getReference(mFirebaseUser.getUid())
                                    .child(key)
                                    .child(uri.getLastPathSegment());

                            putImageInStorage(storageReference, uri, key);
                        } else {
                            Log.w(TAG, "Unable to write message to database.");
                        }
                    });
                }
            }
        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this, task -> {
            if (task.isSuccessful()) {
                FriendlyMessage message = new FriendlyMessage(null, mUserName, mPhotoUrl, task.getResult().getMetadata().getDownloadUrl().toString());
                mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(key).setValue(message);
            } else {
                Log.w(TAG, "Image upload task was not successful");
            }
        });
    }
}
