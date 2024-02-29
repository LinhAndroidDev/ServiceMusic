package com.example.serviceandroid.helper

import com.example.serviceandroid.R
import com.example.serviceandroid.model.Song

object Data {
    fun listMusic(): ArrayList<Song> {
        return arrayListOf(
            Song("Lạ Lùng", "Vũ", R.drawable.la_lung, R.raw.la_lung, 262),
            Song("Âm thầm bên em", "Sơn Tùng MTP", R.drawable.am_tham_ben_em, R.raw.am_tham_ben_em, 230),
            Song("Đi để trở về", "Soobin Hoàng Sơn", R.drawable.di_de_tro_ve, R.raw.di_de_tro_ve, 209),
            Song("Đồi hoa mặt trời", "Hoàng Yến Chibi", R.drawable.doi_hoa_mat_troi, R.raw.doi_hoa_mat_troi, 238),
            Song("Một triệu khả năng", "Phừng Đề Mạc", R.drawable.mot_trieu_kha_nang, R.raw.mot_trieu_kha_nang, 275),
            Song("Phi điểu và ve sầu", "Nhậm Nhiên", R.drawable.phi_dieu_va_ve_sau, R.raw.phi_dieu_va_ve_sau, 296),
            Song("Quẻ bói", "Thôi Tử Cách", R.drawable.que_boi, R.raw.que_boi, 215),
            Song("Rượu mừng hoá người dưng", "TLong", R.drawable.ruou_mung_hoa_nguoi_dung, R.raw.ruou_mung_hoa_nguoi_dung, 262),
            Song("Sau tất cả", "Erik (MONSTAR ST.319)", R.drawable.sau_tat_ca, R.raw.sau_tat_ca, 235),
            Song("Tình yêu màu nắng", "Đoàn Thuý Hằng, BigDaddy", R.drawable.tinh_yeu_mau_nang, R.raw.tinh_yeu_mau_nang, 285),
            Song("Yêu là tha thu", "OnlyC", R.drawable.yeu_la_tha_thu, R.raw.yeu_la_tha_thu, 273),
            Song("Yêu lại từ đầu", "Khắc Việt", R.drawable.yeu_lai_tu_dau, R.raw.yeu_lai_tu_dau, 207),
        )
    }
}