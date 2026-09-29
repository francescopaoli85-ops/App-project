package com.francescopaoli.northstar.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.francescopaoli.northstar.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Acquisto una tantum "Rimuovi pubblicità" (Google Play Billing).
 * Il prodotto va creato sulla Play Console con ID [PRODUCT_ID].
 */
class BillingManager(
    context: Context,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    companion object { const val PRODUCT_ID = "remove_ads" }

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var product: ProductDetails? = null
    private val _price = MutableStateFlow<String?>(null)

    /** Prezzo localizzato (es. "2,99 €"); null finché lo store non risponde. */
    val price: StateFlow<String?> = _price

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { loadProduct(); restore() }
            }
            override fun onBillingServiceDisconnected() {} // si riconnette al prossimo connect()
        })
    }

    private suspend fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build(),
            ),
        ).build()
        product = client.queryProductDetails(params).productDetailsList?.firstOrNull()
        _price.value = product?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    /** Ripristina l'acquisto (nuovo telefono, reinstallazione) e gestisce eventuali rimborsi. */
    suspend fun restore() {
        val r = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (r.billingResult.responseCode == BillingClient.BillingResponseCode.OK) handle(r.purchasesList, authoritative = true)
    }

    /** Apre il foglio di pagamento di Google Play. false se il prodotto non è disponibile. */
    fun buy(activity: Activity): Boolean {
        val p = product ?: run { connect(); return false }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(p).build()),
        ).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handle(purchases, authoritative = false) }
        }
    }

    private suspend fun handle(purchases: List<Purchase>, authoritative: Boolean) {
        val owned = purchases.filter { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        // gli acquisti vanno "confermati" entro 3 giorni, altrimenti Google li rimborsa
        owned.filterNot { it.isAcknowledged }.forEach {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
        }
        if (owned.isNotEmpty()) settings.setAdFree(true)
        else if (authoritative) settings.setAdFree(false)
    }
}
