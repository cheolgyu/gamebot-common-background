package com.highserpot.tf

import android.os.SystemClock
import android.util.Log
import com.highserpot.tf.env.LabelInfo
import com.highserpot.tf.tflite.Classifier
import java.util.*
import kotlin.collections.ArrayList

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
    var FORCED_SELECT = false

    var processing_time: Long = 0
    var processing_cnt: Int = 3
    var start_time: Long = 0
    var detections: ArrayList<Classifier.Recognition> = arrayListOf()
    var history_valid: MutableMap<Long, MutableList<Int>> = mutableMapOf()
    var history_forced: MutableMap<Long, Int> = mutableMapOf()
    var history_action: MutableMap<Long, Int> = mutableMapOf()
    var last_target: Int? = null
    var valid_id: MutableList<Int> = mutableListOf()
    var txt = ""
    val no_action = "no_action"

    init {
        FORCED_SELECT = true
    }

    fun get(
        processing_time: Long,
        detections: ArrayList<Classifier.Recognition>
    ): ArrayList<Classifier.Recognition> {
        txt = ""
        Log.d(TAG, "========================================")
        Log.d(TAG, "history=${history_valid}")
        this.start_time = SystemClock.uptimeMillis()
        this.processing_time = processing_time
        this.detections = detections
        valid_add()
        valid()
        val first_id = select_one()
        txt += "select_one = ${first_id},"
        val d_click = if (after_continuous(first_id) == null) {
            //f_id 지속적 클릭
            txt += "after_continuous = null,"
            false
        }else{
            txt += "after_continuous = y,"
            true
        }
        swap(first_id,d_click)

        valid_remove()
        add_last_target()
        Log.d(TAG, "txt=${txt}")
        return this.detections
    }

    fun swap(first_id: Int?, d_click: Boolean){
        if (first_id !=null){
            var f_index: Int? = null
            this.detections.forEachIndexed { index, it->
                run {
                    if (it.lb.getInt("id") == first_id) {
                        f_index = index
                        return@forEachIndexed
                    }
                }
            }
            if (f_index != null){
                Collections.swap(this.detections, 0, f_index!!);
                if (no_action!=this.detections.get(0).lb.getString("action") && d_click){
                    this.detections.get(0).click= d_click
                }
            }

        }
    }

    private fun add_last_target() {
        if (valid_id.isNullOrEmpty()) {
            last_target = null

        } else {
            last_target = valid_id[0]

        }
    }

    private fun select_one(): Int? {
        var first_id: Int?;
        val select_regex = select_one_by_regex()
        first_id = if (select_regex.isEmpty()) {

            val id = select_one_by_other()
            txt += "select_one_by_other=${id},"
            id
        } else {
            txt += "select_regex=${select_regex[0]},"
            select_regex[0]
        }
        val forced_first_id = select_forced()
        if (forced_first_id != null) {
            txt += "forced_first_id=${forced_first_id},"
            first_id = forced_first_id
        } else {
            txt += "forced_first_id=null,"
        }
        save_forced(first_id)
        return first_id
    }

    fun select_one_by_regex(): ArrayList<Int> {
        return chk_regex()
    }

    fun select_one_by_other(): Int? {
        var first_id: Int? = null
        if (last_target != null) {

            if (valid_id.contains(last_target!!) ) {

                if (valid_id.size > 1){
                    first_id = valid_id.find { i -> i != last_target }
                }else if (valid_id.size  == 1){
                    first_id = valid_id[0]
                }
            }

        }

        return first_id
    }

    fun select_one_by_height() {}
    fun select_forced(): Int? {
        if (FORCED_SELECT) {
            val select_standard_time = start_time - processing_time * (processing_cnt + 1)
            history_forced = history_forced.filterKeys { it > select_standard_time }
                .toSortedMap(reverseOrder())
            var first_id: Int? =
                if (history_forced.isNotEmpty()) {
                    val forced_id = LabelInfo.forced.get(history_forced.toList().get(0).second)
                    if (forced_id != null && valid_id.contains(forced_id)) {
                        forced_id
                    } else {
                        null
                    }

                } else {
                    null
                }
            return first_id
        }
        return null
    }

    fun save_forced(first_id: Int?) {
        if (FORCED_SELECT && first_id != null && LabelInfo.forced_key.contains(first_id)) {
            history_forced.put(start_time, first_id)
        }

    }

    fun get_same_cnt(): Int {
        var search_cnt = processing_cnt
        val same_cnt: Int
        if (processing_time > 1000) {
            --search_cnt
            same_cnt = 1
        } else {
            same_cnt = processing_cnt - 1
        }
        return same_cnt
    }

    fun valid() {
        val select_standard_time = start_time - processing_time * processing_cnt
        Log.d(TAG, "${start_time} - ${processing_time * processing_cnt} = ${select_standard_time}")

        val list: MutableList<List<Int>> = mutableListOf()

        history_valid.forEach { k, v ->
            if (k > select_standard_time) {
                list.add(v.toList())
            }
        }
        val intersection =
            list.groupBy { it }.filter { it.value.size > get_same_cnt() }.flatMap { it.value }
                .distinct()


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

    fun valid_remove() {
        val t = start_time - processing_time * (processing_cnt + 1)

        history_valid.takeIf { history_valid.isNotEmpty() }?.apply {
            history_valid = filterKeys { it > t } as MutableMap<Long, MutableList<Int>>
        }
    }

    fun valid_add() {
        var cur = arrayListOf<Int>()
        detections.forEach {
            cur.add(it.lb.getInt("id"))
        }
        history_valid.put(start_time, cur)

    }

    fun after_continuous(f_id: Int?): Int? {
        val select_standard_time = start_time - processing_time * get_same_cnt()
        history_action =
            history_action.filterKeys { it > select_standard_time }.toSortedMap(reverseOrder())


        if (history_action.isNotEmpty() && f_id != null && history_action.values.toList()
                .contains(f_id)
        ) {
            var cc = 0
            history_action.values.toList().forEach { if ( it == f_id ){
                cc++
            } }
            if (cc == history_action.size ){
                return f_id
            }
            return null
        } else {
            return f_id
        }

    }

    fun chk_regex(): ArrayList<Int> {
        var id_string = ""
        val select_regex_id = arrayListOf<Int>()
        if (valid_id.isNotEmpty()) {
            for (i in 0 until LabelInfo.labels.length()) {

                if (valid_id.find { it == i } != null) {
                    id_string += "$i,"
                } else {
                    id_string += "$i-,"
                }
            }

            for ((k, v) in LabelInfo.regex) {

                if (v.find(id_string) != null) {
                    val rg = valid_id.find { it == k }!!
                    select_regex_id.add(rg)

                }
            }
        }



        return select_regex_id
    }

}