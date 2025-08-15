package com.example.sampleview.trip

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.R

class TripInfoActivity : AppCompatActivity(R.layout.activity_trip) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = findViewById<TripPointView>(R.id.pointView)
        view.setTexts(
            "上海虹桥火车站，位于中国上海市西部，是全国最繁忙的交通枢纽之一。它与虹桥机场、地铁站无缝衔接，构成立体化的现代交通网络。车站设施齐全，服务完善，设有候车区、商务候车区、餐饮区域及母婴室等配套服务设施，为旅客提供便捷与舒适的出行体验。站前广场汇集了出租车、网约车、电瓶车等多种接驳方式，是华东地区重要的铁路客运集散中心。",
            "苏州园区金鸡湖风景区，作为江南水乡城市苏州的重要地标，集自然景观、人文艺术、现代商业于一体。游客可漫步湖滨步道，欣赏湖面波光粼粼，也可前往文化艺术中心观看表演展览。园区内拥有高端购物中心、写字楼群及众多国际餐饮品牌，是集休闲、商务、文化于一体的综合性城市空间，深受本地市民与外地游客喜爱。"
        )
        view.setTextSizeSp(14f)

    }

}