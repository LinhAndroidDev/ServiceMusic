# ServiceMusic

Ứng dụng Android mẫu phát nhạc cục bộ (raw resources), kết hợp **Foreground Service**, **MediaStyle notification**, **Navigation Component** và kiến trúc **MVVM** cùng **Dagger Hilt**.

## Mục lục

- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cách chạy dự án](#cách-chạy-dự-án)
- [Tính năng chính](#tính-năng-chính)
- [Kiến trúc & luồng dữ liệu](#kiến-trúc--luồng-dữ-liệu)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Phát nhạc & thông báo](#phát-nhạc--thông-báo)
- [Quyền (Permissions)](#quyền-permissions)
- [Cấu hình bổ sung](#cấu-hình-bổ-sung)
- [Build & kiểm thử](#build--kiểm-thử)

---

## Yêu cầu môi trường

| Thành phần | Phiên bản |
|------------|-----------|
| Android Studio | Hedgehog (2023.1+) hoặc tương đương |
| JDK | 8+ (project dùng `jvmTarget = 1.8`) |
| Android SDK | `compileSdk` / `targetSdk` **34** |
| Thiết bị / emulator | **API 26+** (`minSdk = 26`) |

---

## Cách chạy dự án

1. Clone hoặc mở thư mục dự án trong Android Studio.
2. Đồng bộ Gradle (**File → Sync Project with Gradle Files**).
3. Chọn variant **debug**, chọn thiết bị/emulator, bấm **Run**.

> **Lưu ý:** Trên Android 13+ cần cấp quyền **POST_NOTIFICATIONS** để hiển thị notification media đầy đủ.

---

## Tính năng chính

- **Khám phá / Thư viện / #zingchart / Radio / Cá nhân** — điều hướng bằng bottom bar và Navigation Graph.
- **Danh sách nhạc demo** — nguồn dữ liệu tĩnh qua `SongRepository` (hiện bọc `Data.listMusic()`).
- **Phát nhạc** — `MusicService` (foreground) sở hữu `MediaPlayer`, cập nhật trạng thái qua `PlaybackStateHolder` (`StateFlow`).
- **Mini player** trên `MainActivity` — đồng bộ với service qua `PlaybackViewModel` + `MusicServiceConnector` (bind + `startForegroundService`).
- **Màn hình phát đầy đủ** — `FragmentMusic` (seek, next/prev, repeat, yêu thích với Room).
- **Yêu thích** — Room + `FavouriteSongRepository`.
- **Biểu đồ / tùy chỉnh UI** — MPAndroidChart, bottom sheet, v.v.

---

## Kiến trúc & luồng dữ liệu

### MVVM

- **Activity / Fragment:** chủ yếu binding UI, gọi ViewModel, `collect` state.
- **ViewModel (`@HiltViewModel`):** logic màn hình, coroutine (`viewModelScope`).
- **Repository:** tách nguồn dữ liệu (`SongRepository`, `FavouriteSongRepository`, `SharePreferenceRepository`).

### Phát nhạc (tóm tắt)

```text
UI (MainActivity / FragmentMusic)
    → PlaybackViewModel
        → MusicServiceConnector (bind + startForegroundService)
            → MusicService (MediaPlayer, notification, MediaSessionCompat.Callback)
                → PlaybackStateHolder (StateFlow)
    ← UI collect playbackState
```

- **`PlaybackStateHolder`:** một snapshot UI (`PlaybackUiState`: bài hát hiện tại, index, `isPlaying`, `positionMs`, `durationMs`, …).
- **`MusicServiceConnector`:** `bind`/`unbind` trong lifecycle Activity; gửi lệnh tới service (binder hoặc intent).
- **`MusicService`:** `Foreground Service` (`mediaPlayback`), `MediaStyle`, điều khiển qua **MediaSession callback** và **PendingIntent.getForegroundService** tới chính service.

### Dependency Injection (Hilt)

- `@HiltAndroidApp` — `MyApplication`
- `@AndroidEntryPoint` — `MainActivity`, các `Fragment` / `Service` cần inject
- Module: [`AppModule`](app/src/main/java/com/example/serviceandroid/di/AppModule.kt), [`DatabaseModule`](app/src/main/java/com/example/serviceandroid/di/DatabaseModule.kt)

---

## Cấu trúc thư mục

| Package / thư mục | Vai trò |
|-------------------|--------|
| `base/` | `BaseActivity`, `BaseFragment`, `CoreInterface` |
| `playback/` | `PlaybackViewModel`, `PlaybackStateHolder`, `PlaybackUiState`, `MusicServiceConnector` |
| `service/` | `MusicService` — foreground, media, notification |
| `data/repository/` | `SongRepository` + implementation |
| `database/` | Room (`MusicDatabase`, DAO, entity) |
| `database/repository/` | `FavouriteSongRepository` |
| `fragment/*` | Các màn theo feature + ViewModel tương ứng |
| `di/` | Hilt `@Module` |
| `helper/` | `Constants`, `Data`, `MyApplication` |
| `model/` | `Song`, `Action`, `Repeat`, … |
| `adapter/` | RecyclerView / ViewPager adapters |
| `utils/` | Tiện ích, SharedPreferences abstraction, `getCurrentFragment()` |

> Điều khiển media trên notification dùng **PendingIntent.getForegroundService** trực tiếp tới `MusicService` (không qua `BroadcastReceiver`).

---

## Phát nhạc & thông báo

- **Kênh notification:** `MyApplication.CHANNEL_ID` — tạo khi app khởi động (importance phù hợp media).
- **Notification:** `NotificationCompat` + `MediaStyle` + `MediaSessionCompat` token.
- **Nút Previous / Play-Pause / Next:**  
  - `MediaSessionCompat.Callback` (play, pause, skip)  
  - `addAction` + `PendingIntent.getForegroundService` → `MusicService` với extra `RECEIVER_ACTION_MUSIC`.

Đảm bảo service khai báo `android:foregroundServiceType="mediaPlayback"` trong manifest (đã có).

---

## Quyền (Permissions)

Trong [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) gồm (không đầy đủ liệt kê tại đây):

- `POST_NOTIFICATIONS` — Android 13+
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `INTERNET`, v.v.

---

## Build & kiểm thử

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:compileDebugKotlin
```

Unit test / Android test mặc định của template có thể chạy qua Android Studio (**Run tests**).

---

## Phiên bản ứng dụng

- `versionName`: **1.0**
- `versionCode`: **1**
- `applicationId`: **com.example.serviceandroid**

---

## Tác giả & giấy phép

Dự án mẫu / học tập. Điều chỉnh giấy phép theo nhu cầu nhóm của bạn.
