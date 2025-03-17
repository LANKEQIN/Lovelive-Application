package com.lovelive.dreamycolor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 详情页面的头部组件
 * 
 * 用于显示详情页面的标题和可选的副标题，支持自定义主题颜色。
 * 在角色详情页等地方使用，作为内容的顶部标题区域。
 * 
 * @param title 主标题文本
 * @param subtitle 副标题文本，可为null表示不显示副标题
 * @param themeColor 主题颜色，默认使用Material主题的primary颜色
 */
@Composable
fun DetailHeader(
    title: String,
    subtitle: String? = null,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = themeColor
            )
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 分节标题组件
 * 
 * 用于显示详情页面中各个部分的标题，带有左右两侧的分隔线装饰。
 * 常用于将详情页面分为多个逻辑部分，如"基本信息"、"角色简介"等。
 * 
 * @param title 分节的标题文本
 * @param color 标题和分隔线的颜色，默认使用Material主题的primary颜色
 */
@Composable
fun SectionTitle(
    title: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(0.15f)
                .height(2.dp),
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .height(2.dp),
            color = color.copy(alpha = 0.5f)
        )
    }
}

/**
 * 属性标签组件
 * 
 * 用于显示一个带有背景色的圆角标签，通常用来展示角色的属性、关系类型等简短信息。
 * 标签使用半透明背景和对应的文本颜色，使其在UI中突出显示。
 * 
 * @param modifier 应用于组件的修饰符
 * @param text 标签显示的文本内容
 * @param color 标签的主色调，默认使用Material主题的primary颜色
 */
@Composable
fun AttributeChip(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 信息项组件
 * 
 * 用于显示键值对形式的信息，如角色的年龄、学校、身高等基本信息。
 * 左侧为标签，右侧为对应的值，标签使用主题颜色，值使用默认文本颜色。
 * 
 * @param label 信息的标签名称（如"年龄"、"学校"等）
 * @param value 信息的值
 * @param color 标签的颜色，默认使用Material主题的primary颜色
 */
@Composable
fun InfoItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = color,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 关系项组件
 * 
 * 用于显示角色之间的关系信息，包括关系对象、关系类型和关系描述。
 * 以卡片形式展示，顶部显示关系对象名称和关系类型标签，下方显示关系描述文本。
 * 
 * @param target 关系对象的名称（如另一个角色的名字）
 * @param type 关系类型（如"朋友"、"同学"等）
 * @param description 关系的详细描述
 * @param color 关系对象名称和标签的颜色，默认使用Material主题的primary颜色
 */
@Composable
fun RelationshipItem(
    target: String,
    type: String,
    description: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = target,
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
                AttributeChip(
                    text = type,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 成就项组件
 * 
 * 用于显示带有圆点标记的列表项，通常用于展示角色的成就、特点或其他需要列表展示的信息。
 * 左侧为主题色的圆点，右侧为描述文本。
 * 
 * @param text 成就或特点的描述文本
 */
@Composable
fun AchievementItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
