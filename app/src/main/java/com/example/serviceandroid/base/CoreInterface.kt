package com.example.serviceandroid.base

/**
 * Create By Nguyen Huu Linh 2024/08/11
 * init core view for base activity and base fragment
 */

interface CoreInterface {
    interface AndroidView {
        //Handle init all view here
        fun initView()
        //Handle event on click view here
        fun onClickView()
    }
}