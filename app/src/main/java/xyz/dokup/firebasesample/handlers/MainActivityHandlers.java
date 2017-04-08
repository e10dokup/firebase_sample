package xyz.dokup.firebasesample.handlers;

import android.view.View;

/**
 * Created by e10dokup on 2017/03/29
 **/
public interface MainActivityHandlers {
    void onMessageTextChanged(CharSequence s, int start, int before, int count);
    void onClickSendButton(View view);
    void onClickAddMessageImageView(View view);

}