package mega.privacy.android.app.presentation.login

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityLoginBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.interfaces.OnKeyboardVisibilityListener
import mega.privacy.android.app.main.CreateAccountFragment
import mega.privacy.android.app.main.TourFragment
import mega.privacy.android.app.presentation.extensions.toConstant
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmEmailFragment
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmEmailFragmentV2
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.onboarding.TourFragmentV2
import mega.privacy.android.app.presentation.login.reportissue.ReportIssueViaEmailFragment
import mega.privacy.android.app.presentation.meeting.view.dialog.ACTION_JOIN_AS_GUEST
import mega.privacy.android.app.presentation.openlink.OpenLinkActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Login Activity.
 *
 * @property chatRequestHandler       [MegaChatRequestHandler]
 */
@AndroidEntryPoint
class LoginActivity : BaseActivity(), MegaRequestListenerInterface {

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    private val viewModel by viewModels<LoginViewModel>()

    private lateinit var binding: ActivityLoginBinding

    private var cancelledConfirmationProcess = false

    //Fragments
    private var loginFragment: LoginFragment? = null
    private var createAccountFragment: CreateAccountFragment? = null

    private var visibleFragment = 0
    private var sessionTemp: String? = null
    private var emailTemp: String? = null
    private var passwdTemp: String? = null
    private var firstNameTemp: String? = null
    private var lastNameTemp: String? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Timber.d("onBackPressed")
            retryConnectionsAndSignalPresence()

            when (visibleFragment) {
                Constants.CREATE_ACCOUNT_FRAGMENT -> showFragment(Constants.TOUR_FRAGMENT)
                Constants.REPORT_ISSUE_VIA_EMAIL_FRAGMENT -> showFragment(Constants.LOGIN_FRAGMENT)
                Constants.TOUR_FRAGMENT, Constants.CONFIRM_EMAIL_FRAGMENT -> finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        visibleFragment = intent.getIntExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)

        if (visibleFragment == Constants.LOGIN_FRAGMENT) {
            loginFragment = LoginFragment()
        }

        showFragment(visibleFragment)
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        megaApi.removeRequestListener(this)
        chatRequestHandler.setIsLoggingRunning(false)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        chatRequestHandler.setIsLoggingRunning(true)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()

        lifecycleScope.launch {
            if (savedInstanceState != null) {
                Timber.d("Bundle is NOT NULL")
                visibleFragment =
                    savedInstanceState.getInt(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            } else {
                visibleFragment =
                    intent?.getIntExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                        ?: Constants.LOGIN_FRAGMENT
                Timber.d("There is an intent! VisibleFragment: %s", visibleFragment)
            }

            viewModel.getEphemeral()?.let {
                visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT
                emailTemp = it.email
                passwdTemp = it.password
                sessionTemp = it.session
                firstNameTemp = it.firstName
                lastNameTemp = it.lastName
                megaApi.resumeCreateAccount(sessionTemp, this@LoginActivity)
            }

            showFragment(visibleFragment)
        }
    }

    private fun setupObservers() {
        collectFlow(viewModel.state, Lifecycle.State.RESUMED) { uiState ->
            with(uiState) {
                when {
                    isPendingToFinishActivity -> finish()
                    isPendingToShowFragment != null -> {
                        showFragment(isPendingToShowFragment.toConstant())
                        viewModel.isPendingToShowFragmentConsumed()
                    }

                    loginException != null -> {
                        if (loginException is LoginLoggedOutFromOtherLocation) {
                            showAlertLoggedOut()
                            viewModel.setLoginErrorConsumed()
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a snackbar.
     *
     * @param message Message to show.
     */
    fun showSnackbar(message: String) = showSnackbar(binding.relativeContainerLogin, message)

    /**
     * Show fragment
     *
     * @param fragmentType
     */
    fun showFragment(fragmentType: LoginFragmentType) {
        viewModel.setPendingFragmentToShow(fragmentType)
    }

    /**
     * Shows a fragment.
     *
     * @param visibleFragment The fragment to show.
     */
    private fun showFragment(visibleFragment: Int) {
        Timber.d("visibleFragment: %s", visibleFragment)
        this.visibleFragment = visibleFragment
        restrictOrientation()

        when (visibleFragment) {
            Constants.LOGIN_FRAGMENT -> {
                Timber.d("Show LOGIN_FRAGMENT")
                if (loginFragment == null) {
                    loginFragment = LoginFragment()
                }

                if (passwdTemp != null && emailTemp != null) {
                    viewModel.setTemporalCredentials(email = emailTemp, password = passwdTemp)
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, loginFragment ?: return)
                    .commitNowAllowingStateLoss()

                Util.setDrawUnderStatusBar(this, false)
            }

            Constants.CREATE_ACCOUNT_FRAGMENT -> {
                Timber.d("Show CREATE_ACCOUNT_FRAGMENT")
                if (createAccountFragment == null || cancelledConfirmationProcess) {
                    createAccountFragment = CreateAccountFragment()
                    cancelledConfirmationProcess = false
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, createAccountFragment ?: return)
                    .commitNowAllowingStateLoss()

                Util.setDrawUnderStatusBar(this, false)
            }

            Constants.TOUR_FRAGMENT -> {
                Timber.d("Show TOUR_FRAGMENT")
                // Need to make the call first, otherwise the TourFragment will be shown
                // before we successfully fetch the flag
                lifecycleScope.launch {
                    var isNewTourFragmentEnabled = false
                    runCatching { viewModel.isNewTourFragmentEnabled() }
                        .onSuccess { isNewTourFragmentEnabled = it }
                    val tourFragment =
                        if (isNewTourFragmentEnabled) {
                            TourFragmentV2().apply {
                                onLoginClick = {
                                    showFragment(LoginFragmentType.Login)
                                }
                                onCreateAccountClick = {
                                    showFragment(LoginFragmentType.CreateAccount)
                                }
                                onOpenLink = ::startOpenLinkActivity
                            }
                        } else {
                            when {
                                Constants.ACTION_RESET_PASS == intent?.action -> {
                                    TourFragment.newInstance(intent?.dataString, null)
                                }

                                Constants.ACTION_PARK_ACCOUNT == intent?.action -> {
                                    TourFragment.newInstance(null, intent?.dataString)
                                }

                                else -> {
                                    TourFragment.newInstance(null, null)
                                }
                            }
                        }

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_login, tourFragment)
                        .commitNowAllowingStateLoss()

                    Util.setDrawUnderStatusBar(this@LoginActivity, true)
                }
            }

            Constants.CONFIRM_EMAIL_FRAGMENT -> {
                val confirmEmailFragment =
                    if (viewModel.isFeatureEnabled(AppFeatures.NewConfirmEmailFragment)) {
                        ConfirmEmailFragmentV2.newInstance(emailTemp, firstNameTemp).apply {
                            onShowPendingFragment = ::showFragment
                            onSetTemporalEmail = ::setTemporalEmail
                            onCancelConfirmationAccount = ::cancelConfirmationAccount
                        }
                    } else {
                        ConfirmEmailFragment.newInstance(emailTemp, firstNameTemp)
                    }

                with(supportFragmentManager) {
                    beginTransaction()
                        .replace(R.id.fragment_container_login, confirmEmailFragment)
                        .commitNowAllowingStateLoss()
                }

                Util.setDrawUnderStatusBar(this, false)
            }


            Constants.REPORT_ISSUE_VIA_EMAIL_FRAGMENT -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, ReportIssueViaEmailFragment())
                    .commitNowAllowingStateLoss()

                Util.setDrawUnderStatusBar(this, false)
            }

        }
        if ((application as MegaApplication).isEsid) {
            showAlertLoggedOut()
        }
    }

    /**
     * Restrict to portrait mode always for mobile devices and tablets (already restricted via Manifest).
     * Allow the landscape mode only for tablets and only for TOUR_FRAGMENT.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun restrictOrientation() {
        if (Util.isTablet(this)) {
            requestedOrientation =
                if (visibleFragment == Constants.TOUR_FRAGMENT) {
                    Timber.d("Tablet landscape mode allowed")
                    ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                } else {
                    Timber.d("Tablet landscape mode restricted")
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
        }
    }

    override fun shouldSetStatusBarTextColor() = false

    /**
     * Shows a warning informing the account has been logged out.
     */
    private fun showAlertLoggedOut() {
        Timber.d("showAlertLoggedOut")
        (application as MegaApplication).isEsid = false

        if (!isFinishing) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_alert_logged_out))
                .setMessage(getString(R.string.error_server_expired_session))
                .setPositiveButton(getString(R.string.general_ok), null)
                .show()
        }
    }

    public override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        Util.setAppFontSize(this)

        if (intent == null) return

        if (intent?.action != null) {
            when (intent.action) {
                Constants.ACTION_CANCEL_CAM_SYNC -> showCancelCUWarning()
                Constants.ACTION_OVERQUOTA_TRANSFER -> showGeneralTransferOverQuotaWarning()
            }
        }
    }

    private fun showCancelCUWarning() {
        Timber.d("ACTION_CANCEL_CAM_SYNC")
        val title = getString(R.string.cam_sync_syncing)
        val text = getString(R.string.cam_sync_cancel_sync)

        Util.getCustomAlertBuilder(this, title, text, null)
            .setPositiveButton(getString(R.string.general_yes)) { _, _ ->
                viewModel.stopCameraUploads()
            }.setNegativeButton(getString(R.string.general_no), null)
            .show()
    }

    /**
     * Sets the received string as temporal email.
     *
     * @param emailTemp The temporal email.
     */
    fun setTemporalEmail(emailTemp: String) {
        this.emailTemp = emailTemp
        viewModel.setTemporalEmail(emailTemp)
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart - ${request.requestString}")
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate - ${request.requestString}")
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish - ${request.requestString}_${e.errorCode}")

        if (request.type == MegaRequest.TYPE_CREATE_ACCOUNT) {
            try {
                if (request.paramType == 1) {
                    if (e.errorCode == MegaError.API_OK) {
                        viewModel.setIsWaitingForConfirmAccount()
                    } else {
                        cancelConfirmationAccount()
                    }
                } // In case getParamType == 3 (creating ephemeral account ++) we don't need to trigger a fetch nodes (sdk performs internal)
                if (request.paramType == 4) {
                    if (e.errorCode == MegaError.API_OK) {
                        //Resuming ephemeral account ++, we need to trigger a fetch nodes
                        megaApi.fetchNodes()
                    }
                }
            } catch (exc: Exception) {
                Timber.e(exc)
            }
        }
    }

    /**
     * Cancels the account confirmation.
     */
    fun cancelConfirmationAccount() {
        Timber.d("cancelConfirmationAccount")
        viewModel.clearEphemeral()
        viewModel.clearUserCredentials()
        cancelledConfirmationProcess = true
        passwdTemp = null
        emailTemp = null
        viewModel.setTourAsPendingFragment()
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError - ${request.requestString}")
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putInt(Constants.VISIBLE_FRAGMENT, visibleFragment)
    }

    /**
     * Sets [OnKeyboardVisibilityListener].
     *
     * @param onKeyboardVisibilityListener The listener.
     */
    fun setKeyboardVisibilityListener(onKeyboardVisibilityListener: OnKeyboardVisibilityListener) {
        val parentView = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
        if (parentView == null) {
            Timber.w("Cannot set the keyboard visibility listener. Parent view is NULL.")
            return
        }

        parentView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            private var alreadyOpen = false
            private val defaultKeyboardHeightDP = 100
            private val EstimatedKeyboardDP = defaultKeyboardHeightDP + 48
            private val rect = Rect()

            override fun onGlobalLayout() {
                val estimatedKeyboardHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    EstimatedKeyboardDP.toFloat(),
                    parentView.resources.displayMetrics
                ).toInt()
                parentView.getWindowVisibleDisplayFrame(rect)

                val heightDiff = parentView.rootView.height - (rect.bottom - rect.top)
                val isShown = heightDiff >= estimatedKeyboardHeight

                if (isShown == alreadyOpen) {
                    return
                }
                alreadyOpen = isShown
                onKeyboardVisibilityListener.onVisibilityChanged(isShown)
            }
        })
    }

    /**
     * Sets temporal data for account creation.
     *
     * @param email    Email.
     * @param name     First name.
     * @param lastName Last name.
     * @param password Password.
     */
    fun setTemporalDataForAccountCreation(
        email: String,
        name: String,
        lastName: String,
        password: String,
    ) {
        setTemporalEmail(email)
        firstNameTemp = name
        lastNameTemp = lastName
        passwdTemp = password
        viewModel.setIsWaitingForConfirmAccount()
    }

    private fun startOpenLinkActivity(meetingLink: String) {
        val intent = Intent(this, OpenLinkActivity::class.java)
        intent.putExtra(ACTION_JOIN_AS_GUEST, "any")
        intent.data = Uri.parse(meetingLink)
        startActivity(intent)
    }

    companion object {

        /**
         * Flag for knowing if it was already in the login page.
         */
        @JvmField
        var isBackFromLoginPage = false
    }
}
