package mega.privacy.android.app.main.legacycontact;

import static mega.privacy.android.app.presentation.meeting.model.MeetingState.FREE_PLAN_PARTICIPANTS_LIMIT;
import static mega.privacy.android.app.utils.CallUtil.isNecessaryDisableLocalCamera;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationOpenCamera;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_QR;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_BOTH;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_DEVICE;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA;
import static mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_IS_FROM_MEETING;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACTS_SELECTED;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MAIL;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MAX_USER;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.INVITE_CONTACT;
import static mega.privacy.android.app.utils.Constants.MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR_CONTACT;
import static mega.privacy.android.app.utils.Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE;
import static mega.privacy.android.app.utils.Constants.REQUEST_READ_CONTACTS;
import static mega.privacy.android.app.utils.ContactUtil.getContactDB;
import static mega.privacy.android.app.utils.ContactUtil.getContactNameDB;
import static mega.privacy.android.app.utils.TimeUtils.lastGreenDate;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.hideKeyboard;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuItemCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.arch.extensions.ViewExtensionsKt;
import mega.privacy.android.app.components.HeaderItemDecoration;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.components.scrollBar.FastScrollerScrollListener;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.extensions.EdgeToEdgeExtensionsKt;
import mega.privacy.android.app.main.PhoneContactInfo;
import mega.privacy.android.app.main.ShareContactInfo;
import mega.privacy.android.app.main.adapters.AddContactsAdapter;
import mega.privacy.android.app.main.adapters.MegaAddContactsAdapter;
import mega.privacy.android.app.main.adapters.MegaContactsAdapter;
import mega.privacy.android.app.main.adapters.PhoneContactsAdapter;
import mega.privacy.android.app.main.adapters.ShareContactsAdapter;
import mega.privacy.android.app.main.adapters.ShareContactsHeaderAdapter;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.presentation.meeting.view.ParticipantsLimitWarningView;
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity;
import mega.privacy.android.app.psa.PsaWebBrowser;
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.domain.entity.Contact;
import mega.privacy.android.navigation.MegaNavigator;
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaSet;
import nz.mega.sdk.MegaSetElement;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;
import timber.log.Timber;

@AndroidEntryPoint
public class AddContactActivity extends PasscodeActivity implements View.OnClickListener,
        RecyclerView.OnItemTouchListener, TextWatcher, TextView.OnEditorActionListener,
        MegaRequestListenerInterface, MegaGlobalListenerInterface {

    @Inject
    GetChatChangesUseCase getChatChangesUseCase;
    @Inject
    DatabaseHandler dbH;

    @Inject
    MegaNavigator navigator;

    private AddContactViewModel viewModel;
    private static final int SCAN_QR_FOR_ADD_CONTACTS = 1111;
    public static final String EXTRA_MEGA_CONTACTS = "mega_contacts";
    public static final String EXTRA_CONTACTS = "extra_contacts";
    public static final String EXTRA_MEETING = "extra_meeting";
    public static final String EXTRA_NODE_HANDLE = "node_handle";
    public static final String EXTRA_CHAT_TITLE = "chatTitle";
    public static final String EXTRA_GROUP_CHAT = "groupChat";
    public static final String EXTRA_EKR = "EKR";
    public static final String ALLOW_ADD_PARTICIPANTS = "ALLOW_ADD_PARTICIPANTS";
    public static final String EXTRA_CHAT_LINK = "chatLink";
    public static final String EXTRA_CONTACT_TYPE = "contactType";
    public static final String EXTRA_ONLY_CREATE_GROUP = "onlyCreateGroup";
    public static final String EXTRA_IS_START_CONVERSATION = "isStartConversation";
    public static final String EXTRA_MULTISELECT = "MULTISELECT";

    private DisplayMetrics outMetrics;

    int contactType = 0;

    private ArrayList<String> emailsContactsSelected = new ArrayList<>();

    // Determine if open this page from meeting
    private boolean isFromMeeting;

    private int multipleSelectIntent;
    private long nodeHandle = -1;
    private long[] nodeHandles;
    private long chatId = -1;

    private int maxParticipants = -1;

    private AddContactActivity addContactActivity;

    private Toolbar tB;
    private ActionBar aB;

    private RelativeLayout containerContacts;
    private RecyclerView recyclerViewList;
    private LinearLayoutManager linearLayoutManager;
    private ImageView emptyImageView;
    private TextView emptyTextView;
    private TextView emptySubTextView;
    private Button emptyInviteButton;
    ProgressBar progressBar;
    private RecyclerView addedContactsRecyclerView;
    private RelativeLayout containerAddedContactsRecyclerView;
    private LinearLayoutManager mLayoutManager;

    private TextView mWarningMessage;

    String inputString = "";
    private String savedInputString = "";

    //    Adapter list MEGA contacts
    private MegaContactsAdapter adapterMEGA;
    //    Adapter list chips MEGA contacts
    MegaAddContactsAdapter adapterMEGAContacts;

    private ArrayList<MegaUser> contactsMEGA;
    ArrayList<MegaContactAdapter> visibleContactsMEGA = new ArrayList<>();
    ArrayList<MegaContactAdapter> filteredContactMEGA = new ArrayList<>();
    ArrayList<MegaContactAdapter> addedContactsMEGA = new ArrayList<>();
    ArrayList<MegaContactAdapter> queryContactMEGA = new ArrayList<>();


    //    Adapter list Phone contacts
    private PhoneContactsAdapter adapterPhone;
    //    Adapter list chips Phone contacts
    private AddContactsAdapter adapterContacts;

    private ArrayList<PhoneContactInfo> phoneContacts = new ArrayList<>();
    ArrayList<PhoneContactInfo> addedContactsPhone = new ArrayList<>();
    ArrayList<PhoneContactInfo> filteredContactsPhone = new ArrayList<>();
    ArrayList<PhoneContactInfo> queryContactsPhone = new ArrayList<>();

    //    Adapter list Share contacts
    private ShareContactsHeaderAdapter adapterShareHeader;
    //    Adapter list chips MEGA/Phone contacts
    private ShareContactsAdapter adapterShare;

    ArrayList<ShareContactInfo> addedContactsShare = new ArrayList<>();
    ArrayList<ShareContactInfo> shareContacts = new ArrayList<>();
    ArrayList<ShareContactInfo> filteredContactsShare = new ArrayList<>();


    private RelativeLayout relativeLayout;

    private ParticipantsLimitWarningView participantsLimitWarningView;

    ArrayList<String> savedaddedContacts = new ArrayList<>();

    private MenuItem sendInvitationMenuItem;
    private MenuItem scanQrMenuItem;

    private boolean comesFromChat;
    private boolean isStartConversation;
    private boolean comesFromRecent;
    public static final String FROM_RECENT = "comesFromRecent";
    public static final String IS_ALLOWED_ADD_PARTICIPANTS = "isAllowAddParticipants";

    private RelativeLayout headerContacts;
    private TextView textHeader;

    private boolean fromAchievements = false;
    private ArrayList<String> mailsFromAchievements;

    private MenuItem searchMenuItem;
    SearchView.SearchAutoComplete searchAutoComplete;
    boolean searchExpand = false;

    FilterContactsTask filterContactsTask;
    private GetContactsTask getContactsTask;
    GetPhoneContactsTask getPhoneContactsTask;
    private RecoverContactsTask recoverContactsTask;
    QueryIfContactSouldBeAddedTask queryIfContactSouldBeAddedTask;

    private FastScroller fastScroller;

    private FloatingActionButton fabImageGroup;
    private FloatingActionButton fabButton;
    private EmojiEditText nameGroup;
    boolean onNewGroup = false;
    private boolean isConfirmDeleteShown = false;
    private String confirmDeleteMail;

    private RelativeLayout mailError;
    private RelativeLayout typeContactLayout;
    EditText typeContactEditText;
    private RelativeLayout scanQRButton;
    boolean isConfirmAddShown = false;
    String confirmAddMail;
    private boolean createNewGroup = false;
    private String title = "";

    boolean queryPermissions = true;

    private LinearLayout addContactsLayout;
    private NestedScrollView newGroupLayout;
    private MegaSwitch ekrSwitch;
    private boolean isEKREnabled = false;
    private RelativeLayout getChatLinkLayout;
    private CheckBox getChatLinkBox;
    private MegaSwitch allowAddParticipantsSwitch;
    private boolean isAllowAddParticipantsEnabled = true;
    private LinearLayoutManager newGrouplinearLayoutManager;
    private RecyclerView newGroupRecyclerView;
    private TextView newGroupHeaderList;
    boolean newGroup = false;
    ArrayList<String> contactsNewGroup;

    MegaContactAdapter myContact;

    private boolean onlyCreateGroup;
    boolean waitingForPhoneContacts;

    private boolean isContactVerificationOn;

    private boolean isWarningMessageShown;


    /**
     * Shows the fabButton
     */
    private void showFabButton() {
        setSendInvitationVisibility();
    }

    /**
     * Hides the fabButton
     */
    private void hideFabButton() {
        if (fabButton != null) {
            fabButton.hide();
        }
    }

    @Override
    public void onGlobalSyncStateChanged(@NonNull MegaApiJava api) {
    }

    public String getShareContactMail(ShareContactInfo contact) {
        String mail = null;

        if (contact.isMegaContact() && !contact.isHeader()) {
            if (contact.getMegaContactAdapter().getMegaUser() != null && contact.getMegaContactAdapter().getMegaUser().getEmail() != null) {
                mail = contact.getMegaContactAdapter().getMegaUser().getEmail();
            } else if (contact.getMegaContactAdapter().getContact() != null && contact.getMegaContactAdapter().getContact().getEmail() != null) {
                mail = contact.getMegaContactAdapter().getContact().getEmail();
            }
        } else if (contact.isPhoneContact() && !contact.isHeader()) {
            mail = contact.getPhoneContactInfo().getEmail();
        } else {
            mail = contact.getMail();
        }

        return mail;
    }

    void getDeviceContacts() {
        if (queryPermissions) {
            if (phoneContacts != null) {
                phoneContacts.clear();
            }
            filteredContactsPhone.clear();
            phoneContacts = getPhoneContacts();
            if (phoneContacts != null) {
                for (int i = 0; i < phoneContacts.size(); i++) {
                    filteredContactsPhone.add(phoneContacts.get(i));
                }
            }
        }
    }

    void getBothContacts() {
        getDeviceContacts();
        getVisibleMEGAContacts();
    }

    void setAddedAdapterContacts() {
        if (contactType == CONTACT_TYPE_MEGA) {
            if (adapterMEGAContacts == null) {
                adapterMEGAContacts = new MegaAddContactsAdapter(addContactActivity, addedContactsMEGA);
            } else {
                adapterMEGAContacts.setContacts(addedContactsMEGA);
            }

            if (addedContactsMEGA.size() == 0) {
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            } else {
                containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
            }

            addedContactsRecyclerView.setAdapter(adapterMEGAContacts);
        } else if (contactType == CONTACT_TYPE_DEVICE) {
            if (adapterContacts == null) {
                adapterContacts = new AddContactsAdapter(this, addedContactsPhone);
            } else {
                adapterContacts.setContacts(addedContactsPhone);
            }

            if (addedContactsPhone.size() == 0) {
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            } else {
                containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
            }

            addedContactsRecyclerView.setAdapter(adapterContacts);
        } else {
            if (adapterShare == null) {
                adapterShare = new ShareContactsAdapter(addContactActivity, addedContactsShare);
            } else {
                adapterShare.setContacts(addedContactsShare);
            }

            if (addedContactsShare.size() == 0) {
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            } else {
                containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
            }

            addedContactsRecyclerView.setAdapter(adapterShare);
        }

        setSendInvitationVisibility();
    }

    void setPhoneAdapterContacts(ArrayList<PhoneContactInfo> contacts) {
        if (queryPermissions && filteredContactsPhone != null) {
            if (filteredContactsPhone.size() == 0) {
                showHeader(false);
                String textToShow = String.format(getString(R.string.context_empty_contacts));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                            + "\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                            + "\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
                emptyTextView.setText(result);
            } else {
                emptyTextView.setText(R.string.contacts_list_empty_text_loading);
            }
        } else {
            if (!queryPermissions) {
                emptyTextView.setText(R.string.no_contacts_permissions);
            }
            Timber.d("PhoneContactsTask: Phone contacts null");
            boolean hasReadContactsPermission = hasPermissions(getApplicationContext(), Manifest.permission.READ_CONTACTS);
            if (!hasReadContactsPermission) {
                Timber.w("PhoneContactsTask: No read contacts permission");
            }
        }

        if (adapterPhone == null) {
            adapterPhone = new PhoneContactsAdapter(addContactActivity, contacts);

            recyclerViewList.setAdapter(adapterPhone);

            adapterPhone.SetOnItemClickListener(new PhoneContactsAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, int position) {
                    itemClick(view, position);
                }
            });
        } else {
            adapterPhone.setContacts(contacts);
        }

        if (adapterPhone != null) {
            if (adapterPhone.getItemCount() == 0) {
                showHeader(false);
                if (contactType == CONTACT_TYPE_BOTH) {
                    if (adapterMEGA != null) {
                        if (adapterMEGA.getItemCount() == 0) {
                            setEmptyStateVisibility(true);
                        } else {
                            setEmptyStateVisibility(false);
                        }
                    }
                } else {
                    setEmptyStateVisibility(true);
                }
            } else {
                showHeader(true);
                setEmptyStateVisibility(false);
            }
        }
    }

    void setMegaAdapterContacts(ArrayList<MegaContactAdapter> contacts, int adapter) {
        if (onNewGroup) {
            adapterMEGA = new MegaContactsAdapter(addContactActivity, contacts, newGroupRecyclerView, adapter);

            adapterMEGA.setPositionClicked(-1);
            newGroupRecyclerView.setAdapter(adapterMEGA);
        } else {
            if (adapterMEGA == null) {
                adapterMEGA = new MegaContactsAdapter(addContactActivity, contacts, recyclerViewList, adapter);
            } else {
                adapterMEGA.setAdapterType(adapter);
                adapterMEGA.setContacts(contacts);
            }

            adapterMEGA.setPositionClicked(-1);
            recyclerViewList.setAdapter(adapterMEGA);

            if (adapterMEGA.getItemCount() == 0) {
                String textToShow = getString(R.string.context_empty_contacts);
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                            + "\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                            + "\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned result = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
                emptyTextView.setText(result);
                showHeader(false);
                recyclerViewList.setVisibility(View.GONE);
                setEmptyStateVisibility(true);
            } else {
                showHeader(true);
                recyclerViewList.setVisibility(View.VISIBLE);
                setEmptyStateVisibility(false);
            }

            addSelectedContactMEGA();
        }
    }

    void setShareAdapterContacts(ArrayList<ShareContactInfo> contacts) {
        if (adapterShareHeader == null) {
            adapterShareHeader = new ShareContactsHeaderAdapter(addContactActivity, contacts);
            recyclerViewList.setAdapter(adapterShareHeader);
            adapterShareHeader.SetOnItemClickListener(new ShareContactsHeaderAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, int position) {
                    itemClick(view, position);
                }
            });
        } else {
            adapterShareHeader.setContacts(contacts);
        }

        if (adapterShareHeader.getItemCount() == 0) {
            String textToShow = String.format(getString(R.string.context_empty_contacts));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'"
                        + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                        + "\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'"
                        + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                        + "\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            } catch (Exception e) {
            }
            Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            emptyTextView.setText(result);
        } else {
            setEmptyStateVisibility(false);
        }
    }

    private final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            Timber.d("isValid");
            return EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    void setSendInvitationVisibility() {
        if (fabButton != null) {
            if (contactType == CONTACT_TYPE_MEGA && !onNewGroup &&
                    (createNewGroup || (comesFromChat &&
                            ((adapterMEGAContacts != null && adapterMEGAContacts.getItemCount() > 0) ||
                                    (emailsContactsSelected != null && !emailsContactsSelected.isEmpty()))))) {
                fabButton.show();
            } else if (contactType == CONTACT_TYPE_DEVICE && adapterContacts != null && adapterContacts.getItemCount() > 0) {
                fabButton.show();
            } else if (contactType == CONTACT_TYPE_BOTH && adapterShare != null && adapterShare.getItemCount() > 0) {
                fabButton.show();
            } else {
                fabButton.hide();
            }
        }
        if (sendInvitationMenuItem != null) {
            if (contactType == CONTACT_TYPE_MEGA && onNewGroup) {
                sendInvitationMenuItem.setVisible(true);
            } else {
                sendInvitationMenuItem.setVisible(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_add_contact, menu);

        searchMenuItem = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setHint(getString(R.string.hint_action_search));
        View v = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        v.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

        searchView.setIconifiedByDefault(true);

        searchAutoComplete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(addContactActivity, 0);
                    return true;
                }
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Timber.d("onMenuItemActionExpand");
                searchExpand = true;
                typeContactEditText.getText().clear();
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask(AddContactActivity.this);
                filterContactsTask.execute();
                setSendInvitationVisibility();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Timber.d("onMenuItemActionCollapse");
                searchExpand = false;
                setSendInvitationVisibility();
                setTitleAB();
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask.cancel(true);
                }
                return true;
            }
        });
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Timber.d("onQueryTextChange searchView");
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask(AddContactActivity.this);
                filterContactsTask.execute();
                return true;
            }

        });

        scanQrMenuItem = menu.findItem(R.id.action_scan_qr);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && contactType != CONTACT_TYPE_MEGA) {
            scanQrMenuItem.setVisible(true);
        } else {
            scanQrMenuItem.setVisible(false);
        }

        sendInvitationMenuItem = menu.findItem(R.id.action_send_invitation);
        setSendInvitationVisibility();

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.expandActionView();
            if (searchView != null) {
                Timber.d("searchView != null inputString: %s", savedInputString);
                searchView.setQuery(savedInputString, false);
                if (recoverContactsTask != null && recoverContactsTask.getStatus() == AsyncTask.Status.FINISHED) {
                    filterContactsTask = new FilterContactsTask(this);
                    filterContactsTask.execute();
                }
            }
        }
        setSearchVisibility();

        if (!queryPermissions && contactType == CONTACT_TYPE_DEVICE) {
            searchMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    void setSearchVisibility() {
        if (searchMenuItem == null) {
            return;
        }

        boolean visible;

        if ((contactType == CONTACT_TYPE_MEGA && filteredContactMEGA.isEmpty())
                || (contactType == CONTACT_TYPE_DEVICE && filteredContactsPhone.isEmpty())
                || (contactType == CONTACT_TYPE_BOTH && filteredContactsShare.isEmpty())) {
            visible = false;
        } else {
            visible = true;
        }

        if (searchMenuItem.isVisible() != visible) {
            searchMenuItem.setVisible(visible);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Timber.d("onPrepareOptionsMenu");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected");

        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        } else if (id == R.id.action_scan_qr) {
            initScanQR();
        } else if (id == R.id.action_send_invitation) {
            if (contactType == CONTACT_TYPE_MEGA) {
                setResultContacts(addedContactsMEGA, true);
            } else {
                shareWith(addedContactsShare);
            }
            hideKeyboard(addContactActivity, 0);
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshKeyboard() {

        String s = typeContactEditText.getText().toString();
        int imeOptions = typeContactEditText.getImeOptions();

        if (s != null) {
            if (s.length() == 0 && (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty())) {
                typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
            } else {
                typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
        } else if (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty()) {
            typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        } else {
            typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }

        int imeOptionsNew = typeContactEditText.getImeOptions();
        if (imeOptions != imeOptionsNew) {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.restartInput(view);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("fromAchievements", fromAchievements);
        outState.putStringArrayList("mailsFromAchievements", mailsFromAchievements);
        outState.putBoolean("searchExpand", searchExpand);
        if (searchExpand) {
            if (searchAutoComplete != null) {
                if (searchAutoComplete != null) {
                    outState.putString("inputString", searchAutoComplete.getText().toString());
                }
            }
        } else {
            if (typeContactEditText != null) {
                outState.putString("inputString", typeContactEditText.getText().toString());
            }
        }
        outState.putBoolean("onNewGroup", onNewGroup);
        outState.putBoolean("isConfirmDeleteShown", isConfirmDeleteShown);
        outState.putString("confirmDeleteMail", confirmDeleteMail);
        outState.putBoolean(FROM_RECENT, comesFromRecent);
        if (isAsyncTaskRunning(queryIfContactSouldBeAddedTask)) {
            isConfirmAddShown = true;
            queryIfContactSouldBeAddedTask.cancel(true);
        }
        outState.putBoolean("isConfirmAddShown", isConfirmAddShown);
        outState.putString("confirmAddMail", confirmAddMail);
        outState.putBoolean("createNewGroup", createNewGroup);
        outState.putBoolean("queryPermissions", queryPermissions);
        outState.putBoolean("isEKREnabled", isEKREnabled);
        outState.putBoolean(IS_ALLOWED_ADD_PARTICIPANTS, isAllowAddParticipantsEnabled);
        outState.putBoolean("newGroup", newGroup);
        outState.putBoolean("onlyCreateGroup", onlyCreateGroup);
        outState.putBoolean("warningBannerShown", isWarningMessageShown);
        saveContactsAdded(outState);
    }

    boolean isAsyncTaskRunning(AsyncTask asyncTask) {
        return asyncTask != null && asyncTask.getStatus() == AsyncTask.Status.RUNNING;
    }

    private void saveContactsAdded(Bundle outState) {

        boolean finished = true;

        if (isAsyncTaskRunning(getContactsTask)) {
            getContactsTask.cancel(true);
            finished = false;
            if (contactType == CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", null);
                outState.putParcelableArrayList("filteredContactsPhone", null);
                outState.putParcelableArrayList("phoneContacts", null);
            } else {
                outState.putStringArrayList("savedaddedContacts", null);
            }
        } else if (isAsyncTaskRunning(getPhoneContactsTask)) {
            getPhoneContactsTask.cancel(true);
            finished = false;
            outState.putStringArrayList("savedaddedContacts", null);
        } else if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask.cancel(true);
            finished = true;
        } else if (isAsyncTaskRunning(recoverContactsTask)) {
            recoverContactsTask.cancel(true);
            finished = false;
            if (contactType == CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", addedContactsPhone);
                outState.putParcelableArrayList("filteredContactsPhone", filteredContactsPhone);
                outState.putParcelableArrayList("phoneContacts", phoneContacts);
            } else {
                outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
            }
        }

        if (finished) {
            savedaddedContacts.clear();
            if (contactType == CONTACT_TYPE_MEGA) {
                if (onNewGroup) {
                    createMyContact();
                    if (addedContactsMEGA.contains(myContact)) {
                        addedContactsMEGA.remove(myContact);
                    }
                }
                for (int i = 0; i < addedContactsMEGA.size(); i++) {
                    if (getMegaContactMail(addedContactsMEGA.get(i)) != null) {
                        savedaddedContacts.add(getMegaContactMail(addedContactsMEGA.get(i)));
                    } else {
                        savedaddedContacts.add(addedContactsMEGA.get(i).getFullName());
                    }
                }
                outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
            } else if (contactType == CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", addedContactsPhone);
                outState.putParcelableArrayList("filteredContactsPhone", filteredContactsPhone);
                outState.putParcelableArrayList("phoneContacts", phoneContacts);
            } else {
                for (int i = 0; i < addedContactsShare.size(); i++) {
                    if (addedContactsShare.get(i).isMegaContact()) {
                        savedaddedContacts.add(getMegaContactMail(addedContactsShare.get(i).getMegaContactAdapter()));
                    } else if (addedContactsShare.get(i).isPhoneContact()) {
                        savedaddedContacts.add(addedContactsShare.get(i).getPhoneContactInfo().getEmail());
                    } else {
                        savedaddedContacts.add(addedContactsShare.get(i).getMail());
                    }
                }
                outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (megaApi != null) {
            megaApi.removeGlobalListener(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdgeExtensionsKt.enableEdgeToEdgeAndConsumeInsets(this);
        super.onCreate(savedInstanceState);

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }
        viewModel = new ViewModelProvider(this).get(AddContactViewModel.class);

        if (getIntent() != null) {
            contactType = getIntent().getIntExtra(INTENT_EXTRA_KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA);
            emailsContactsSelected = getIntent().getStringArrayListExtra(INTENT_EXTRA_KEY_CONTACTS_SELECTED);
            isFromMeeting = getIntent().getBooleanExtra(INTENT_EXTRA_IS_FROM_MEETING, false);
            chatId = getIntent().getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, -1);
            maxParticipants = getIntent().getIntExtra(INTENT_EXTRA_KEY_MAX_USER, FREE_PLAN_PARTICIPANTS_LIMIT);
            newGroup = getIntent().getBooleanExtra("newGroup", false);
            comesFromRecent = getIntent().getBooleanExtra(FROM_RECENT, false);
            if (newGroup) {
                createNewGroup = true;
                contactsNewGroup = getIntent().getStringArrayListExtra("contactsNewGroup");
            }
            fromAchievements = getIntent().getBooleanExtra("fromAchievements", false);
            if (fromAchievements) {
                mailsFromAchievements = getIntent().getStringArrayListExtra(EXTRA_CONTACTS);
            }
            comesFromChat = getIntent().getBooleanExtra(INTENT_EXTRA_KEY_CHAT, false);
            if (comesFromChat) {
                title = getIntent().getStringExtra(INTENT_EXTRA_KEY_TOOL_BAR_TITLE);
            }
            onlyCreateGroup = getIntent().getBooleanExtra(EXTRA_ONLY_CREATE_GROUP, false);
            isStartConversation = getIntent().getBooleanExtra(EXTRA_IS_START_CONVERSATION, false);
            if (contactType == CONTACT_TYPE_MEGA || contactType == CONTACT_TYPE_BOTH) {
                multipleSelectIntent = getIntent().getIntExtra(EXTRA_MULTISELECT, -1);
                if (multipleSelectIntent == 0) {
                    nodeHandle = getIntent().getLongExtra(EXTRA_NODE_HANDLE, -1);
                } else if (multipleSelectIntent == 1) {
                    Timber.d("Multiselect YES!");
                    nodeHandles = getIntent().getLongArrayExtra(EXTRA_NODE_HANDLE);
                }
            }
        }

        ViewExtensionsKt.collectFlow(this, viewModel.getSensitiveItemsCountFlow(), Lifecycle.State.STARTED, type -> {
            if (type != null) {
                if (type == 0) {
                    initialize(savedInstanceState);
                } else {
                    showSharingSensitiveItemsWarningDialog(savedInstanceState, type);
                }
                viewModel.clearSensitiveItemsCheck();
            }
            return Unit.INSTANCE;
        });

        List<Long> handles = new ArrayList<>();
        if (nodeHandle != -1) {
            handles.add(nodeHandle);
        } else if (nodeHandles != null) {
            for (long handle : nodeHandles) {
                handles.add(handle);
            }
        }

        viewModel.checkSensitiveItems(handles);
    }

    private void initialize(Bundle savedInstanceState) {
        viewModel.setChatId(chatId);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        megaApi.addGlobalListener(this);

        addContactActivity = this;

        checkChatChanges();

        setContentView(R.layout.activity_add_contact);

        tB = (Toolbar) findViewById(R.id.add_contact_toolbar);
        if (tB == null) {
            Timber.w("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setTitle("");
        aB.setSubtitle("");

        participantsLimitWarningView = findViewById(R.id.participants_limit_warning_view);

        relativeLayout = (RelativeLayout) findViewById(R.id.relative_container_add_contact);

        fabButton = (FloatingActionButton) findViewById(R.id.fab_button_next);
        fabButton.setOnClickListener(this);

        mailError = (RelativeLayout) findViewById(R.id.add_contact_email_error);
        mailError.setVisibility(View.GONE);
        typeContactLayout = (RelativeLayout) findViewById(R.id.layout_type_mail);
        typeContactLayout.setVisibility(View.GONE);
        typeContactEditText = (EditText) findViewById(R.id.type_mail_edit_text);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(40, outMetrics));
            typeContactLayout.setLayoutParams(params1);
        }
        typeContactEditText.addTextChangedListener(this);
        typeContactEditText.setOnEditorActionListener(this);
        typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        typeContactEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (searchExpand) {
                        if (searchAutoComplete != null) {
                            searchAutoComplete.getText().clear();
                        }
                        if (searchMenuItem != null) {
                            searchMenuItem.collapseActionView();
                        }
                        if (isAsyncTaskRunning(filterContactsTask)) {
                            filterContactsTask.cancel(true);
                        }
                        filterContactsTask = new FilterContactsTask(AddContactActivity.this);
                        filterContactsTask.execute();
                    }
                }
            }
        });
        scanQRButton = (RelativeLayout) findViewById(R.id.layout_scan_qr);
        scanQRButton.setOnClickListener(this);
        scanQRButton.setVisibility(View.GONE);
        addContactsLayout = (LinearLayout) findViewById(R.id.add_contacts_container);
        mWarningMessage = (TextView) findViewById(R.id.text_warning_message);
        addedContactsRecyclerView = (RecyclerView) findViewById(R.id.contact_adds_recycler_view);
        containerAddedContactsRecyclerView = (RelativeLayout) findViewById(R.id.contacts_adds_container);
        containerAddedContactsRecyclerView.setVisibility(View.GONE);
        fabImageGroup = (FloatingActionButton) findViewById(R.id.image_group_floating_button);
        nameGroup = findViewById(R.id.name_group_edittext);
        nameGroup.setSingleLine();
        nameGroup.setImeOptions(EditorInfo.IME_ACTION_DONE);
        nameGroup.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_ALLOWED_CHARACTERS_AND_EMOJIS)});

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        addedContactsRecyclerView.setLayoutManager(mLayoutManager);
        addedContactsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        headerContacts = (RelativeLayout) findViewById(R.id.header_list);
        textHeader = (TextView) findViewById(R.id.text_header_list);

        fastScroller = (FastScroller) findViewById(R.id.fastscroll);

        linearLayoutManager = new LinearLayoutManager(this);
        recyclerViewList = (RecyclerView) findViewById(R.id.add_contact_list);
        recyclerViewList.setClipToPadding(false);
        recyclerViewList.addOnItemTouchListener(this);
        recyclerViewList.setItemAnimator(new DefaultItemAnimator());
        fastScroller.setRecyclerView(recyclerViewList);

        if (contactType == CONTACT_TYPE_MEGA) {
            recyclerViewList.setLayoutManager(linearLayoutManager);
            showHeader(true);
            textHeader.setText(getString(R.string.section_contacts));
            recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this));
        } else if (contactType == CONTACT_TYPE_DEVICE) {
            typeContactLayout.setVisibility(View.VISIBLE);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                scanQRButton.setVisibility(View.VISIBLE);
            }
            recyclerViewList.setLayoutManager(linearLayoutManager);
            showHeader(true);
            textHeader.setText(getString(R.string.contacts_phone));
            recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this));
        } else {
            typeContactLayout.setVisibility(View.VISIBLE);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                scanQRButton.setVisibility(View.VISIBLE);
            }
            recyclerViewList.setLayoutManager(linearLayoutManager);
            recyclerViewList.addItemDecoration(new HeaderItemDecoration(this));
            showHeader(false);
        }

        containerContacts = (RelativeLayout) findViewById(R.id.container_list_contacts);

        emptyImageView = (ImageView) findViewById(R.id.add_contact_list_empty_image);
        emptyTextView = (TextView) findViewById(R.id.add_contact_list_empty_text);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            emptyImageView.setImageResource(R.drawable.empty_contacts_portrait);
        } else {
            // auto scroll to the bottom to show the invite button
            final ScrollView scrollView = findViewById(R.id.scroller);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }, 100);
            emptyImageView.setImageResource(R.drawable.empty_contacts_landscape);
        }
        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);
        emptySubTextView = (TextView) findViewById(R.id.add_contact_list_empty_subtext);
        emptyInviteButton = (Button) findViewById(R.id.add_contact_list_empty_invite_button);
        emptyInviteButton.setText(R.string.contact_invite);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) emptySubTextView.getLayoutParams();
            params1.setMargins(dp2px(34, outMetrics), 0, dp2px(34, outMetrics), 0);
            emptyTextView.setLayoutParams(params1);
            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) emptyInviteButton.getLayoutParams();
            params2.setMargins(0, dp2px(5, outMetrics), 0, dp2px(32, outMetrics));
            emptyInviteButton.setLayoutParams(params2);
        }

        emptyInviteButton.setOnClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.add_contact_progress_bar);

        newGroupLayout = (NestedScrollView) findViewById(R.id.new_group_layout);
        newGroupLayout.setVisibility(View.GONE);
        ekrSwitch = (MegaSwitch) findViewById(R.id.ekr_switch);
        ekrSwitch.setOnClickListener(this);
        getChatLinkBox = (CheckBox) findViewById(R.id.get_chat_link_checkbox);
        getChatLinkLayout = (RelativeLayout) findViewById(R.id.get_chat_link_layout);
        newGroupHeaderList = (TextView) findViewById(R.id.new_group_text_header_list);
        allowAddParticipantsSwitch = findViewById(R.id.allow_add_participants_switch);
        allowAddParticipantsSwitch.setOnClickListener(this);
        newGroupRecyclerView = (RecyclerView) findViewById(R.id.new_group_add_contact_list);
        newGroupRecyclerView.setClipToPadding(false);
        newGroupRecyclerView.addOnItemTouchListener(this);
        newGroupRecyclerView.setItemAnimator(new DefaultItemAnimator());
        newGrouplinearLayoutManager = new LinearLayoutManager(this);
        newGroupRecyclerView.setLayoutManager(newGrouplinearLayoutManager);
        newGroupRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));

        //Get MEGA contacts and phone contacts: first name, last name and email
        if (savedInstanceState != null) {
            onNewGroup = savedInstanceState.getBoolean("onNewGroup", onNewGroup);
            isConfirmDeleteShown = savedInstanceState.getBoolean("isConfirmDeleteShown", false);
            confirmDeleteMail = savedInstanceState.getString("confirmDeleteMail");
            comesFromRecent = savedInstanceState.getBoolean(FROM_RECENT, false);
            searchExpand = savedInstanceState.getBoolean("searchExpand", false);
            savedInputString = savedInstanceState.getString("inputString");
            isConfirmAddShown = savedInstanceState.getBoolean("isConfirmAddShown", false);
            confirmAddMail = savedInstanceState.getString("confirmAddMail");
            createNewGroup = savedInstanceState.getBoolean("createNewGroup", false);
            queryPermissions = savedInstanceState.getBoolean("queryPermissions", true);
            isEKREnabled = savedInstanceState.getBoolean("isEKREnabled", false);
            ekrSwitch.setChecked(isEKREnabled);
            isAllowAddParticipantsEnabled = savedInstanceState.getBoolean(IS_ALLOWED_ADD_PARTICIPANTS, true);
            allowAddParticipantsSwitch.setChecked(isAllowAddParticipantsEnabled);
            onlyCreateGroup = savedInstanceState.getBoolean("onlyCreateGroup", false);
            isWarningMessageShown = savedInstanceState.getBoolean("warningBannerShown", false);
            mWarningMessage.setVisibility(isWarningMessageShown && isContactVerificationOn ? View.VISIBLE : View.GONE);
            if (contactType == CONTACT_TYPE_MEGA || contactType == CONTACT_TYPE_BOTH) {
                savedaddedContacts = savedInstanceState.getStringArrayList("savedaddedContacts");

                if (createNewGroup) {
                    setTitleAB();
                }

                if (savedaddedContacts == null && (contactType == CONTACT_TYPE_MEGA || contactType == CONTACT_TYPE_BOTH)) {
                    setAddedAdapterContacts();
                    getContactsTask = new GetContactsTask(this);
                    getContactsTask.execute();
                } else {
                    recoverContactsTask = new RecoverContactsTask(this);
                    recoverContactsTask.execute();
                }
            } else if (contactType == CONTACT_TYPE_DEVICE) {
                addedContactsPhone = savedInstanceState.getParcelableArrayList("addedContactsPhone");
                filteredContactsPhone = savedInstanceState.getParcelableArrayList("filteredContactsPhone");
                phoneContacts = savedInstanceState.getParcelableArrayList("phoneContacts");

                setAddedAdapterContacts();

                if (queryPermissions && filteredContactsPhone == null && phoneContacts == null) {
                    queryIfHasReadContactsPermissions();
                } else {
                    if (addedContactsPhone.size() == 0) {
                        containerAddedContactsRecyclerView.setVisibility(View.GONE);
                    } else {
                        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
                    }
                    addedContactsRecyclerView.setAdapter(adapterContacts);

                    if (phoneContacts != null && !phoneContacts.isEmpty()) {
                        if (filteredContactsPhone == null && (addedContactsPhone == null || addedContactsPhone.isEmpty())) {
                            for (int i = 0; i < phoneContacts.size(); i++) {
                                filteredContactsPhone.add(phoneContacts.get(i));
                            }
                        }
                        setPhoneAdapterContacts(filteredContactsPhone);
                    } else if (addedContactsPhone != null && !addedContactsPhone.isEmpty()) {
                        setEmptyStateVisibility(true);
                    } else {
                        setEmptyStateVisibility(true);

                        progressBar.setVisibility(View.VISIBLE);
                        getContactsTask = new GetContactsTask(this);
                        getContactsTask.execute();
                    }
                }
                setTitleAB();
                setRecyclersVisibility();
            }
        } else {
            isEKREnabled = false;
            ekrSwitch.setChecked(isEKREnabled);
            allowAddParticipantsSwitch.setChecked(isAllowAddParticipantsEnabled);
            setAddedAdapterContacts();

            if (contactType == CONTACT_TYPE_MEGA) {
                progressBar.setVisibility(View.VISIBLE);
                getContactsTask = new GetContactsTask(this);
                getContactsTask.execute();
            } else {
                queryIfHasReadContactsPermissions();
            }
        }

        if (onlyCreateGroup) {
            createNewGroup = true;
            setTitleAB();
        }

        setGetChatLinkVisibility();

        if (comesFromRecent && !onNewGroup) {
            if (isAsyncTaskRunning(getContactsTask)) {
                getContactsTask.cancel(true);
            }

            newGroup();
        }
        observeFlow();
    }

    private void showSharingSensitiveItemsWarningDialog(Bundle bundle, int type) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(AddContactActivity.this);

        String title = type % 2 == 1 ?
                getString(mega.privacy.android.shared.resources.R.string.hidden_item) :
                getString(mega.privacy.android.shared.resources.R.string.hidden_items);
        builder.setTitle(title);

        String message = "";
        if (type == 1) {
            message = getString(mega.privacy.android.shared.resources.R.string.share_hidden_item_link_description);
        } else if (type == 2) {
            message = getString(mega.privacy.android.shared.resources.R.string.share_hidden_item_links_description);
        } else if (type == 3) {
            message = getString(mega.privacy.android.shared.resources.R.string.share_hidden_folder_description);
        } else if (type == 4) {
            message = getString(mega.privacy.android.shared.resources.R.string.share_hidden_folders_description);
        }
        builder.setMessage(message);

        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.button_continue), (dialog, which) -> initialize(bundle));
        builder.setNegativeButton(getString(R.string.general_cancel), (dialog, which) -> finish());
        builder.show();
    }

    private void setEmptyStateVisibility(boolean visible) {
        if (visible) {
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
            if (contactType == CONTACT_TYPE_MEGA && (addedContactsMEGA == null || addedContactsMEGA.isEmpty())) {
                if (!isFromMeeting) {
                    emptyInviteButton.setVisibility(View.VISIBLE);
                } else {
                    emptyInviteButton.setVisibility(View.GONE);
                }
            } else {
                emptySubTextView.setVisibility(View.GONE);
                emptyInviteButton.setVisibility(View.GONE);
            }
        } else {
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
            emptySubTextView.setVisibility(View.GONE);
            emptyInviteButton.setVisibility(View.GONE);
        }
    }

    private void setGetChatLinkVisibility() {
        getChatLinkLayout.setVisibility(isEKREnabled ? View.GONE : View.VISIBLE);
    }

    private void queryIfHasReadContactsPermissions() {
        boolean hasReadContactsPermission = hasPermissions(this, Manifest.permission.READ_CONTACTS);
        if (!hasReadContactsPermission) {
            Timber.w("No read contacts permission");
            requestPermission((AddContactActivity) this,
                    REQUEST_READ_CONTACTS,
                    Manifest.permission.READ_CONTACTS);
            if (contactType == CONTACT_TYPE_DEVICE) {
                return;
            }
        }

        if (waitingForPhoneContacts) {
            filteredContactsShare.add(new ShareContactInfo());
            getPhoneContactsTask = new GetPhoneContactsTask(this);
            getPhoneContactsTask.execute();
            return;
        }

        setEmptyStateVisibility(true);
        progressBar.setVisibility(View.VISIBLE);
        getContactsTask = new GetContactsTask(this);
        getContactsTask.execute();

    }

    void setTitleAB() {
        Timber.d("setTitleAB");
        if (aB != null) {
            if (contactType == CONTACT_TYPE_MEGA) {
                if (comesFromChat) {
                    aB.setTitle(title);
                    if (addedContactsMEGA.size() > 0) {
                        aB.setSubtitle(getResources().getString(R.string.selected_items, addedContactsMEGA.size()));
                    } else {
                        aB.setSubtitle(null);
                    }
                } else if (createNewGroup && !onNewGroup) {
                    aB.setTitle(getString(R.string.title_new_group));
                    if (addedContactsMEGA.size() > 0) {
                        aB.setSubtitle(getResources().getString(R.string.selected_items, addedContactsMEGA.size()));
                    } else {
                        aB.setSubtitle(getString(R.string.add_participants_menu_item));
                    }
                }
            } else if (contactType == CONTACT_TYPE_DEVICE) {
                aB.setTitle(getString(R.string.invite_contacts));
                if (addedContactsPhone.size() > 0) {
                    aB.setSubtitle(getResources().getQuantityString(R.plurals.general_selection_num_contacts,
                            addedContactsPhone.size(), addedContactsPhone.size()));
                } else {
                    aB.setSubtitle(null);
                }
            } else {
                aB.setTitle(getString(R.string.share_with));
                if (addedContactsShare.size() > 0) {
                    aB.setSubtitle(getResources().getQuantityString(R.plurals.general_selection_num_contacts,
                            addedContactsShare.size(), addedContactsShare.size()));
                } else {
                    aB.setSubtitle(null);
                }
            }
        }
    }

    private void setError() {
        Timber.d("setError");
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp2px(18, outMetrics), dp2px(-10, outMetrics), dp2px(18, outMetrics), 0);
            typeContactEditText.setLayoutParams(params);
        }
        mailError.setVisibility(View.VISIBLE);

        ColorUtils.setErrorAwareInputAppearance(typeContactEditText, true);
    }

    private void quitError() {
        Timber.d("quitError");
        if (mailError.getVisibility() != View.GONE) {
            mailError.setVisibility(View.GONE);
        }
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp2px(18, outMetrics), dp2px(0, outMetrics), dp2px(18, outMetrics), 0);
            typeContactEditText.setLayoutParams(params);
        }

        ColorUtils.setErrorAwareInputAppearance(typeContactEditText, false);
    }

    void addShareContact(ShareContactInfo contact) {
        Timber.d("addShareContact");

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.collapseActionView();
        }
        if (!typeContactEditText.getText().toString().equals("")) {
            typeContactEditText.getText().clear();
        }
        typeContactEditText.clearFocus();
        hideKeyboard(addContactActivity, 0);

        int foundIndex = -1;
        for (int i = 0; i < addedContactsShare.size(); i++) {
            if (getShareContactMail(addedContactsShare.get(i)).equals(getShareContactMail(contact))) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex != -1) {
            deleteContact(foundIndex);
            return;
        } else {
            addedContactsShare.add(contact);
        }
        adapterShare.setContacts(addedContactsShare);
        isWarningMessageShown = checkForUnVerifiedContacts();
        mWarningMessage.setVisibility(isWarningMessageShown && isContactVerificationOn ? View.VISIBLE : View.GONE);
        if (adapterShare.getItemCount() - 1 >= 0) {
            mLayoutManager.scrollToPosition(adapterShare.getItemCount() - 1);
        }
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();

        if (adapterShareHeader != null) {
            if (adapterShareHeader.getItemCount() == 0) {
                setEmptyStateVisibility(true);

                String textToShow = String.format(getString(R.string.context_empty_contacts));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                            + "\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                            + "\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                emptyTextView.setText(result);
            } else {
                setEmptyStateVisibility(false);
            }
        }
        setRecyclersVisibility();
        refreshKeyboard();
    }

    /**
     * Method that selects the initially selected participants
     */
    private void addSelectedContactMEGA() {
        if (emailsContactsSelected != null && !emailsContactsSelected.isEmpty()) {
            for (String email : emailsContactsSelected) {
                if (filteredContactMEGA != null && !filteredContactMEGA.isEmpty()) {
                    for (int i = 0; i < filteredContactMEGA.size(); i++) {
                        MegaContactAdapter contact = filteredContactMEGA.get(i);
                        if (contact != null) {
                            if (getMegaContactMail(contact).equals(email)) {
                                contact.setSelected(true);
                                addContactMEGA(contact);
                                adapterMEGA.setContacts(filteredContactMEGA);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void addContactMEGA(MegaContactAdapter contact) {
        Timber.d("Contact: %s", contact.getFullName());

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.collapseActionView();
        }
        hideKeyboard(addContactActivity, 0);

        if (addedContactsMEGA.contains(contact)) {
            deleteContact(addedContactsMEGA.indexOf(contact));
            return;
        } else {
            addedContactsMEGA.add(contact);
        }
        adapterMEGAContacts.setContacts(addedContactsMEGA);
        if (adapterMEGAContacts.getItemCount() - 1 >= 0) {
            mLayoutManager.scrollToPosition(adapterMEGAContacts.getItemCount() - 1);
        }
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();
        if (adapterMEGA != null) {
            if (adapterMEGA.getItemCount() == 0) {
                showHeader(false);
                setEmptyStateVisibility(true);

                String textToShow = String.format(getString(R.string.context_empty_contacts));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                            + "\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                            + "\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                emptyTextView.setText(result);
            }
        }
        setRecyclersVisibility();
        refreshKeyboard();
    }

    void addContact(PhoneContactInfo contact) {
        Timber.d("Contact: %s, Mail: %s", contact.getName(), contact.getEmail());

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.collapseActionView();
        }
        if (!typeContactEditText.getText().toString().equals("")) {
            typeContactEditText.getText().clear();
        }
        typeContactEditText.clearFocus();
        hideKeyboard(addContactActivity, 0);

        boolean found = false;
        for (int i = 0; i < addedContactsPhone.size(); i++) {
            found = false;
            if (addedContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
                found = true;
                break;
            }
        }
        if (found) {
            showSnackbar(getString(R.string.contact_not_added));
        } else {
            addedContactsPhone.add(contact);
        }

        adapterContacts.setContacts(addedContactsPhone);
        if (adapterContacts.getItemCount() - 1 >= 0) {
            mLayoutManager.scrollToPosition(adapterContacts.getItemCount() - 1);
        }
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();

        if (adapterPhone != null) {
            if (adapterPhone.getItemCount() == 0) {
                showHeader(false);
                setEmptyStateVisibility(true);

                String textToShow = String.format(getString(R.string.context_empty_contacts));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                            + "\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                            + "\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                emptyTextView.setText(result);
            }
        }
        setRecyclersVisibility();
        refreshKeyboard();
    }

    public void deleteContact(int position) {
        Timber.d("Position: %s", position);
        if (position < 0) {
            return;
        }
        if (contactType == CONTACT_TYPE_MEGA) {
            if (position >= addedContactsMEGA.size()) {
                return;
            }
            MegaContactAdapter deleteContact = addedContactsMEGA.get(position);
            addedContactsMEGA.remove(deleteContact);

            int filteredPosition = filteredContactMEGA.indexOf(deleteContact);
            if (filteredPosition != INVALID_POSITION) {
                filteredContactMEGA.get(filteredPosition).setSelected(false);
            }
            adapterMEGA.setContacts(filteredContactMEGA);

            setSendInvitationVisibility();
            adapterMEGAContacts.setContacts(addedContactsMEGA);
            if (addedContactsMEGA.size() == 0) {
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        } else if (contactType == CONTACT_TYPE_DEVICE) {
            if (position >= addedContactsPhone.size()) {
                return;
            }
            PhoneContactInfo deleteContact = addedContactsPhone.get(position);
            if (deleteContact.getName() != null) {
                addFilteredContact(deleteContact);
            }
            addedContactsPhone.remove(deleteContact);
            setSendInvitationVisibility();
            adapterContacts.setContacts(addedContactsPhone);
            if (addedContactsPhone.size() == 0) {
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        } else {
            if (position >= addedContactsShare.size()) {
                return;
            }
            ShareContactInfo deleteContact = addedContactsShare.get(position);

            if (deleteContact.isPhoneContact()) {
                addFilteredContact(deleteContact.getPhoneContactInfo());
            } else if (deleteContact.isMegaContact()) {
                int filteredPosition = filteredContactsShare.indexOf(deleteContact);
                if (filteredPosition != INVALID_POSITION) {
                    filteredContactsShare.get(filteredPosition).getMegaContactAdapter().setSelected(false);
                    adapterShareHeader.setContacts(filteredContactsShare);
                }
            }

            addedContactsShare.remove(deleteContact);
            setSendInvitationVisibility();
            adapterShare.setContacts(addedContactsShare);
            if (addedContactsShare.size() == 0) {
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        }
        setTitleAB();
        setRecyclersVisibility();
        refreshKeyboard();
        setSearchVisibility();
        isWarningMessageShown = checkForUnVerifiedContacts();
        mWarningMessage.setVisibility(isWarningMessageShown && isContactVerificationOn ? View.VISIBLE : View.GONE);
    }

    public void showSnackbar(String message) {
        hideKeyboard(addContactActivity, 0);
        showSnackbar(relativeLayout, message);
    }

    private void addMEGAFilteredContact(MegaContactAdapter contact) {
        filteredContactMEGA.add(contact);
        Collections.sort(filteredContactMEGA, new Comparator<MegaContactAdapter>() {

            public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                String name1 = c1.getFullName();
                String name2 = c2.getFullName();
                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                if (res == 0) {
                    res = name1.compareTo(name2);
                }
                return res;
            }
        });

        int index = filteredContactMEGA.indexOf(contact);
        if (searchExpand) {
            if (searchAutoComplete != null) {
                inputString = searchAutoComplete.getText().toString();
            }
        } else {
            inputString = typeContactEditText.getText().toString();
        }

        if (contactType == CONTACT_TYPE_BOTH) {
            int i = filteredContactMEGA.indexOf(contact);
            ShareContactInfo contactToAdd = new ShareContactInfo(null, contact, null);
            if (filteredContactMEGA.size() == 1) {
                filteredContactsShare.add(0, new ShareContactInfo(true, true, false));
                filteredContactsShare.add(1, contactToAdd);
            } else {
                filteredContactsShare.add(i + 1, contactToAdd);
            }
            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask(this);
                filterContactsTask.execute();
            } else {
                adapterShareHeader.setContacts(filteredContactsShare);
                if (index >= 0 && index < adapterShareHeader.getItemCount()) {
                    linearLayoutManager.scrollToPosition(index);
                }
            }
            if (adapterShareHeader.getItemCount() != 0) {
                setEmptyStateVisibility(false);
            }
        } else {
            if (!onNewGroup) {
                if (inputString != null && !inputString.equals("")) {
                    filterContactsTask = new FilterContactsTask(this);
                    filterContactsTask.execute();
                } else {
                    adapterMEGA.setContacts(filteredContactMEGA);
                    if (index >= 0 && index < adapterMEGA.getItemCount()) {
                        linearLayoutManager.scrollToPosition(index);
                    }
                    if (adapterMEGA.getItemCount() != 0) {
                        showHeader(true);
                        setEmptyStateVisibility(false);
                    }
                    recyclerViewList.setVisibility(
                            adapterMEGA.getItemCount() > 0 ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    private void addFilteredContact(PhoneContactInfo contact) {
        Timber.d("addFilteredContact");
        filteredContactsPhone.add(contact);
        Collections.sort(filteredContactsPhone);
        int index = filteredContactsPhone.indexOf(contact);
        int position;

        Timber.d("Size filteredContactsPhone: %s", filteredContactsPhone.size());

        if (searchExpand) {
            if (searchAutoComplete != null) {
                inputString = searchAutoComplete.getText().toString();
            }
        } else {
            inputString = typeContactEditText.getText().toString();
        }

        if (contactType == CONTACT_TYPE_BOTH) {
            if (filteredContactsPhone.size() == 1) {
                filteredContactsShare.add(filteredContactsShare.size(), new ShareContactInfo(true, false, true));
                filteredContactsShare.add(filteredContactsShare.size(), new ShareContactInfo(contact, null, null));
                position = filteredContactsShare.size();
            } else {
                position = (adapterShareHeader.getItemCount() - filteredContactsPhone.size()) + index;
                if (position > 0 && (position + 1 <= filteredContactsShare.size())) {
                    filteredContactsShare.add(position + 1, new ShareContactInfo(contact, null, null));
                }
            }
            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask(this);
                filterContactsTask.execute();
            } else {
                adapterShareHeader.setContacts(filteredContactsShare);
                if (position >= 0 && position < adapterShareHeader.getItemCount()) {
                    linearLayoutManager.scrollToPosition(position);
                }
            }
            if (adapterShareHeader.getItemCount() != 0) {
                setEmptyStateVisibility(false);
            }
        } else {
            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask(this);
                filterContactsTask.execute();
            } else {
                adapterPhone.setContacts(filteredContactsPhone);
                if (index >= 0 && index < adapterPhone.getItemCount()) {
                    linearLayoutManager.scrollToPosition(index);
                }
                if (adapterPhone != null) {
                    if (adapterPhone.getItemCount() != 0) {
                        showHeader(true);
                        setEmptyStateVisibility(false);
                    }
                }
            }
        }
    }

    private MegaContactAdapter createMegaContact(MegaUser contact) {
        Contact contactDB = getContactDB(contact.getHandle());
        String fullName = getContactNameDB(contactDB);
        if (fullName == null) {
            fullName = contact.getEmail();
        }

        return new MegaContactAdapter(contactDB, contact, fullName);
    }

    void getVisibleMEGAContacts() {
        contactsMEGA = megaApi.getContacts();
        visibleContactsMEGA.clear();
        filteredContactMEGA.clear();
        addedContactsMEGA.clear();

        if (chatId != -1) {
            Timber.d("Add participant to chat");
            if (megaChatApi != null) {
                MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
                if (chat != null) {
                    long participantsCount = chat.getPeerCount();
                    boolean isModerator = chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR;
                    participantsLimitWarningView.setModerator(isModerator);
                    for (int i = 0; i < contactsMEGA.size(); i++) {
                        if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {

                            boolean found = false;

                            for (int j = 0; j < participantsCount; j++) {

                                long peerHandle = chat.getPeerHandle(j);

                                if (peerHandle == contactsMEGA.get(i).getHandle()) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                visibleContactsMEGA.add(createMegaContact(contactsMEGA.get(i)));
                            } else {
                                Timber.d("Removed from list - already included on chat: ");
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < contactsMEGA.size(); i++) {
                        Timber.d("Contact: %s_%d", contactsMEGA.get(i).getEmail(), contactsMEGA.get(i).getVisibility());
                        if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                            MegaContactAdapter megaContactAdapter = createMegaContact(contactsMEGA.get(i));
                            visibleContactsMEGA.add(megaContactAdapter);
                        }
                    }
                }
            }
        } else if ((multipleSelectIntent == 0 && nodeHandle != -1) || (multipleSelectIntent == 1 && nodeHandles.length == 1)) {
            ArrayList<MegaShare> shared = new ArrayList<>();
            if (multipleSelectIntent == 0) {
                shared.addAll(megaApi.getOutShares(megaApi.getNodeByHandle(nodeHandle)));
            } else {
                shared.addAll(megaApi.getOutShares(megaApi.getNodeByHandle(nodeHandles[0])));
            }
            boolean found;
            for (int i = 0; i < contactsMEGA.size(); i++) {
                found = false;
                Timber.d("Contact: %s_%d", contactsMEGA.get(i).getEmail(), contactsMEGA.get(i).getVisibility());
                if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    MegaContactAdapter megaContactAdapter = createMegaContact(contactsMEGA.get(i));
                    for (int j = 0; j < shared.size(); j++) {
                        if (getMegaContactMail(megaContactAdapter).equals(shared.get(j).getUser())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        visibleContactsMEGA.add(megaContactAdapter);
                    }

                }
            }
        } else {
            for (int i = 0; i < contactsMEGA.size(); i++) {
                Timber.d("Contact: %s_%d", contactsMEGA.get(i).getEmail(), contactsMEGA.get(i).getVisibility());
                if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    MegaContactAdapter megaContactAdapter = createMegaContact(contactsMEGA.get(i));
                    visibleContactsMEGA.add(megaContactAdapter);
                }
            }
        }

        Collections.sort(visibleContactsMEGA, new Comparator<MegaContactAdapter>() {

            public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                String name1 = c1.getFullName();
                String name2 = c2.getFullName();
                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                if (res == 0) {
                    res = name1.compareTo(name2);
                }
                return res;
            }
        });

        long handle;
        for (int i = 0; i < visibleContactsMEGA.size(); i++) {
            filteredContactMEGA.add(visibleContactsMEGA.get(i));
            if (contactType == CONTACT_TYPE_MEGA) {
                //Ask for presence info and last green
                handle = getMegaContactHandle(visibleContactsMEGA.get(i));
                if (handle != -1) {
                    int userStatus = megaChatApi.getUserOnlineStatus(handle);
                    if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                        Timber.d("Request last green for user");
                        megaChatApi.requestLastGreen(handle);
                    }
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    //Get the contacts explicitly added
    private ArrayList<PhoneContactInfo> getPhoneContacts() {
        Timber.d("getPhoneContacts");
        ArrayList<PhoneContactInfo> contactList = new ArrayList<>();

        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},
                    null,
                    null,
                    null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String name = cursor.getString(1);

                    String emailAddress = null;
                    Cursor cursor2 = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{String.valueOf(id)},
                            ContactsContract.Contacts.SORT_KEY_PRIMARY);

                    if (cursor2 != null && cursor2.moveToFirst()) {
                        try {
                            emailAddress = cursor2.getString(cursor2.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA));
                        } catch (IllegalArgumentException exception) {
                            Timber.w(exception, "Exception getting contact email");
                        }
                        cursor2.close();
                    }

                    if (emailAddress != null && emailAddress.contains("@") && !emailAddress.contains("s.whatsapp.net")) {
                        contactList.add(new PhoneContactInfo(id, name, emailAddress, null));
                    }
                }

                cursor.close();
            }
        } catch (Exception e) {
            Timber.w(e, "Exception getting phone contacts");
        }

        return contactList;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        quitError();
        refreshKeyboard();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Timber.d("onTextChanged: %s_ %d__%d__%d", s.toString(), start, before, count);
        if (contactType == CONTACT_TYPE_DEVICE) {
            if (s != null) {
                if (s.length() > 0) {
                    String temp = s.toString();
                    char last = s.charAt(s.length() - 1);
                    if (last == ' ') {
                        boolean isValid = isValidEmail(temp.trim());
                        if (isValid) {
                            confirmAddMail = temp.trim();
                            queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask(this);
                            queryIfContactSouldBeAddedTask.execute(false);
                            typeContactEditText.getText().clear();
                        } else {
                            setError();
                        }
                        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            hideKeyboard(addContactActivity, 0);
                        }
                    } else {
                        Timber.d("Last character is: %s", last);
                    }
                }
            }
        } else if (contactType == CONTACT_TYPE_BOTH) {
            if (s != null) {
                if (s.length() > 0) {
                    String temp = s.toString();
                    char last = s.charAt(s.length() - 1);
                    if (last == ' ') {
                        boolean isValid = isValidEmail(temp.trim());
                        if (isValid) {
                            confirmAddMail = temp.trim();
                            queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask(this);
                            queryIfContactSouldBeAddedTask.execute(false);
                            typeContactEditText.getText().clear();
                        } else {
                            setError();
                        }
                        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            hideKeyboard(addContactActivity, 0);
                        }
                    } else {
                        Timber.d("Last character is: %s", last);
                    }
                }
            }
        }

        if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask.cancel(true);
        }
        filterContactsTask = new FilterContactsTask(this);
        filterContactsTask.execute();
        refreshKeyboard();
    }

    @Override
    public void afterTextChanged(Editable editable) {
        refreshKeyboard();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        refreshKeyboard();
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String s = v.getText().toString();
            Timber.d("s: %s", s);
            if (s.isEmpty() || s.equals("null") || s.equals("")) {
                hideKeyboard(addContactActivity, 0);
            } else {
                if (contactType == CONTACT_TYPE_DEVICE) {
                    boolean isValid = isValidEmail(s.trim());
                    if (isValid) {
                        confirmAddMail = s.trim();
                        queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask(this);
                        queryIfContactSouldBeAddedTask.execute(false);
                        typeContactEditText.getText().clear();
                        hideKeyboard(addContactActivity, 0);
                    } else {
                        setError();
                    }
                    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideKeyboard(addContactActivity, 0);
                    }
                } else if (contactType == CONTACT_TYPE_BOTH) {
                    boolean isValid = isValidEmail(s.trim());
                    if (isValid) {
                        confirmAddMail = s.trim();
                        queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask(this);
                        queryIfContactSouldBeAddedTask.execute(false);
                        typeContactEditText.getText().clear();
                        hideKeyboard(addContactActivity, 0);
                    } else {
                        setError();
                    }
                    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideKeyboard(addContactActivity, 0);
                    }
                }
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask(this);
                filterContactsTask.execute();
            }
            return true;
        }
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
            if (contactType == CONTACT_TYPE_DEVICE) {
                if (addedContactsPhone.isEmpty() || addedContactsPhone == null) {
                    hideKeyboard(addContactActivity, 0);
                } else {
                    inviteContacts(addedContactsPhone);
                }
            } else if (contactType == CONTACT_TYPE_MEGA) {
                if (addedContactsMEGA.isEmpty() || addedContactsMEGA == null) {
                    hideKeyboard(addContactActivity, 0);
                } else {
                    setResultContacts(addedContactsMEGA, true);
                }
            } else {
                if (addedContactsShare.isEmpty() || addedContactsShare == null) {
                    hideKeyboard(addContactActivity, 0);
                } else {
                    shareWith(addedContactsShare);
                }
            }
            return true;
        }
        return false;
    }

    public void itemClick(String email, int adapter) {

        Timber.d("itemClick");

        if (contactType == CONTACT_TYPE_MEGA) {
            if (createNewGroup || comesFromChat) {
                if (adapter == MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT) {
                    if (searchExpand) {
                        if (searchAutoComplete != null) {
                            inputString = searchAutoComplete.getText().toString();
                        }
                    } else {
                        inputString = typeContactEditText.getText().toString();
                    }
                    if (inputString != null && !inputString.equals("")) {
                        for (int i = 0; i < queryContactMEGA.size(); i++) {
                            MegaContactAdapter contact = queryContactMEGA.get(i);
                            if (getMegaContactMail(contact).equals(email)) {
                                int filteredPosition = filteredContactMEGA.indexOf(contact);
                                if (filteredPosition != INVALID_POSITION) {
                                    filteredContactMEGA.get(filteredPosition).setSelected(true);
                                }
                                addContactMEGA(contact);
                                break;
                            }
                        }
                    } else {
                        for (int i = 0; i < filteredContactMEGA.size(); i++) {
                            MegaContactAdapter contact = filteredContactMEGA.get(i);
                            if (getMegaContactMail(contact).equals(email)) {
                                contact.setSelected(true);
                                addContactMEGA(contact);
                                adapterMEGA.setContacts(filteredContactMEGA);
                                break;
                            }
                        }
                    }

                    if (addedContactsMEGA.size() == 0) {
                        setSendInvitationVisibility();
                    }
                } else {
                    for (int i = 0; i < addedContactsMEGA.size(); i++) {
                        if (getMegaContactMail(addedContactsMEGA.get(i)).equals(email)) {
                            showConfirmationDeleteFromChat(addedContactsMEGA.get(i));
                            break;
                        }
                    }
                }
            } else {
                ArrayList<String> contacts = new ArrayList<>();
                contacts.add(email);
                startConversation(contacts, true, null);
            }
        }
        setSearchVisibility();
    }

    private void showConfirmationDeleteFromChat(final MegaContactAdapter contact) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE: {
                        addMEGAFilteredContact(contact);
                        addedContactsMEGA.remove(contact);
                        newGroupHeaderList.setText(getResources().getQuantityString(R.plurals.subtitle_of_group_chat, addedContactsMEGA.size(), addedContactsMEGA.size()));
                        adapterMEGA.setContacts(addedContactsMEGA);
                        adapterMEGAContacts.setContacts(addedContactsMEGA);

                        break;
                    }

                    case DialogInterface.BUTTON_NEGATIVE: {
                        //No button clicked
                        isConfirmDeleteShown = false;
                        break;
                    }
                }
            }
        };

        confirmDeleteMail = getMegaContactMail(contact);
        builder.setMessage(getString(R.string.confirmation_delete_contact, contact.getFullName()));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isConfirmDeleteShown = false;
            }
        });
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        isConfirmDeleteShown = true;
    }

    private void itemClick(View view, int position) {
        if (searchExpand) {
            if (searchAutoComplete != null) {
                inputString = searchAutoComplete.getText().toString();
            }
        } else {
            inputString = typeContactEditText.getText().toString();
        }
        if (contactType == CONTACT_TYPE_DEVICE) {

            if (adapterPhone == null) {
                return;
            }

            final PhoneContactInfo contact = adapterPhone.getItem(position);
            if (contact == null) {
                return;
            }

            if (inputString != null && !inputString.equals("")) {
                for (int i = 0; i < queryContactsPhone.size(); i++) {
                    if (queryContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
                        filteredContactsPhone.remove(queryContactsPhone.get(i));
                        queryContactsPhone.remove(i);
                        adapterPhone.setContacts(queryContactsPhone);
                    }
                }
            } else {
                for (int i = 0; i < filteredContactsPhone.size(); i++) {
                    if (filteredContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
                        filteredContactsPhone.remove(i);
                        adapterPhone.setContacts(filteredContactsPhone);
                        break;
                    }
                }
            }
            addContact(contact);
        } else if (contactType == CONTACT_TYPE_BOTH) {
            if (adapterShareHeader == null) {
                return;
            }

            final ShareContactInfo contact = adapterShareHeader.getItem(position);
            if (contact == null || contact.isHeader() || contact.isProgress()) {
                return;
            }

            if (contact.isPhoneContact()) {
                filteredContactsPhone.remove(contact.getPhoneContactInfo());
                if (filteredContactsPhone.size() == 0) {
                    filteredContactsShare.remove(filteredContactsShare.size() - 2);
                }
                filteredContactsShare.remove(contact);
            } else if (contact.isMegaContact()) {
                int contactPosition = filteredContactsShare.indexOf(contact);
                if (contactPosition != INVALID_POSITION) {
                    filteredContactsShare.get(contactPosition).getMegaContactAdapter().setSelected(true);
                }
            }

            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask(this);
                filterContactsTask.execute();
            } else {
                adapterShareHeader.setContacts(filteredContactsShare);
            }

            addShareContact(contact);
        }
        setSearchVisibility();
    }

    String getMegaContactMail(MegaContactAdapter contact) {
        String mail = null;
        if (contact != null) {
            if (contact.getMegaUser() != null && contact.getMegaUser().getEmail() != null) {
                mail = contact.getMegaUser().getEmail();
            } else if (contact.getContact() != null && contact.getContact().getEmail() != null) {
                mail = contact.getContact().getEmail();
            }
        }
        return mail;
    }

    private long getMegaContactHandle(MegaContactAdapter contact) {
        long handle = -1;
        if (contact != null) {
            if (contact.getMegaUser() != null && contact.getMegaUser().getHandle() != -1) {
                handle = contact.getMegaUser().getHandle();
            } else if (contact.getContact() != null && contact.getContact().getEmail() != null) {
                handle = contact.getContact().getUserId();
            }
        }
        return handle;
    }

    private void setResultContacts(ArrayList<MegaContactAdapter> addedContacts, boolean megaContacts) {
        Timber.d("setResultContacts");
        ArrayList<String> contactsSelected = new ArrayList<String>();
        String contactEmail;

        if (addedContacts != null) {
            for (int i = 0; i < addedContacts.size(); i++) {
                if (addedContacts.get(i).getMegaUser() != null && addedContacts.get(i).getContact() != null) {
                    contactEmail = addedContacts.get(i).getMegaUser().getEmail();
                } else {
                    contactEmail = addedContacts.get(i).getFullName();
                }
                if (contactEmail != null) {
                    contactsSelected.add(contactEmail);
                }
            }
        }
        Timber.d("Contacts selected: %s", contactsSelected.size());

        if (comesFromChat) {
            addParticipants(contactsSelected);
        } else if (onNewGroup) {
            String chatTitle = "";
            if (nameGroup != null && nameGroup.getText().length() > 0) {
                chatTitle = nameGroup.getText().toString();
                startConversation(contactsSelected, megaContacts, chatTitle);
            } else {
                startConversation(contactsSelected, megaContacts, null);
            }
        } else {
            newGroup();
        }
        hideKeyboard(addContactActivity, 0);
    }

    private void toInviteContact() {
        navigator.openInviteContactActivity(
                this,
                false
        );
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.layout_scan_qr) {
            Timber.d("Scan QR code pressed");
            if (isNecessaryDisableLocalCamera() != MEGACHAT_INVALID_HANDLE) {
                showConfirmationOpenCamera(this, ACTION_OPEN_QR, true);
                return;
            }
            initScanQR();
        } else if (id == R.id.add_contact_list_empty_invite_button) {
            toInviteContact();
        } else if (id == R.id.ekr_switch) {
            isEKREnabled = ekrSwitch.isChecked();
            setGetChatLinkVisibility();
        } else if (id == R.id.allow_add_participants_switch) {
            isAllowAddParticipantsEnabled = allowAddParticipantsSwitch.isChecked();
        } else if (id == R.id.fab_button_next) {
            if (contactType == CONTACT_TYPE_DEVICE) {
                inviteContacts(addedContactsPhone);
            } else if (contactType == CONTACT_TYPE_MEGA) {
                if (onlyCreateGroup && !isStartConversation && addedContactsMEGA.isEmpty()) {
                    showSnackbar(getString(R.string.error_creating_group_and_attaching_file));
                    return;
                }
                setResultContacts(addedContactsMEGA, true);
            } else {
                shareWith(addedContactsShare);
            }
            hideKeyboard(this, 0);
        }
    }

    public void initScanQR() {
        Intent intent = new Intent(this, QRCodeComposeActivity.class);
        intent.putExtra(INVITE_CONTACT, true);
        startActivityForResult(intent, SCAN_QR_FOR_ADD_CONTACTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Timber.d("onActivityResult");
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCAN_QR_FOR_ADD_CONTACTS && resultCode == Activity.RESULT_OK && intent != null) {
            String mail = intent.getStringExtra(INTENT_EXTRA_KEY_MAIL);

            if (mail != null && !mail.equals("")) {
                confirmAddMail = mail;
                queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask(this);
                queryIfContactSouldBeAddedTask.execute(true);
            }
        } else if (requestCode == REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
            Timber.d("REQUEST_INVITE_CONTACT_FROM_DEVICE OK");

            if (intent == null) {
                Timber.w("Return.....");
                return;
            }

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);

            if (contactsData != null) {
                ContactController cC = new ContactController(this);
                cC.inviteMultipleContacts(contactsData);
            }
        }
    }

    @Override
    public void onBackPressed() {
        PsaWebBrowser psaWebBrowser = getPsaWebBrowser();
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        retryConnectionsAndSignalPresence();

        if (onNewGroup) {
            if (addedContactsMEGA.contains(myContact)) {
                addedContactsMEGA.remove(myContact);
            }
            if (comesFromRecent) {
                finish();
            } else {
                returnToAddContacts();
                createMyContact();
            }
        } else if (createNewGroup && (newGroup || onlyCreateGroup)) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private void addParticipants(ArrayList<String> contacts) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contacts);

        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivity, 0);
        finish();
    }

    private void returnToAddContacts() {
        onNewGroup = false;
        setTitleAB();
        setRecyclersVisibility();
        addContactsLayout.setVisibility(View.VISIBLE);
        if (addedContactsMEGA.size() == 0) {
            containerAddedContactsRecyclerView.setVisibility(View.GONE);
        }
        setMegaAdapterContacts(filteredContactMEGA, MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
        newGroupLayout.setVisibility(View.GONE);
        visibilityFastScroller();
        setSendInvitationVisibility();
        setSearchVisibility();
        if (visibleContactsMEGA == null || visibleContactsMEGA.isEmpty()) {
            onBackPressed();
        }
    }

    private void createMyContact() {
        if (myContact == null) {
            Contact contactDB = dbH.findContactByHandle(MegaApiJava.base64ToUserHandle(megaApi.getMyUserHandle()));
            String myFullName = megaChatApi.getMyFullname();

            if (myFullName != null) {
                if (myFullName.trim().isEmpty()) {
                    myFullName = megaChatApi.getMyEmail();
                }
            } else {
                myFullName = megaChatApi.getMyEmail();
            }

            myContact = new MegaContactAdapter(contactDB, megaApi.getMyUser(), getString(R.string.chat_me_text_bracket, myFullName));
        }
    }

    void newGroup() {
        Timber.d("newGroup");

        if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask.cancel(true);
        }
        onNewGroup = true;
        searchExpand = false;
        if (aB != null) {
            aB.setTitle(getString(R.string.title_new_group));
            aB.setSubtitle(getString(R.string.subtitle_new_group));
        }

        createMyContact();

        addedContactsMEGA.add(myContact);
        newGroupHeaderList.setText(getResources().getQuantityString(R.plurals.subtitle_of_group_chat, addedContactsMEGA.size(), addedContactsMEGA.size()));

        if (searchMenuItem != null) {
            searchMenuItem.setVisible(false);
        }

        addContactsLayout.setVisibility(View.GONE);
        newGroupLayout.setVisibility(View.VISIBLE);

        setSendInvitationVisibility();
        setMegaAdapterContacts(addedContactsMEGA, MegaContactsAdapter.ITEM_VIEW_TYPE_LIST_GROUP_CHAT);
        visibilityFastScroller();
        setRecyclersVisibility();
        if (isConfirmDeleteShown) {
            for (int i = 0; i < addedContactsMEGA.size(); i++) {
                if (getMegaContactMail(addedContactsMEGA.get(i)).equals(confirmDeleteMail)) {
                    showConfirmationDeleteFromChat(addedContactsMEGA.get(i));
                    break;
                }
            }
        }
    }

    private void startConversation(ArrayList<String> contacts, boolean megaContacts, String chatTitle) {
        Timber.d("startConversation");
        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contacts);

        intent.putExtra(EXTRA_MEGA_CONTACTS, megaContacts);

        if (getChatLinkBox.isChecked() && (chatTitle == null || chatTitle.trim().isEmpty())) {
            new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                    .setTitle(getString(R.string.enter_group_name))
                    .setMessage(getString(R.string.alert_enter_group_name))
                    .setPositiveButton(getString(R.string.general_ok), null)
                    .show();
            return;
        }

        if (chatTitle != null) {
            intent.putExtra(EXTRA_CHAT_TITLE, chatTitle);
        }

        if (onNewGroup) {
            intent.putExtra(EXTRA_EKR, isEKREnabled);
            intent.putExtra(ALLOW_ADD_PARTICIPANTS, isAllowAddParticipantsEnabled);
            intent.putExtra(EXTRA_GROUP_CHAT, onNewGroup);
            intent.putExtra(EXTRA_CHAT_LINK, getChatLinkBox.isChecked());
        }

        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivity, 0);
        finish();
    }

    private void shareWith(ArrayList<ShareContactInfo> addedContacts) {
        Timber.d("shareWith");

        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null) {
            for (int i = 0; i < addedContacts.size(); i++) {
                contactsSelected.add(getShareContactMail(addedContacts.get(i)));
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        if (multipleSelectIntent == 0) {
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
            intent.putExtra(EXTRA_MULTISELECT, 0);
        } else if (multipleSelectIntent == 1) {
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
            intent.putExtra(EXTRA_MULTISELECT, 1);
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivity, 0);
        finish();
    }

    private void inviteContacts(ArrayList<PhoneContactInfo> addedContacts) {
        Timber.d("inviteContacts");

        String contactEmail = null;
        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null) {
            for (int i = 0; i < addedContacts.size(); i++) {
                contactEmail = addedContacts.get(i).getEmail();
                if (fromAchievements) {
                    if (contactEmail != null && !mailsFromAchievements.contains(contactEmail)) {
                        contactsSelected.add(contactEmail);
                    }
                } else {
                    if (contactEmail != null) {
                        contactsSelected.add(contactEmail);
                    }
                }
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        for (int i = 0; i < contactsSelected.size(); i++) {
            Timber.d("setResultContacts: %s", contactsSelected.get(i));
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivity, 0);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Timber.d("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_CONTACTS: {
                Timber.d("REQUEST_READ_CONTACTS");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean hasReadContactsPermissions = hasPermissions(this, Manifest.permission.READ_CONTACTS);
                    if (hasReadContactsPermissions && contactType == CONTACT_TYPE_DEVICE) {
                        filteredContactsPhone.clear();
                        setEmptyStateVisibility(true);

                        progressBar.setVisibility(View.VISIBLE);
                        new GetContactsTask(this).execute();
                    } else if (hasReadContactsPermissions && contactType == CONTACT_TYPE_BOTH) {
                        progressBar.setVisibility(View.VISIBLE);
                        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);

                        getContactsTask = new GetContactsTask(this);
                        getContactsTask.execute();
                    }
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    boolean hasReadContactsPermissions = hasPermissions(this, Manifest.permission.READ_CONTACTS);
                    queryPermissions = false;
                    supportInvalidateOptionsMenu();
                    if (!hasReadContactsPermissions && contactType == CONTACT_TYPE_DEVICE) {
                        Timber.w("Permission denied");
                        setTitleAB();
                        filteredContactsPhone.clear();
                        setEmptyStateVisibility(true);
                        emptyTextView.setText(R.string.no_contacts_permissions);

                        progressBar.setVisibility(View.GONE);
                    }
                }
                break;
            }
        }
    }

    void setRecyclersVisibility() {
        if (contactType == CONTACT_TYPE_MEGA) {
            if (filteredContactMEGA.size() > 0) {
                containerContacts.setVisibility(View.VISIBLE);
            } else {
                if (onNewGroup) {
                    containerContacts.setVisibility(View.VISIBLE);
                } else {
                    containerContacts.setVisibility(View.GONE);
                }
            }
        } else if (contactType == CONTACT_TYPE_DEVICE) {
            if (filteredContactsPhone.size() > 0) {
                containerContacts.setVisibility(View.VISIBLE);
            } else {
                containerContacts.setVisibility(View.GONE);
            }
        } else {
            if (filteredContactsShare.size() >= 2) {
                containerContacts.setVisibility(View.VISIBLE);
            } else {
                containerContacts.setVisibility(View.GONE);
            }
        }
    }

    void visibilityFastScroller() {
        fastScroller.setRecyclerView(recyclerViewList);
        if (contactType == CONTACT_TYPE_MEGA) {
            if (adapterMEGA == null) {
                fastScroller.setVisibility(View.GONE);
            } else {
                if (adapterMEGA.getItemCount() < MIN_ITEMS_SCROLLBAR_CONTACT) {
                    fastScroller.setVisibility(View.GONE);
                } else {
                    fastScroller.setVisibility(View.VISIBLE);
                }
            }
        } else if (contactType == CONTACT_TYPE_DEVICE) {
            if (adapterPhone == null) {
                fastScroller.setVisibility(View.GONE);
            } else {
                if (adapterPhone.getItemCount() < MIN_ITEMS_SCROLLBAR_CONTACT) {
                    fastScroller.setVisibility(View.GONE);
                } else {
                    fastScroller.setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (adapterShareHeader == null) {
                fastScroller.setVisibility(View.GONE);
            } else {
                if (adapterShareHeader.getItemCount() < MIN_ITEMS_SCROLLBAR_CONTACT) {
                    fastScroller.setVisibility(View.GONE);
                } else {
                    fastScroller.setVisibility(View.VISIBLE);
                }
            }
        }
        fastScroller.setUpScrollListener(new FastScrollerScrollListener() {
            @Override
            public void onScrolled() {
                hideFabButton();
            }

            @Override
            public void onScrolledToTop() {
                showFabButton();
            }
        });
    }

    private void showHeader(boolean isVisible) {
        headerContacts.setVisibility(!comesFromChat && isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {

    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {

    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {

    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
        if (requests != null) {
            for (int i = 0; i < requests.size(); i++) {
                MegaContactRequest cr = requests.get(i);
                if (cr != null) {
                    if ((cr.getStatus() == MegaContactRequest.STATUS_ACCEPTED) && (cr.isOutgoing())) {
                        Timber.d("ACCEPT OPR: %s cr.isOutgoing: %s cr.getStatus: %d", cr.getSourceEmail(), cr.isOutgoing(), cr.getStatus());
                        getContactsTask = new GetContactsTask(this);
                        getContactsTask.execute();
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    @Override
    public void onSetsUpdate(MegaApiJava api, ArrayList<MegaSet> sets) {

    }

    @Override
    public void onSetElementsUpdate(MegaApiJava api, ArrayList<MegaSetElement> elements) {

    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestStart: %s", request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.getNumber());

            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("OK INVITE CONTACT: %s", request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_DELETE) {
                        showSnackbar(getString(R.string.context_contact_invitation_deleted));
                    }
                } else {
                    Timber.w("ERROR: %d___%s", e.getErrorCode(), e.getErrorString());
                    if (e.getErrorCode() == MegaError.API_EEXIST) {
                        showSnackbar(getString(R.string.context_contact_already_exists, request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode() == MegaError.API_EARGS) {
                        showSnackbar(getString(R.string.error_own_email_as_contact));
                    } else {
                        showSnackbar(getString(R.string.general_error));
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    private void onChatPresenceLastGreen(long userhandle, int lastGreen) {
        Timber.d("onChatPresenceLastGreen");
        int state = megaChatApi.getUserOnlineStatus(userhandle);
        if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
            String formattedDate = lastGreenDate(this, lastGreen);
            if (userhandle != megaChatApi.getMyUserHandle()) {
                Timber.d("Status last green for the user: %s", userhandle);
//                Replace on visible MEGA contacts (all my visible contacts)
                ListIterator<MegaContactAdapter> itrReplace = visibleContactsMEGA.listIterator();
                while (itrReplace.hasNext()) {
                    MegaContactAdapter contactToUpdate = itrReplace.next();
                    if (contactToUpdate != null) {
                        if (getMegaContactHandle(contactToUpdate) == userhandle) {
                            contactToUpdate.setLastGreen(formattedDate);
                            break;
                        }
                    } else {
                        break;
                    }
                }
//                Replace on list adapter (filtered or search filtered MEGA contacts)
                int indexToReplace = -1;
                if (adapterMEGA != null && adapterMEGA.getContacts() != null) {
                    ListIterator<MegaContactAdapter> itrReplace2 = adapterMEGA.getContacts().listIterator();
                    while (itrReplace2.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace2.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                indexToReplace = itrReplace2.nextIndex() - 1;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (indexToReplace != -1) {
                        adapterMEGA.updateContactStatus(indexToReplace);
                    }
                }
//                Replace on filtered MEGA contacts (without search)
                if (filteredContactMEGA != null && filteredContactMEGA.size() > 0) {
                    ListIterator<MegaContactAdapter> itrReplace3 = filteredContactMEGA.listIterator();
                    while (itrReplace3.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace3.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
//                Replace, if exist, on search filtered MEGA contacts
                if (queryContactMEGA != null && queryContactMEGA.size() > 0) {
                    ListIterator<MegaContactAdapter> itrReplace4 = queryContactMEGA.listIterator();
                    while (itrReplace4.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace4.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
//                Replace, if exist, on added adapter and added MEGA contacts
                if (addedContactsMEGA != null && addedContactsMEGA.size() > 0) {
                    ListIterator<MegaContactAdapter> itrReplace5 = addedContactsMEGA.listIterator();
                    while (itrReplace5.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace5.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (adapterMEGAContacts != null) {
                        adapterMEGAContacts.setContacts(addedContactsMEGA);
                    }
                }
            }
            Timber.d("Date last green: %s", formattedDate);
        }
    }

    /**
     * Receive changes to OnChatPresenceLastGreen and make the necessary changes
     */
    private void checkChatChanges() {
        Disposable chatSubscription = getChatChangesUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(result -> result instanceof GetChatChangesUseCase.Result.OnChatPresenceLastGreen)
                .map(result -> (GetChatChangesUseCase.Result.OnChatPresenceLastGreen) result)
                .subscribe((next) -> {
                    long userHandle = next.component1();
                    int lastGreen = next.component2();
                    onChatPresenceLastGreen(userHandle, lastGreen);
                }, Timber::e);

        composite.add(chatSubscription);
    }

    private boolean checkForUnVerifiedContacts() {
        for (ShareContactInfo info : addedContactsShare) {
            if (!info.isMegaContact()) {
                return true;
            } else {
                boolean isVerified = megaApi.areCredentialsVerified(info.getMegaContactAdapter().getMegaUser());
                if (!isVerified) return true;
            }
        }
        return false;
    }

    private void observeFlow() {
        ViewExtensionsKt.collectFlow(this, viewModel.getState(), Lifecycle.State.STARTED, addContactState -> {
            if (adapterShare == null) {
                adapterShare = new ShareContactsAdapter(addContactActivity, addedContactsShare);
            }
            adapterShare.updateContactVerification(addContactState.isContactVerificationWarningEnabled());
            isContactVerificationOn = addContactState.isContactVerificationWarningEnabled();
            participantsLimitWarningView.setVisibility(addContactState.getShowUserLimitWarningDialog() ? View.VISIBLE : View.GONE);
            return Unit.INSTANCE;
        });
    }
}
