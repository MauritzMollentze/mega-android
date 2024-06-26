package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.AccountType
import javax.inject.Inject

/**
 * Get subscription options filtered to subscription options available for purchase in the app, e.g. Pro I, Pro II or Pro III plans
 */
class GetAppSubscriptionOptionsUseCase @Inject constructor(
    private val getSubscriptionOptionsUseCase: GetSubscriptionOptionsUseCase
) {
    /**
     * Invoke
     * filter SubscriptionOptions to get monthly subscriptions options
     * @param months [Int]
     * @return [List<SubscriptionOptions>]
     */
    suspend operator fun invoke(months: Int) =
        getSubscriptionOptionsUseCase().filter { plan ->
            plan.accountType !== AccountType.BUSINESS &&
                    plan.accountType !== AccountType.PRO_FLEXI &&
                    plan.accountType !== AccountType.STARTER &&
                    plan.accountType !== AccountType.BASIC &&
                    plan.accountType !== AccountType.ESSENTIAL &&
                    plan.accountType !== AccountType.UNKNOWN &&
                    plan.months == months
        }
}