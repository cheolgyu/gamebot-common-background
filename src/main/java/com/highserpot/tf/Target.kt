package com.highserpot.tf

import android.os.SystemClock
import android.util.Log
import com.highserpot.tf.env.LabelInfo
import com.highserpot.tf.tflite.Classifier

/*
        1. 클릭대상 선정은? ==> 클릭대상목록 만들기, 유효한 객체들
        인식시간 * 2 시간에 인식된것이 인식시간횟수 만큼 있다면 인식된 것으로 처리한다.
        인식시간 * 3 시간 안에 인식횟수가 인식시간횟수 만큼 있어야 인식된 것으로 처리한다.
        ==============
        2. 액션대상 선정은?
        -----------------
        2-1. 내가정한 액션대상 선정
        정규식을 통해 선정되면 번갈아 선정에서 제외한다.
        2-2. 번갈아 액션대상 선정
        내가 정한 액션대상에 선정되지 않으면 번갈아 선정한다.
        클릭대상이 선정되면 액션목록의  마지막 값에 따라 다른것을 액션으로 선정해야된다.
        2-3 한개의 화면에 같은 객체가 여러개 일떄
        한개의 화면에 같은 객체가 여러개일떄는 높이가 높은것이 우선적이다.
        ==============
        3. 강제 액션은?
        예) 분해결과 후에만 홈 클릭!
        강제액션이 활성되면 선정된 액션대상의 상태값이 존재한다면 강제액션목록에 저장한다.
        ---
        강제액션이 활성되면
        인식시간 * 3 시간의 강제액션 목록에 있다면 미리 만든 강제액션설정 변수에서
        해당 액션에 해당하는 객체id을 꺼내고 선정된 클릭대상 목록에서 객체id를 찾아서
        인식목록의 위치를 바꿔서 클릭되게 한다.
        ==============
         4. 지속적 액션은?
        인식시간이 1초 보다 적으면
        클릭대상이 선정이 되면 인식시간의 빠르기에 따라 지속적인 클릭이 되니깐
        인식시간이 1초 미만 이면서 인식시간*2의 액션목록에 같은 객체가 있으면 취소한다.
        ==============
        핸드폰에서 광고영역은?
        핸드폰에서 광고영역이 너무 크다. 애뮬과 비교해서 처리하기는 부정확하고
        대기모드와 실행모드로 만들자.
        대기모드는 배너이고 실행모드는 좀더큰 광고영역으로 하고 영역고정시키자.
    */
class Target {
    val TAG = this.javaClass.simpleName
    var FORCE_SELECT = false

    var processing_time: Long = 0
    var processing_cnt: Int = 3
    var start_time: Long = 0
    var detections: ArrayList<Classifier.Recognition> = arrayListOf()
    var history: MutableMap<Long, MutableList<Int>> = mutableMapOf()
    var valid_id: MutableList<Int> = mutableListOf()

    init {
        FORCE_SELECT = true
    }

    fun get(
        processing_time: Long,
        detections: ArrayList<Classifier.Recognition>
    ): ArrayList<Classifier.Recognition> {
        Log.d(TAG, "========================================")
        Log.d(TAG, "history=${history}")
        this.start_time = SystemClock.uptimeMillis()
        this.processing_time = processing_time
        this.detections = detections
        add_history()
        valid()

        rm_history()
        Log.d(TAG, "========================================")
        return this.detections
    }

    fun valid() {
        var search_cnt = processing_cnt
        val same_cnt: Int
        if (processing_time > 1000) {
            --search_cnt
            same_cnt = 1
        } else {
            same_cnt = processing_cnt - 1
        }

        val select_standard_time = start_time - processing_time * search_cnt
        Log.d(TAG, "${start_time} - ${processing_time * processing_cnt} = ${select_standard_time}")

        val list: MutableList<List<Int>> = mutableListOf()

        history.forEach { k, v ->
            if (k > select_standard_time) {
                list.add(v.toList())
            }
        }
        val intersection =
            list.groupBy { it }.filter { it.value.size > same_cnt }.flatMap { it.value }.distinct()


        if (intersection.isNotEmpty() && intersection[0].isNotEmpty()) {
            valid_id = intersection[0] as MutableList<Int>
        } else {
            valid_id = mutableListOf()
        }
        Log.d(TAG, "valid_id=${valid_id}")
        //https://zion830.tistory.com/127

        //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/associate-with.html
        //https://iosroid.tistory.com/87

    }

    fun rm_history() {
        val t = start_time - processing_time * (processing_cnt + 1)

        history.takeIf { history.isNotEmpty() }?.apply {
            history = filterKeys { it > t } as MutableMap<Long, MutableList<Int>>
        }
    }

    fun add_history() {
        var cur = arrayListOf<Int>()
        detections.forEach {
            cur.add(it.lb.getInt("id"))
        }
        history.put(start_time, cur)

    }

    fun select_one() {}
    fun select_one_by_regex() {}
    fun select_one_by_other() {}
    fun select_one_by_height() {}
    fun select_force() {}
    fun after_continuous() {}

    fun detections_info(): String {
        var id_string = ""
        for (i in 0 until LabelInfo.labels.length()) {
            if (detections.find { it.lb.getInt("id") == i } != null) {
                id_string += "$i,"
            } else {
                id_string += "$i-,"
            }
        }
        return id_string
    }

}