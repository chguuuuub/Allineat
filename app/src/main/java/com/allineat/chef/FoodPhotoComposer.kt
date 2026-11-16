package com.allineat.chef

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class IngredientPhotoInput(val name: String, val amount: String)

data class FoodPhotoResult(
    val bitmap: Bitmap?,
    val credits: List<String>,
    val missing: List<String>
)

object FoodPhotoComposer {
    private val searchTerms = mapOf(
        "土豆" to "potato vegetable", "番茄" to "tomato", "洋葱" to "onion",
        "胡萝卜" to "carrot", "青椒" to "green bell pepper", "西兰花" to "broccoli",
        "茄子" to "eggplant", "白菜" to "napa cabbage", "黄瓜" to "cucumber",
        "玉米" to "corn cob", "大蒜" to "garlic", "生姜" to "ginger root",
        "香菇" to "shiitake mushroom", "口蘑" to "button mushroom",
        "金针菇" to "enoki mushroom", "木耳" to "wood ear mushroom",
        "牛肉" to "raw beef meat", "猪肉" to "pork meat", "鸡肉" to "chicken meat",
        "鸭肉" to "duck meat", "羊肉" to "lamb meat", "培根" to "bacon",
        "三文鱼" to "salmon fillet", "鳕鱼" to "cod fish", "鲈鱼" to "sea bass fish",
        "虾仁" to "peeled shrimp", "鱿鱼" to "squid", "扇贝" to "scallop",
        "鸡蛋" to "egg food", "鸭蛋" to "duck egg", "豆腐" to "tofu",
        "粉丝" to "glass noodles", "腐竹" to "dried tofu skin", "紫菜" to "nori seaweed",
        "干香菇" to "dried shiitake mushroom"
    )

    suspend fun create(items: List<IngredientPhotoInput>, cacheDir: File): FoodPhotoResult = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Pair<IngredientPhotoInput, Bitmap>>()
        val credits = mutableListOf<String>()
        val missing = mutableListOf<String>()
        val dir = File(cacheDir, "commons_food_photos").apply { mkdirs() }

        items.forEach { item ->
            val key = item.name
            val cached = File(dir, "$key.jpg")
            val creditFile = File(dir, "$key.txt")
            var bitmap = if (cached.exists()) BitmapFactory.decodeFile(cached.absolutePath) else null
            var credit = if (creditFile.exists()) creditFile.readText() else ""

            if (bitmap == null) {
                val term = searchTerms[key]
                if (term != null) {
                    try {
                        val found = findCommonsPhoto(term)
                        if (found != null) {
                            bitmap = found.first
                            credit = found.second
                            cached.outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                            creditFile.writeText(credit)
                        }
                    } catch (_: Exception) {
                        bitmap = null
                    }
                }
            }
            if (bitmap != null) {
                photos += item to bitmap
                if (credit.isNotBlank()) credits += "$key：$credit"
            } else {
                missing += key
            }
        }

        FoodPhotoResult(
            bitmap = if (photos.isEmpty()) null else composePlate(photos),
            credits = credits,
            missing = missing
        )
    }

    private fun findCommonsPhoto(term: String): Pair<Bitmap, String>? {
        val encoded = URLEncoder.encode("filetype:bitmap $term", "UTF-8")
        val api = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=12&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=700&format=json"
        val connection = (URL(api).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 16000
            setRequestProperty("User-Agent", "AllineatAndroid/1.0 (food photo composition app)")
        }
        val jsonText = try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val root = JSONObject(jsonText)
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val meta = info.optJSONObject("extmetadata") ?: continue
            val license = clean(meta.optJSONObject("LicenseShortName")?.optString("value").orEmpty())
            val allowed = license.contains("CC0", true) ||
                license.contains("Public domain", true) ||
                license.contains("CC BY", true) ||
                license.contains("CC-BY", true)
            if (!allowed) continue

            val thumb = info.optString("thumburl").takeIf { it.startsWith("https://") } ?: continue
            val author = clean(meta.optJSONObject("Artist")?.optString("value").orEmpty()).ifBlank { "作者未注明" }
            val pageUrl = info.optString("descriptionurl")
            val imageConnection = (URL(thumb).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 16000
                setRequestProperty("User-Agent", "AllineatAndroid/1.0")
            }
            val bitmap = try {
                imageConnection.inputStream.use { BitmapFactory.decodeStream(it) }
            } finally {
                imageConnection.disconnect()
            }
            if (bitmap != null) {
                return bitmap to "$author；$license；$pageUrl"
            }
        }
        return null
    }

    private fun clean(value: String): String =
        value.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ")
            .replace("&amp;", "&").replace(Regex("\\s+"), " ").trim()

    private fun composePlate(photos: List<Pair<IngredientPhotoInput, Bitmap>>): Bitmap {
        val width = 900
        val height = 620
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.rgb(239, 234, 224))
        val centerX = width / 2f
        val centerY = height / 2f
        val plateRadius = 274f
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(190, 181, 166) }
        canvas.drawCircle(centerX, centerY + 8f, plateRadius + 8f, shadow)
        val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(centerX, centerY, plateRadius, plate)
        plate.color = Color.rgb(247, 244, 237)
        canvas.drawCircle(centerX, centerY, plateRadius - 13f, plate)

        val count = photos.size
        photos.forEachIndexed { index, entry ->
            val (picked, bitmap) = entry
            val angle = (2.0 * Math.PI * index / count) - Math.PI / 2.0
            val radius = if (count <= 2) 62f else 112f
            val cx = centerX + cos(angle).toFloat() * radius
            val cy = centerY + sin(angle).toFloat() * radius
            val itemScale = (picked.amount.toFloatOrNull() ?: 100f).coerceIn(20f, 500f) / 100f
            val w = (220f * min(1.18f, 0.78f + itemScale * 0.22f)).coerceIn(150f, 255f)
            val h = w * 0.64f
            val rect = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
            val oval = Path().apply { addOval(rect, Path.Direction.CW) }
            val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(45, 40, 30, 20) }
            canvas.drawOval(RectF(rect.left + 2f, rect.top + 5f, rect.right + 2f, rect.bottom + 5f), edge)
            canvas.save()
            canvas.clipPath(oval)
            val source = centerCropRect(bitmap, w / h)
            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, source, rect, imagePaint)
            canvas.restore()
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(90, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawOval(rect, border)
        }
        return output
    }

    private fun centerCropRect(bitmap: Bitmap, targetRatio: Float): Rect {
        val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (sourceRatio > targetRatio) {
            val cropWidth = (bitmap.height * targetRatio).toInt().coerceAtLeast(1)
            val left = (bitmap.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, bitmap.height)
        } else {
            val cropHeight = (bitmap.width / targetRatio).toInt().coerceAtLeast(1)
            val top = (bitmap.height - cropHeight) / 2
            Rect(0, top, bitmap.width, top + cropHeight)
        }
    }
}
