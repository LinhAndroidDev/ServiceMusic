# ServiceMusic

Ứng dụng Android phát nhạc online lấy catalog từ **Firebase Firestore**, stream audio bằng **Media3 ExoPlayer**, chạy nền qua **Foreground Service** với **MediaStyle notification** và **MediaSession**. Giao diện dùng **Navigation Component**, kiến trúc **MVVM** và **Dagger Hilt**.

## Mục lục

- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cách chạy dự án](#cách-chạy-dự-án)
- [Tính năng chính](#tính-năng-chính)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc & luồng dữ liệu](#kiến-trúc--luồng-dữ-liệu)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Phát nhạc & thông báo](#phát-nhạc--thông-báo)
- [#zingchart — biểu đồ xếp hạng](#zingchart--biểu-đồ-xếp-hạng)
- [Firestore & xử lý offline](#firestore--xử-lý-offline)
- [Quyền (Permissions)](#quyền-permissions)
- [Build & kiểm thử](#build--kiểm-thử)
- [Tài liệu liên quan](#tài-liệu-liên-quan)

---

## Yêu cầu môi trường

| Thành phần | Phiên bản |
|------------|-----------|
| Android Studio | Hedgehog (2023.1+) hoặc tương đương |
| JDK | 8+ (`jvmTarget = 1.8`) |
| Gradle | 8.2 |
| Kotlin | 1.9.22 |
| Android SDK | `compileSdk` / `targetSdk` **34** |
| Thiết bị / emulator | **API 26+** (`minSdk = 26`) |
| Firebase | `app/google-services.json` khớp `applicationId` |

---

## Cách chạy dự án

1. Clone hoặc mở thư mục dự án trong Android Studio.
2. Đặt `app/google-services.json` (Firebase project đã cấu hình Firestore — xem [`app/ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md)).
3. Đồng bộ Gradle (**File → Sync Project with Gradle Files**).
4. Chọn variant **debug**, thiết bị/emulator có mạng, bấm **Run**.

> **Lưu ý:** Trên Android 13+ cần cấp quyền **POST_NOTIFICATIONS** để hiển thị notification media đầy đủ.

---

## Tính năng chính

### Điều hướng & màn hình

| Tab / màn hình | Mô tả |
|----------------|-------|
| **Splash** | Màn hình khởi động, chuyển sang Home |
| **Khám phá (Home)** | Banner quảng cáo, chủ đề, bài mới, lọc Việt/Quốc tế, pull-to-refresh |
| **Thư viện** | Thư viện cá nhân, điều hướng tới bài yêu thích |
| **#zingchart** | Top bài hát + biểu đồ xếp hạng tương tác |
| **Radio** | Màn Radio (UI) |
| **Cá nhân** | Hồ sơ người dùng |
| **Tìm kiếm** | Prefix search theo tiêu đề trên Firestore |
| **Phát nhạc** | Màn full player: seek, next/prev, repeat, lyric đồng bộ, tab ca sĩ |
| **Yêu thích** | Danh sách bài đã lưu (Room) |

Bottom bar tùy chỉnh (`CustomBottomBar`) + **ViewPager2** trên `MainActivity` để chuyển tab chính.

### Dữ liệu & phát nhạc

- **Catalog online** — `FirestoreMusicRepository` đọc `songs`, `singers`, `categories`, `advertisements`.
- **Cache playlist** — `SongRepository` giữ bản sao trong memory (latest / top / playback queue).
- **Stream audio** — `MusicService` dùng **ExoPlayer** phát `Song.audioUrl`, tăng `views` khi bắt đầu phát.
- **Mini player** — `MainActivity` đồng bộ với service qua `PlaybackViewModel` + `MusicServiceConnector`.
- **Lyric LRC** — tải từ `lyricUrl`, parse bằng `LrcLineParser`, highlight theo vị trí phát.
- **Yêu thích** — Room (`SongEntity`, database version **1**).
- **Ảnh bìa** — Glide / Coil load `thumbnailUrl`.

---

## Công nghệ sử dụng

| Nhóm | Thư viện |
|------|----------|
| UI | View Binding, Data Binding, Material, ConstraintLayout, SwipeRefreshLayout |
| Navigation | Navigation Component 2.7.7 + Safe Args |
| DI | Dagger Hilt 2.48 |
| Async | Kotlin Coroutines, Flow, StateFlow |
| Backend | Firebase Firestore (BoM 33.7.0) |
| Local DB | Room 2.6.1 |
| Audio | Media3 ExoPlayer 1.4.1, MediaSessionCompat |
| Image | Glide 4.16, Coil 2.6 |
| Chart | MPAndroidChart v3.1.0 |
| Banner dots | ScrollingPagerIndicator 1.2.5 |

---

## Kiến trúc & luồng dữ liệu

### MVVM

- **Activity / Fragment:** binding UI, observe ViewModel, `collect` state.
- **ViewModel (`@HiltViewModel`):** logic màn hình, coroutine (`viewModelScope`).
- **Repository:** tách lớp dữ liệu — Firestore, cache playlist, Room favourites, SharedPreferences.

### Luồng dữ liệu (tóm tắt)

```text
UI (Home / ZingChart / Search)
    → ViewModel.refreshPlaylist / search
        → SongRepository / FirestoreMusicRepository
            → Firestore (cache offline + fallback)

UI (MainActivity / FragmentMusic)
    → PlaybackViewModel
        → MusicServiceConnector
            → MusicService (ExoPlayer, notification, incrementViews)
                → PlaybackStateHolder (StateFlow)
    ← UI collect playbackState
```

- **`PlaybackStateHolder`:** snapshot UI (`PlaybackUiState` — bài hát, index, `isPlaying`, `positionMs`, `durationMs`, …).
- **`MusicService`:** Foreground Service (`mediaPlayback`), ExoPlayer stream, Glide thumbnail cho notification, khôi phục vị trí phát từ SharedPreferences.

### Dependency Injection (Hilt)

- `@HiltAndroidApp` — `MyApplication`
- `@AndroidEntryPoint` — `MainActivity`, Fragment, `MusicService`
- Modules:
  - [`AppModule`](app/src/main/java/com/example/serviceandroid/di/AppModule.kt)
  - [`DatabaseModule`](app/src/main/java/com/example/serviceandroid/di/DatabaseModule.kt)
  - [`FirebaseModule`](app/src/main/java/com/example/serviceandroid/di/FirebaseModule.kt)

---

## Cấu trúc thư mục

```text
app/src/main/java/com/example/serviceandroid/
├── data/
│   ├── firestore/          # FirestoreSong, FirestoreMusicRepository
│   └── repository/         # SongRepository (cache playlist)
├── database/               # Room — SongEntity, FavouriteSongDao
├── playback/               # PlaybackViewModel, PlaybackStateHolder, MusicServiceConnector
├── service/                # MusicService — ExoPlayer, notification
├── fragment/               # Home, ZingChart, Music, Search, Library, …
├── lyrics/                 # SongLyricsLoader, LrcLineParser, LineLyricsAdapter
├── custom/                 # CustomLineChartRenderer, bottom sheets, dialogs
├── adapter/                # RecyclerView / ViewPager adapters
├── model/                  # Song, Singer, Advertisement, Repeat, …
├── di/                     # Hilt modules
├── helper/                 # Constants, MyApplication
└── utils/                  # DateUtils, SharePreferenceRepository, …
```

---

## Phát nhạc & thông báo

- **Audio:** stream HTTPS từ `Song.audioUrl` qua **ExoPlayer**.
- **Notification:** `MediaStyle` + `MediaSessionCompat` + Glide `thumbnailUrl`; tap mở `FragmentMusic` với `song_id`.
- **Điều khiển:** play/pause, next/prev, seek; repeat một bài / toàn playlist.
- **Khôi phục:** lưu `queueIndex` + `positionMs` trong SharedPreferences khi process bị kill.
- **Mini player:** hiển thị trên `MainActivity`, throttle cập nhật seek để giảm jank.

---

## #zingchart — biểu đồ xếp hạng

Màn `#zingchart` hiển thị top bài hát kèm biểu đồ **LineChart** (MPAndroidChart):

- 3 đường biểu diễn xu hướng xếp hạng (LineChart1 / 2 / 3).
- Avatar bài hát (thumbnail top 1–3) vẽ trên điểm chart qua `CustomLineChartRenderer`.
- **Animation chuyển chart:** khi highlight chuyển giữa các line, avatar di chuyển mượt từ vị trí cũ sang vị trí mới (`ValueAnimator` + `ChartAvatarState`).
- Tự động chuyển highlight mỗi 5 giây hoặc khi tap vào chart.
- Pull-to-refresh để tải lại top songs từ Firestore.

---

## Firestore & xử lý offline

`FirestoreMusicRepository` đọc dữ liệu an toàn khi mất mạng:

- Ưu tiên `Source.DEFAULT`, fallback `Source.CACHE` khi client offline (`UNAVAILABLE`).
- Trả `null` / `emptyList()` thay vì crash app.
- `SongRepository` giữ cache cũ nếu refresh từ server thất bại (không xóa playlist khi offline).
- `incrementViews` bọc `runCatching` — không crash khi không ghi được Firestore.

Schema Firestore, query và hướng dẫn tích hợp chi tiết: [`app/ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md).

---

## Quyền (Permissions)

| Quyền | Mục đích |
|-------|----------|
| `INTERNET` | Firestore, stream audio, lyric, ảnh |
| `POST_NOTIFICATIONS` | Notification media (Android 13+) |
| `FOREGROUND_SERVICE` | Chạy service phát nhạc nền |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Loại foreground service media |

---

## Build & kiểm thử

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:compileDebugKotlin
```

**Kiểm thử thủ công gợi ý:**

- Phát / pause / seek / next / prev / repeat
- Mini player ↔ full player ↔ notification
- Thêm / xóa yêu thích (Room)
- Tìm kiếm bài hát
- Pull-to-refresh Home & #zingchart
- Animation avatar trên chart (#zingchart)
- Lyric đồng bộ theo thời gian phát
- Tắt mạng — app không crash, dữ liệu cache vẫn hiển thị

---

## Tài liệu liên quan

| File | Nội dung |
|------|----------|
| [`app/ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md) | Schema Firestore, data class, query, banner, offline |

---

## Phiên bản ứng dụng

| Thuộc tính | Giá trị |
|------------|---------|
| `applicationId` | `com.example.serviceandroid` |
| `versionName` | `1.0` |
| `versionCode` | `1` |

---

## Tác giả & giấy phép

Dự án mẫu / học tập. Điều chỉnh giấy phép theo nhu cầu nhóm của bạn.
