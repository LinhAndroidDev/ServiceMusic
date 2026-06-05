# ServiceMusic

Ứng dụng Android phát nhạc online qua **Firestore**, stream audio bằng **MediaPlayer**, kết hợp **Foreground Service**, **MediaStyle notification**, **Navigation Component** và kiến trúc **MVVM** cùng **Dagger Hilt**.

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
| Firebase | `google-services.json` khớp `applicationId` |

---

## Cách chạy dự án

1. Clone hoặc mở thư mục dự án trong Android Studio.
2. Đặt `app/google-services.json` (Firebase project đã cấu hình Firestore theo [`ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md)).
3. Đồng bộ Gradle (**File → Sync Project with Gradle Files**).
4. Chọn variant **debug**, thiết bị/emulator có mạng, bấm **Run**.

> **Lưu ý:** Trên Android 13+ cần cấp quyền **POST_NOTIFICATIONS** để hiển thị notification media đầy đủ.

---

## Tính năng chính

- **Khám phá / Thư viện / #zingchart / Radio / Cá nhân** — điều hướng bằng bottom bar và Navigation Graph.
- **Catalog online** — `FirestoreMusicRepository` đọc `songs`, `singers`, `categories`; `SongRepository` cache playlist trong memory.
- **Phát nhạc stream** — `MusicService` dùng `MediaPlayer.setDataSource(audioUrl)` + `prepareAsync()`, tăng `views` khi bắt đầu phát.
- **Mini player** trên `MainActivity` — đồng bộ với service qua `PlaybackViewModel` + `MusicServiceConnector`.
- **Màn hình phát đầy đủ** — `FragmentMusic` (seek, next/prev, repeat, lyric remote, yêu thích Room).
- **Tìm kiếm** — prefix query Firestore theo tiêu đề bài hát.
- **Yêu thích** — Room v3 (`SongEntity` với Firestore `id` + URL fields).
- **Ảnh bìa / lyric** — Glide `thumbnailUrl`; lyric tải HTTP từ `lyricUrl` + `LrcLineParser`.

---

## Kiến trúc & luồng dữ liệu

### MVVM

- **Activity / Fragment:** binding UI, gọi ViewModel, `collect` state.
- **ViewModel (`@HiltViewModel`):** logic màn hình, coroutine (`viewModelScope`).
- **Repository:** `FirestoreMusicRepository`, `SongRepository`, `FavouriteSongRepository`, `SharePreferenceRepository`.

### Luồng dữ liệu (tóm tắt)

```text
UI (Home / ZingChart / Search)
    → ViewModel.refreshPlaylist / search
        → SongRepository / FirestoreMusicRepository
            → Firestore

UI (MainActivity / FragmentMusic)
    → PlaybackViewModel
        → MusicServiceConnector
            → MusicService (MediaPlayer stream, notification, incrementViews)
                → PlaybackStateHolder (StateFlow)
    ← UI collect playbackState
```

- **`PlaybackStateHolder`:** snapshot UI (`PlaybackUiState`: bài hát, index, `isPlaying`, `positionMs`, `durationMs`, …).
- **`MusicService`:** Foreground Service (`mediaPlayback`), stream URL, Glide thumbnail cho notification.

### Dependency Injection (Hilt)

- `@HiltAndroidApp` — `MyApplication`
- `@AndroidEntryPoint` — `MainActivity`, Fragment, `MusicService`
- Module: [`AppModule`](app/src/main/java/com/example/serviceandroid/di/AppModule.kt), [`DatabaseModule`](app/src/main/java/com/example/serviceandroid/di/DatabaseModule.kt), [`FirebaseModule`](app/src/main/java/com/example/serviceandroid/di/FirebaseModule.kt)

Chi tiết schema Firestore và query: [`app/ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md).

---

## Cấu trúc thư mục

| Package / thư mục | Vai trò |
|-------------------|--------|
| `data/firestore/` | `FirestoreSong`, `FirestoreMusicRepository` |
| `data/repository/` | `SongRepository` cache playlist |
| `playback/` | `PlaybackViewModel`, `PlaybackStateHolder`, `MusicServiceConnector` |
| `service/` | `MusicService` — foreground, stream, notification |
| `database/` | Room favourites v3 |
| `fragment/*` | Các màn theo feature + ViewModel |
| `lyrics/` | `SongLyricsLoader`, `LrcLineParser` |
| `di/` | Hilt modules (App, Database, Firebase) |
| `helper/` | `Constants`, `MyApplication` |
| `model/` | `Song` (String id, URLs), `Action`, `Repeat`, … |

---

## Phát nhạc & thông báo

- **Audio:** stream từ `Song.audioUrl` (HTTPS).
- **Notification:** `MediaStyle` + Glide `thumbnailUrl` + tap mở `FragmentMusic` với `song_id`.
- **Khôi phục:** snapshot `queueIndex` + `positionMs` trong SharedPreferences khi process bị kill.

---

## Quyền (Permissions)

- `POST_NOTIFICATIONS` — Android 13+
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `INTERNET` — Firestore, stream audio, lyric, ảnh

---

## Build & kiểm thử

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:compileDebugKotlin
```

Kiểm thử thủ công: phát/nền/notification/seek/favourite/search/top chart/views increment.

---

## Phiên bản ứng dụng

- `versionName`: **1.0**
- `versionCode`: **1**
- `applicationId`: **com.example.serviceandroid**

---

## Tác giả & giấy phép

Dự án mẫu / học tập. Điều chỉnh giấy phép theo nhu cầu nhóm của bạn.
