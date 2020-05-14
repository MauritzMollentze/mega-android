package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaTransfer.*;

public class TransferWidget {

    private Context context;
    private MegaApiAndroid megaApi;

    private RelativeLayout transfersWidget;
    private ImageButton button;
    private ProgressBar progressBar;
    private ImageView status;

    public TransferWidget(Context context, RelativeLayout transfersWidget) {
        this.context = context;
        megaApi = MegaApplication.getInstance().getMegaApi();

        this.transfersWidget = transfersWidget;
        transfersWidget.setVisibility(GONE);
        button = transfersWidget.findViewById(R.id.transfers_button);
        progressBar = transfersWidget.findViewById(R.id.transfers_progress);
        status = transfersWidget.findViewById(R.id.transfers_status);
    }

    public void hide() {
        transfersWidget.setVisibility(GONE);
    }

    public void update() {
        update(-1);
    }

    public void update(int transferType) {
        if (!isOnline(context)
                || (context instanceof ManagerActivityLollipop && ManagerActivityLollipop.getDrawerItem() == ManagerActivityLollipop.DrawerItem.TRANSFERS)) return;

        if (getPendingTransfers() > 0) {
            updateState();
            setProgress(getProgress(), transferType);
        } else {
            hide();
        }
    }

    public void updateState() {
        if (megaApi.areTransfersPaused(TYPE_DOWNLOAD) || megaApi.areTransfersPaused(TYPE_UPLOAD)) {
            setPausedTransfers();
        } else {
            status.setVisibility(GONE);
        }
    }

    private void setPausedTransfers() {
        status.setVisibility(VISIBLE);
        status.setImageDrawable(getDrawable(R.drawable.ic_transfers_paused));
    }

    public void setFailedTransfers() {
        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_warning_progress_bar));
        status.setVisibility(VISIBLE);
        status.setImageDrawable(getDrawable(R.drawable.ic_transfers_error));
    }

    private void setProgress(int progress) {
        if (transfersWidget.getVisibility() != VISIBLE) {
            transfersWidget.setVisibility(VISIBLE);
        }
        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_progress_bar));
        progressBar.setProgress(progress);
    }

    public void setProgress(int progress, int typeTransfer) {
        setProgress(progress);

        int numPendingDownloads = megaApi.getNumPendingDownloads();
        int numPendingUploads = megaApi.getNumPendingUploads();
        boolean pendingDownloads = numPendingDownloads > 0;
        boolean pendingUploads = numPendingUploads > 0;

        if (typeTransfer == TYPE_DOWNLOAD || pendingDownloads && !pendingUploads) {
            button.setImageDrawable(getDrawable(R.drawable.ic_transfers_download));
        } else if (typeTransfer == TYPE_UPLOAD || pendingUploads && !pendingDownloads) {
            button.setImageDrawable(getDrawable(R.drawable.ic_transfers_upload));
        } else {
            button.setImageDrawable(getDrawable(numPendingDownloads > numPendingUploads ? R.drawable.ic_transfers_download : R.drawable.ic_transfers_upload));
        }
    }

    private Drawable getDrawable(int drawable) {
        return ContextCompat.getDrawable(context, drawable);
    }

    private int getPendingTransfers() {
        return megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();
    }

    private int getProgress() {
        long totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApi.getTotalUploadBytes();
        long totalSizeTransfered = megaApi.getTotalDownloadedBytes() + megaApi.getTotalUploadedBytes();

        return (int) Math.round((double) totalSizeTransfered / totalSizePendingTransfer * 100);
    }
}
