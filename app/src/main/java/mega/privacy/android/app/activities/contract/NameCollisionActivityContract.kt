package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.namecollision.NameCollisionActivity.Companion.MESSAGE_RESULT
import mega.privacy.android.app.namecollision.NameCollisionActivity.Companion.getIntentForList
import mega.privacy.android.app.namecollision.NameCollisionActivity.Companion.getIntentForSingleItem
import mega.privacy.android.domain.entity.node.NameCollision

/**
 * A contract to start NameCollisionActivity and manage its result.
 */
class NameCollisionActivityContract : ActivityResultContract<ArrayList<NameCollision>, String?>() {

    override fun createIntent(context: Context, input: ArrayList<NameCollision>): Intent =
        if (input.size == 1) {
            getIntentForSingleItem(context, input.first())
        } else {
            getIntentForList(context, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? =
        when (resultCode) {
            Activity.RESULT_OK -> intent?.getStringExtra(MESSAGE_RESULT)
            else -> null
        }
}