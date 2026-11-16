package com.allineat.chef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AllineatApp() }
    }
}

private data class Food(val name: String, val category: String)
private data class PickedFood(val food: Food, val amount: String = "100", val cut: String = "切块")

private val categories = listOf("全部", "果蔬", "菌菇", "肉禽", "鱼类", "蛋类", "海鲜", "干货")
private val foods = listOf(
    Food("土豆","果蔬"), Food("番茄","果蔬"), Food("洋葱","果蔬"), Food("胡萝卜","果蔬"),
    Food("青椒","果蔬"), Food("西兰花","果蔬"), Food("茄子","果蔬"), Food("白菜","果蔬"),
    Food("黄瓜","果蔬"), Food("玉米","果蔬"), Food("大蒜","果蔬"), Food("生姜","果蔬"),
    Food("香菇","菌菇"), Food("口蘑","菌菇"), Food("金针菇","菌菇"), Food("木耳","菌菇"),
    Food("牛肉","肉禽"), Food("猪肉","肉禽"), Food("鸡肉","肉禽"), Food("鸭肉","肉禽"),
    Food("羊肉","肉禽"), Food("培根","肉禽"), Food("三文鱼","鱼类"), Food("鳕鱼","鱼类"),
    Food("鲈鱼","鱼类"), Food("虾仁","海鲜"), Food("鱿鱼","海鲜"), Food("扇贝","海鲜"),
    Food("鸡蛋","蛋类"), Food("鸭蛋","蛋类"), Food("豆腐","干货"), Food("粉丝","干货"),
    Food("腐竹","干货"), Food("紫菜","干货"), Food("干香菇","干货")
)
private val seasonings = listOf("盐","白糖","生抽","老抽","料酒","米醋","蚝油","豆瓣酱","黑胡椒","辣椒","黄油","橄榄油","奶油","芝士","迷迭香","罗勒")
private val methods = listOf("炒","煎","蒸","煮","炖","煲","烤","焗","炸","凉拌")
private val cuts = listOf("切块","切片","切丝","切丁","剁碎","整块")
private val vessels = listOf("家常炒锅","汤碗","深盘","浅盘","砂锅","烤盘","蒸盘")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllineatApp() {
    val picked = remember { mutableStateListOf<PickedFood>() }
    val season = remember { mutableStateListOf<String>() }
    val cook = remember { mutableStateListOf("炒") }
    var category by remember { mutableStateOf("全部") }
    var search by remember { mutableStateOf("") }
    var vessel by remember { mutableStateOf("家常炒锅") }
    var cuisine by remember { mutableStateOf("中式") }
    var result by remember { mutableStateOf("") }
    var dish by remember { mutableStateOf("") }
    var dishImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoStatus by remember { mutableStateOf("生成菜肴后，将从开放素材库获取真实食材照片并在本机合成。") }
    var photoCredits by remember { mutableStateOf<List<String>>(emptyList()) }
    var missingPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    MaterialTheme(colorScheme = lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF28745B),
        secondary = androidx.compose.ui.graphics.Color(0xFFE59B43),
        background = androidx.compose.ui.graphics.Color(0xFFF6F7F3)
    )) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Allineat", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("全能厨师 · 从食材组合一道菜", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AssistChip(onClick = { cuisine = if (cuisine == "中式") "西式" else "中式" }, label = { Text(cuisine) })
                }

                Heading("01 选择食材", "选择食材后可调整用量和切法")
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索食材，例如：牛肉、番茄") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(categories) { c -> FilterChip(category == c, { category = c }, label = { Text(c) }) }
                }
                val shown = foods.filter { (category == "全部" || it.category == category) && (search.isBlank() || it.name.contains(search.trim())) }
                shown.chunked(3).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.forEach { food ->
                            val exists = picked.any { it.food.name == food.name }
                            OutlinedButton(
                                onClick = { if (exists) picked.removeAll { it.food.name == food.name } else picked.add(PickedFood(food)) },
                                modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (exists) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            ) { Text((if (exists) "✓ " else "") + food.name, fontSize = 12.sp) }
                        }
                        repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                if (picked.isNotEmpty()) {
                    Heading("已选食材（${picked.size}）", "修改数量与加工方式")
                    picked.toList().forEach { item ->
                        val idx = picked.indexOfFirst { it.food.name == item.food.name }
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.food.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { picked.removeAll { it.food.name == item.food.name } }) { Text("移除") }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text("用量")
                                    OutlinedTextField(
                                        value = item.amount,
                                        onValueChange = { v -> if (v.length <= 5 && v.all { it.isDigit() }) picked[idx] = item.copy(amount = v) },
                                        modifier = Modifier.width(112.dp), singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        suffix = { Text("g") }
                                    )
                                    var expanded by remember(item.food.name) { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { expanded = true }) { Text(item.cut, fontSize = 12.sp) }
                                        DropdownMenu(expanded, { expanded = false }) {
                                            cuts.forEach { cut -> DropdownMenuItem(text = { Text(cut) }, onClick = {
                                                picked[idx] = item.copy(cut = cut); expanded = false
                                            }) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Heading("02 调味搭配", "可多选中式或西式调味")
                ChipGrid(seasonings, season) { v -> if (season.contains(v)) season.remove(v) else season.add(v) }

                Heading("03 烹饪方式", "按希望的顺序选择，可多选")
                ChipGrid(methods, cook) { v ->
                    if (cook.contains(v)) { if (cook.size > 1) cook.remove(v) } else cook.add(v)
                }

                Heading("04 餐具", "选择成品呈现方式")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(vessels) { v -> FilterChip(vessel == v, { vessel = v }, label = { Text(v) }) }
                }

                Button(
                    onClick = {
                        val selected = picked.toList()
                        dish = makeName(selected, cook.toList(), cuisine)
                        result = makeRecipe(selected, season.toList(), cook.toList(), vessel, cuisine)
                        dishImage = null
                        photoCredits = emptyList()
                        missingPhotos = emptyList()
                        photoStatus = "正在获取真实食材照片并整理预览……"
                        scope.launch {
                            val photoResult = FoodPhotoComposer.create(selected.map { IngredientPhotoInput(it.food.name, it.amount) }, context.cacheDir)
                            dishImage = photoResult.bitmap?.asImageBitmap()
                            photoCredits = photoResult.credits
                            missingPhotos = photoResult.missing
                            photoStatus = if (photoResult.bitmap == null) {
                                "没有找到可用且许可信息明确的食材照片。请联网后重试，或减少食材种类。"
                            } else if (photoResult.missing.isNotEmpty()) {
                                "已整理找到照片的食材；以下食材缺少素材：" + photoResult.missing.joinToString("、")
                            } else {
                                "下方为真实食材照片预览，按食材分别展示；不是烹饪成品照片。"
                            }
                        }
                    },
                    enabled = picked.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Icon(Icons.Default.Restaurant, null)
                    Spacer(Modifier.width(8.dp))
                    Text("生成我的菜肴", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }

                if (result.isNotBlank()) {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("生成结果 · 食材实景预览", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            DishPhoto(dishImage, photoStatus)
                            if (photoCredits.isNotEmpty()) {
                                Text("照片来源与许可", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                photoCredits.forEach { credit -> Text(credit, fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Text(dish, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("摆盘：$vessel · ${cuisine}风格", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider()
                            Text(result, fontSize = 14.sp, lineHeight = 22.sp)
                            Text("安全提示：本地规则仅提供烹饪建议。肉类、鱼类和海鲜须确认中心充分熟透；实际时间需按厚度、设备和食品安全规范调整。", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Text("Allineat · Food for everyone", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DishPhoto(image: ImageBitmap?, status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEFEAE0)
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "所选食材的真实照片分项预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
                Text(status, color = Color(0xFF625B50), fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
    if (image != null) {
        Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Heading(title: String, note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChipGrid(options: List<String>, selected: List<String>, toggle: (String) -> Unit) {
    options.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            row.forEach { value -> FilterChip(selected.contains(value), { toggle(value) }, label = { Text(value, fontSize = 12.sp) }) }
        }
    }
}

private fun makeName(items: List<PickedFood>, methods: List<String>, cuisine: String): String {
    val main = items.firstOrNull { it.food.category in listOf("肉禽", "鱼类", "海鲜", "蛋类") } ?: items.first()
    val veg = items.firstOrNull { it.food.category == "果蔬" && it.food.name != main.food.name }
    val method = methods.firstOrNull() ?: "创意"
    return (if (cuisine == "西式") method + "制" else "") + main.food.name + (veg?.food?.name ?: "") + (if (cuisine == "中式") method + "风味" else "")
}

private fun makeRecipe(items: List<PickedFood>, season: List<String>, methods: List<String>, vessel: String, cuisine: String): String {
    val out = mutableListOf<String>()
    out += "【食材清单】"
    items.forEach { out += "• ${it.food.name} ${it.amount.ifBlank { "适量" }}g，${it.cut}" }
    out += ""
    out += "【调味】"
    out += if (season.isEmpty()) "盐及基础调味按口味少量添加" else season.joinToString("、")
    out += ""
    out += "【制作步骤】"
    out += "1. 将食材清洗干净，按所选切法处理；肉类与即食食材分开操作。"
    out += "2. 按食材成熟速度安排下锅次序，较难熟的先处理，易熟食材后加入。"
    out += "3. 烹饪流程：${methods.joinToString(" → ")}。控制火力，分批操作，避免锅内食材过多。"
    if (methods.any { it in listOf("蒸", "煮", "炖", "煲") }) out += "4. 加热至所有食材达到安全熟度；需要汤汁时可适量加水或高汤。"
    if (methods.any { it in listOf("煎", "炒", "炸") }) out += "4. 锅具预热后下料，适时翻动，注意避免外焦内生。"
    if (methods.any { it in listOf("烤", "焗") }) out += "4. 烤箱预热后烹制，中途观察上色并确认内部熟度。"
    out += ""
    out += "【装盘】"
    out += "完成后装入$vessel，整理形态并按口味点缀。"
    out += ""
    out += "【风味方向】"
    out += if (cuisine == "西式") "可用黄油、橄榄油、黑胡椒或香草建立香气层次，突出食材本味。" else "先建立咸鲜底味，再按需要增加酸、甜、辣；分次调味，避免过咸。"
    return out.joinToString("\n")
}
