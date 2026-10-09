package io.github.areswjd.piggybank.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.model.AppDesign
import io.github.areswjd.piggybank.ui.theme.spec

/** 화면 디자인 세 가지를 나란히 보여주고 하나를 고르게 한다. 각 칸은 그 디자인의 색과 글꼴로 그린다. */
@Composable
fun DesignPicker(selected: AppDesign, onSelect: (AppDesign) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AppDesign.entries.forEach { design ->
            DesignTile(
                design = design,
                selected = design == selected,
                onClick = { onSelect(design) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DesignTile(design: AppDesign, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spec = design.spec()
    val scheme = spec.light
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = scheme.background,
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer)
                        .border(1.dp, scheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(scheme.primary))
                }
                if (selected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .background(scheme.background, CircleShape),
                    )
                }
            }
            Box(Modifier.height(8.dp))
            Text(
                stringResource(design.labelRes()),
                style = spec.typography.titleSmall,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (design == AppDesign.DEFAULT) {
                Text(
                    stringResource(R.string.design_default),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun AppDesign.labelRes(): Int = when (this) {
    AppDesign.STRAWBERRY -> R.string.design_strawberry
    AppDesign.MINT -> R.string.design_mint
    AppDesign.BUTTER -> R.string.design_butter
}
