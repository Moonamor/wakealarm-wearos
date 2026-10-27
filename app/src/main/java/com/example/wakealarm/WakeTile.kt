package com.example.wakealarm

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Watch-face carousel tile ("widget"): shows the listening state and toggles it with one tap.
 */
class WakeTile : TileService() {

    private companion object {
        const val RES_VERSION = "2"
        const val ID_TOGGLE = "toggle"
        const val ID_OPEN = "open"
        const val IMG_BELL = "bell"

        val YELLOW = argb(0xFFFFD600.toInt())
        val INK = argb(0xFF0B0F14.toInt())
        val CARD = argb(0xFF161B22.toInt())
        val MUTED = argb(0xFF9AA4AE.toInt())
        val RED = argb(0xFFE53935.toInt())
        val WHITE = argb(0xFFFFFFFF.toInt())
    }

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        // A tap on the toggle chip arrives here as a LoadAction with the clickable id.
        if (requestParams.currentState.lastClickableId == ID_TOGGLE) Listening.toggle(this)
        val on = Listening.load(this)

        val layout = Layout.Builder().setRoot(root(on, requestParams.deviceConfiguration)).build()
        val tile = Tile.Builder()
            .setResourcesVersion(RES_VERSION)
            .setTileTimeline(Timeline.fromLayoutElement(layout.root!!))
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(
            Resources.Builder()
                .setVersion(RES_VERSION)
                .addIdToImageMapping(
                    IMG_BELL,
                    ImageResource.Builder()
                        .setAndroidResourceByResId(
                            AndroidImageResourceByResId.Builder().setResourceId(R.drawable.ic_bell).build()
                        ).build()
                ).build()
        )

    private fun root(on: Boolean, device: DeviceParameters): LayoutElementBuilders.LayoutElement {
        val toggle = Clickable.Builder()
            .setId(ID_TOGGLE)
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .build()
        val open = Clickable.Builder()
            .setId(ID_OPEN)
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build()
                    ).build()
            ).build()

        val accent = if (on) YELLOW else MUTED

        val badge = Box.Builder()
            .setWidth(dp(48f)).setHeight(dp(48f))
            .setModifiers(
                Modifiers.Builder()
                    .setClickable(open)
                    .setBackground(
                        Background.Builder().setColor(accent)
                            .setCorner(Corner.Builder().setRadius(dp(24f)).build()).build()
                    ).build()
            )
            .addContent(
                LayoutElementBuilders.Image.Builder()
                    .setResourceId(IMG_BELL)
                    .setWidth(dp(26f)).setHeight(dp(26f))
                    .setColorFilter(LayoutElementBuilders.ColorFilter.Builder().setTint(INK).build())
                    .build()
            ).build()

        val title = Text.Builder(this, if (on) "يستمع للتنبيهات" else "الاستماع متوقف")
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(if (on) WHITE else MUTED)
            .build()

        val chip = Chip.Builder(this, toggle, device)
            .setPrimaryLabelContent(if (on) "إيقاف الاستماع" else "تشغيل الاستماع")
            .setWidth(dp(150f))
            .setChipColors(
                if (on) ChipColors(RED, WHITE) else ChipColors(YELLOW, INK)
            )
            .build()

        val column = Column.Builder()
            .setWidth(expand()).setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(badge)
            .addContent(Spacer.Builder().setHeight(dp(8f)).build())
            .addContent(title)
            .addContent(Spacer.Builder().setHeight(dp(10f)).build())
            .addContent(chip)
            .build()

        return Box.Builder()
            .setWidth(expand()).setHeight(expand())
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(Background.Builder().setColor(INK).build())
                    .setPadding(Padding.Builder().setAll(dp(10f)).build())
                    .build()
            )
            .addContent(column)
            .build()
    }
}
