# ServiceMusic

Ứng dụng Android nghe nhạc online. Catalog lấy từ **Firebase Firestore**, audio stream bằng **Media3 ExoPlayer**, phát nền qua **Foreground Service** với **MediaStyle notification** và **MediaSession**. Giao diện dùng **Navigation Component**, kiến trúc **MVVM** và **Dagger Hilt**.

Đăng nhập Google để lưu dữ liệu theo tài khoản: bài yêu thích, playlist, nghệ sĩ quan tâm và lịch sử tìm kiếm.

## Mục lục

- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cách chạy dự án](#cách-chạy-dự-án)
- [Tính năng chính](#tính-năng-chính)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc & luồng dữ liệu](#kiến-trúc--luồng-dữ-liệu)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Phát nhạc & thông báo](#phát-nhạc--thông-báo)
- [Tài khoản & dữ liệu người dùng](#tài-khoản--dữ-liệu-người-dùng)
- [#zingchart](#zingchart)
- [Firestore & xử lý offline](#firestore--xử-lý-offline)
- [Quyền](#quyền)
- [Build & kiểm thử](#build--kiểm-thử)
- [Tài liệu liên quan](#tài-liệu-liên-quan)

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

Firebase cần bật **Firestore** và **Authentication (Google)**. Quảng cáo mở app dùng **Google AdMob** (App Open).

## Cách chạy dự án

1. Mở thư mục dự án trong Android Studio.
2. Đặt `app/google-services.json` (xem [`app/ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md)).
3. Đồng bộ Gradle (**File → Sync Project with Gradle Files**).
4. Chọn variant **debug**, thiết bị có mạng, bấm **Run**.

Bản debug dùng đơn vị quảng cáo thử của Google. Bản release dùng đơn vị App Open thật khai báo trong `AppOpenAdController`.

Trên Android 13+ cần cấp **POST_NOTIFICATIONS** để notification media hiện đủ. Tìm bằng giọng nói cần **RECORD_AUDIO**.

## Tính năng chính

### Điều hướng

Thanh dưới (`CustomBottomBar`) chuyển năm tab bằng Navigation: **Thư viện**, **Khám phá**, **#zingchart**, **Radio**, **Cá nhân**.

| Màn hình | Mô tả |
|----------|--------|
| **Splash** | Màn khởi động. Tải quảng cáo App Open, hiện quảng cáo rồi vào Home. Tải lỗi thì vào Home luôn |
| **Khám phá** | Banner, chủ đề, bài mới, lọc Việt/Quốc tế, kéo để tải lại |
| **#zingchart** | Top bài hát theo lượt nghe và biểu đồ xếp hạng |
| **Radio** | Giao diện radio |
| **Cá nhân** | Hồ sơ, đăng nhập / đăng xuất Google |
| **Tìm kiếm** | Bài hát và ca sĩ, đề xuất từ cache, lịch sử tìm kiếm, tìm bằng giọng nói |
| **Ca sĩ** | Ảnh, số bài, giới thiệu rút gọn, Quan tâm, Phát nhạc, danh sách bài |
| **Phát nhạc** | Player toàn màn: seek, next/prev, repeat, hẹn giờ tắt, lyric, kéo xuống để đóng |
| **Thư viện** | Yêu thích, đã tải, nghệ sĩ, nghe gần đây, playlist |
| **Playlist** | Tạo, thêm bài, sửa thứ tự, xóa bài, xóa playlist. Ảnh và tên có shared element khi mở chi tiết |
| **Yêu thích** | Bài đã lưu theo tài khoản |
| **Đã tải** | Bài lưu trên máy |
| **Nghệ sĩ** | Ca sĩ đã bấm Quan tâm |

Mini player nằm trên thanh dưới. Tên bài dài chạy ngang, mép chữ được làm mờ khi đang cuộn.

### Dữ liệu

- **Catalog** — `FirestoreMusicRepository` đọc `songs`, `singers`, `categories`, `advertisements`.
- **Hàng đợi phát** — `SongRepository` giữ playlist đang phát trong memory. Bấm một bài thì hàng đợi là danh sách đang nhìn thấy.
- **Theo tài khoản (Firestore)** — yêu thích, playlist, nghệ sĩ quan tâm, lịch sử tìm kiếm. Chưa đăng nhập thì app yêu cầu đăng nhập trước khi ghi.
- **Trên máy (Room, version 5)** — bài đã tải và nghe gần đây.
- **Ảnh** — Coil / Glide tải thumbnail và avatar.

## Công nghệ sử dụng

| Nhóm | Thư viện |
|------|----------|
| UI | View Binding, Data Binding, Material, ConstraintLayout, SwipeRefreshLayout |
| Navigation | Navigation Component 2.7.7 + Safe Args |
| DI | Dagger Hilt 2.48 |
| Async | Kotlin Coroutines, Flow, StateFlow |
| Backend | Firebase Firestore, Firebase Auth (BoM 33.7.0) |
| Đăng nhập | Credentials + Google Identity |
| Quảng cáo | Play Services Ads 23.6.0 (App Open) |
| Local DB | Room 2.6.1 |
| Tải nền | WorkManager 2.9.0 |
| Audio | Media3 ExoPlayer 1.4.1, MediaSessionCompat |
| Image | Glide 4.16, Coil 2.6 |
| Chart | MPAndroidChart v3.1.0 |
| Banner dots | ScrollingPagerIndicator 1.2.5 |

## Kiến trúc & luồng dữ liệu

- **Fragment / Activity** gắn UI và `collect` state.
- **ViewModel** giữ logic màn hình trong `viewModelScope`.
- **Repository** tách Firestore, cache phát nhạc, Room và SharedPreferences.

```text
Home / ZingChart / Search / Singer
    → ViewModel
        → SongRepository hoặc FirestoreMusicRepository
            → Firestore (cache khi mất mạng)

Yêu thích / Playlist / Nghệ sĩ / Lịch sử tìm kiếm
    → Repository
        → Firestore users/{uid}/...
        → cần đăng nhập; offline thì không ghi

MainActivity / FragmentMusic
    → PlaybackViewModel
        → MusicServiceConnector
            → MusicService (ExoPlayer, notification)
                → PlaybackStateHolder
    ← UI collect playbackState
```

`PlaybackStateHolder` giữ bài đang phát, index, trạng thái play, vị trí và thời lượng. `MusicService` là foreground service loại `mediaPlayback`.

Hilt:

- `@HiltAndroidApp` — `MyApplication`
- `@AndroidEntryPoint` — `MainActivity`, fragment, `MusicService`
- Module: [`AppModule`](app/src/main/java/com/example/serviceandroid/di/AppModule.kt), [`DatabaseModule`](app/src/main/java/com/example/serviceandroid/di/DatabaseModule.kt), [`FirebaseModule`](app/src/main/java/com/example/serviceandroid/di/FirebaseModule.kt)

## Cấu trúc thư mục

```text
app/src/main/java/com/example/serviceandroid/
├── ads/                  # App Open Ad lúc mở app
├── data/
│   ├── artist/           # Nghệ sĩ đã quan tâm
│   ├── auth/             # Google Sign-In, AuthRepository
│   ├── firestore/        # Catalog songs, singers, categories
│   ├── playlist/         # Playlist theo tài khoản
│   ├── recent/           # Nghe gần đây
│   ├── repository/       # Cache playlist / hàng đợi phát
│   ├── search/           # Lịch sử và gợi ý tìm kiếm
│   └── user/
├── database/             # Room — bài đã tải, nghe gần đây
├── download/             # WorkManager tải bài
├── playback/             # PlaybackViewModel, connector, hẹn giờ tắt
├── service/              # MusicService
├── fragment/             # Home, thư viện, playlist, ca sĩ, tìm kiếm, player
├── lyrics/               # Tải và parse file .lrc
├── custom/               # Bottom bar, bottom sheet, dialog, marquee
├── adapter/
├── model/
├── di/
└── utils/
```

## Phát nhạc & thông báo

- Stream `Song.audioUrl` bằng ExoPlayer.
- Notification `MediaStyle`: play/pause, next/prev. Bấm notification mở player với `song_id`.
- Repeat một bài hoặc cả danh sách. Hẹn giờ tắt nhạc từ menu player.
- Kéo player xuống để đóng. Vuốt xuống có quán tính thì đóng luôn. Kéo chậm chỉ đóng khi qua khoảng 10% chiều cao.
- Tên bài trên mini player cuộn vòng nếu dài hơn một dòng.
- Vị trí phát được lưu để khôi phục khi process bị hệ thống dừng.

## Tài khoản & dữ liệu người dùng

Đăng nhập Google. Dữ liệu ghi dưới `users/{uid}`:

| Dữ liệu | Nơi lưu | Ghi chú |
|---------|---------|---------|
| Bài yêu thích | Firestore | Cần mạng và đăng nhập |
| Playlist | Firestore | Tạo, sửa, sắp xếp, xóa bài |
| Nghệ sĩ quan tâm | Firestore `followedSingers` | Nút Quan tâm trên màn ca sĩ |
| Lịch sử tìm kiếm | Firestore | Khi đã đăng nhập |
| Bài đã tải | Room | Phát được khi không có mạng |
| Nghe gần đây | Room | Hiện trên Thư viện |

Màn ca sĩ: mục **Thông tin** căn trái, tối đa 3 dòng, **xem thêm** / **thu gọn** nếu dài hơn. **Phát nhạc** phát bài đầu tiên và lấy cả danh sách ca sĩ làm hàng đợi.

## #zingchart

Top bài hát theo lượt nghe, kèm **LineChart** (MPAndroidChart):

- Ba đường xu hướng. Avatar top bài được vẽ trên điểm chart (`CustomLineChartRenderer`).
- Highlight tự chuyển hoặc khi chạm chart. Avatar trượt giữa các điểm.
- Kéo để tải lại.

## Firestore & xử lý offline

`FirestoreMusicRepository` ưu tiên mạng, rồi dùng cache Firestore khi offline. Refresh thất bại thì giữ cache cũ, không xóa danh sách đang hiện.

Ghi yêu thích, playlist và nghệ sĩ quan tâm cần mạng. Mất mạng thì báo cho người dùng, không crash.

Schema catalog: [`app/ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md).

## Quyền

| Quyền | Mục đích |
|-------|----------|
| `INTERNET` | Firestore, stream, lyric, ảnh, quảng cáo |
| `ACCESS_NETWORK_STATE` | Banner mất mạng / có mạng lại |
| `POST_NOTIFICATIONS` | Notification media (Android 13+) |
| `FOREGROUND_SERVICE` | Service phát nhạc |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Loại service media |
| `RECORD_AUDIO` | Tìm kiếm bằng giọng nói |

## Build & kiểm thử

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:compileDebugKotlin
```

Nên thử:

- Splash: quảng cáo đóng thì vào Home. Tải quảng cáo lỗi thì vào Home.
- Phát, pause, seek, next, prev, repeat, hẹn giờ tắt.
- Mini player, player toàn màn, notification.
- Kéo player xuống để đóng.
- Tên bài dài trên mini player.
- Đăng nhập Google, rồi yêu thích, tạo playlist, quan tâm ca sĩ.
- Tìm kiếm, gợi ý, giọng nói.
- Tải một bài và mở mục Đã tải.
- #zingchart và lyric.
- Tắt mạng: catalog cache vẫn hiện, thao tác cần tài khoản thì báo offline.

## Tài liệu liên quan

| File | Nội dung |
|------|----------|
| [`app/ANDROID_INTEGRATION.md`](app/ANDROID_INTEGRATION.md) | Schema Firestore catalog, data class, query, banner |

## Phiên bản ứng dụng

| Thuộc tính | Giá trị |
|------------|---------|
| `applicationId` | `com.example.serviceandroid` |
| `versionName` | `1.0` |
| `versionCode` | `1` |
| Room | `MusicDatabase.VERSION` = 5 |

## Tác giả & giấy phép

Dự án học tập. Điều chỉnh giấy phép theo nhu cầu nhóm.
