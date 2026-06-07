# Tích hợp Android (Kotlin) với Firestore

Tài liệu hướng dẫn app Android (Kotlin) đọc dữ liệu nhạc từ **cùng một project Firestore** mà web `music-admin` đang ghi vào. Bao gồm: cấu trúc dữ liệu, data class Kotlin, cách kết nối và các truy vấn thường dùng (danh sách, tìm kiếm, phân trang, top bài hát, banner quảng cáo, tăng lượt xem, lắng nghe realtime).

## Mục lục

- [1. Cấu trúc dữ liệu Firestore](#1-cấu-trúc-dữ-liệu-firestore)
- [2. Thiết lập Firebase trong Android](#2-thiết-lập-firebase-trong-android)
- [3. Data class Kotlin](#3-data-class-kotlin)
- [4. Đọc dữ liệu](#4-đọc-dữ-liệu)
- [5. Phân trang](#5-phân-trang)
- [6. Tìm kiếm theo tên](#6-tìm-kiếm-theo-tên)
- [7. Top bài hát & tăng lượt xem](#7-top-bài-hát--tăng-lượt-xem)
- [8. Banner quảng cáo](#8-banner-quảng-cáo)
- [9. Lắng nghe realtime](#9-lắng-nghe-realtime)
- [10. Lưu ý quan trọng](#10-lưu-ý-quan-trọng)

---

## 1. Cấu trúc dữ liệu Firestore

Có 4 collection ở cấp gốc: `songs`, `singers`, `categories`, `advertisements`. Các collection `songs` dùng **id tham chiếu + tên denormalized** (đã copy sẵn tên vào document) nên app **không cần join** — đọc 1 document là có đủ tên ca sĩ và thể loại để hiển thị.

### Collection `songs`

| Trường | Kiểu Firestore | Kiểu Kotlin | Ghi chú |
|--------|----------------|-------------|---------|
| `title` | string | `String` | Tên bài hát |
| `singerIds` | array<string> | `List<String>` | **Danh sách id ca sĩ** (nguồn chính) |
| `singerNames` | array<string> | `List<String>` | **Danh sách tên ca sĩ** (đã denormalized) |
| `singerId` | string | `String` | Ca sĩ đầu tiên — *tương thích ngược* |
| `singerName` | string | `String` | Tên ca sĩ đầu tiên — *tương thích ngược* |
| `categoryId` | string | `String` | Id thể loại |
| `categoryName` | string | `String` | Tên thể loại (denormalized) |
| `thumbnailUrl` | string | `String` | URL ảnh bìa |
| `audioUrl` | string | `String` | URL file MP3 |
| `lyricUrl` | string | `String` | URL file lyric (.lrc) — **có thể rỗng** |
| `duration` | number | `Long` | Thời lượng (giây) |
| `views` | number | `Long` | Lượt xem |
| `createdAt` | timestamp | `Timestamp` | Thời điểm tạo (server timestamp) |

> **Một bài hát có thể có nhiều ca sĩ.** Hãy ưu tiên dùng `singerIds` / `singerNames`. Hai trường `singerId` / `singerName` chỉ là ca sĩ đầu tiên, giữ lại cho dữ liệu cũ.

### Collection `singers`

| Trường | Kiểu | Kotlin |
|--------|------|--------|
| `name` | string | `String` |
| `avatarUrl` | string | `String` |
| `description` | string | `String` |

### Collection `categories`

| Trường | Kiểu | Kotlin |
|--------|------|--------|
| `name` | string | `String` |

### Collection `advertisements`

Banner hiển thị trên màn hình chính app (carousel/slider). Tên field khớp trực tiếp với data class `Advertisement` trên Android.

| Trường | Kiểu Firestore | Kiểu Kotlin | Ghi chú |
|--------|----------------|-------------|---------|
| `image` | string | `String` | URL ảnh banner |
| `update` | string | `String` | Tiêu đề ngắn (vd: "Hay nhất của V-POP") |
| `detail` | string | `String` | Mô tả chi tiết |
| `createdAt` | timestamp | *(bỏ qua)* | Chỉ dùng admin sắp xếp — app không cần map |

**Ví dụ document:**

```json
{
  "image": "https://photo-resize-zmp3.zmdcdn.me/w600_r1x1_jpeg/banner/2/7/b/d/27bdc67fef29c7928298c5759de08534.jpg",
  "update": "Hay nhất của V-POP",
  "detail": "Thiên Lý Ơi đưa Jack - J97 trở lại với Top Trending",
  "createdAt": "2026-06-05T10:00:00Z"
}
```

---

## 2. Thiết lập Firebase trong Android

### 2.1. Thêm app Android vào Firebase Console

1. Mở Firebase Console → chọn **đúng project** mà web đang dùng (cùng `projectId`).
2. **Project settings → Your apps → Add app → Android**.
3. Nhập **package name** đúng với `applicationId` trong `app/build.gradle.kts`.
4. Tải `google-services.json` về và đặt vào thư mục `app/`.

### 2.2. Gradle

`build.gradle.kts` (project-level) hoặc trong `settings.gradle.kts` plugins:

```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

`app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BoM quản lý version đồng bộ
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firestore (KTX đã gộp vào artifact chính từ BoM mới)
    implementation("com.google.firebase:firebase-firestore")

    // Coroutines hỗ trợ await() cho Task
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
```

### 2.3. Khởi tạo

Firebase tự khởi tạo nhờ plugin `google-services`. Lấy instance Firestore ở bất kỳ đâu:

```kotlin
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

val db = Firebase.firestore
```

---

## 3. Data class Kotlin

Đặt trong package `data/model`. Firestore map document → object bằng cách so khớp **tên field**, nên tên thuộc tính phải trùng tên field (hoặc dùng `@PropertyName`). Luôn để **giá trị mặc định** cho mọi thuộc tính để Firestore có constructor rỗng.

```kotlin
package com.example.music.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Song(
    @DocumentId
    val id: String = "",
    val title: String = "",

    // Nhiều ca sĩ — ưu tiên dùng các trường này
    val singerIds: List<String> = emptyList(),
    val singerNames: List<String> = emptyList(),

    // Tương thích ngược (ca sĩ đầu tiên) — chỉ dùng khi list rỗng
    val singerId: String = "",
    val singerName: String = "",

    val categoryId: String = "",
    val categoryName: String = "",

    val thumbnailUrl: String = "",
    val audioUrl: String = "",
    val lyricUrl: String = "",   // có thể rỗng -> bài hát chưa có lyric

    val duration: Long = 0,
    val views: Long = 0,
    val createdAt: Timestamp? = null,
) {
    /** Danh sách ca sĩ dùng để hiển thị, tự xử lý dữ liệu cũ. */
    val displaySingerIds: List<String>
        get() = if (singerIds.isNotEmpty()) singerIds
                else if (singerId.isNotEmpty()) listOf(singerId)
                else emptyList()

    val displaySingerNames: List<String>
        get() = if (singerNames.isNotEmpty()) singerNames
                else if (singerName.isNotEmpty()) listOf(singerName)
                else emptyList()

    /** Tên ca sĩ gộp để hiển thị, vd: "Sơn Tùng, Snoop Dogg". */
    val artistText: String
        get() = displaySingerNames.joinToString(", ")

    val hasLyric: Boolean
        get() = lyricUrl.isNotBlank()
}

@IgnoreExtraProperties
data class Singer(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val description: String = "",
)

@IgnoreExtraProperties
data class Category(
    @DocumentId
    val id: String = "",
    val name: String = "",
)

@IgnoreExtraProperties
data class Advertisement(
    @DocumentId
    val id: String = "",
    val image: String = "",
    val update: String = "",
    val detail: String = "",
)
```

**Ví dụ khởi tạo thủ công (không qua Firestore):**

```kotlin
Advertisement(
    image = "https://photo-resize-zmp3.zmdcdn.me/w600_r1x1_jpeg/banner/2/7/b/d/27bdc67fef29c7928298c5759de08534.jpg",
    update = "Hay nhất của V-POP",
    detail = "Thiên Lý Ơi đưa Jack - J97 trở lại với Top Trending",
)
```

Giải thích các annotation:

- `@DocumentId` — gán **id của document** vào thuộc tính `id` (id không nằm trong field dữ liệu).
- `@IgnoreExtraProperties` — bỏ qua field lạ trong document để không crash khi schema thay đổi.
- Giá trị mặc định (`= ""`, `= emptyList()`, ...) — **bắt buộc** để Firestore deserialize được.
- `@PropertyName("ten_field")` — dùng khi muốn tên thuộc tính Kotlin khác tên field Firestore (ở đây không cần vì đã trùng).

---

## 4. Đọc dữ liệu

Dùng coroutine + `await()` cho gọn. Bọc trong Repository.

```kotlin
package com.example.music.data

import com.example.music.data.model.Advertisement
import com.example.music.data.model.Category
import com.example.music.data.model.Singer
import com.example.music.data.model.Song
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class MusicRepository {

    private val db = Firebase.firestore

    private val songs get() = db.collection("songs")
    private val singers get() = db.collection("singers")
    private val categories get() = db.collection("categories")
    private val advertisements get() = db.collection("advertisements")

    /** Lấy 1 bài hát theo id. */
    suspend fun getSong(id: String): Song? =
        songs.document(id).get().await().toObject(Song::class.java)

    /** Lấy danh sách bài hát mới nhất. */
    suspend fun getLatestSongs(limit: Long = 20): List<Song> =
        songs.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects()

    /** Tất cả ca sĩ (sắp theo tên). */
    suspend fun getSingers(): List<Singer> =
        singers.orderBy("name").get().await().toObjects()

    /** Tất cả thể loại. */
    suspend fun getCategories(): List<Category> =
        categories.orderBy("name").get().await().toObjects()

    /** Bài hát theo thể loại. */
    suspend fun getSongsByCategory(categoryId: String, limit: Long = 50): List<Song> =
        songs.whereEqualTo("categoryId", categoryId)
            .limit(limit)
            .get()
            .await()
            .toObjects()

    /** Bài hát của một ca sĩ — dùng array-contains trên singerIds. */
    suspend fun getSongsBySinger(singerId: String, limit: Long = 50): List<Song> =
        songs.whereArrayContains("singerIds", singerId)
            .limit(limit)
            .get()
            .await()
            .toObjects()

    /** Danh sách banner — sắp mới nhất trước. */
    suspend fun getAdvertisements(): List<Advertisement> =
        advertisements.orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects()
}
```

> `toObjects()` (KTX) tự map `QuerySnapshot` → `List<Song>`. Nếu không dùng KTX, gọi `snapshot.toObjects(Song::class.java)`.

---

## 5. Phân trang

Web dùng phân trang **cursor** (`startAfter`). Trên Android cũng nên dùng cursor để hiệu quả với Firestore (không nên dùng offset).

```kotlin
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query

data class SongPage(
    val songs: List<Song>,
    val lastVisible: DocumentSnapshot?,  // truyền lại cho trang kế tiếp
    val hasMore: Boolean,
)

class SongPagingSource(private val db: com.google.firebase.firestore.FirebaseFirestore) {

    suspend fun loadPage(pageSize: Long, startAfter: DocumentSnapshot?): SongPage {
        var query: Query = db.collection("songs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize + 1)   // lấy dư 1 để biết còn trang sau không

        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }

        val snapshot = query.get().await()
        val docs = snapshot.documents
        val hasMore = docs.size > pageSize
        val pageDocs = if (hasMore) docs.subList(0, pageSize.toInt()) else docs

        return SongPage(
            songs = pageDocs.mapNotNull { it.toObject(Song::class.java) },
            lastVisible = pageDocs.lastOrNull(),
            hasMore = hasMore,
        )
    }
}
```

Cách dùng:

```kotlin
val pager = SongPagingSource(Firebase.firestore)

// Trang 1
val page1 = pager.loadPage(pageSize = 20, startAfter = null)

// Trang 2 (truyền lastVisible của trang trước)
val page2 = pager.loadPage(pageSize = 20, startAfter = page1.lastVisible)
```

> Nếu dùng Jetpack Paging 3, có thể bọc logic này trong một `PagingSource<DocumentSnapshot, Song>`.

---

## 6. Tìm kiếm theo tên

Web tìm kiếm bằng **range query theo prefix** trên field `title`. App nên làm giống hệt để dùng chung index:

```kotlin
suspend fun searchSongsByTitle(term: String, limit: Long = 20): List<Song> {
    val keyword = term.trim()
    if (keyword.isEmpty()) return emptyList()

    return Firebase.firestore.collection("songs")
        .orderBy("title")
        .startAt(keyword)
        .endAt(keyword + "\uf8ff")   // \uf8ff = ký tự Unicode cao, bao trùm mọi prefix
        .limit(limit)
        .get()
        .await()
        .toObjects()
}
```

> Đây là tìm kiếm **prefix, phân biệt hoa/thường**. Muốn full-text/typo-tolerant thì cần dịch vụ ngoài (Algolia, Typesense, ...).

---

## 7. Top bài hát & tăng lượt xem

### Top theo lượt xem

```kotlin
suspend fun getTopSongs(limit: Long = 10): List<Song> =
    Firebase.firestore.collection("songs")
        .orderBy("views", Query.Direction.DESCENDING)
        .limit(limit)
        .get()
        .await()
        .toObjects()
```

### Tăng lượt xem (atomic)

Dùng `FieldValue.increment` để cộng dồn an toàn (không cần đọc trước rồi ghi):

```kotlin
import com.google.firebase.firestore.FieldValue

suspend fun incrementViews(songId: String) {
    Firebase.firestore.collection("songs")
        .document(songId)
        .update("views", FieldValue.increment(1))
        .await()
}
```

> Gọi khi user thực sự phát bài hát. Lưu ý: rules hiện cho phép ghi tự do (xem mục 9).

---

## 8. Banner quảng cáo

Banner được admin tạo trên web tại route `/advertisements`, lưu vào collection `advertisements`. App chỉ cần **đọc** — không cần ghi từ client.

### Lấy danh sách banner

```kotlin
suspend fun getAdvertisements(): List<Advertisement> =
    Firebase.firestore.collection("advertisements")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .get()
        .await()
        .toObjects()
```

### Hiển thị trên UI (ViewPager2 / Compose)

```kotlin
// Trong ViewModel
val banners = MutableStateFlow<List<Advertisement>>(emptyList())

init {
    viewModelScope.launch {
        banners.value = repository.getAdvertisements()
    }
}

// Compose — ví dụ slider
@Composable
fun BannerCarousel(banners: List<Advertisement>) {
    val pagerState = rememberPagerState { banners.size }
    HorizontalPager(state = pagerState) { page ->
        val banner = banners[page]
        Column {
            AsyncImage(
                model = banner.image,
                contentDescription = banner.update,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Text(text = banner.update, style = MaterialTheme.typography.titleMedium)
            Text(text = banner.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

> Field `createdAt` tồn tại trong Firestore để admin sắp xếp, nhưng **không cần** khai báo trong `Advertisement` nhờ `@IgnoreExtraProperties`. App hiển thị banner theo thứ tự admin tạo (mới nhất trước).

---

## 9. Lắng nghe realtime

Khi muốn UI tự cập nhật lúc admin thay đổi dữ liệu, dùng snapshot listener thay vì `get()`. Có thể bọc thành `Flow`:

```kotlin
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun latestSongsFlow(limit: Long = 20): Flow<List<Song>> = callbackFlow {
    val registration = Firebase.firestore.collection("songs")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects())
            }
        }
    awaitClose { registration.remove() }   // hủy listener khi Flow bị cancel
}
```

Trong ViewModel:

```kotlin
val songs = latestSongsFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

**Banner realtime** — tương tự, bọc listener cho collection `advertisements`:

```kotlin
fun advertisementsFlow(): Flow<List<Advertisement>> = callbackFlow {
    val registration = Firebase.firestore.collection("advertisements")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects())
            }
        }
    awaitClose { registration.remove() }
}
```

---

## 10. Lưu ý quan trọng

- **Banner:** collection `advertisements`, 3 field chính `image` / `update` / `detail`. Dùng Coil/Glide load `image` trực tiếp. Có thể cần index `createdAt` (DESC) nếu chưa có.
- **Nhiều ca sĩ:** luôn ưu tiên `singerIds` / `singerNames`. Dùng `whereArrayContains("singerIds", id)` để lọc bài hát theo ca sĩ. Hai trường `singerId`/`singerName` chỉ để fallback cho document cũ.
- **Lyric có thể rỗng:** kiểm tra `song.hasLyric` (hoặc `lyricUrl.isNotBlank()`) trước khi tải/hiển thị lyric.
- **Composite index:** các truy vấn kết hợp `where` + `orderBy` có thể cần index. Khi chạy, nếu log Firestore báo lỗi `FAILED_PRECONDITION` kèm một URL, hãy mở URL đó để tạo index tự động.
- **Đơn vị `duration`:** tính bằng **giây** (kiểu `Long`). Đổi sang `mm:ss` khi hiển thị.
- **Múi giờ `createdAt`:** là `Timestamp` của Firebase; gọi `createdAt?.toDate()` để lấy `java.util.Date`.
- **Bảo mật rules:** Firestore hiện đang mở (`allow read, write: if true`) — chỉ phù hợp dev. Trước khi phát hành app thật, cần siết rules (ví dụ chỉ cho `read` công khai, chặn `write` từ client; việc tăng `views` nên qua Cloud Function hoặc rule riêng cho field `views`).
- **Offline:** Firestore Android tự bật cache offline. Có thể cấu hình `db.firestoreSettings` nếu cần kiểm soát.
- **URL media:** `audioUrl` / `thumbnailUrl` / `lyricUrl` / `image` (banner) là link Cloudinary hoặc URL nhập tay — dùng trực tiếp với ExoPlayer/Coil/Glide, không cần qua Firebase Storage.
