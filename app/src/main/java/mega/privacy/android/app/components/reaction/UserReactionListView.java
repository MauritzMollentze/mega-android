package mega.privacy.android.app.components.reaction;

import android.content.Context;
import android.widget.ListView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaHandleList;

public final class UserReactionListView extends ListView {
    ArrayList<Long> arrayOfUsers = new ArrayList<>();
    private UserReactionAdapter userArrayAdapter;

    public UserReactionListView(Context context) {
        super(context);
        setVerticalScrollBarEnabled(true);
    }

    public void updateUsers(String reaction, long msgId, long chatId) {
        arrayOfUsers.clear();
        MegaHandleList listUsers = MegaApplication.getInstance().getMegaChatApi().getReactionUsers(chatId, msgId, reaction);
        if (listUsers == null || listUsers.size() == 0)
            return;

        for (int i = 0; i < listUsers.size(); i++) {
            arrayOfUsers.add(new Long(listUsers.get(i)));
        }
        userArrayAdapter = new UserReactionAdapter(getContext(), arrayOfUsers, chatId, msgId);
        setAdapter(userArrayAdapter);
    }

    public UserReactionListView init(String reaction, long msgId, long chatId) {
        updateUsers(reaction, msgId, chatId);
        return this;
    }

}