package com.clicktoearn.linkbox.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val onPurchaseComplete: (purchase: Purchase) -> Unit
) : PurchasesUpdatedListener {

    private val _billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    // SKU Constants - Updated to match Play Console configuration
    companion object {
        const val PRODUCT_PREMIUM_WEEKLY = "remove_ad"
        
        // Point Bundles
        const val PRODUCT_POINTS_100 = "points_100"
        const val PRODUCT_POINTS_350 = "point350"
        const val PRODUCT_POINTS_1000 = "points_1000"
    }

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails = _productDetails.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    fun startConnection() {
        _billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    queryProductDetails()
                    queryPurchases() // Check for existing purchases
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                // Implement re-connection logic here if needed, or simply let the user retry
            }
        })
    }

    private fun queryProductDetails() {
        val inAppList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM_WEEKLY)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_POINTS_100)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_POINTS_350)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_POINTS_1000)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(inAppList).build()

        _billingClient.queryProductDetailsAsync(inAppParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                updateProductDetailsMap(productDetailsList)
            }
        }
        
        // Keep subscription query capability just in case, but keep it empty for now 
        // unless you add actual subscription IDs later.
    }

    private fun updateProductDetailsMap(newList: List<ProductDetails>) {
        synchronized(this) {
            val currentMap = _productDetails.value.toMutableMap()
            newList.forEach { currentMap[it.productId] = it }
            _productDetails.value = currentMap
        }
    }
    
    // Check for existing active purchases (e.g. restores)
    private fun queryPurchases() {
        if (!_isConnected.value) return
        
        // Query Subscriptions
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
            
        _billingClient.queryPurchasesAsync(subsParams) { billingResult, purchasesList ->
             if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                 purchasesList.forEach { purchase ->
                     if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                         handlePurchase(purchase)
                     }
                 }
             }
        }

        // Query In-App Products
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        _billingClient.queryPurchasesAsync(inAppParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                    }
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productDetails = _productDetails.value[productId]
        
        if (productDetails == null) {
            android.util.Log.e("BillingManager", "Product details not found for: $productId. Make sure it's active in Play Console.")
            // If the activity is a component that can show messages, we could pass feedback
            // For now, let's at least log it. The UI should ideally check isConnected.
            return
        }
        
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        
        // Only set offer token for subscriptions
        if (productDetails.productType == BillingClient.ProductType.SUBS) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle user cancel
        } else {
            // Handle other errors
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Check if it's an INAPP product (consumable)
            val isConsumable = purchase.products.any { pid -> 
                pid == PRODUCT_POINTS_100 || pid == PRODUCT_POINTS_350 || pid == PRODUCT_POINTS_1000 || pid == PRODUCT_PREMIUM_WEEKLY
            }

            if (isConsumable) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                CoroutineScope(Dispatchers.IO).launch {
                    _billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            // Grant entitlement to the user
                            onPurchaseComplete(purchase)
                        }
                    }
                }
            } else if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                CoroutineScope(Dispatchers.IO).launch {
                    _billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                         if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                             // Grant entitlement to the user
                             onPurchaseComplete(purchase)
                         }
                    }
                }
            } else {
                 // Already acknowledged, just grant entitlement
                 onPurchaseComplete(purchase)
            }
        }
    }
}
