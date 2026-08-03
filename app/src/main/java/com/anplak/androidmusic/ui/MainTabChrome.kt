@file:Suppress("ktlint:standard:function-naming", "FunctionName")

package com.anplak.androidmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.anplak.androidmusic.ui.theme.Dimens

/**
 * Slim trailing action row for main tabs that need controls without a large title.
 *
 * Status-bar inset is owned by the outer [MainTabsContent] scaffold — do not add
 * [androidx.compose.foundation.layout.statusBarsPadding] here (avoids double padding).
 */
@Composable
fun CompactTabActions(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("compact_tab_actions"),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.compactTabActionsHeight)
                    .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
        HorizontalDivider()
    }
}
