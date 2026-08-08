package com.example.ads

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeDocumentAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.getNativeAdUnitId()
) {
    val context = LocalContext.current
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                loadedNativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoadFailed = true
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            loadedNativeAd?.destroy()
        }
    }

    val ad = loadedNativeAd
    if (ad != null && !adLoadFailed) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                AndroidView(
                    factory = { ctx ->
                        createNativeAdView(ctx, ad)
                    },
                    update = { view ->
                        populateNativeAdView(ad, view)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun createNativeAdView(context: Context, nativeAd: NativeAd): NativeAdView {
    val nativeAdView = NativeAdView(context)
    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Header Row: Ad Badge + Headline + AdChoices
    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Ad / Sponsored Label
    val badgeView = TextView(context).apply {
        text = "Ad"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setTextColor(AndroidColor.WHITE)
        setBackgroundColor(AndroidColor.parseColor("#3B82F6")) // DocuPro Blue
        setPadding(12, 4, 12, 4)
        typeface = Typeface.DEFAULT_BOLD
    }
    headerRow.addView(badgeView)

    // Headline
    val headlineView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(AndroidColor.parseColor("#0F172A"))
        typeface = Typeface.DEFAULT_BOLD
        setPadding(16, 0, 16, 0)
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
    }
    headerRow.addView(headlineView)
    nativeAdView.headlineView = headlineView

    // AdChoices
    val adChoicesView = AdChoicesView(context)
    headerRow.addView(adChoicesView)
    nativeAdView.adChoicesView = adChoicesView

    rootLayout.addView(headerRow)

    // Body Row: Icon + Body Text + Call to Action Button
    val contentRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 12, 0, 0)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Icon
    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(96, 96).apply {
            marginEnd = 16
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    contentRow.addView(iconView)
    nativeAdView.iconView = iconView

    // Body text
    val bodyView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(AndroidColor.parseColor("#475569"))
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
    }
    contentRow.addView(bodyView)
    nativeAdView.bodyView = bodyView

    // CTA Button
    val ctaButton = Button(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(AndroidColor.WHITE)
        setBackgroundColor(AndroidColor.parseColor("#2563EB"))
        setPadding(24, 8, 24, 8)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 12
        }
    }
    contentRow.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    rootLayout.addView(contentRow)
    nativeAdView.addView(rootLayout)

    populateNativeAdView(nativeAd, nativeAdView)
    return nativeAdView
}

private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

    val body = nativeAd.body
    if (body != null) {
        nativeAdView.bodyView?.visibility = View.VISIBLE
        (nativeAdView.bodyView as? TextView)?.text = body
    } else {
        nativeAdView.bodyView?.visibility = View.GONE
    }

    val icon = nativeAd.icon
    if (icon != null) {
        nativeAdView.iconView?.visibility = View.VISIBLE
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(icon.drawable)
    } else {
        nativeAdView.iconView?.visibility = View.GONE
    }

    val cta = nativeAd.callToAction
    if (cta != null) {
        nativeAdView.callToActionView?.visibility = View.VISIBLE
        (nativeAdView.callToActionView as? Button)?.text = cta
    } else {
        nativeAdView.callToActionView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}
