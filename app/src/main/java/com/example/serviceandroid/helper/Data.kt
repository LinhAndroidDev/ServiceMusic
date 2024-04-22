package com.example.serviceandroid.helper

import com.example.serviceandroid.R
import com.example.serviceandroid.model.Song

object Data {
    fun listMusic(): ArrayList<Song> {
        return arrayListOf(
            Song(0,"Lạ Lùng", "Vũ", R.drawable.la_lung, R.raw.la_lung, 262, 0),
            Song(1, "Âm thầm bên em", "Sơn Tùng MTP", R.drawable.am_tham_ben_em, R.raw.am_tham_ben_em, 292, 0),
            Song(2, "Đi để trở về", "Soobin Hoàng Sơn", R.drawable.di_de_tro_ve, R.raw.di_de_tro_ve, 209, 0),
            Song(3, "Đồi hoa mặt trời", "Hoàng Yến Chibi", R.drawable.doi_hoa_mat_troi, R.raw.doi_hoa_mat_troi, 238, 0),
            Song(4, "Một triệu khả năng", "Phùng Đề Mạc", R.drawable.mot_trieu_kha_nang, R.raw.mot_trieu_kha_nang, 275, 1),
            Song(5, "Phi điểu và ve sầu", "Nhậm Nhiên", R.drawable.phi_dieu_va_ve_sau, R.raw.phi_dieu_va_ve_sau, 296, 1),
            Song(6, "Quẻ bói", "Thôi Tử Cách", R.drawable.que_boi, R.raw.que_boi, 215, 1),
            Song(7, "Rượu mừng hoá người dưng", "TLong", R.drawable.ruou_mung_hoa_nguoi_dung, R.raw.ruou_mung_hoa_nguoi_dung, 262, 0),
            Song(8, "Sau tất cả", "Erik (MONSTAR ST.319)", R.drawable.sau_tat_ca, R.raw.sau_tat_ca, 235, 0),
            Song(9, "Tình yêu màu nắng", "Đoàn Thuý Hằng, BigDaddy", R.drawable.tinh_yeu_mau_nang, R.raw.tinh_yeu_mau_nang, 285, 0),
            Song(10, "Yêu là tha thu", "OnlyC", R.drawable.yeu_la_tha_thu, R.raw.yeu_la_tha_thu, 273, 0),
            Song(11, "Yêu lại từ đầu", "Khắc Việt", R.drawable.yeu_lai_tu_dau, R.raw.yeu_lai_tu_dau, 207, 0),
        )
    }

//    fun listAddress(): ArrayList<Address> {
//        return arrayListOf(
//            Address(21.009126380635323, 105.82881319521661, "12 P. Chùa Bộc, Quang Trung, Đống Đa, Hà Nội 100000, Việt Nam", "Học Viện Ngân Hàng"),
//            Address(21.007326427857965, 105.84265432405154, "1 Đại Cồ Việt, Bách Khoa, Hai Bà Trưng, Hà Nội, Việt Nam"),
//            Address(21.000204359074544, 105.84250992405134, "207 Giải Phóng, Đồng Tâm, Hai Bà Trưng, Hà Nội, Việt Nam"),
//            Address(21.035522432956434, 106.27592126638233, "27PG+597, Mỹ Hương, Lương Tài, Bắc Ninh, Việt Nam"),
//            Address(21.023083054040022, 105.80543699521698, "Học viện Ngân hàng"),
//            Address(21.028511, 105.804817, "91 P. Chùa Láng, Láng Thượng, Đống Đa, Hà Nội, Việt Nam"),
//            Address(21.028511, 105.804817, "Học viện Ngân hàng"),
//            Address(21.028511, 105.804817, "Học viện Ngân hàng"),
//            Address(21.028511, 105.804817, "Học viện Ngân hàng"),
//            Address(21.028511, 105.804817, "Học viện Ngân hàng"),
//        )
//    }
}