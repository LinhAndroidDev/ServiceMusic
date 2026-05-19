package com.example.serviceandroid.helper

import com.example.serviceandroid.R
import com.example.serviceandroid.model.Song

object Data {
    fun listMusic(): ArrayList<Song> {
        return arrayListOf(
            Song(1,"Lạ Lùng", "Vũ", R.drawable.la_lung, R.raw.la_lung, 262, 0),
            Song(2, "Âm thầm bên em", "Sơn Tùng MTP", R.drawable.am_tham_ben_em, R.raw.am_tham_ben_em, 292, 0),
            Song(3, "Đi để trở về", "Soobin Hoàng Sơn", R.drawable.di_de_tro_ve, R.raw.di_de_tro_ve, 209, 0),
            Song(4, "Đồi hoa mặt trời", "Hoàng Yến Chibi", R.drawable.doi_hoa_mat_troi, R.raw.doi_hoa_mat_troi, 238, 0),
            Song(5, "Một triệu khả năng", "Phùng Đề Mạc", R.drawable.mot_trieu_kha_nang, R.raw.mot_trieu_kha_nang, 275, 1),
            Song(6, "Phi điểu và ve sầu", "Nhậm Nhiên", R.drawable.phi_dieu_va_ve_sau, R.raw.phi_dieu_va_ve_sau, 296, 1),
            Song(7, "Quẻ bói", "Thôi Tử Cách", R.drawable.que_boi, R.raw.que_boi, 215, 1),
            Song(8, "Rượu mừng hoá người dưng", "TLong", R.drawable.ruou_mung_hoa_nguoi_dung, R.raw.ruou_mung_hoa_nguoi_dung, 262, 0),
            Song(9, "Sau tất cả", "Erik (MONSTAR ST.319)", R.drawable.sau_tat_ca, R.raw.sau_tat_ca, 235, 0),
            Song(10, "Tình yêu màu nắng", "Đoàn Thuý Hằng, BigDaddy", R.drawable.tinh_yeu_mau_nang, R.raw.tinh_yeu_mau_nang, 285, 0),
            Song(11, "Yêu là tha thu", "OnlyC", R.drawable.yeu_la_tha_thu, R.raw.yeu_la_tha_thu, 273, 0),
            Song(12, "Yêu lại từ đầu", "Khắc Việt", R.drawable.yeu_lai_tu_dau, R.raw.yeu_lai_tu_dau, 207, 0),
            Song(13, "Đếm ngày xa em", "Only C, Lou Hoàng", R.drawable.dem_ngay_xa_em, R.raw.dem_ngay_xa_em, 264, 0),
            Song(14, "Reality", "Janieck Devy", R.drawable.reality, R.raw.reality, 159, 1),
            Song(15, "Shape of You", "Ed Sheeran", R.drawable.shape_of_you, R.raw.shape_of_you, 263, 1),
            Song(16, "Tệ Thật, Anh Nhớ Em", "Orange", R.drawable.te_that_anh_nho_em, R.raw.te_that_anh_nho_em, 325, 0),
            Song(17, "Yêu em dại khờ", "Lou Hoàng", R.drawable.yeu_em_dai_kho, R.raw.yeu_em_dai_kho, 325, 0),
            Song(18, "Bông Hoa Đẹp Nhất", "Quân A.P", R.drawable.bong_hoa_dep_nhat, R.raw.bong_hoa_dep_nhat, 315, 0),
            Song(19, "Haru Haru", "BIGBANG", R.drawable.haru_haru, R.raw.haru_haru, 256, 1),
        )
    }
}